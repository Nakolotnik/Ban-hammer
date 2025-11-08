package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.Map;

public class BedMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        BanMace plugin = BanMace.getInstance();
        Location bedLocation = target.getBedSpawnLocation();

        String actionDetails;

        if (bedLocation == null) {
            bedLocation = target.getWorld().getSpawnLocation();

            String spawnMessage = plugin.getMessage("no_bed_spawn", Map.of("player", target.getName()));
            damager.sendMessage(spawnMessage);
            target.sendMessage(plugin.getMessage("teleported_to_spawn"));

            actionDetails = "Teleported to world spawn (no bed found).";

        } else {
            target.sendMessage(plugin.getMessage("bed_teleport"));
            damager.sendMessage(plugin.getMessage("teleported_to_bed", Map.of("player", target.getName())));

            actionDetails = String.format("Teleported to bed at (X: %d, Y: %d, Z: %d)",
                    bedLocation.getBlockX(), bedLocation.getBlockY(), bedLocation.getBlockZ());
        }

        target.teleport(bedLocation);
        plugin.playTeleportEffects(target);

        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);
    }

    @Override
    public String getModeName() {
        return "Bed Teleport";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (BanMace.isHoldingBanMace(damager) && BanMace.getCurrentMode() instanceof BedMode) {
            if (target.hasPermission("banmace.bypass")) {
                damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
                return;
            }
            execute(damager, target);
            event.setCancelled(true);
        }
    }
}