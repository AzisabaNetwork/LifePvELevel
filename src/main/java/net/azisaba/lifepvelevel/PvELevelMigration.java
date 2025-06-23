package net.azisaba.lifepvelevel;

import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import me.staartvin.statz.Statz;
import me.staartvin.statz.datamanager.player.PlayerStat;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PvELevelMigration {
    public static void doMigration(@NotNull CommandSender sender) {
        Statz statz = (Statz) Bukkit.getPluginManager().getPlugin("Statz");
        if (statz == null) {
            sender.sendMessage("Statz is not installed.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.getInstance(), () -> {
            long migrated = 0;
            for (String username : mcMMO.getDatabaseManager().getStoredUsers()) {
                try {
                    migratePlayer(username);
                    migrated++;
                } catch (Exception e) {
                    SpigotPlugin.getInstance().getLogger().warning("Failed to migrate " + username);
                    e.printStackTrace();
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Migrated " + migrated + " players.");
        });
    }

    public static void migratePlayer(@NotNull String username) {
        PlayerProfile profile = mcMMO.getDatabaseManager().loadPlayerProfile(username);
        Statz statz = (Statz) Bukkit.getPluginManager().getPlugin("Statz");
        if (statz == null) {
            throw new IllegalStateException("Statz is not installed.");
        }
        int acrobaticsLevel = profile.getSkillLevel(PrimarySkillType.ACROBATICS);
        long kills = (long) Math.floor(statz.getStatzAPI().getTotalOf(PlayerStat.KILLS_MOBS, profile.getUniqueId(), null));
        long exp = LevelCalculator.toExp(Math.sqrt(kills / 5.0) + (acrobaticsLevel / 75.0));
        if (DBConnector.getExpUncached(profile.getUniqueId()) > exp) {
            return;
        }
        DBConnector.setExp(profile.getUniqueId(), exp);
    }

    public static void migratePlayer(@NotNull UUID uuid) {
        PlayerProfile profile = mcMMO.getDatabaseManager().loadPlayerProfile(uuid);
        Statz statz = (Statz) Bukkit.getPluginManager().getPlugin("Statz");
        if (statz == null) {
            throw new IllegalStateException("Statz is not installed.");
        }
        int acrobaticsLevel = profile.getSkillLevel(PrimarySkillType.ACROBATICS);
        statz.getDataManager().loadPlayerData(uuid, PlayerStat.KILLS_MOBS);
        long kills = (long) Math.floor(statz.getStatzAPI().getTotalOf(PlayerStat.KILLS_MOBS, uuid, null));
        long exp = LevelCalculator.toExp(Math.sqrt(kills / 5.0) + (acrobaticsLevel / 75.0));
        if (DBConnector.getExpUncached(uuid) > exp) {
            return;
        }
        DBConnector.setExp(uuid, exp);
    }
}
