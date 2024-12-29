package org.nakolotnik.banMace.utils.mods;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

 import java.util.Map;

public class FreezeMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 9));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 20, 0));

        damager.sendMessage(BanMace.getInstance().getMessage("freeze_applied", Map.of("target", target.getName())));
        target.sendMessage(BanMace.getInstance().getMessage("freeze_received"));
    }

    @Override
    public String getModeName() {
        return "FREEZE";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }
            if (BanMace.getCurrentMode() instanceof FreezeMode) {
                execute(damager, target);
                event.setCancelled(true);
            }
        }
    }
}
