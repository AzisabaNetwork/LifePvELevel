package net.azisaba.lifepvelevel.network;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.util.Util;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PacketRewriter {
    public static List<Object> processIncomingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof PacketPlayInUseEntity) {
            PacketPlayInUseEntity p = (PacketPlayInUseEntity) packet;
            if (p.c() == null) {
                if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                    return Collections.emptyList();
                }
            } else {
                if (checkItem(packetData, p.c())) return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInUseItem) {
            PacketPlayInUseItem p = (PacketPlayInUseItem) packet;
            if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                EntityPlayer player = ((CraftPlayer) packetData.getPlayer()).getHandle();
                Bukkit.getScheduler().runTask(SpigotPlugin.getInstance(), () -> {
                    player.playerConnection.sendPacket(new PacketPlayOutBlockChange(player.world, p.c().getBlockPosition()));
                    player.playerConnection.sendPacket(new PacketPlayOutBlockChange(player.world, p.c().getBlockPosition().shift(p.c().getDirection())));
                });
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInBlockDig) {
            if (((PacketPlayInBlockDig) packet).d() == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK) {
                if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                    return Collections.emptyList();
                }
            }
        } else if (packet instanceof PacketPlayInBlockPlace) {
            if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInArmAnimation) {
            if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInCloseWindow) {
            if (packetData.getPlayer().getOpenInventory().getType() == InventoryType.MERCHANT) {
                // re-add lore after trading
                Bukkit.getScheduler().runTask(SpigotPlugin.getInstance(), () -> packetData.getPlayer().updateInventory());
            }
        }
        return Collections.singletonList(packet);
    }

    /**
     * Checks the item in the given hand and sends a packet to update the inventory if the item cannot be used.
     * @param packetData the packet data
     * @param hand the hand to check
     * @return true if the item cannot be used
     */
    private static boolean checkItem(@NotNull PacketData packetData, EnumHand hand) {
        EquipmentSlot slot = hand == EnumHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        org.bukkit.inventory.ItemStack stack = packetData.getPlayer().getInventory().getItem(slot);
        if (!Util.canUseItem(packetData.getPlayer(), stack)) {
            SpigotPlugin.scheduleInventoryUpdate(packetData.getPlayer());
            Messages.sendActionBarFormatted(packetData.getPlayer(), "item.cannot_use_item_" + hand.name().toLowerCase(), Util.getRequiredLevel(stack));
            return true;
        }
        return false;
    }
}
