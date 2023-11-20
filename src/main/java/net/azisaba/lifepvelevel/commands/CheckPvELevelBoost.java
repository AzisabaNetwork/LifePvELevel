package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.model.BoostData;
import net.azisaba.lifepvelevel.sql.DBConnector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CheckPvELevelBoost implements TabExecutor {
    private final SpigotPlugin plugin;

    public CheckPvELevelBoost(@NotNull SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage("§3現在PvE経験値ブースト倍率が§f§l" + (1 + (DBConnector.getBoostedPercentage() / 100.0)) + "倍§3になっています！");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (BoostData boostData : DBConnector.getBoostDataList()) {
                long remaining = (boostData.end() - System.currentTimeMillis()) / 1000;
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                sender.sendMessage(ChatColor.GOLD + "あと" + ChatColor.RED + minutes + "分" + seconds + "秒" + ChatColor.GOLD + ": " +
                        ChatColor.GREEN + ChatColor.BOLD + "+" + boostData.percentage() + "%" + ChatColor.GRAY + " (" + boostData.getName().orElse("?") + ")");
            }
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
