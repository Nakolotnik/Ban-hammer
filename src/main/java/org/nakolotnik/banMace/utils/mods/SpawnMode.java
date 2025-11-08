package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.Map;

public class SpawnMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        BanMace plugin = BanMace.getInstance();

        Location spawn = target.getWorld().getSpawnLocation();
        target.teleport(spawn);
        plugin.playTeleportEffects(target);

        damager.sendMessage(plugin.getMessage("player_teleported_spawn", Map.of("player", target.getName())));
        target.sendMessage(plugin.getMessage("teleported_to_spawn"));

        String actionDetails = String.format("Teleported to world spawn at (X: %d, Y: %d, Z: %d)",
                spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);
    }

    @Override
    public String getModeName() {
        return "Spawn Teleport";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (BanMace.isHoldingBanMace(damager) && BanMace.getCurrentMode() instanceof SpawnMode) {
            if (target.hasPermission("banmace.bypass")) {
                damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
                return;
            }
            execute(damager, target);
            event.setCancelled(true);
        }
    }
}