package net.azisaba.lifepvelevel.model;

import net.azisaba.lifepvelevel.sql.DBConnector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class BoostData {
    private final UUID uuid;
    private final long percentage;
    private final long start;
    private final long end;

    public BoostData(@NotNull UUID uuid, long percentage, long start, long end) {
        this.uuid = uuid;
        this.percentage = percentage;
        this.start = start;
        this.end = end;
    }

    public @NotNull UUID uuid() {
        return uuid;
    }

    public long percentage() {
        return percentage;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public @NotNull Optional<String> getName() {
        return DBConnector.getName(uuid());
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoostData)) return false;
        BoostData boostData = (BoostData) o;
        return percentage == boostData.percentage && start == boostData.start && end == boostData.end && Objects.equals(uuid, boostData.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, percentage, start, end);
    }

    @Override
    public String toString() {
        return "BoostData{" +
                "uuid=" + uuid +
                ", percentage=" + percentage +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
