package net.azisaba.lifepvelevel.sql;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Properties;

public final class DatabaseConfig {
    private final String driver;
    private final String scheme;
    private final String hostname;
    private final int port;
    private final String name;
    private final String username;
    private final String password;
    private final Properties properties;

    public DatabaseConfig(@NotNull ConfigurationSection section) {
        String driver = section.getString("driver");
        String scheme = Objects.requireNonNull(section.getString("scheme"));
        String hostname = Objects.requireNonNull(section.getString("hostname"));
        int port = section.getInt("port", 3306);
        String name = Objects.requireNonNull(section.getString("name"));
        String username = section.getString("username");
        String password = section.getString("password");
        Properties properties = new Properties();
        ConfigurationSection props = section.getConfigurationSection("properties");
        if (props != null) {
            props.getValues(true).forEach(((key, value) -> properties.setProperty(key, String.valueOf(value))));
        }
        this.driver = driver;
        this.scheme = scheme;
        this.hostname = hostname;
        this.port = port;
        this.name = name;
        this.username = username;
        this.password = password;
        this.properties = properties;
    }

    @Contract(pure = true)
    @Nullable
    public String driver() {
        return driver;
    }

    @Contract(pure = true)
    @NotNull
    public String scheme() {
        return scheme;
    }

    @Contract(pure = true)
    @NotNull
    public String hostname() {
        return hostname;
    }

    @Contract(pure = true)
    public int port() {
        return port;
    }

    @Contract(pure = true)
    @NotNull
    public String name() {
        return name;
    }

    @Contract(pure = true)
    @Nullable
    public String username() {
        return username;
    }

    @Contract(pure = true)
    @Nullable
    public String password() {
        return password;
    }

    @Contract(pure = true)
    @NotNull
    public Properties properties() {
        return properties;
    }

    @Contract(pure = true)
    @NotNull
    public String toUrl() {
        return scheme() + "://" + hostname() + ":" + port() + "/" + name();
    }
}
