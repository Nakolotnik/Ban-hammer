package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Bukkit;
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
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        target.teleport(spawn);
        BanMace.getInstance().playTeleportEffects(target);
        damager.sendMessage(BanMace.getInstance().getMessage("player_teleported_spawn", Map.of("player", target.getName())));
        target.sendMessage(BanMace.getInstance().getMessage("teleported_to_spawn"));
    }
    @Override
    public String getModeName() {
        return "SPAWN";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }
            if (BanMace.getCurrentMode() instanceof SpawnMode) {
                execute(damager, target);
                event.setCancelled(true);
            }
        }
    }
}
