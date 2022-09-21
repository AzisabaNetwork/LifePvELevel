package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.PvELevelMigration;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import net.azisaba.lifepvelevel.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PvELevelCommand implements TabExecutor {
    private static final String PERMISSION_NODE = "lifepvelevel.command.pvelevel.admin";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || !sender.hasPermission(PERMISSION_NODE)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "/pvelevel <setLevel|setExp> ...");
                return true;
            }
            Player player = (Player) sender;
            long exp = DBConnector.getExp(player.getUniqueId());
            long level = LevelCalculator.toLevel(exp);
            long expForNextLevel = LevelCalculator.toExp(level + 1) - exp;
            Messages.sendFormatted(player, "command.pvelevel.overview", level, exp, expForNextLevel);
            return true;
        }
        if (args[0].equalsIgnoreCase("setLevel")) {
            if (args.length <= 2) {
                sender.sendMessage(ChatColor.RED + "/pvelevel setLevel <player> <level>");
                return true;
            }
            long exp = LevelCalculator.toExp(Long.parseLong(args[2]));
            Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
                Optional<UUID> uuid = DBConnector.getUniqueId(args[1]);
                if (!uuid.isPresent()) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return;
                }
                DBConnector.setExp(uuid.get(), exp);
                Messages.sendFormatted(sender, "command.pvelevel.set", args[1], args[2], exp);
            });
        } else if (args[0].equalsIgnoreCase("setExp")) {
            if (args.length <= 2) {
                sender.sendMessage(ChatColor.RED + "/pvelevel setExp <player> <exp>");
                return true;
            }
            long exp = Long.parseLong(args[2]);
            Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
                Optional<UUID> uuid = DBConnector.getUniqueId(args[1]);
                if (!uuid.isPresent()) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return;
                }
                DBConnector.setExp(uuid.get(), exp);
                Messages.sendFormatted(sender, "command.pvelevel.set", args[1], LevelCalculator.toLevel(exp), exp);
            });
        } else if (args[0].equalsIgnoreCase("^migrate$")) {
            Plugin statz = Bukkit.getPluginManager().getPlugin("Statz");
            if (statz == null || !statz.isEnabled()) {
                sender.sendMessage(ChatColor.RED + "Statz is not installed.");
                return true;
            }
            Plugin mcMMO = Bukkit.getPluginManager().getPlugin("mcMMO");
            if (mcMMO == null || !mcMMO.isEnabled()) {
                sender.sendMessage(ChatColor.RED + "mcMMO is not installed.");
                return true;
            }
            PvELevelMigration.doMigration(sender);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
                Optional<UUID> uuid = DBConnector.getUniqueId(args[0]);
                if (!uuid.isPresent()) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                    return;
                }
                long exp = DBConnector.getExpUncached(uuid.get());
                long level = LevelCalculator.toLevel(exp);
                long expForNextLevel = LevelCalculator.toExp(level + 1) - exp;
                Messages.sendFormatted(sender, "command.pvelevel.overview_other", args[0], level, exp, expForNextLevel);
            });
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_NODE)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Util.suggest(Arrays.asList("setLevel", "setExp"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equals("setLevel") || args[0].equals("setExp")) {
                return Util.suggest(Bukkit.getOnlinePlayers().stream().map(Player::getName), args[1]);
            }
        }
        return Collections.emptyList();
    }
}
