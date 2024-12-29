package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.Date;

public class BanMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        int banDurationSeconds = 60;
        String reason = "Banned by Ban Mace";

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                target.getName(),
                reason,
                new Date(System.currentTimeMillis() + banDurationSeconds * 1000L),
                damager.getName()
        );
        target.kickPlayer(reason);

        damager.sendMessage(target.getName() + " has been banned for " + banDurationSeconds + " seconds.");
    }

    @Override
    public String getModeName() {
        return "BAN";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }
            if (BanMace.getCurrentMode() instanceof BanMode) {
                execute(damager, target);
                event.setCancelled(true);
            }

        }
    }
}
