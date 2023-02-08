package net.azisaba.lifepvelevel.commands;

import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import me.staartvin.statz.Statz;
import me.staartvin.statz.datamanager.player.PlayerStat;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ResetPvELevelCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Statz statz = (Statz) Bukkit.getPluginManager().getPlugin("Statz");
        if (statz == null) {
            sender.sendMessage("Statz is not installed.");
            return true;
        }
        Player player = (Player) sender;
        PlayerProfile profile = mcMMO.getDatabaseManager().loadPlayerProfile(player.getName());
        int acrobaticsLevel = profile.getSkillLevel(PrimarySkillType.ACROBATICS);
        long kills = (long) Math.floor(statz.getStatzAPI().getTotalOf(PlayerStat.KILLS_MOBS, profile.getUniqueId(), null));
        long exp = LevelCalculator.toExp(Math.sqrt(kills / 5.0) + (acrobaticsLevel / 75.0));
        DBConnector.setExp(profile.getUniqueId(), exp);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
