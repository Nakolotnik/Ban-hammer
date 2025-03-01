package org.nakolotnik.banMace;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nakolotnik.banMace.utils.LanguageManager;
import org.nakolotnik.banMace.utils.mods.TeleportToMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final BanMace plugin;

    public CommandHandler(BanMace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("bm-give")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("must_be_player"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage(plugin.getMessage("no_permission"));
                return true;
            }
            ItemStack banMace = plugin.createBanMace();
            player.getInventory().addItem(banMace);
            player.sendMessage(plugin.getMessage("mace_given"));
            return true;
        }
        if (command.getName().equalsIgnoreCase("bm-setlanguage")) {
            if (args.length == 1) {
                String newLanguage = args[0].toLowerCase();
                LanguageManager languageManager = plugin.getLanguageManager();
                List<String> availableLanguages = languageManager.getAvailableLanguages();
                if (!availableLanguages.contains(newLanguage)) {
                    String languages = languageManager.getAvailableLanguagesString();
                    sender.sendMessage(languageManager.getMessage("invalid_language", Map.of("languages", languages)));
                    return true;
                }
                languageManager.loadMessages(newLanguage);
                plugin.getConfig().set("language", newLanguage);
                plugin.saveConfig();
                sender.sendMessage(languageManager.getMessage("language_set", Map.of("language", newLanguage)));
            } else {
                sender.sendMessage(plugin.getLanguageManager().getMessage("language_usage"));
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("bm-tpcoordinates")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("must_be_player"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(plugin.getMessage("invalid_usage", Map.of("usage", "/bm-tpcoordinates <x> <y> <z>")));
                return true;
            }
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                Player player = (Player) sender;
                if (BanMace.isHoldingBanMace(player)) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item != null && item.hasItemMeta()) {
                        var meta = item.getItemMeta();
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        if (lore == null) {
                            lore = new ArrayList<>();
                        }
                        lore.removeIf(line -> line.startsWith("Coordinates:"));
                        lore.add("Coordinates: X=" + x + " Y=" + y + " Z=" + z);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessage("invalid_usage", Map.of("usage", "/bm-tpcoordinates <x> <y> <z>")));
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("bm-changeview")) {
            if (args.length != 1) {
                sender.sendMessage(plugin.getMessage("invalid_usage", Map.of("usage", "/bm-changeview <chat|actionbar|title>")));
                return true;
            }
            String newMode = args[0].toLowerCase();
            List<String> validModes = List.of("chat", "actionbar", "title");
            if (!validModes.contains(newMode)) {
                sender.sendMessage(plugin.getMessage("invalid_mode"));
                return true;
            }
            plugin.getConfig().set("message_display_mode", newMode);
            plugin.saveConfig();
            sender.sendMessage(plugin.getMessage("view_mode_changed", Map.of("mode", newMode)));
            return true;
        }

        if (command.getName().equalsIgnoreCase("bm-help")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("must_be_player"));
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage(plugin.getMessage("command_help"));
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("bm-setlanguage")) {
            if (args.length == 1) {
                List<String> availableLanguages = plugin.getLanguageManager().getAvailableLanguages();
                String partialInput = args[0].toLowerCase();
                List<String> suggestions = new ArrayList<>();
                for (String language : availableLanguages) {
                    if (language.startsWith(partialInput)) {
                        suggestions.add(language);
                    }
                }
                return suggestions;
            }
        }
        if (command.getName().equalsIgnoreCase("bm-tpcoordinates") && sender instanceof Player) {
            Player player = (Player) sender;
            Location loc = player.getLocation();
            if (args.length == 1) {
                return List.of(String.valueOf((int) loc.getX()));
            } else if (args.length == 2) {
                return List.of(String.valueOf((int) loc.getY()));
            } else if (args.length == 3) {
                return List.of(String.valueOf((int) loc.getZ()));
            }
        }
        if (command.getName().equalsIgnoreCase("bm-changeview")) {
            if (args.length == 1) {
                String partialInput = args[0].toLowerCase();
                List<String> validModes = List.of("chat", "actionbar", "title");
                List<String> suggestions = new ArrayList<>();
                for (String mode : validModes) {
                    if (mode.startsWith(partialInput)) {
                        suggestions.add(mode);
                    }
                }
                return suggestions;
            }
        }
        if (command.getName().equalsIgnoreCase("bm-help")) {
            return new ArrayList<>();
        }
        return null;
    }
}
