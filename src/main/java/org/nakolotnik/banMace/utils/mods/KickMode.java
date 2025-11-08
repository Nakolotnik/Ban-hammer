package org.nakolotnik.banMace.utils.mods;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.Map;

public class KickMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        BanMace plugin = BanMace.getInstance();

        String reason = plugin.getMessage("kick_reason");
        target.kickPlayer(reason);

        damager.sendMessage(plugin.getMessage("kick_applied", Map.of("player", target.getName())));

        String actionDetails = "Reason: " + reason;
        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);
    }

    @Override
    public String getModeName() {
        return "Kick";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (BanMace.isHoldingBanMace(damager) && BanMace.getCurrentMode() instanceof KickMode) {
            if (target.hasPermission("banmace.bypass")) {
                damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
                return;
            }
            execute(damager, target);
            event.setCancelled(true);
        }
    }
}