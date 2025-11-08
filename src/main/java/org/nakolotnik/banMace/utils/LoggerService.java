package org.nakolotnik.banMace.utils;

import org.bukkit.entity.Player;
import org.nakolotnik.banMace.BanMace;

import java.awt.Color;

public class LoggerService {

    private final BanMace plugin;
    private DiscordWebhook discordWebhook;

    public LoggerService(BanMace plugin) {
        this.plugin = plugin;
        reloadWebhook();
    }

    public void reloadWebhook() {
        String webhookUrl = plugin.getConfig().getString("logging.discord.webhook_url", "");
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            this.discordWebhook = new DiscordWebhook(webhookUrl, plugin);
        } else {
            this.discordWebhook = null;
        }
    }

    public void log(String message) {
        if (plugin.getConfig().getBoolean("logging.console.enabled", true)) {
            plugin.getLogger().info(message);
        }
    }

    public void logMaceAction(Player actor, Player target, String modeName, String actionDetails) {
        String formattedModeName = modeName.replace("_", " ").toUpperCase();

        String logMessage = String.format("%s used BanMace (%s mode) on %s. Details: %s",
                actor.getName(), formattedModeName, target.getName(), actionDetails);
        log(logMessage);

        if (plugin.getConfig().getBoolean("logging.discord.enabled", false) && discordWebhook != null) {
            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

            embed.setAuthor(actor.getName() + " (" + actor.getUniqueId() + ")", "https://cravatar.eu/helmavatar/" + actor.getUniqueId() + "/64.png");
            embed.setTitle("BanMace Action: " + formattedModeName);

            String description = String.format(
                    "**Target:** %s\n**Action:** %s\n**Location:** %s at (X: %d, Y: %d, Z: %d)",
                    target.getName(), actionDetails, target.getWorld().getName(),
                    target.getLocation().getBlockX(), target.getLocation().getBlockY(), target.getLocation().getBlockZ()
            );
            embed.setDescription(description);

            Color awtColor = switch (modeName.toUpperCase()) {
                case "BAN", "KICK" -> Color.RED;
                case "FREEZE" -> new Color(0, 255, 255);
                case "SPAWN", "BED", "TELEPORT_TO" -> Color.YELLOW;
                default -> Color.GRAY;
            };

            embed.setColor(awtColor.getRGB() & 0xFFFFFF);

            embed.setFooter("BanMace v" + plugin.getDescription().getVersion());
            discordWebhook.sendEmbed(embed);
        }
    }
}