package net.azisaba.lifepvelevel.listener;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.PacketUtil;
import net.azisaba.lifepvelevel.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        try {
            // ensure player data is loaded
            DBConnector.updatePlayerSync(e.getUniqueId(), e.getName());
        } catch (Exception ex) {
            e.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            e.setKickMessage(ChatColor.YELLOW + "[LifePvELevel] " + ChatColor.RED + "Failed to load player data. Please try again later.");
            SpigotPlugin.getInstance().getLogger().severe("Failed to load player data of " + e.getUniqueId() + " (" + e.getName() + ")");
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        PacketUtil.inject(e.getPlayer());
        // TODO: better (and safe) way to remove illegal armor
        Bukkit.getScheduler().runTaskLater(SpigotPlugin.getInstance(), () -> checkIllegalArmor(e.getPlayer()), 20 * 5);
    }

    public static void checkIllegalArmor(@NotNull Player player) {
        if (!player.isOnline()) return;
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestPlate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        boolean illegal =
                !Util.canUseItem(player, helmet)
                        || !Util.canUseItem(player, chestPlate)
                        || !Util.canUseItem(player, leggings)
                        || !Util.canUseItem(player, boots);
        if (illegal) {
            if (helmet != null && player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(helmet);
                helmet = null;
            }
            if (chestPlate != null && player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(chestPlate);
                chestPlate = null;
            }
            if (leggings != null && player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(leggings);
                leggings = null;
            }
            if (boots != null && player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(boots);
                boots = null;
            }
            player.getInventory().setHelmet(boots);
            player.getInventory().setChestplate(leggings);
            player.getInventory().setLeggings(chestPlate);
            player.getInventory().setBoots(helmet);
            player.updateInventory();
            if (helmet != null || chestPlate != null || leggings != null || boots != null) {
                player.sendMessage("" + ChatColor.GOLD + ChatColor.STRIKETHROUGH + "========================================");
                player.sendMessage("");
                Messages.sendFormatted(player, "item.illegal_armor");
                player.sendMessage("");
                player.sendMessage("" + ChatColor.GOLD + ChatColor.STRIKETHROUGH + "========================================");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        PacketUtil.eject(e.getPlayer());
    }

    /*
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() == null) {
            return;
        }
        String hand = e.getHand() == EquipmentSlot.HAND ? "_main_hand" : "_off_hand";
        ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item" + hand, Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        checkInteract(e);
    }

    private void checkInteract(PlayerInteractEntityEvent e) {
        String hand = e.getHand() == EquipmentSlot.HAND ? "_main_hand" : "_off_hand";
        ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item" + hand, Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        checkInteract(e);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return;
        }
        Player damager = (Player) e.getDamager();
        ItemStack mainHand = damager.getInventory().getItemInMainHand();
        if (!Util.canUseItem(damager, mainHand)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(damager, "item.cannot_use_item_main_hand", Util.getRequiredLevel(mainHand));
        }
        ItemStack offHand = damager.getInventory().getItemInOffHand();
        if (!Util.canUseItem(damager, offHand)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(damager, "item.cannot_use_item_off_hand", Util.getRequiredLevel(offHand));
        }
    }
    */

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        ItemStack stack = e.getItem();
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item", Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerItemConsume(PlayerShearEntityEvent e) {
        ItemStack stack = e.getItem();
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item", Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerArmorChange(InventoryClickEvent e) {
        ItemStack itemToCheck = null;
        if (!Util.isEmpty(e.getCursor())
                && e.getSlotType() == InventoryType.SlotType.ARMOR
                && (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT)) {
            // player is attempting to equip an item (cursor)
            itemToCheck = e.getCursor();
        }
        if (Util.isEmpty(e.getCursor())
                && e.getSlotType() != InventoryType.SlotType.ARMOR
                && e.getClickedInventory() != null
                && e.getClickedInventory().getType() == InventoryType.PLAYER
                && e.getInventory().getType() == InventoryType.CRAFTING
                && !Util.isEmpty(e.getCurrentItem())
                && (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
            if (Util.isEmpty(e.getWhoClicked().getInventory().getHelmet())) {
                if (e.getCurrentItem().getType().name().endsWith("_HELMET")
                        || e.getCurrentItem().getType() == Material.CARVED_PUMPKIN
                        || e.getCurrentItem().getType() == Material.PLAYER_HEAD
                        || e.getCurrentItem().getType() == Material.SKELETON_SKULL
                        || e.getCurrentItem().getType() == Material.WITHER_SKELETON_SKULL
                        || e.getCurrentItem().getType() == Material.CREEPER_HEAD
                        || e.getCurrentItem().getType() == Material.DRAGON_HEAD
                        || e.getCurrentItem().getType() == Material.ZOMBIE_HEAD
                        || e.getCurrentItem().getType().name().endsWith("_BANNER")) {
                    // player is attempting to shift-click an item into the helmet slot
                    itemToCheck = e.getCurrentItem();
                }
            }
            if (Util.isEmpty(e.getWhoClicked().getInventory().getChestplate())) {
                if (e.getCurrentItem().getType().name().endsWith("_CHESTPLATE")) {
                    // player is attempting to shift-click an item into the chestplate slot
                    itemToCheck = e.getCurrentItem();
                }
            }
            if (Util.isEmpty(e.getWhoClicked().getInventory().getLeggings())) {
                if (e.getCurrentItem().getType().name().endsWith("_LEGGINGS")) {
                    // player is attempting to shift-click an item into the leggings slot
                    itemToCheck = e.getCurrentItem();
                }
            }
            if (Util.isEmpty(e.getWhoClicked().getInventory().getBoots())) {
                if (e.getCurrentItem().getType().name().endsWith("_BOOTS")) {
                    // player is attempting to shift-click an item into the boots slot
                    itemToCheck = e.getCurrentItem();
                }
            }
        }
        if (Util.isEmpty(e.getCursor())
                && e.getSlotType() == InventoryType.SlotType.ARMOR
                && e.getClick() == ClickType.NUMBER_KEY) {
            // player is attempting to equip an item using number key
            itemToCheck = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
        }
        if (itemToCheck == null) {
            return;
        }
        if (!Util.canUseItem((Player) e.getWhoClicked(), itemToCheck)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getWhoClicked(), "item.cannot_use_item", Util.getRequiredLevel(itemToCheck));
        }
    }

    @EventHandler
    public void onBlockDispenseArmor(BlockDispenseArmorEvent e) {
        if (!(e.getTargetEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getTargetEntity();
        if (!Util.canUseItem(player, e.getItem())) {
            e.setCancelled(true);
        }
    }
}
