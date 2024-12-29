package org.nakolotnik.banMace.utils.mods;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

public class KickMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        String reason = "Kicked by Ban Mace";
        target.kickPlayer(reason);

        damager.sendMessage(target.getName() + " has been kicked.");
    }

    @Override
    public String getModeName() {
        return "KICK";
    }
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }
            if (BanMace.getCurrentMode() instanceof KickMode) {
                execute(damager, target);
                event.setCancelled(true);
            }
        }
    }
}
