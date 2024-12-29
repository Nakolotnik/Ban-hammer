package org.nakolotnik.banMace.utils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.NETHERITE_AXE && item.getItemMeta() != null) {
            var meta = item.getItemMeta();

            String baseName = BanMace.getInstance().getConfig().getString("item_customization.bm-name", "Ban Mace");
            String nameColor = BanMace.getInstance().getConfig().getString("item_customization.name_color", "§7");
            String expectedNamePrefix = nameColor + baseName;

            if (meta.getDisplayName() != null && meta.getDisplayName().startsWith(expectedNamePrefix)) {
                switchMode(player);
                updateItemDisplayName(item, BanMace.getCurrentMode().getModeName());
                playSwitchEffects(player);
            }
        }
    }

    private void switchMode(Player player) {
        BanMaceMode currentMode = null;
        for (BanMaceMode mode : BanMaceMode.values()) {
            if (BanMace.getCurrentMode().getClass().equals(mode.getHandler().getClass())) {
                currentMode = mode;
                break;
            }
        }

        BanMaceMode nextMode;
        if (currentMode == null) {
            nextMode = BanMaceMode.SPAWN;
        } else {
            int nextIndex = (currentMode.ordinal() + 1) % BanMaceMode.values().length;
            nextMode = BanMaceMode.values()[nextIndex];
        }

        BanMace.setMode(nextMode.getHandler());

        String message = BanMace.getInstance().getMessage("mode_switched", Map.of("mode", nextMode.name()));
        BanMace.getInstance().displayMessage(player, message);
    }

    private void updateItemDisplayName(ItemStack item, String modeName) {
        if (item.getItemMeta() != null) {
            var meta = item.getItemMeta();

            String nameColor = BanMace.getInstance().getConfig().getString("item_customization.name_color", "§7");
            String baseName = BanMace.getInstance().getConfig().getString("item_customization.bm-name", "Ban Mace");

            String displayName = nameColor + baseName + " (" + modeName + ")";

            meta.setDisplayName(displayName);

            item.setItemMeta(meta);
        }
    }

    private void playSwitchEffects(Player player) {
        // Получение конфигурации эффектов
        String soundName = BanMace.getInstance().getConfig().getString("additional_effects.mode_switch_sound", "BLOCK_NOTE_BLOCK_PLING");
        String particleName = BanMace.getInstance().getConfig().getString("additional_effects.mode_switch_particle", "SPELL_WITCH");

        // Воспроизведение звука
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            BanMace.getInstance().getLogger().warning("Invalid sound: " + soundName);
        }

        // Создание частиц
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException e) {
            BanMace.getInstance().getLogger().warning("Invalid particle: " + particleName);
        }
    }
}
