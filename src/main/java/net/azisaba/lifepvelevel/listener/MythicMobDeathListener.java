package net.azisaba.lifepvelevel.listener;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobLootDropEvent;
import net.azisaba.lifepvelevel.messages.Messages;
import net.azisaba.lifepvelevel.sql.DBConnector;
import net.azisaba.lifepvelevel.util.LevelCalculator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobDeathListener implements Listener {
    @EventHandler
    public void giveExp(MythicMobLootDropEvent e) {
        if (!(e.getKiller() instanceof Player)) return;
        Player killer = (Player) e.getKiller();
        int exp = (int) (e.getExp() * (1 + (DBConnector.getBoostedPercentage() / 100.0)));
        long currentExp = DBConnector.getExp(killer.getUniqueId());
        long oldLevel = LevelCalculator.toLevel(currentExp);
        long newLevel = LevelCalculator.toLevel(currentExp + exp);
        DBConnector.addExp(killer.getUniqueId(), exp);
        if (newLevel > oldLevel) {
            Messages.sendFormatted(killer, "level_up.self", oldLevel, newLevel);
            Messages.sendFormatted(Bukkit.getConsoleSender(), "level_up.everyone", killer.getName(), newLevel);
            killer.updateInventory(); // player might have unlocked new items
            if (newLevel % 100 == 0) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Messages.sendFormatted(player, "level_up.everyone", killer.getName(), newLevel);
                }
            }
        }
    }
}
