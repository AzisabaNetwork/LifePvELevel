package net.azisaba.lifepvelevel.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.model.BoostData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.*;
import org.mariadb.jdbc.Driver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DBConnector {
    private static final Map<String, Long> REQUIRED_LEVELS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PLAYER_EXP = new ConcurrentHashMap<>();
    private static @Nullable HikariDataSource dataSource;
    private static List<BoostData> boostDataList = new ArrayList<>();
    private static long lastUpdate = 0L;

    /**
     * Initializes the data source and pool.
     * @throws SQLException if an error occurs while initializing the pool
     */
    public static void init() throws SQLException {
        new Driver();
        HikariConfig config = new HikariConfig();
        if (SpigotPlugin.getInstance().getDatabaseConfig().driver() != null) {
            config.setDriverClassName(SpigotPlugin.getInstance().getDatabaseConfig().driver());
        }
        config.setJdbcUrl(SpigotPlugin.getInstance().getDatabaseConfig().toUrl());
        config.setUsername(SpigotPlugin.getInstance().getDatabaseConfig().username());
        config.setPassword(SpigotPlugin.getInstance().getDatabaseConfig().password());
        config.setDataSourceProperties(SpigotPlugin.getInstance().getDatabaseConfig().properties());
        dataSource = new HikariDataSource(config);
        createTables();
    }

    public static void createTables() throws SQLException {
        runPrepareStatement("CREATE TABLE IF NOT EXISTS `players` (\n" +
                "  `uuid` VARCHAR(36) NOT NULL,\n" +
                "  `name` VARCHAR(36) NOT NULL,\n" +
                "  `exp` BIGINT NOT NULL DEFAULT 0,\n" +
                "  PRIMARY KEY (`uuid`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;", PreparedStatement::execute);
        runPrepareStatement("CREATE TABLE IF NOT EXISTS `items` (\n" +
                "  `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                "  `mmid` LONGTEXT NOT NULL UNIQUE,\n" +
                "  `required_level` INT NOT NULL DEFAULT 0,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;", PreparedStatement::execute);
        runPrepareStatement("CREATE TABLE IF NOT EXISTS `boosts` (\n" +
                "  `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                "  `uuid` VARCHAR(36) NOT NULL,\n" +
                "  `percentage` BIGINT NOT NULL,\n" +
                "  `start` BIGINT NOT NULL,\n" +
                "  `end` BIGINT NOT NULL,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;", PreparedStatement::execute);
    }

    /**
     * Returns the data source. Throws an exception if the data source is not initialized using {@link #init()}.
     * @return the data source
     * @throws NullPointerException if the data source is not initialized using {@link #init()}
     */
    @Contract(pure = true)
    @NotNull
    public static HikariDataSource getDataSource() {
        return Objects.requireNonNull(dataSource, "#init was not called");
    }

    @NotNull
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    @Contract(pure = true)
    public static <R> R use(@NotNull SQLThrowableFunction<Connection, R> action) throws SQLException {
        try (Connection connection = getConnection()) {
            return action.apply(connection);
        }
    }

    @Contract(pure = true)
    public static void use(@NotNull SQLThrowableConsumer<Connection> action) throws SQLException {
        try (Connection connection = getConnection()) {
            action.accept(connection);
        }
    }

    @Contract(pure = true)
    public static void runPrepareStatement(@Language("SQL") @NotNull String sql, @NotNull SQLThrowableConsumer<PreparedStatement> action) throws SQLException {
        use(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                action.accept(statement);
            }
        });
    }

    @Contract(pure = true)
    public static <R> R getPrepareStatement(@Language("SQL") @NotNull String sql, @NotNull SQLThrowableFunction<PreparedStatement, R> action) throws SQLException {
        return use(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                return action.apply(statement);
            }
        });
    }

    @Contract(pure = true)
    public static void useStatement(@NotNull SQLThrowableConsumer<Statement> action) throws SQLException {
        use(connection -> {
            try (Statement statement = connection.createStatement()) {
                action.accept(statement);
            }
        });
    }

    /**
     * Closes the data source if it is initialized.
     */
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Async.Schedule
    public static void updateAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), DBConnector::updateSync);
    }

    public static void updateSync() {
        try {
            useStatement(statement -> {
                try (ResultSet rs = statement.executeQuery("SELECT * FROM `items`")) {
                    Set<String> receivedKeys = new HashSet<>();
                    while (rs.next()) {
                        String mmid = rs.getString("mmid");
                        receivedKeys.add(mmid);
                        REQUIRED_LEVELS.put(mmid, rs.getLong("required_level"));
                    }
                    REQUIRED_LEVELS.keySet().removeIf(key -> !receivedKeys.contains(key));
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updatePlayerSync(@NotNull UUID uuid, @NotNull String username) {
        try {
            runPrepareStatement("INSERT INTO `players` (`uuid`, `name`, `exp`) VALUES (?, ?, 0) ON DUPLICATE KEY UPDATE `name` = VALUES(`name`)", statement -> {
                statement.setString(1, uuid.toString());
                statement.setString(2, username);
                statement.executeUpdate();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            runPrepareStatement("SELECT `exp` FROM `players` WHERE `uuid` = ?", statement -> {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        PLAYER_EXP.put(uuid, rs.getLong("exp"));
                    }
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Async.Schedule
    public static void setRequiredLevels(@NotNull String mmid, long level) {
        REQUIRED_LEVELS.put(mmid, level);
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            try {
                runPrepareStatement("INSERT INTO `items` (`mmid`, `required_level`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `required_level` = VALUES(`required_level`)", statement -> {
                    statement.setString(1, mmid);
                    statement.setLong(2, level);
                    statement.executeUpdate();
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static long getRequiredLevel(@NotNull String mmid) {
        return REQUIRED_LEVELS.getOrDefault(mmid, 0L);
    }

    public static void addExp(@NotNull UUID uuid, long exp) {
        PLAYER_EXP.put(uuid, PLAYER_EXP.getOrDefault(uuid, 0L) + exp);
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            try {
                runPrepareStatement("UPDATE `players` SET `exp` = `exp` + ? WHERE `uuid` = ?", statement -> {
                    statement.setLong(1, exp);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void setExp(@NotNull UUID uuid, long exp) {
        try {
            runPrepareStatement("UPDATE `players` SET `exp` = ? WHERE `uuid` = ?", statement -> {
                statement.setLong(1, exp);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            });
            PLAYER_EXP.put(uuid, exp);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static long getExp(@NotNull UUID uuid) {
        return PLAYER_EXP.getOrDefault(uuid, 0L);
    }

    public static long getExpUncached(@NotNull UUID uuid) {
        try {
            return getPrepareStatement("SELECT `exp` FROM `players` WHERE `uuid` = ?", statement -> {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("exp");
                    }
                }
                return 0L;
            });
        } catch (SQLException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    @NotNull
    public static Optional<UUID> getUniqueId(@NotNull String username) {
        try {
            return getPrepareStatement("SELECT `uuid` FROM `players` WHERE LOWER(`name`) = ?", statement -> {
                statement.setString(1, username.toLowerCase(Locale.ROOT));
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(UUID.fromString(rs.getString("uuid")));
                    }
                }
                return Optional.empty();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static Optional<String> getName(@NotNull UUID uuid) {
        try {
            return getPrepareStatement("SELECT `name` FROM `players` WHERE `uuid` = ?", statement -> {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getString("name"));
                    }
                }
                return Optional.empty();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addBoostAsync(@NotNull UUID uuid, long percentage, long start, long end) {
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            try {
                runPrepareStatement("INSERT INTO `boosts` (`uuid`, `percentage`, `start`, `end`) VALUES (?, ?, ?, ?)", statement -> {
                    statement.setString(1, uuid.toString());
                    statement.setLong(2, percentage);
                    statement.setLong(3, start);
                    statement.setLong(4, end);
                    statement.executeUpdate();
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void addBoostAsyncByDuration(@NotNull UUID uuid, long percentage, long start, long durationMillis) {
        addBoostAsync(uuid, percentage, start, start + durationMillis);
    }

    public static void tryRefreshBoostData() {
        if (System.currentTimeMillis() - lastUpdate > 5000L) {
            refreshBoostData();
        }
    }

    public static void refreshBoostData() {
        try {
            runPrepareStatement("SELECT * FROM `boosts` WHERE `start` < ? AND `end` > ?", statement -> {
                statement.setLong(1, System.currentTimeMillis());
                statement.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = statement.executeQuery()) {
                    List<BoostData> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new BoostData(UUID.fromString(rs.getString("uuid")), rs.getLong("percentage"), rs.getLong("start"), rs.getLong("end")));
                    }
                    for (BoostData boostData : boostDataList) {
                        if (!list.contains(boostData)) {
                            Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () ->
                                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[PvE経験値ブースト] " +
                                            ChatColor.WHITE + getName(boostData.uuid()).orElse("Unknown") +
                                            ChatColor.LIGHT_PURPLE + "の" + ChatColor.GOLD + boostData.percentage() + "%" + ChatColor.LIGHT_PURPLE + "ブーストが期限切れになりました。")
                            );
                        }
                    }
                    boostDataList = list;
                    lastUpdate = System.currentTimeMillis();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    public static @NotNull @UnmodifiableView List<@NotNull BoostData> getBoostDataList() {
        return Collections.unmodifiableList(boostDataList);
    }

    public static long getBoostedPercentage() {
        return boostDataList.stream().mapToLong(BoostData::percentage).sum();
    }
}
