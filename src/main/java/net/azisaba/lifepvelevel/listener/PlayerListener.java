package net.azisaba.lifepvelevel.listener;

import net.azisaba.itemstash.ItemStash;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.PacketUtil;
import net.azisaba.lifepvelevel.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
        boolean wasIllegalItemFound = false;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.getType() != org.bukkit.Material.AIR) {
            if (!Util.canUseItem(player, helmet)) {
                wasIllegalItemFound = true;
                player.getInventory().setHelmet(null);
                if (!player.getInventory().addItem(helmet).isEmpty()) {
                    ItemStash.getInstance().addItemToStash(player.getUniqueId(), helmet);
                }
            }
        }

        ItemStack chestPlate = player.getInventory().getChestplate();
        if (chestPlate != null && chestPlate.getType() != org.bukkit.Material.AIR) {
            if (!Util.canUseItem(player, chestPlate)) {
                wasIllegalItemFound = true;
                player.getInventory().setChestplate(null);
                if (!player.getInventory().addItem(chestPlate).isEmpty()) {
                    ItemStash.getInstance().addItemToStash(player.getUniqueId(), chestPlate);
                }
            }
        }

        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && leggings.getType() != org.bukkit.Material.AIR) {
            if (!Util.canUseItem(player, leggings)) {
                wasIllegalItemFound = true;
                player.getInventory().setLeggings(null);
                if (!player.getInventory().addItem(leggings).isEmpty()) {
                    ItemStash.getInstance().addItemToStash(player.getUniqueId(), leggings);
                }
            }
        }

        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
            if (!Util.canUseItem(player, boots)) {
                wasIllegalItemFound = true;
                player.getInventory().setBoots(null);
                if (!player.getInventory().addItem(boots).isEmpty()) {
                    ItemStash.getInstance().addItemToStash(player.getUniqueId(), boots);
                }
            }
        }

        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem != null && offHandItem.getType() != org.bukkit.Material.AIR) {
            if (!Util.canUseItem(player, offHandItem)) {
                wasIllegalItemFound = true;
                player.getInventory().setItemInOffHand(null);
                if (!player.getInventory().addItem(offHandItem).isEmpty()) {
                    ItemStash.getInstance().addItemToStash(player.getUniqueId(), offHandItem);
                }
            }
        }

        if (wasIllegalItemFound) {
            player.updateInventory();
            player.sendMessage("" + ChatColor.GOLD + ChatColor.STRIKETHROUGH + "========================================");
            player.sendMessage("");
            Messages.sendFormatted(player, "item.illegal_armor");
            player.sendMessage("");
            player.sendMessage("" + ChatColor.GOLD + ChatColor.STRIKETHROUGH + "========================================");
        } else {
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        PacketUtil.eject(e.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() == InventoryType.MERCHANT) {
            // remove lore when trading
            ((Player) e.getPlayer()).updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        ItemStack stack = e.getItem();
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item", Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerShearEntity(PlayerShearEntityEvent e) {
        ItemStack stack = e.getItem();
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item", Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent e) {
        boolean check = false;
        for (int slot : e.getRawSlots()) {
            if (slot == 5 || slot == 6 || slot == 7 || slot == 8 || slot == 45) { // armor slots and offhand
                check = true;
                break;
            }
        }
        if (!check) {
            return;
        }
        ItemStack stack = e.getOldCursor();
        if (!Util.canUseItem((Player) e.getWhoClicked(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getWhoClicked(), "item.cannot_use_item", Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
        ItemStack stack = e.getOffHandItem();
        if (!Util.canUseItem(e.getPlayer(), stack)) {
            e.setCancelled(true);
            Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item_main_hand", Util.getRequiredLevel(stack));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack itemToCheckLevelRequirement = null;
        ItemStack itemToCheck = null;
        if (e.getInventory().getType() == InventoryType.GRINDSTONE
                && e.getClickedInventory() != null) {
            if (!Util.isEmpty(e.getCursor()) && e.getClickedInventory().getType() == InventoryType.GRINDSTONE) {
                itemToCheckLevelRequirement = e.getCursor();
            } else if (!Util.isEmpty(e.getCurrentItem()) && e.getClickedInventory().getType() != InventoryType.GRINDSTONE) {
                itemToCheckLevelRequirement = e.getCurrentItem();
            } else if (e.getClick() == ClickType.NUMBER_KEY) {
                itemToCheckLevelRequirement = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
            }
        }
        if (!Util.isEmpty(e.getCursor())
                && e.getRawSlot() == 45
                && (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT)) {
            // player is attempting to equip an item on the offhand
            itemToCheck = e.getCursor();
        }
        if (Util.isEmpty(e.getCursor())
                && !Util.isEmpty(e.getCurrentItem())
                && e.getClick() == ClickType.UNKNOWN) { // The "swap item with offhand" key (defaults to F).
            itemToCheck = e.getCurrentItem();
        }
        if (e.getRawSlot() == 45 && e.getClick() == ClickType.NUMBER_KEY) {
            itemToCheck = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
        }
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
                // player is attempting to shift-click an item into the helmet slot
                itemToCheck = e.getCurrentItem();
            }
            if (Util.isEmpty(e.getWhoClicked().getInventory().getChestplate())) {
                // player is attempting to shift-click an item into the chestplate slot
                itemToCheck = e.getCurrentItem();
            }
            if (Util.isEmpty(e.getWhoClicked().getInventory().getLeggings())) {
                // player is attempting to shift-click an item into the leggings slot
                itemToCheck = e.getCurrentItem();
            }
            if (Util.isEmpty(e.getWhoClicked().getInventory().getBoots())) {
                // player is attempting to shift-click an item into the boots slot
                itemToCheck = e.getCurrentItem();
            }
        }
        if (Util.isEmpty(e.getCursor())
                && e.getSlotType() == InventoryType.SlotType.ARMOR
                && e.getClick() == ClickType.NUMBER_KEY) {
            // player is attempting to equip an item using number key
            itemToCheck = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());
        }
        if (itemToCheckLevelRequirement != null
                && !Util.canBypass(e.getWhoClicked(), itemToCheckLevelRequirement)
                && Util.getRequiredLevel(itemToCheckLevelRequirement) != 0) {
            e.setCancelled(true);
            return;
        }
        if (itemToCheck != null && !Util.canUseItem((Player) e.getWhoClicked(), itemToCheck)) {
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

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        for (ItemStack stack : e.getInventory()) {
            if (stack == null) {
                continue;
            }
            if (Util.canBypass(e.getView().getPlayer(), stack)) {
                continue;
            }
            if (Util.getRequiredLevel(stack) != 0) {
                e.getInventory().setRepairCost(1000000000);
                e.setResult(null);
                ((Player) e.getView().getPlayer()).updateInventory();
                return;
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().startsWith("/hat") ||
                e.getMessage().startsWith("/ehat") ||
                e.getMessage().startsWith("/head") ||
                e.getMessage().startsWith("/ehead") ||
                e.getMessage().startsWith("/essentials:hat") ||
                e.getMessage().startsWith("/essentials:ehat") ||
                e.getMessage().startsWith("/essentials:head") ||
                e.getMessage().startsWith("/essentials:ehead")) {
            ItemStack mainHand = e.getPlayer().getInventory().getItemInMainHand();
            if (!Util.canUseItem(e.getPlayer(), mainHand)) {
                e.setCancelled(true);
                Messages.sendActionBarFormatted(e.getPlayer(), "item.cannot_use_item_main_hand", Util.getRequiredLevel(mainHand));
            }
        }
    }
}
