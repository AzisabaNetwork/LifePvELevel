package net.azisaba.lifepvelevel;

import net.azisaba.lifepvelevel.commands.*;
import net.azisaba.lifepvelevel.listener.MythicMobDeathListener;
import net.azisaba.lifepvelevel.listener.PlayerListener;
import net.azisaba.lifepvelevel.listener.PvELevelBoostItemListener;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.sql.DatabaseConfig;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import net.azisaba.lifepvelevel.util.PacketUtil;
import net.azisaba.lifepvelevel.util.Util;
import net.azisaba.loreeditor.api.event.EventBus;
import net.azisaba.loreeditor.api.event.ItemEvent;
import net.azisaba.loreeditor.libs.net.kyori.adventure.text.Component;
import net.azisaba.loreeditor.libs.net.kyori.adventure.text.format.TextDecoration;
import net.azisaba.loreeditor.libs.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.azisaba.tabbukkitbridge.data.DataKey;
import net.azisaba.tabbukkitbridge.tab.TheTAB;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class SpigotPlugin extends JavaPlugin {
    private static final DecimalFormat FORMATTER_COMMAS = new DecimalFormat("#,###");
    private static SpigotPlugin instance;
    private DatabaseConfig databaseConfig;
    private boolean alwaysBypass = false;
    private boolean alwaysBypassIfNotNegative = false;
    private final List<String> enforcedMythicType = new ArrayList<>();

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
        alwaysBypass = getConfig().getBoolean("always-bypass", false);
        alwaysBypassIfNotNegative = getConfig().getBoolean("always-bypass-if-not-negative", false);
        enforcedMythicType.addAll(getConfig().getStringList("enforced-mythic-type"));

        // load database
        try {
            DBConnector.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // register listener
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
        Bukkit.getPluginManager().registerEvents(new MythicMobDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PvELevelBoostItemListener(this), this);

        // register commands
        Objects.requireNonNull(Bukkit.getPluginCommand("pvelevel")).setExecutor(new PvELevelCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("pvelevelitem")).setExecutor(new PvELevelItemCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("resetpvelevel")).setExecutor(new ResetPvELevelCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("pvelevelranking")).setExecutor(new PvELevelRankingCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("checkpvelevelboost")).setExecutor(new CheckPvELevelBoost(this));
        Objects.requireNonNull(Bukkit.getPluginCommand("givepvelevelboostitem")).setExecutor(new GivePvELevelBoostItemCommand(this));

        // update items data (blocking)
        DBConnector.updateSync();

        // schedule task
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, DBConnector::updateAsync, 20 * 60, 20 * 60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, DBConnector::refreshBoostData, 20 * 5, 20 * 5);

        // inject packet handler
        Bukkit.getOnlinePlayers().forEach(p -> {
            PacketUtil.inject(p);
            DBConnector.updatePlayerSync(p.getUniqueId(), p.getName());
        });

        EventBus.INSTANCE.register(this, ItemEvent.class, 0, e -> {
            String text = getRequiredLevelText(e.getPlayer(), e.getBukkitItem());
            if (text != null) {
                e.addLore(Component.space());
                e.addLore(LegacyComponentSerializer.legacySection().deserialize(text).decoration(TextDecoration.ITALIC, false));
            }
        });

        registerPlaceholders();
        TheTAB.enable();
    }

    private static @Nullable String getRequiredLevelText(@NotNull Player player, @NotNull ItemStack item) {
        NBTTagCompound tag = CraftItemStack.asNMSCopy(item).getTag();
        if (tag == null) {
            return null;
        }
        String key = tag.hasKeyOfType("MYTHIC_TYPE", 8) ? tag.getString("MYTHIC_TYPE") : null;
        boolean keyed = false;
        if (key == null) {
            key = ":" + item.getType().name() + ":" + Util.toSortedString(tag);
            keyed = true;
        }
        long requiredLevel = DBConnector.getRequiredLevel(key);
        if (keyed && requiredLevel == 0) {
            requiredLevel = DBConnector.getRequiredLevel(":" + item.getType().name() + ":null");
            if (requiredLevel == 0) {
                // prevent showing level requirement for unrelated items
                return null;
            }
        }
        long playerLevel = LevelCalculator.toLevel(DBConnector.getExp(player.getUniqueId()));
        ChatColor color = ChatColor.RED;
        if (Util.canBypass(player, item)) {
            color = ChatColor.GREEN;
        }
        if (!SpigotPlugin.getInstance().getEnforcedMythicType().contains(Util.getMythicType(item)) &&
                requiredLevel >= 0 && (SpigotPlugin.getInstance().isAlwaysBypassIfNotNegative() || playerLevel >= requiredLevel)) {
            color = ChatColor.GREEN;
        }
        return Messages.getFormattedText(player, "item.lore.required_level", "" + color + requiredLevel);
    }

    private static <T> void registerPlayerPlaceholder(@NotNull T defaultValue, @NotNull String placeholder, @NotNull Function<Player, T> valueSupplier) {
        DataKey<Player, T> dataKey = new DataKey<>(defaultValue);
        dataKey.getPlaceholders().add(placeholder);
        dataKey.register(p -> true, valueSupplier);
    }

    private static void registerPlaceholders() {
        registerPlayerPlaceholder(
                "0",
                "pvelevel_level",
                p -> FORMATTER_COMMAS.format(LevelCalculator.toLevel(DBConnector.getExp(p.getUniqueId())))
        );
        registerPlayerPlaceholder(
                "0",
                "pvelevel_exp",
                p -> FORMATTER_COMMAS.format(LevelCalculator.toLevel(DBConnector.getExp(p.getUniqueId())))
        );
        registerPlayerPlaceholder(
                "0",
                "pvelevel_exp_for_next_level",
                p -> {
                    long exp = DBConnector.getExp(p.getUniqueId());
                    return FORMATTER_COMMAS.format(LevelCalculator.toExp(LevelCalculator.toLevel(exp) + 1) - exp);
                }
        );
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

    public boolean isAlwaysBypass() {
        return alwaysBypass;
    }

    public boolean isAlwaysBypassIfNotNegative() {
        return alwaysBypassIfNotNegative;
    }

    public @NotNull List<String> getEnforcedMythicType() {
        return enforcedMythicType;
    }
}
