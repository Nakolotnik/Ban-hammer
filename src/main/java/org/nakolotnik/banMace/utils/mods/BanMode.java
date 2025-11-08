package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.Date;
import java.util.Map;

public class BanMode implements ModeHandler, Listener {

    @Override
    public void execute(Player damager, Player target) {
        BanMace plugin = BanMace.getInstance();

        String defaultDuration = plugin.getConfig().getString("ban_mode.default_duration", "1h");
        long banDurationSeconds = parseDuration(defaultDuration);

        String targetName = target.getName();
        String damagerName = damager.getName();
        String formattedDuration = formatDuration(banDurationSeconds);

        Date expiryDate = (banDurationSeconds > 0) ? new Date(System.currentTimeMillis() + (banDurationSeconds * 1000L)) : null;

        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                targetName,
                plugin.getMessage("ban_reason"),
                expiryDate,
                damagerName
        );

        target.kickPlayer(plugin.getMessage("banned_message", Map.of("duration", formattedDuration)));

        damager.sendMessage(plugin.getMessage("ban_applied", Map.of(
                "player", targetName,
                "duration", formattedDuration
        )));

        String actionDetails = "Duration: " + formattedDuration;
        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);
    }

    @Override
    public String getModeName() {
        return "Ban";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (BanMace.isHoldingBanMace(damager) && BanMace.getCurrentMode() instanceof BanMode) {
            if (target.hasPermission("banmace.bypass")) {
                damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
                return;
            }
            execute(damager, target);
            event.setCancelled(true);
        }
    }

    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 3600;
        }

        if (duration.equalsIgnoreCase("permanent") || duration.equalsIgnoreCase("perm")) {
            return -1;
        }

        try {
            long value = Long.parseLong(duration.substring(0, duration.length() - 1));
            char unit = duration.toLowerCase().charAt(duration.length() - 1);

            return switch (unit) {
                case 's' -> value;
                case 'm' -> value * 60;
                case 'h' -> value * 3600;
                case 'd' -> value * 86400;
                case 'w' -> value * 604800;
                case 'y' -> value * 31536000;
                default -> 3600;
            };
        } catch (Exception e) {
            BanMace.getInstance().getLogger().warning("[BanMace] Invalid duration format in config: '" + duration + "'. Defaulting to 1 hour.");
            return 3600;
        }
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "Permanent";
        }

        long years = seconds / 31536000L;
        seconds %= 31536000L;
        long weeks = seconds / 604800L;
        seconds %= 604800L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append(" year(s) ");
        if (weeks > 0) sb.append(weeks).append(" week(s) ");
        if (days > 0) sb.append(days).append(" day(s) ");
        if (hours > 0) sb.append(hours).append(" hour(s) ");
        if (minutes > 0) sb.append(minutes).append(" minute(s) ");
        if (remainingSeconds > 0) sb.append(remainingSeconds).append(" second(s) ");

        return sb.toString().trim();
    }
}