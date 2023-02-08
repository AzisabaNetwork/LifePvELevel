package net.azisaba.lifepvelevel;

import net.azisaba.lifepvelevel.commands.BypassPvELevelCommand;
import net.azisaba.lifepvelevel.commands.PvELevelCommand;
import net.azisaba.lifepvelevel.commands.PvELevelItemCommand;
import net.azisaba.lifepvelevel.commands.ResetPvELevelCommand;
import net.azisaba.lifepvelevel.listener.MythicMobDeathListener;
import net.azisaba.lifepvelevel.listener.PlayerListener;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.sql.DatabaseConfig;
import net.azisaba.lifepvelevel.util.BypassList;
import net.azisaba.lifepvelevel.util.PacketUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public final class SpigotPlugin extends JavaPlugin {
    private static SpigotPlugin instance;
    private DatabaseConfig databaseConfig;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Loading messages");
        try {
            Messages.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getLogger().info("Loading config");
        saveDefaultConfig();
        reloadConfig();
        databaseConfig = new DatabaseConfig(Objects.requireNonNull(getConfig().getConfigurationSection("database"), "database"));

        // load database
        try {
            DBConnector.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // register listener
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
        Bukkit.getPluginManager().registerEvents(new MythicMobDeathListener(), this);

        // register commands
        Objects.requireNonNull(Bukkit.getPluginCommand("pvelevel")).setExecutor(new PvELevelCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("pvelevelitem")).setExecutor(new PvELevelItemCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("resetpvelevel")).setExecutor(new ResetPvELevelCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("bypasspvelevel")).setExecutor(new BypassPvELevelCommand());

        // update items data (blocking)
        DBConnector.updateSync();

        // schedule task
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, DBConnector::updateAsync, 20 * 60, 20 * 60);

        // inject packet handler
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("lifepvelevel.bypass")) {
                BypassList.SET.add(p.getUniqueId());
            }
            PacketUtil.inject(p);
            DBConnector.updatePlayerSync(p.getUniqueId(), p.getName());
        });
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(PacketUtil::eject);
        DBConnector.close();
    }

    @NotNull
    public DatabaseConfig getDatabaseConfig() {
        return Objects.requireNonNull(databaseConfig);
    }

    @NotNull
    public static SpigotPlugin getInstance() {
        return Objects.requireNonNull(instance, "plugin is not enabled yet");
    }

    public static void scheduleInventoryUpdate(@NotNull Player player) {
        Bukkit.getScheduler().runTaskLater(SpigotPlugin.getInstance(), () -> {
            if (player.isOnline()) player.updateInventory();
        }, 1);
    }
}
