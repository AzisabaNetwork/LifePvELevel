package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.PvELevelMigration;
import net.azisaba.lifepvelevel.SpigotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResetPvELevelCommand implements TabExecutor {
    private static final String USED_PLAYERS_FILE = "plugins/LifePvELevel/used-reset-players.txt";
    private final Set<UUID> usedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();

    public ResetPvELevelCommand() {
        loadUsedPlayers();
    }

    private void loadUsedPlayers() {
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            File file = new File(USED_PLAYERS_FILE);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                return;
            }

            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    try {
                        usedPlayers.add(UUID.fromString(line.trim()));
                    } catch (IllegalArgumentException e) {
                        SpigotPlugin.getInstance().getLogger().warning("Invalid UUID in used players file: " + line);
                    }
                }
            } catch (IOException e) {
                SpigotPlugin.getInstance().getLogger().warning("Error loading used players file: " + e.getMessage());
            }
        });
    }

    private void saveUsedPlayer(UUID playerId) {
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            try {
                File file = new File(USED_PLAYERS_FILE);
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), (playerId.toString() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), 
                           StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                SpigotPlugin.getInstance().getLogger().warning("Error saving used player to file: " + e.getMessage());
            }
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみが実行できます。");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Check if player has already used the command
        if (usedPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.RED + "PvEレベルのリセットは一度のみ使用可能です。既に使用されています。");
            return true;
        }

        // Check if player is already being processed
        if (processingPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "PvEレベルのリセットを処理中です。しばらくお待ちください。");
            return true;
        }

        // Add to processing set to prevent duplicate requests
        processingPlayers.add(playerId);
        player.sendMessage(ChatColor.YELLOW + "PvEレベルのリセットを開始しています...");

        // Run the entire operation asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            try {
                PvELevelMigration.migratePlayer(playerId);
                usedPlayers.add(playerId);
                saveUsedPlayer(playerId);
                
                // Send success message on main thread
                Bukkit.getScheduler().runTask(SpigotPlugin.getInstance(), () -> {
                    player.sendMessage(ChatColor.GREEN + "PvEレベルがリセットされました。(現在のレベルが計算されたレベルよりも高い場合はなにも変わっていません)");
                });
            } catch (Exception e) {
                SpigotPlugin.getInstance().getLogger().warning("Error resetting PvE level for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                
                // Send error message on main thread
                Bukkit.getScheduler().runTask(SpigotPlugin.getInstance(), () -> {
                    player.sendMessage(ChatColor.RED + "PvEレベルのリセット中にエラーが発生しました。");
                });
            } finally {
                // Remove from processing set
                processingPlayers.remove(playerId);
            }
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
