package net.azisaba.lifepvelevel.api;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PvELevelAPI {
    long getExp(@NotNull UUID uuid);

    long getExp(@NotNull OfflinePlayer player);

    long getLevel(@NotNull UUID uuid);

    long getLevel(@NotNull OfflinePlayer player);
}
