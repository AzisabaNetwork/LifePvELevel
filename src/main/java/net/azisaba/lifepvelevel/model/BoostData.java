package net.azisaba.lifepvelevel.model;

import net.azisaba.lifepvelevel.sql.DBConnector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record BoostData(UUID uuid, long percentage, long start, long end) {
    public BoostData(@NotNull UUID uuid, long percentage, long start, long end) {
        this.uuid = uuid;
        this.percentage = percentage;
        this.start = start;
        this.end = end;
    }

    @Override
    public @NotNull UUID uuid() {
        return uuid;
    }

    public @NotNull Optional<String> getName() {
        return DBConnector.getName(uuid());
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoostData(UUID uuid1, long percentage1, long start1, long end1))) return false;
        return percentage == percentage1 && start == start1 && end == end1 && Objects.equals(uuid, uuid1);
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
