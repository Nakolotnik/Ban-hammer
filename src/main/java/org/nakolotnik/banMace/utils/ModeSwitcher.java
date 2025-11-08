package org.nakolotnik.banMace.utils;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.BanMace.BanMaceMode;

import java.util.Map;

public class ModeSwitcher implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!BanMace.isHoldingBanMace(player)) {
            return;
        }

        switchMode(player);
        playSwitchEffects(player);
    }

    private void switchMode(Player player) {
        BanMace plugin = BanMace.getInstance();
        BanMaceMode currentModeEnum = null;
        for (BanMaceMode mode : BanMaceMode.values()) {
            if (BanMace.getCurrentMode().getClass().equals(mode.getHandler().getClass())) {
                currentModeEnum = mode;
                break;
            }
        }
        BanMaceMode nextModeEnum = (currentModeEnum == null)
                ? BanMaceMode.SPAWN
                : BanMaceMode.values()[(currentModeEnum.ordinal() + 1) % BanMaceMode.values().length];

        BanMace.setMode(player, nextModeEnum.getHandler());
        String modeNameKey = "mode_" + nextModeEnum.name().toLowerCase();
        String translatedModeName = plugin.getMessage(modeNameKey);
        String message = plugin.getMessage("mode_switched", Map.of("mode", translatedModeName));
        plugin.displayMessage(player, message);
    }

    private void playSwitchEffects(Player player) {
        BanMace plugin = BanMace.getInstance();
        String soundName = plugin.getConfig().getString("additional_effects.mode_switch_sound", "block.note_block.pling");
        String particleName = plugin.getConfig().getString("additional_effects.mode_switch_particle", "witch");

        Sound sound = plugin.getSoundFromString(soundName);
        if (sound != null) {
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.5f);
        } else {
            plugin.getLogger().warning("Invalid mode_switch_sound in config: " + soundName);
        }

        Particle particle = plugin.getParticleFromString(particleName);
        if (particle != null) {
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        } else {
            plugin.getLogger().warning("Invalid mode_switch_particle in config: " + particleName);
        }
    }
}