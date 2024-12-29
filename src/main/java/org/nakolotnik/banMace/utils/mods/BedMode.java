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

public class BedMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        Location bedLocation = target.getBedSpawnLocation();

        if (bedLocation == null) {
            bedLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            String spawnMessage = BanMace.getInstance().getLanguageManager().getMessage(
                    "no_bed_spawn",
                    Map.of("player", target.getName())
            );
            damager.sendMessage(spawnMessage);
            target.sendMessage(BanMace.getInstance().getLanguageManager().getMessage("teleported_to_spawn"));
        } else {
            target.sendMessage(BanMace.getInstance().getLanguageManager().getMessage("bed_teleport"));
            damager.sendMessage(BanMace.getInstance().getLanguageManager().getMessage(
                    "teleported_to_bed",
                    Map.of("player", target.getName())
            ));
        }

        target.teleport(bedLocation);
        BanMace.getInstance().playTeleportEffects(target);
    }

    @Override
    public String getModeName() {
        return "BED";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();

            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }

            if (BanMace.getCurrentMode() instanceof BedMode) {
                execute(damager, target);
                event.setCancelled(true);
            }
        }
    }
}
