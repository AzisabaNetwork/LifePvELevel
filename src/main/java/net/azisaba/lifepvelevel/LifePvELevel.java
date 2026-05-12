package net.azisaba.lifepvelevel;

import net.azisaba.lifepvelevel.api.PvELevelAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class LifePvELevel {
    private LifePvELevel() {}

    public static @Nullable PvELevelAPI getAPI() {
        RegisteredServiceProvider<PvELevelAPI> registration =
                Bukkit.getServicesManager().getRegistration(PvELevelAPI.class);
        return registration == null ? null : registration.getProvider();
    }

    public static boolean isAvailable() {
        return getAPI() != null;
    }

    public static long getExp(@NotNull UUID uuid) {
        PvELevelAPI api = getAPI();
        return api == null ? 0L : api.getExp(uuid);
    }

    public static long getExp(@NotNull OfflinePlayer player) {
        PvELevelAPI api = getAPI();
        return api == null ? 0L : api.getExp(player);
    }

    public static long getLevel(@NotNull UUID uuid) {
        PvELevelAPI api = getAPI();
        return api == null ? 0L : api.getLevel(uuid);
    }

    public static long getLevel(@NotNull OfflinePlayer player) {
        PvELevelAPI api = getAPI();
        return api == null ? 0L : api.getLevel(player);
    }
}
