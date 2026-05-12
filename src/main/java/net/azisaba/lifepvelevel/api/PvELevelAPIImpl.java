package net.azisaba.lifepvelevel.api;

import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PvELevelAPIImpl implements PvELevelAPI {
    @Override
    public long getExp(@NotNull UUID uuid) {
        return DBConnector.getExp(uuid);
    }

    @Override
    public long getExp(@NotNull OfflinePlayer player) {
        return getExp(player.getUniqueId());
    }

    @Override
    public long getLevel(@NotNull UUID uuid) {
        return LevelCalculator.toLevel(getExp(uuid));
    }

    @Override
    public long getLevel(@NotNull OfflinePlayer player) {
        return getLevel(player.getUniqueId());
    }
}
