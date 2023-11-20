package net.azisaba.lifepvelevel.commands;

import net.azisaba.itemstash.ItemStash;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.sql.DBConnector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GivePvELevelBoostItemCommand implements TabExecutor {
    private final SpigotPlugin plugin;

    public GivePvELevelBoostItemCommand(SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /givepvelevelboostitem <player> <duration in seconds> <percentage> [amount] [player]");
            return true;
        }
        Player player = Bukkit.getPlayerExact(args[0]);
        if (player == null) {
            sender.sendMessage("Player not found");
            return true;
        }
        long durationSeconds = Long.parseLong(args[1]);
        long durationMinutes = durationSeconds / 60L;
        long percentage = Long.parseLong(args[2]);
        int amount = 1;
        if (args.length >= 4) {
            amount = Integer.parseInt(args[3]);
        }
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();
        if (args.length >= 5) {
            playerName = args[4];
            playerUUID = DBConnector.getUniqueId(args[4]).orElseThrow(NoSuchElementException::new);
        }
        double multi = 1 + (percentage / 100.0);
        ItemStack stack = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        meta.setCustomModelData(57);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName("§3PvE経験値ブースト §f" + durationMinutes + "分" + (durationSeconds % 60) + "秒" + multi + "倍");
        meta.setLore(Arrays.asList(
                "§6 - 使い方 -",
                "§3空中で左クリックでPvE経験値ブーストを発動",
                "§3効果時間： §f§l" + durationMinutes + "分" + (durationSeconds % 60) + "秒",
                "§3ドロップ上昇率： §f§l" + multi + "倍 §7(+" + percentage + "%)",
                "§f",
                "§d" + playerName + " 専用",
                "§c§l【 取引禁止 】",
                "§f",
                "§7このアイテムは別で購入したブーストとスタックできません。",
                "§8[Soulbound: " + playerUUID + "]"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "duration"), PersistentDataType.LONG, durationSeconds);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "percentage"), PersistentDataType.LONG, percentage);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "player_name"), PersistentDataType.STRING, playerName);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "player_uuid"), PersistentDataType.STRING, playerUUID.toString());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "original_amount"), PersistentDataType.INTEGER, amount);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);
        for (ItemStack item : player.getInventory().addItem(stack).values()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getLogger().warning("Target player (" + player.getName() + ") inventory was full, adding item to stash: " + item);
                ItemStash.getInstance().addItemToStash(player.getUniqueId(), item);
            });
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("<player>");
        }
        if (args.length == 2) {
            return Collections.singletonList("<duration in seconds>");
        }
        if (args.length == 3) {
            return Collections.singletonList("<percentage>");
        }
        if (args.length == 4) {
            return Collections.singletonList("[amount]");
        }
        if (args.length == 5) {
            return Collections.singletonList("[player name]");
        }
        return Collections.emptyList();
    }
}
