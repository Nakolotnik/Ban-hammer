package org.nakolotnik.banMace.utils.mods;

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

    private void execute(Player damager, Player target, double x, double y, double z) {
        BanMace plugin = BanMace.getInstance();

        if (target.getWorld() != null) {
            Location targetLocation = new Location(target.getWorld(), x, y, z);
            target.teleport(targetLocation);
            plugin.playTeleportEffects(target);

            damager.sendMessage(plugin.getMessage("teleported_to_coordinates", Map.of(
                    "player", target.getName(),
                    "x", String.valueOf((int)x),
                    "y", String.valueOf((int)y),
                    "z", String.valueOf((int)z)
            )));
        }

        String actionDetails = String.format("Teleported to coordinates (X: %.1f, Y: %.1f, Z: %.1f)", x, y, z);
        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);
    }

    @Override
    public String getModeName() {
        return "Coordinate Teleport";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (!BanMace.isHoldingBanMace(damager) || !(BanMace.getCurrentMode() instanceof TeleportToMode)) {
            return;
        }

        event.setCancelled(true);

        if (target.hasPermission("banmace.bypass")) {
            damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
            return;
        }

        ItemStack item = damager.getInventory().getItemInMainHand();

        if (item.getItemMeta() == null || item.getItemMeta().getLore() == null) {
            damager.sendMessage(BanMace.getInstance().getMessage("coordinates_not_set"));
            return;
        }

        List<String> lore = item.getItemMeta().getLore();
        for (String line : lore) {
            if (line.toLowerCase().contains("coordinates:")) {
                try {
                    String coordsPart = line.substring(line.toLowerCase().indexOf("coordinates:") + "coordinates:".length()).trim();
                    String[] parts = coordsPart.split(" ");
                    double x = 0, y = 0, z = 0;
                    for (String part : parts) {
                        String[] kv = part.split("=");
                        if (kv.length == 2) {
                            switch(kv[0].toUpperCase()) {
                                case "X": x = Double.parseDouble(kv[1]); break;
                                case "Y": y = Double.parseDouble(kv[1]); break;
                                case "Z": z = Double.parseDouble(kv[1]); break;
                            }
                        }
                    }
                    execute(damager, target, x, y, z);
                    return;
                } catch (Exception e) {
                    damager.sendMessage(BanMace.getInstance().getMessage("invalid_coordinates"));
                    return;
                }
            }
        }
        damager.sendMessage(BanMace.getInstance().getMessage("coordinates_not_set"));
    }
}