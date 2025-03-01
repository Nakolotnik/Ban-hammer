package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.Date;
import java.util.Map;

public class BanMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        String defaultDuration = BanMace.getInstance().getConfig().getString("ban_mode.default_duration", "1h");
        long banDuration = parseDuration(defaultDuration);

        // Бан игрока
        String targetName = target.getName();
        String damagerName = damager.getName();
        Date expiryDate = new Date(System.currentTimeMillis() + (banDuration * 1000));

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                targetName,
                BanMace.getInstance().getMessage("ban_reason"),
                expiryDate,
                damagerName
        );

        target.kickPlayer(BanMace.getInstance().getMessage("banned_message", Map.of("duration", formatDuration(banDuration))));
        damager.sendMessage(BanMace.getInstance().getMessage("ban_applied", Map.of(
                "player", targetName,
                "duration", formatDuration(banDuration)
        )));

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(targetName);
            }
        }.runTaskLater(BanMace.getInstance(), banDuration * 20);
    }



    @Override
    public String getModeName() {
        return "BAN";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();

            if (!BanMace.isHoldingBanMace(damager)) {
                return;
            }

            if (BanMace.getCurrentMode() instanceof BanMode) {
                execute(damager, target);
                event.setCancelled(true);
            }
        }
    }

    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 60;
        }

        Map<Character, Long> timeUnits = Map.of(
                's', 1L,
                'm', 60L,
                'h', 3600L,
                'd', 86400L,
                'w', 604800L,
                'y', 31536000L
        );

        try {
            char lastChar = duration.charAt(duration.length() - 1);
            if (Character.isDigit(lastChar)) {
                return Long.parseLong(duration);
            }

            Long multiplier = timeUnits.get(lastChar);
            if (multiplier != null) {
                String numericPart = duration.substring(0, duration.length() - 1);
                return Long.parseLong(numericPart) * multiplier;
            }

            throw new IllegalArgumentException("Unknown time unit: " + lastChar);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[BanMace] Invalid duration format: " + duration + ". Defaulting to 60 seconds.");
            return 60;
        }
    }

    private String formatDuration(long seconds) {
        Map<String, Long> timeUnits = Map.of(
                "year(s)", 31536000L,
                "week(s)", 604800L,
                "day(s)", 86400L,
                "hour(s)", 3600L,
                "minute(s)", 60L
        );

        for (Map.Entry<String, Long> entry : timeUnits.entrySet()) {
            long unitValue = entry.getValue();
            if (seconds >= unitValue) {
                return (seconds / unitValue) + " " + entry.getKey();
            }
        }

        return seconds + " second(s)";
    }
}
