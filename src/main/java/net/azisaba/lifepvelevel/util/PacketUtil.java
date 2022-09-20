package net.azisaba.lifepvelevel.util;

import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.network.ChannelHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;

public class PacketUtil {
    private static final String NAME = "lifepvelevel";

    public static void inject(@NotNull Player player) {
        ChannelHandler handler = new ChannelHandler(player);
        try {
            Util.getChannel(player).pipeline().addBefore("packet_handler", NAME, handler);
            SpigotPlugin.getInstance().getLogger().info("Injected packet handler for " + player.getName());
        } catch (NoSuchElementException ex) {
            Bukkit.getScheduler().runTaskLater(SpigotPlugin.getInstance(), () -> {
                if (!player.isOnline()) return;
                try {
                    Util.getChannel(player).pipeline().addBefore("packet_handler", NAME, handler);
                    SpigotPlugin.getInstance().getLogger().info("Injected packet handler for " + player.getName());
                } catch (NoSuchElementException ignore) {
                    SpigotPlugin.getInstance().getLogger().warning("Failed to inject packet handler to " + player.getName());
                }
            }, 10);
        }
    }

    public static void eject(@NotNull Player player) {
        try {
            if (Util.getChannel(player).pipeline().get(NAME) != null) {
                Util.getChannel(player).pipeline().remove(NAME);
                SpigotPlugin.getInstance().getLogger().info("Ejected packet handler from " + player.getName());
            }
        } catch (RuntimeException e) {
            SpigotPlugin.getInstance().getLogger().info("Failed to eject packet handler from " + player.getName() + ", are they already disconnected?");
        }
    }
}
