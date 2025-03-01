package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.List;
import java.util.Map;

public class TeleportToMode implements ModeHandler, Listener {


    @Override
    public void execute(Player damager, Player target) {
    }

    public void execute(Player damager, Player target, double x, double y, double z) {
        Location targetLocation = new Location(Bukkit.getWorlds().get(0), x, y, z);
        target.teleport(targetLocation);
        BanMace.getInstance().playTeleportEffects(target);
        damager.sendMessage(BanMace.getInstance().getMessage("teleported_to_coordinates", Map.of(
                "x", String.valueOf(x),
                "y", String.valueOf(y),
                "z", String.valueOf(z)
        )));
    }

    @Override
    public String getModeName() {
        return "TELEPORT_TO";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }
            ItemStack item = damager.getInventory().getItemInMainHand();
            if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) {
                return;
            }
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                if (line.startsWith("Coordinates:")) {
                    try {
                        String coords = line.substring("Coordinates:".length()).trim();
                        String[] parts = coords.split(" ");
                        double x = 0, y = 0, z = 0;
                        for (String part : parts) {
                            if (part.startsWith("X=")) {
                                x = Double.parseDouble(part.substring(2));
                            } else if (part.startsWith("Y=")) {
                                y = Double.parseDouble(part.substring(2));
                            } else if (part.startsWith("Z=")) {
                                z = Double.parseDouble(part.substring(2));
                            }
                        }
                        execute(damager, target, x, y, z);
                        event.setCancelled(true);
                        return;
                    } catch (Exception e) {
                        damager.sendMessage(BanMace.getInstance().getMessage("invalid_coordinates"));
                    }
                }
            }
        }
    }
}
