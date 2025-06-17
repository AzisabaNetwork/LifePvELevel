package net.azisaba.lifepvelevel.commands;

import com.google.common.io.Files;
import net.azisaba.lifepvelevel.PvELevelMigration;
import net.azisaba.lifepvelevel.SpigotPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ResetPvELevelCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        try {
            List<String> lines = Files.readLines(new File("plugins/LifePvELevel/reset-allowed.txt"), StandardCharsets.UTF_8);
            int count = 0;
            for (String uuid : lines) {
                try {
                    PvELevelMigration.migratePlayer(UUID.fromString(uuid));
                    count++;
                } catch (Exception e) {
                    SpigotPlugin.getInstance().getLogger().warning("Error migrating player " + uuid);
                    e.printStackTrace();
                }
            }
            player.sendMessage(ChatColor.GREEN + "PvEレベルを調整しました。 (" + count + "/" + lines.size() + ")");
        } catch (Exception e) {
            SpigotPlugin.getInstance().getLogger().warning("Error reading reset-allowed.txt: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
