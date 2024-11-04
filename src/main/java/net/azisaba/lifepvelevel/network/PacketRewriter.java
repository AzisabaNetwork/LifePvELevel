package net.azisaba.lifepvelevel.network;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.util.Util;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

// TODO: some plugin may interfere with this (e.g. ProtocolLib and other plugins depending on it)
public class PacketRewriter {
    public static List<Object> processIncomingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof ServerboundInteractPacket p) {
            if (checkItem(packetData, InteractionHand.MAIN_HAND) || checkItem(packetData, InteractionHand.OFF_HAND)) {
                return Collections.emptyList();
            }
        } else if (packet instanceof ServerboundUseItemOnPacket p) {
            if (checkItem(packetData, InteractionHand.MAIN_HAND) || checkItem(packetData, InteractionHand.OFF_HAND)) {
                ServerPlayer player = ((CraftPlayer) packetData.getPlayer()).getHandle();
                Bukkit.getScheduler().runTask(SpigotPlugin.getInstance(), () -> {
                    player.connection.sendPacket(new ClientboundBlockUpdatePacket(player.level(), p.getHitResult().getBlockPos()));
                    player.connection.sendPacket(new ClientboundBlockUpdatePacket(player.level(), p.getHitResult().getBlockPos().relative(p.getHitResult().getDirection())));
                });
                return Collections.emptyList();
            }
        } else if (packet instanceof ServerboundPlayerActionPacket p) {
            if (p.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                if (checkItem(packetData, InteractionHand.MAIN_HAND) || checkItem(packetData, InteractionHand.OFF_HAND)) {
                    return Collections.emptyList();
                }
            }
        } else if (packet instanceof ServerboundUseItemPacket) {
            if (checkItem(packetData, InteractionHand.MAIN_HAND) || checkItem(packetData, InteractionHand.OFF_HAND)) {
                return Collections.emptyList();
            }
        } else if (packet instanceof ServerboundSwingPacket) {
            if (checkItem(packetData, InteractionHand.MAIN_HAND) || checkItem(packetData, InteractionHand.OFF_HAND)) {
                return Collections.emptyList();
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
    private static boolean checkItem(@NotNull PacketData packetData, InteractionHand hand) {
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        org.bukkit.inventory.ItemStack stack = packetData.getPlayer().getInventory().getItem(slot);
        if (!Util.canUseItem(packetData.getPlayer(), stack)) {
            SpigotPlugin.scheduleInventoryUpdate(packetData.getPlayer());
            Messages.sendActionBarFormatted(packetData.getPlayer(), "item.cannot_use_item_" + hand.name().toLowerCase(), Util.getRequiredLevel(stack));
            return true;
        }
        return false;
    }
}
