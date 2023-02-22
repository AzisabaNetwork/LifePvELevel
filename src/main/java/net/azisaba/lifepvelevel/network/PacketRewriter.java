package net.azisaba.lifepvelevel.network;

import com.google.gson.JsonParseException;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import net.azisaba.lifepvelevel.util.Util;
import net.minecraft.server.v1_15_R1.ChatComponentText;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.EnumHand;
import net.minecraft.server.v1_15_R1.IChatBaseComponent;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
import net.minecraft.server.v1_15_R1.NBTTagString;
import net.minecraft.server.v1_15_R1.Packet;
import net.minecraft.server.v1_15_R1.PacketPlayInArmAnimation;
import net.minecraft.server.v1_15_R1.PacketPlayInBlockPlace;
import net.minecraft.server.v1_15_R1.PacketPlayInCloseWindow;
import net.minecraft.server.v1_15_R1.PacketPlayInSetCreativeSlot;
import net.minecraft.server.v1_15_R1.PacketPlayInUseEntity;
import net.minecraft.server.v1_15_R1.PacketPlayInUseItem;
import net.minecraft.server.v1_15_R1.PacketPlayInWindowClick;
import net.minecraft.server.v1_15_R1.PacketPlayOutBlockChange;
import net.minecraft.server.v1_15_R1.PacketPlayOutSetSlot;
import net.minecraft.server.v1_15_R1.PacketPlayOutWindowItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PacketRewriter {
    public static List<Object> processIncomingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof PacketPlayInSetCreativeSlot) {
            reverseProcessItemStack(packetData.getField("b"));
        } else if (packet instanceof PacketPlayInUseEntity) {
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
        } else if (packet instanceof PacketPlayInBlockPlace) {
            if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInArmAnimation) {
            if (checkItem(packetData, EnumHand.MAIN_HAND) || checkItem(packetData, EnumHand.OFF_HAND)) {
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInWindowClick) {
            reverseProcessItemStack(packetData.getField("item"));
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

    public static @NotNull @Unmodifiable List<Object> processOutgoingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof PacketPlayOutWindowItems) {
            if (packetData.getPlayer().getOpenInventory().getType() != InventoryType.MERCHANT) {
                for (ItemStack stack : packetData.<List<ItemStack>>getField("b")) {
                    processItemStack(packetData, stack);
                }
            }
        } else if (packet instanceof PacketPlayOutSetSlot) {
            if (packetData.getPlayer().getOpenInventory().getType() != InventoryType.MERCHANT) {
                processItemStack(packetData, packetData.getField("c"));
            }
        }
        return Collections.singletonList(packet);
    }

    public static void processItemStack(@NotNull PacketData data, @Nullable ItemStack item) {
        if (item == null) return;
        String text = getRequiredLevelText(data, item);
        if (text == null) return;
        boolean hadTag = item.hasTag();
        NBTTagCompound tag;
        if (hadTag) {
            tag = item.getOrCreateTag();
        } else {
            tag = new NBTTagCompound();
        }
        if (tag.hasKeyOfType("LifePvELevel.HadTag", 99)) {
            return;
        }
        if (tag.hasKeyOfType("LifePvELevel.modifiedTag", 99)) {
            // should not happen but just in case
            return;
        }
        AtomicReference<NBTTagCompound> displayTag = new AtomicReference<>(tag.getCompound("display"));
        AtomicInteger lines = new AtomicInteger();
        boolean hadDisplayTag = tag.hasKeyOfType("display", 10);
        boolean hadLoreTag = false;
        // false if successful; true if item does not have required level text
        final NBTTagCompound tag2 = tag;
        Consumer<NBTTagList> addLore = list -> {
            list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(new ChatComponentText(" "))));
            list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(new ChatComponentText(text).a(cm -> cm.setItalic(false)))));
            lines.addAndGet(2);
            if (displayTag.get() != null) {
                displayTag.get().set("Lore", list);
            }
            tag2.set("display", displayTag.get());
        };
        if (displayTag.get() != null) {
            if (displayTag.get().hasKeyOfType("Lore", 8) || displayTag.get().hasKeyOfType("Lore", 9)) {
                hadLoreTag = true;
                if (displayTag.get().hasKeyOfType("Lore", 8)) {
                    try {
                        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(displayTag.get().getString("Lore"));
                        if (component != null) {
                            component.addSibling(new ChatComponentText(" "));
                            component.addSibling(new ChatComponentText(text).a(cm -> cm.setItalic(false)));
                            lines.addAndGet(2);
                            displayTag.get().setString("Lore", IChatBaseComponent.ChatSerializer.a(component));
                            tag.set("display", displayTag.get());
                        }
                    } catch (JsonParseException ignored) {
                    }
                } else {
                    NBTTagList list = displayTag.get().getList("Lore", 8);
                    addLore.accept(list);
                }
            } else {
                NBTTagList list = new NBTTagList();
                addLore.accept(list);
            }
        } else {
            displayTag.set(new NBTTagCompound());
            NBTTagList list = new NBTTagList();
            addLore.accept(list);
            tag.set("display", displayTag.get());
        }
        if (lines.get() >= 1) {
            tag.setInt("LifePvELevel.modifiedTag", lines.get());
        } else {
            return;
        }
        tag.setBoolean("LifePvELevel.HadDisplayTag", hadDisplayTag);
        tag.setBoolean("LifePvELevel.HadLoreTag", hadLoreTag);
        tag.setBoolean("LifePvELevel.HadTag", hadTag);
        item.setTag(tag);
    }

    private static @Nullable String getRequiredLevelText(@NotNull PacketData data, @NotNull ItemStack item) {
        NBTTagCompound tag = item.getTag();
        if (tag == null) {
            return null;
        }
        String key = tag.hasKeyOfType("MYTHIC_TYPE", 8) ? tag.getString("MYTHIC_TYPE") : null;
        boolean keyed = false;
        if (key == null) {
            key = ":" + CraftItemStack.asBukkitCopy(item).getType().name() + ":" + Util.toSortedString(tag);
            keyed = true;
        }
        long requiredLevel = DBConnector.getRequiredLevel(key);
        if (keyed && requiredLevel == 0) {
            requiredLevel = DBConnector.getRequiredLevel(":" + CraftItemStack.asBukkitCopy(item).getType().name() + ":null");
            if (requiredLevel == 0) {
                // prevent showing level requirement for unrelated items
                return null;
            }
        }
        long playerLevel = LevelCalculator.toLevel(DBConnector.getExp(data.getPlayer().getUniqueId()));
        ChatColor color = ChatColor.RED;
        if (Util.canBypass(data.getPlayer(), item)) {
            color = ChatColor.GREEN;
        }
        if (!SpigotPlugin.getInstance().getEnforcedMythicType().contains(Util.getMythicType(item)) &&
                requiredLevel >= 0 && (SpigotPlugin.getInstance().isAlwaysBypassIfNotNegative() || playerLevel >= requiredLevel)) {
            color = ChatColor.GREEN;
        }
        return Messages.getFormattedText(data.getPlayer(), "item.lore.required_level", "" + color + requiredLevel);
    }

    public static void reverseProcessItemStack(@Nullable ItemStack item) {
        if (item == null) return;
        if (!item.hasTag()) return;
        NBTTagCompound tag = item.getOrCreateTag();
        if (!tag.hasKeyOfType("LifePvELevel.HadTag", 99)) {
            return;
        }
        if (!tag.hasKeyOfType("LifePvELevel.modifiedTag", 99)) {
            return;
        }
        boolean hadTag = tag.getBoolean("LifePvELevel.HadTag");
        if (!hadTag) {
            item.setTag(null);
            return;
        }
        Runnable removeTags = () -> {
            tag.remove("LifePvELevel.HadTag");
            tag.remove("LifePvELevel.HadDisplayTag");
            tag.remove("LifePvELevel.HadLoreTag");
            tag.remove("LifePvELevel.modifiedTag");
            item.setTag(tag);
        };
        boolean hadDisplayTag = tag.getBoolean("LifePvELevel.HadDisplayTag");
        if (!hadDisplayTag) {
            tag.remove("display");
            removeTags.run();
            return;
        }
        boolean hadLoreTag = tag.getBoolean("LifePvELevel.HadLoreTag");
        int count = tag.getInt("LifePvELevel.modifiedTag");
        NBTTagCompound displayTag = tag.getCompound("display");
        if (displayTag != null && !hadLoreTag) {
            displayTag.remove("Lore");
            tag.set("display", displayTag);
            removeTags.run();
            return;
        }
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
                displayTag.set("Lore", list);
                tag.set("display", displayTag);
            }
        }
        removeTags.run();
    }
}
