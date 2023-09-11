package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class PvELevelRankingCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        int page = 0;
        if (args.length > 0) {
            page = Math.max(0, Integer.parseInt(args[0]) - 1);
        }
        int offset = page * 10;
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            try {
                DBConnector.runPrepareStatement("SELECT * FROM `players` ORDER BY `exp` DESC LIMIT 10 OFFSET " + offset, stmt -> {
                    try (ResultSet rs = stmt.executeQuery()) {
                        int idx = 0;
                        while (rs.next()) {
                            String name = rs.getString("name");
                            long exp = rs.getLong("exp");
                            long level = LevelCalculator.toLevel(exp);
                            long exp2 = exp - LevelCalculator.toExp(level);
                            sender.sendMessage(ChatColor.GOLD.toString() + (++idx) + ". " + ChatColor.LIGHT_PURPLE + name + ChatColor.WHITE + ": Lv" + ChatColor.GREEN + level + ChatColor.GRAY + " (+" + exp2 + ")");
                        }
                    }
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
