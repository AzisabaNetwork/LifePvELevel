package net.azisaba.lifepvelevel.util;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
    @NotNull
    public static Channel getChannel(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel;
    }

    @Nullable
    public static Field findField(@NotNull Class<?> clazz, @NotNull String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (ReflectiveOperationException ignore) {}
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            Field field = findField(superClass, name);
            if (field != null) return field;
        }
        for (Class<?> c : clazz.getInterfaces()) {
            Field field = findField(c, name);
            if (field != null) return field;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(@Nullable Class<?> clazz, @NotNull String name, Object instance) {
        if (clazz == null) clazz = instance.getClass();
        try {
            Field f = findField(clazz, name);
            if (f == null) throw new RuntimeException("Could not find field '" + name + "' in " + clazz.getTypeName() + " and its superclass/interfaces");
            f.setAccessible(true);
            return (T) f.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract("_, _, _, _ -> param4")
    public static <T> T setField(@Nullable Class<?> clazz, @NotNull String name, Object instance, T value) {
        if (clazz == null) clazz = instance.getClass();
        try {
            Field f = findField(clazz, name);
            if (f == null) throw new RuntimeException("Could not find field '" + name + "' in " + clazz.getTypeName() + " and its superclass/interfaces");
            f.setAccessible(true);
            f.set(instance, value);
            return value;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(value = "_ -> new", pure = true)
    @NotNull
    public static <T, R> Function<T, R> memorize(@NotNull Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T t) {
                return cache.computeIfAbsent(t, function);
            }
        };
    }

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    public static <T, R> Function<T, R> memorize(long expireAfter, @NotNull Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, Map.Entry<R, Long>> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T t) {
                long now = System.currentTimeMillis();
                Map.Entry<R, Long> entry = cache.getOrDefault(t, new AbstractMap.SimpleImmutableEntry<>(function.apply(t), 0L));
                if (entry.getValue() == 0L || (expireAfter > 0 && now - entry.getValue() > expireAfter)) {
                    if (entry.getValue() > 0) {
                        entry = new AbstractMap.SimpleImmutableEntry<>(function.apply(t), now);
                    }
                    cache.put(t, entry);
                }
                return entry.getKey();
            }
        };
    }

    @NotNull
    public static List<String> suggest(@NotNull List<String> stack, @NotNull String arg) {
        return suggest(stack.stream(), arg);
    }

    @NotNull
    public static List<String> suggest(@NotNull Stream<String> stack, @NotNull String arg) {
        return stack.filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }

    @Nullable
    private static String getMMIDInMainHand(@NotNull Player player) {
        return getMythicType(player.getInventory().getItemInMainHand());
    }

    public static @Nullable CompoundTag getCustomData(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        CustomData customData = nms.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag();
    }

    public static @NotNull CompoundTag getCustomDataOrEmpty(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return new CompoundTag();
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        CustomData customData = nms.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return new CompoundTag();
        return customData.copyTag();
    }

    public static @NotNull ItemStack setCustomData(@NotNull ItemStack item, @Nullable CompoundTag tag) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        if (tag == null) {
            nms.remove(DataComponents.CUSTOM_DATA);
        } else {
            nms.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return CraftItemStack.asBukkitCopy(nms);
    }

    @Contract("null -> null")
    public static String getMythicType(@Nullable ItemStack item) {
        CompoundTag tag = getCustomDataOrEmpty(item);
        var publicBukkitValues = tag.getCompound("PublicBukkitValues");
        if (!publicBukkitValues.contains("MYTHIC_TYPE", 8)) return null;
        return publicBukkitValues.getString("MYTHIC_TYPE");
    }

    @Contract("null -> null")
    public static String getMythicType(@Nullable net.minecraft.world.item.ItemStack nms) {
        if (nms == null) return null;
        CustomData customData = nms.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        var publicBukkitValues = tag.getCompound("PublicBukkitValues");
        if (!publicBukkitValues.contains("MYTHIC_TYPE", 8)) return null;
        return publicBukkitValues.getString("MYTHIC_TYPE");
    }

    @Nullable
    private static String getMMIDOrTagInMainHand(@NotNull Player player) {
        CompoundTag tag = getCustomData(player.getInventory().getItemInMainHand());
        if (tag == null) return null;
        var publicBukkitValues = tag.getCompound("PublicBukkitValues");
        if (publicBukkitValues.contains("mythicmobs:type", 8)) return publicBukkitValues.getString("mythicmobs:type");
        return toSortedString(tag);
    }

    @Nullable
    public static String getMMIDOrKeyInMainHand(@NotNull Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType().isAir()) return null;
        String mmid = getMMIDInMainHand(player);
        if (mmid != null) {
            return mmid;
        }
        return ":" + stack.getType().name() + ":" + getMMIDOrTagInMainHand(player);
    }

    public static long getRequiredLevel(@NotNull ItemStack stack) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(stack);
        CustomData customData = nms.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            String key = ":" + stack.getType().name() + ":null";
            return DBConnector.getRequiredLevel(key);
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.getCompound("PublicBukkitValues").contains("mythicmobs:type", 8)) {
            String key = ":" + stack.getType().name() + ":" + toSortedString(tag);
            return DBConnector.getRequiredLevel(key);
        }
        String mmid = tag.getCompound("PublicBukkitValues").getString("mythicmobs:type");
        return DBConnector.getRequiredLevel(mmid);
    }

    @Contract("_, null -> true")
    public static boolean canUseItem(@NotNull Player player, @Nullable ItemStack stack) {
        if (stack == null) return true;
        if (canBypass(player, stack)) return true;
        long requiredLevel = getRequiredLevel(stack);
        if (requiredLevel == 0) return true;
        if (requiredLevel < 0) return false;
        if (!SpigotPlugin.getInstance().getEnforcedMythicType().contains(getMythicType(stack)) &&
                SpigotPlugin.getInstance().isAlwaysBypassIfNotNegative()) return true;
        long playerLevel = LevelCalculator.toLevel(DBConnector.getExp(player.getUniqueId()));
        return playerLevel >= requiredLevel;
    }

    public static @NotNull String toSortedString(@NotNull CompoundTag tag) {
        Map<String, Tag> map = getField(CompoundTag.class, "tags", tag);
        StringBuilder sb = new StringBuilder("{");
        List<String> keys = Lists.newArrayList(map.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            if (sb.length() != 1) {
                sb.append(',');
            }
            sb.append(quote(key)).append(':').append(map.get(key));
        }

        return sb.append('}').toString();
    }

    private static final Pattern TAG_PATTERN = Pattern.compile("[A-Za-z0-9._+-]+");

    protected static String quote(@NotNull String s) {
        return TAG_PATTERN.matcher(s).matches() ? s : StringTag.quoteAndEscape(s);
    }

    @Contract("null -> true")
    public static boolean isEmpty(@Nullable ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    public static double randomBetween(double min, double max) {
        return min + Math.random() * (max - min);
    }

    public static boolean canBypass(@NotNull Permissible permissible, @Nullable ItemStack stack) {
        if (permissible.hasPermission("lifepvelevel.bypass_level")) {
            return true;
        }
        if (SpigotPlugin.getInstance().getEnforcedMythicType().contains(getMythicType(stack))) {
            return false;
        }
        return SpigotPlugin.getInstance().isAlwaysBypass();
    }

    public static boolean canBypass(@NotNull Permissible permissible, @Nullable net.minecraft.world.item.ItemStack stack) {
        if (permissible.hasPermission("lifepvelevel.bypass_level")) {
            return true;
        }
        if (SpigotPlugin.getInstance().getEnforcedMythicType().contains(getMythicType(stack))) {
            return false;
        }
        return SpigotPlugin.getInstance().isAlwaysBypass();
    }
}
