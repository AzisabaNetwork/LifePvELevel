package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.util.BypassList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BypassPvELevelCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (BypassList.SET.contains(((Player) sender).getUniqueId())) {
            BypassList.SET.remove(((Player) sender).getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "PvEレベル制限を適用するようになりました。");
        } else {
            BypassList.SET.add(((Player) sender).getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "PvEレベル制限を無視するようになりました。");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
