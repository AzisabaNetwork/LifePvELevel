package net.azisaba.lifepvelevel.network;

import com.google.gson.JsonParseException;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import net.azisaba.lifepvelevel.util.Util;
import net.minecraft.server.v1_15_R1.ChatComponentText;
import net.minecraft.server.v1_15_R1.EnumHand;
import net.minecraft.server.v1_15_R1.IChatBaseComponent;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.MerchantRecipe;
import net.minecraft.server.v1_15_R1.MerchantRecipeList;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
import net.minecraft.server.v1_15_R1.NBTTagString;
import net.minecraft.server.v1_15_R1.Packet;
import net.minecraft.server.v1_15_R1.PacketPlayInArmAnimation;
import net.minecraft.server.v1_15_R1.PacketPlayInBlockPlace;
import net.minecraft.server.v1_15_R1.PacketPlayInSetCreativeSlot;
import net.minecraft.server.v1_15_R1.PacketPlayInUseEntity;
import net.minecraft.server.v1_15_R1.PacketPlayInUseItem;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_15_R1.PacketPlayOutOpenWindowMerchant;
import net.minecraft.server.v1_15_R1.PacketPlayOutSetSlot;
import net.minecraft.server.v1_15_R1.PacketPlayOutWindowItems;
import org.bukkit.ChatColor;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PacketRewriter {
    public static List<Object> processIncomingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof PacketPlayInSetCreativeSlot) {
            reverseProcessItemStack(packetData.getField("b"));
        } else if (packet instanceof PacketPlayInUseEntity) {
            PacketPlayInUseEntity p = (PacketPlayInUseEntity) packet;
            if (p.c() == null) {
                if (checkItem(packetData, EnumHand.MAIN_HAND)) return Collections.emptyList();
                if (checkItem(packetData, EnumHand.OFF_HAND)) return Collections.emptyList();
            } else {
                if (checkItem(packetData, p.c())) return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInUseItem) {
            PacketPlayInUseItem p = (PacketPlayInUseItem) packet;
            if (checkItem(packetData, p.b())) return Collections.emptyList();
        } else if (packet instanceof PacketPlayInBlockPlace) {
            PacketPlayInBlockPlace p = (PacketPlayInBlockPlace) packet;
            if (checkItem(packetData, p.b())) return Collections.emptyList();
        } else if (packet instanceof PacketPlayInArmAnimation) {
            PacketPlayInArmAnimation p = (PacketPlayInArmAnimation) packet;
            if (checkItem(packetData, p.b())) return Collections.emptyList();
        }
        return Collections.singletonList(packet);
    }

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

    public static List<Object> processOutgoingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof PacketPlayOutWindowItems) {
            for (ItemStack stack : packetData.<List<ItemStack>>getField("b")) {
                processItemStack(packetData, stack);
            }
        } else if (packet instanceof PacketPlayOutOpenWindowMerchant) {
            for (MerchantRecipe merchantRecipe : packetData.<MerchantRecipeList>getField("b")) {
                processItemStack(packetData, merchantRecipe.buyingItem1);
                processItemStack(packetData, merchantRecipe.buyingItem2);
                processItemStack(packetData, merchantRecipe.sellingItem);
            }
        } else if (packet instanceof PacketPlayOutEntityEquipment) {
            processItemStack(packetData, packetData.getField("c"));
        } else if (packet instanceof PacketPlayOutSetSlot) {
            processItemStack(packetData, packetData.getField("c"));
        }
        return Collections.singletonList(packet);
    }

    public static void processItemStack(@NotNull PacketData data, @Nullable ItemStack item) {
        if (item == null) return;
        if (!item.hasTag()) return;
        NBTTagCompound tag = item.getOrCreateTag();
        if (tag.hasKeyOfType("LifePvELevel.modifiedTag", 99)) {
            // should not happen but just in case
            return;
        }
        NBTTagCompound displayTag = tag.getCompound("display");
        int lines = 0;
        if (displayTag != null && (displayTag.hasKeyOfType("Lore", 8) || displayTag.hasKeyOfType("Lore", 9))) {
            if (displayTag.hasKeyOfType("Lore", 8)) {
                try {
                    IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(displayTag.getString("Lore"));
                    if (component != null) {
                        String mmid = tag.hasKeyOfType("MYTHIC_TYPE", 8) ? tag.getString("MYTHIC_TYPE") : null;
                        if (mmid != null) {
                            long requiredLevel = DBConnector.getRequiredLevel(mmid);
                            String text = getRequiredLevelText(data, requiredLevel);
                            component.addSibling(new ChatComponentText(" "));
                            component.addSibling(new ChatComponentText(text).a(cm -> cm.setItalic(false)));
                            component.addSibling(new ChatComponentText(" "));
                            lines += 3;
                        }
                        displayTag.setString("Lore", IChatBaseComponent.ChatSerializer.a(component));
                        tag.set("display", displayTag);
                    }
                } catch (JsonParseException ignored) {
                }
            } else {
                NBTTagList list = displayTag.getList("Lore", 8);
                String mmid = tag.hasKeyOfType("MYTHIC_TYPE", 8) ? tag.getString("MYTHIC_TYPE") : null;
                if (mmid != null) {
                    long requiredLevel = DBConnector.getRequiredLevel(mmid);
                    String text = getRequiredLevelText(data, requiredLevel);
                    list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(new ChatComponentText(" "))));
                    list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(new ChatComponentText(text).a(cm -> cm.setItalic(false)))));
                    list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(new ChatComponentText(" "))));
                    lines += 3;
                }
            }
        }
        if (lines >= 1) {
            tag.setInt("LifePvELevel.modifiedTag", lines);
        }
        item.setTag(tag);
    }

    private static @NotNull String getRequiredLevelText(@NotNull PacketData data, long requiredLevel) {
        long playerLevel = LevelCalculator.toLevel(DBConnector.getExp(data.getPlayer().getUniqueId()));
        ChatColor color = ChatColor.RED;
        if (data.getPlayer().hasPermission("lifepvelevel.bypass_level")
                || (requiredLevel >= 0 && playerLevel >= requiredLevel)) {
            color = ChatColor.GREEN;
        }
        return Messages.getFormattedText(data.getPlayer(), "item.lore.required_level", "" + color + requiredLevel);
    }

    public static void reverseProcessItemStack(@Nullable ItemStack item) {
        if (item == null) return;
        if (!item.hasTag()) return;
        NBTTagCompound tag = item.getOrCreateTag();
        if (!tag.hasKeyOfType("LifePvELevel.modifiedTag", 99)) {
            return;
        }
        int count = tag.getInt("LifePvELevel.modifiedTag");
        NBTTagCompound displayTag = tag.getCompound("display");
        if (displayTag != null && (displayTag.hasKeyOfType("Lore", 8) || displayTag.hasKeyOfType("Lore", 9))) {
            if (displayTag.hasKeyOfType("Lore", 8)) {
                try {
                    IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(displayTag.getString("Lore"));
                    if (component != null) {
                        for (int i = 0; i < count; i++) {
                            component.getSiblings().remove(component.getSiblings().size() - 1);
                        }
                        displayTag.setString("Lore", IChatBaseComponent.ChatSerializer.a(component));
                        tag.set("display", displayTag);
                    }
                } catch (JsonParseException ignored) {
                }
            } else {
                NBTTagList list = displayTag.getList("Lore", 8);
                for (int i = 0; i < count; i++) {
                    list.remove(list.size() - 1);
                }
            }
        }
        tag.remove("LifePvELevel.modifiedTag");
        item.setTag(tag);
    }
}
