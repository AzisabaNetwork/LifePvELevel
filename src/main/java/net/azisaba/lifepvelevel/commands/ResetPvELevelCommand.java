package net.azisaba.lifepvelevel.commands;

import net.azisaba.lifepvelevel.PvELevelMigration;
import net.azisaba.lifepvelevel.SpigotPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResetPvELevelCommand implements TabExecutor {
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_DURATION = 48 * 60 * 60 * 1000L; // 48 hours in milliseconds

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみが実行できます。");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check cooldown
        if (cooldowns.containsKey(playerId)) {
            long lastUsed = cooldowns.get(playerId);
            long timeLeft = (lastUsed + COOLDOWN_DURATION) - currentTime;
            
            if (timeLeft > 0) {
                long hoursLeft = timeLeft / (60 * 60 * 1000);
                long minutesLeft = (timeLeft % (60 * 60 * 1000)) / (60 * 1000);
                player.sendMessage(ChatColor.RED + "PvEレベルのリセットはクールダウン中です。残り時間: " + hoursLeft + "時間" + minutesLeft + "分");
                return true;
            }
        }

        try {
            PvELevelMigration.migratePlayer(playerId);
            cooldowns.put(playerId, currentTime);
            player.sendMessage(ChatColor.GREEN + "PvEレベルがリセットされました。(現在のレベルが計算されたレベルよりも高い場合はなにも変わっていません)");
        } catch (Exception e) {
            SpigotPlugin.getInstance().getLogger().warning("Error resetting PvE level for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "PvEレベルのリセット中にエラーが発生しました。");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
