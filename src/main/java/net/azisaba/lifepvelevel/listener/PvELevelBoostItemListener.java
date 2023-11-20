package net.azisaba.lifepvelevel.listener;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.sql.DBConnector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PvELevelBoostItemListener implements Listener {
    private final SpigotPlugin plugin;

    public PvELevelBoostItemListener(@NotNull SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (e.getPlayer().getGameMode() != GameMode.ADVENTURE && e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        ItemStack stack = e.getItem();
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        long durationSeconds;
        long percentage;
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        Long tempDuration = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "duration"), PersistentDataType.LONG);
        if (tempDuration != null) {
            durationSeconds = tempDuration;
        } else {
            return;
        }
        Long tempPercentage = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "percentage"), PersistentDataType.LONG);
        if (tempPercentage != null) {
            percentage = tempPercentage;
        } else {
            return;
        }
        Integer originalAmount = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "original_amount"), PersistentDataType.INTEGER);
        if (originalAmount != null && originalAmount < stack.getAmount()) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            plugin.getLogger().warning("Player " + e.getPlayer().getName() + " tried to use a duplicated item with id " + itemId);
            plugin.getLogger().warning("original amount: " + originalAmount + ", current amount: " + stack.getAmount());
            return;
        }
        if (durationSeconds == 0 || percentage == 0) {
            return;
        }
        DBConnector.refreshBoostData();
        long boostPercentage = DBConnector.getBoostedPercentage();
        if ((boostPercentage + percentage) > 101 && !e.getPlayer().hasPermission("lifepvelevel.bypass_boost_limit")) { // cap at +101% (allow 1% for debugging)
            e.getPlayer().sendMessage(ChatColor.RED + "使用するとブースト倍率が2倍を超えるため、このブーストは使用できません。");
            return;
        }

        // add boost to boostHolder
        DBConnector.addBoostAsyncByDuration(e.getPlayer().getUniqueId(), percentage, System.currentTimeMillis(), durationSeconds * 1000L);
        e.getItem().setAmount(e.getItem().getAmount() - 1);

        String sp = Long.toString(percentage);
        if (percentage >= 0) {
            sp = "+" + sp;
        }
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[PvE経験値ブースト] " + ChatColor.WHITE + ChatColor.BOLD + e.getPlayer().getName() + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "が" +
                ChatColor.WHITE + ChatColor.BOLD + sp + "%" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "ブーストを使用しました！" +
                ChatColor.GRAY + "(" + (durationSeconds / 60) + "分" + (durationSeconds % 60) + "秒間有効)");

        float boostMulti = (percentage + boostPercentage + 100) / 100.0f;

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[PvE経験値ブースト] " + ChatColor.LIGHT_PURPLE + "現在のブースト倍率は" + ChatColor.WHITE + ChatColor.BOLD + boostMulti + ChatColor.LIGHT_PURPLE + "倍です！");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, DBConnector::refreshBoostData);
    }
}
