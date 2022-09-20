package net.azisaba.lifepvelevel.messages;

import com.google.common.base.MoreObjects;
import net.azisaba.lifepvelevel.util.Util;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Messages {
    private static final Yaml YAML = new Yaml();
    private static final Map<String, MessageInstance> LOCALES = new ConcurrentHashMap<>();
    private static MessageInstance fallback;

    public static void load() throws IOException {
        fallback = MoreObjects.firstNonNull(load(Locale.ENGLISH.getLanguage()), MessageInstance.FALLBACK);
        for (String language : Locale.getISOLanguages()) {
            MessageInstance instance = Messages.load(language);
            if (instance != null) {
                LOCALES.put(language, instance);
            } else {
                LOCALES.put(language, fallback);
            }
        }
    }

    @Nullable
    public static MessageInstance load(@NotNull String language) throws IOException {
        try (InputStream in = Messages.class.getResourceAsStream("/messages_" + language + ".yml")) {
            if (in == null) {
                return null;
            }
            Map<Object, Object> map = YAML.load(in);
            return MessageInstance.createSimple(Util.memorize(s -> String.valueOf(map.get(s))));
        }
    }

    @NotNull
    public static MessageInstance getInstance(@Nullable String locale) {
        Objects.requireNonNull(fallback, "messages not loaded yet");
        if (locale == null) {
            return fallback;
        }
        return LOCALES.getOrDefault(locale, fallback);
    }

    @Contract("_, _ -> new")
    @NotNull
    public static String format(@NotNull String s, Object... args) {
        return ChatColor.translateAlternateColorCodes('&', String.format(Locale.ROOT, s, args));
    }

    @NotNull
    public static String getFormattedText(@NotNull CommandSender source, @NotNull String key, Object @NotNull ... args) {
        String locale = "en";
        if (source instanceof Player) {
            locale = ((Player) source).getLocale();
        }
        if (locale.contains("_")) {
            locale = locale.substring(0, locale.indexOf('_'));
        }
        String rawMessage = getInstance(locale).get(key);
        return format(rawMessage, args);
    }

    public static void sendFormatted(@NotNull CommandSender source, @NotNull String key, Object @NotNull ... args) {
        source.sendMessage(getFormattedText(source, key, args));
    }

    public static void sendActionBarFormatted(@NotNull CommandSender sender, @NotNull String key, Object @NotNull ... args) {
        String text = getFormattedText(sender, key, args);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
        } else {
            sender.sendMessage(text);
        }
    }
}
