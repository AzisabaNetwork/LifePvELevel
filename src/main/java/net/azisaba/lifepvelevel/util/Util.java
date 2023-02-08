package net.azisaba.lifepvelevel.util;

import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.minecraft.server.v1_15_R1.NBTBase;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagString;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        return ((CraftPlayer) player)
                .getHandle()
                .playerConnection
                .networkManager
                .channel;
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
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType().isAir()) return null;
        net.minecraft.server.v1_15_R1.ItemStack nms = CraftItemStack.asNMSCopy(stack);
        if (!nms.hasTag()) return null;
        NBTTagCompound tag = nms.getOrCreateTag();
        if (!tag.hasKeyOfType("MYTHIC_TYPE", 8)) return null;
        return tag.getString("MYTHIC_TYPE");
    }

    @Nullable
    private static String getMMIDOrTagInMainHand(@NotNull Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType().isAir()) return null;
        net.minecraft.server.v1_15_R1.ItemStack nms = CraftItemStack.asNMSCopy(stack);
        if (!nms.hasTag()) return null;
        NBTTagCompound tag = nms.getOrCreateTag();
        if (tag.hasKeyOfType("MYTHIC_TYPE", 8)) return tag.getString("MYTHIC_TYPE");
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
        net.minecraft.server.v1_15_R1.ItemStack nms = CraftItemStack.asNMSCopy(stack);
        if (!nms.hasTag()) {
            String key = ":" + stack.getType().name() + ":null";
            return DBConnector.getRequiredLevel(key);
        }
        NBTTagCompound tag = nms.getOrCreateTag();
        if (!tag.hasKeyOfType("MYTHIC_TYPE", 8)) {
            String key = ":" + stack.getType().name() + ":" + toSortedString(tag);
            return DBConnector.getRequiredLevel(key);
        }
        String mmid = tag.getString("MYTHIC_TYPE");
        return DBConnector.getRequiredLevel(mmid);
    }

    @Contract("_, null -> true")
    public static boolean canUseItem(@NotNull Player player, @Nullable ItemStack stack) {
        if (stack == null) return true;
        if (player.hasPermission("lifepvelevel.bypass_level") || BypassList.SET.contains(player.getUniqueId())) return true;
        long requiredLevel = getRequiredLevel(stack);
        if (requiredLevel == 0) return true;
        if (requiredLevel < 0) return false;
        long playerLevel = LevelCalculator.toLevel(DBConnector.getExp(player.getUniqueId()));
        return playerLevel >= requiredLevel;
    }

    public static @NotNull String toSortedString(@NotNull NBTTagCompound tag) {
        Map<String, NBTBase> map = getField(NBTTagCompound.class, "map", tag);
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
        return TAG_PATTERN.matcher(s).matches() ? s : NBTTagString.b(s);
    }

    @Contract("null -> true")
    public static boolean isEmpty(@Nullable ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    public static double randomBetween(double min, double max) {
        return min + Math.random() * (max - min);
    }
}
