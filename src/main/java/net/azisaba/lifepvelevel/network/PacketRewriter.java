package net.azisaba.lifepvelevel.network;

import com.google.gson.JsonParseException;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import net.azisaba.lifepvelevel.util.Util;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
import net.minecraft.network.protocol.game.PacketPlayInBlockPlace;
import net.minecraft.network.protocol.game.PacketPlayInCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.network.protocol.game.PacketPlayInUseItem;
import net.minecraft.network.protocol.game.PacketPlayInWindowClick;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutWindowItems;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PacketRewriter {
    public static List<Object> processIncomingPacket(@NotNull PacketData packetData) {
        Packet<?> packet = packetData.getPacket();
        if (packet instanceof PacketPlayInSetCreativeSlot) {
            reverseProcessItemStack(packetData.getField("b"));
        } else if (packet instanceof PacketPlayInUseEntity) {
            Object action = packetData.getField("b"); // PacketPlayInUseEntity.b : EnumEntityUseAction
            Optional<EnumHand> hand = Util.getFieldOptional(null, "a", action); // EnumEntityUseAction.a : EnumHand
            if (hand.isEmpty()) {
                if (checkItem(packetData, EnumHand.a) || checkItem(packetData, EnumHand.b)) { // a = MAIN_HAND, b = OFF_HAND
                    return Collections.emptyList();
                }
            } else {
                if (checkItem(packetData, hand.get())) return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInUseItem p) {
            if (checkItem(packetData, EnumHand.a) || checkItem(packetData, EnumHand.b)) { // a = MAIN_HAND, b = OFF_HAND
                EntityPlayer player = ((CraftPlayer) packetData.getPlayer()).getHandle();
                Bukkit.getScheduler().runTask(SpigotPlugin.getInstance(), () -> {
                    // player.playerConnection.sendPacket
                    // p.getWorldServer(), p.getHitResult().getBlockPos()
                    // p.getWorldServer(), p.getHitResult().getBlockPos().shift(p.getHitResult().getDirection())
                    player.b.a(new PacketPlayOutBlockChange(player.x(), p.c().a()));
                    player.b.a(new PacketPlayOutBlockChange(player.x(), p.c().a().a(p.c().b())));
                });
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInBlockPlace) {
            if (checkItem(packetData, EnumHand.a) || checkItem(packetData, EnumHand.b)) { // a = MAIN_HAND, b = OFF_HAND
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInArmAnimation) {
            if (checkItem(packetData, EnumHand.a) || checkItem(packetData, EnumHand.b)) { // a = MAIN_HAND, b = OFF_HAND
                return Collections.emptyList();
            }
        } else if (packet instanceof PacketPlayInWindowClick) {
            reverseProcessItemStack(packetData.getField("g"));
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
        EquipmentSlot slot = hand == EnumHand.a ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND; // a = MAIN_HAND
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
                for (ItemStack stack : packetData.<List<ItemStack>>getField("c")) {
                    processItemStack(packetData, stack);
                }
            }
        } else if (packet instanceof PacketPlayOutSetSlot) {
            if (packetData.getPlayer().getOpenInventory().getType() != InventoryType.MERCHANT) {
                processItemStack(packetData, packetData.getField("f"));
            }
        }
        return Collections.singletonList(packet);
    }

    public static void processItemStack(@NotNull PacketData data, @Nullable ItemStack item) {
        if (item == null) return;
        String text = getRequiredLevelText(data, item);
        if (text == null) return;
        boolean hadTag = item.t(); // hasTag
        NBTTagCompound tag;
        if (hadTag) {
            tag = item.v(); // getOrCreateTag
        } else {
            tag = new NBTTagCompound();
        }
        if (tag.b("LifePvELevel.HadTag", 99)) { // hasKeyOfType
            return;
        }
        if (tag.b("LifePvELevel.modifiedTag", 99)) { // hasKeyOfType
            // should not happen but just in case
            return;
        }
        AtomicReference<NBTTagCompound> displayTag = new AtomicReference<>(tag.p("display")); // getCompound
        AtomicInteger lines = new AtomicInteger();
        boolean hadDisplayTag = tag.b("display", 10); // hasKeyOfType
        boolean hadLoreTag = false;
        // false if successful; true if item does not have required level text
        final NBTTagCompound tag2 = tag;
        Consumer<NBTTagList> addLore = list -> {
            list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(IChatBaseComponent.a(" ")))); // valueOf(serialize(literal(" ")))
            IChatMutableComponent textComponent = IChatBaseComponent.b(text); // literal(text)
            ChatModifier cm = textComponent.a(); // getChatModifier
            textComponent.a(cm.b(false)); // setChatModifier(cm.setItalic(false))
            list.add(NBTTagString.a(IChatBaseComponent.ChatSerializer.a(textComponent))); // valueOf(serialize(textComponent))
            lines.addAndGet(2);
            if (displayTag.get() != null) {
                displayTag.get().a("Lore", list); // set
            }
            tag2.a("display", displayTag.get()); // set
        };
        if (displayTag.get() != null) {
            if (displayTag.get().b("Lore", 8) || displayTag.get().b("Lore", 9)) { // hasKeyOfType, hasKeyOfType
                hadLoreTag = true;
                if (displayTag.get().b("Lore", 8)) { // hasKeyOfType
                    try {
                        IChatMutableComponent component = IChatBaseComponent.ChatSerializer.a(displayTag.get().l("Lore")); // serialize(getString("Lore")
                        if (component != null) {
                            component.b(IChatBaseComponent.b(" ")); // addSibling(literal(" "))
                            IChatMutableComponent textComponent = IChatBaseComponent.b(text); // literal(text)
                            ChatModifier cm = textComponent.a(); // getChatModifier
                            textComponent.a(cm.b(false)); // setChatModifier(cm.setItalic(false))
                            component.b(textComponent); // addSibling(textComponent)
                            lines.addAndGet(2);
                            displayTag.get().a("Lore", IChatBaseComponent.ChatSerializer.a(component)); // set(serialize(component))
                            tag.a("display", displayTag.get()); // set
                        }
                    } catch (JsonParseException ignored) {
                    }
                } else {
                    NBTTagList list = displayTag.get().c("Lore", 8); // getList
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
            tag.a("display", displayTag.get()); // set
        }
        if (lines.get() >= 1) {
            tag.a("LifePvELevel.modifiedTag", lines.get()); // setInt
        } else {
            return;
        }
        tag.a("LifePvELevel.HadDisplayTag", hadDisplayTag); // setBoolean
        tag.a("LifePvELevel.HadLoreTag", hadLoreTag); // setBoolean
        tag.a("LifePvELevel.HadTag", hadTag); // setBoolean
        item.c(tag); // setTag
    }

    private static @Nullable String getRequiredLevelText(@NotNull PacketData data, @NotNull ItemStack item) {
        NBTTagCompound tag = item.u(); // getTag
        if (tag == null) {
            return null;
        }
        String key = tag.b("MYTHIC_TYPE", 8) ? tag.l("MYTHIC_TYPE") : null; // hasKeyOfType ? getString : null
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
        if (!item.t()) return; // hasTag
        NBTTagCompound tag = item.v(); // getOrCreateTag
        if (!tag.b("LifePvELevel.HadTag", 99)) { // hasKeyOfType
            return;
        }
        if (!tag.b("LifePvELevel.modifiedTag", 99)) { // hasKeyOfType
            return;
        }
        boolean hadTag = tag.b("LifePvELevel.HadTag"); // getBoolean
        if (!hadTag) {
            item.c((NBTTagCompound) null); // setTag
            return;
        }
        Runnable removeTags = () -> {
            tag.r("LifePvELevel.HadTag"); // remove
            tag.r("LifePvELevel.HadDisplayTag"); // remove
            tag.r("LifePvELevel.HadLoreTag"); // remove
            tag.r("LifePvELevel.modifiedTag"); // remove
            item.c(tag); // setTag
        };
        boolean hadDisplayTag = tag.q("LifePvELevel.HadDisplayTag"); // getBoolean
        if (!hadDisplayTag) {
            tag.r("display"); // remove
            removeTags.run();
            return;
        }
        boolean hadLoreTag = tag.q("LifePvELevel.HadLoreTag"); // getBoolean
        int count = tag.h("LifePvELevel.modifiedTag"); // getInt
        NBTTagCompound displayTag = tag.p("display"); // getCompound
        if (displayTag != null && !hadLoreTag) {
            displayTag.r("Lore"); // remove
            tag.a("display", displayTag); // set
            removeTags.run();
            return;
        }
        if (displayTag != null && (displayTag.b("Lore", 8) || displayTag.b("Lore", 9))) { // hasKeyOfType, hasKeyOfType
            if (displayTag.b("Lore", 8)) { // hasKeyOfType
                try {
                    IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(displayTag.l("Lore")); // getString
                    if (component != null) {
                        for (int i = 0; i < count; i++) {
                            component.c().remove(component.c().size() - 1); // getSiblings().remove(component.getSiblings().size() - 1)
                        }
                        displayTag.a("Lore", IChatBaseComponent.ChatSerializer.a(component)); // setString
                        tag.a("display", displayTag); // set
                    }
                } catch (JsonParseException ignored) {
                }
            } else {
                NBTTagList list = displayTag.c("Lore", 8); // getList
                for (int i = 0; i < count; i++) {
                    list.remove(list.size() - 1);
                }
                displayTag.a("Lore", list); // set
                tag.a("display", displayTag); // set
            }
        }
        removeTags.run();
    }
}
