package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import net.azisaba.lifepvelevel.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PvELevelItemCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                String mmid = Util.getMMIDInMainHand((Player) sender);
                if (mmid != null) {
                    long requiredLevel = DBConnector.getRequiredLevel(mmid);
                    long requiredExp = LevelCalculator.toExp(requiredLevel);
                    Messages.sendFormatted(sender, "command.pvelevelitem.overview", mmid, requiredLevel, requiredExp);
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "/pvelevelitem <set> ...");
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.RED + "/pvelevelitem set <level> [mmid]");
                return true;
            }
            String mmid;
            if (args.length == 2) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must specify mmid.");
                    return true;
                }
                mmid = Util.getMMIDInMainHand((Player) sender);
                if (mmid == null) {
                    Messages.sendFormatted(sender, "command.generic.no_mythic_item");
                    return true;
                }
            } else {
                mmid = args[2];
            }
            long level = Long.parseLong(args[1]);
            DBConnector.setRequiredLevels(mmid, level);
            Messages.sendFormatted(sender, "command.pvelevelitem.set", mmid, level, LevelCalculator.toExp(level));
        } else {
            // TODO: implement?
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Util.suggest(Arrays.asList("set"), args[0]);
        }
        return Collections.emptyList();
    }
}
