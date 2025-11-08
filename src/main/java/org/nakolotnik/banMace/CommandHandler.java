package org.nakolotnik.banMace;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nakolotnik.banMace.utils.LanguageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final BanMace plugin;
    private static final List<String> VIEW_MODES = List.of("chat", "actionbar", "title");

    public CommandHandler(BanMace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        return switch (command.getName().toLowerCase()) {
            case "bm-give" -> handleGiveCommand(sender);
            case "bm-setlanguage" -> handleSetLanguageCommand(sender, args);
            case "bm-tpcoordinates" -> handleTpCoordinatesCommand(sender, args);
            case "bm-changeview" -> handleChangeViewCommand(sender, args);
            case "bm-help" -> handleHelpCommand(sender);
            case "bm-settings" -> handleSettingsCommand(sender);
            default -> false;
        };
    }

    private boolean handleGiveCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("must_be_player"));
            return true;
        }

        player.getInventory().addItem(plugin.createBanMace());
        player.sendMessage(plugin.getMessage("mace_given"));

        plugin.getLoggerService().log(String.format("Player '%s' gave themselves the BanMace.", player.getName()));
        return true;
    }

    private boolean handleSetLanguageCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getMessage("language_usage"));
            return true;
        }

        String newLanguage = args[0].toLowerCase();
        LanguageManager languageManager = plugin.getLanguageManager();
        if (!languageManager.getAvailableLanguages().contains(newLanguage)) {
            String languages = languageManager.getAvailableLanguagesString();
            sender.sendMessage(plugin.getMessage("invalid_language", Map.of("languages", languages)));
            return true;
        }

        languageManager.loadMessages(newLanguage);
        plugin.getConfig().set("language", newLanguage);
        plugin.saveConfig();
        sender.sendMessage(plugin.getMessage("language_set", Map.of("language", newLanguage.toUpperCase())));

        plugin.getLoggerService().log(String.format("'%s' changed plugin language to %s.", sender.getName(), newLanguage.toUpperCase()));
        return true;
    }

    private boolean handleTpCoordinatesCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
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

            if (BanMace.isHoldingBanMace(player)) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getItemMeta() != null) {
                    var meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.removeIf(line -> line.toLowerCase().contains("coordinates:"));
                    lore.add(String.format("§7Coordinates: X=%.1f Y=%.1f Z=%.1f", x, y, z));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    player.sendMessage(plugin.getMessage("coordinates_set", Map.of("x", String.format("%.1f", x), "y", String.format("%.1f", y), "z", String.format("%.1f", z))));

                    plugin.getLoggerService().log(String.format("Player '%s' set mace coordinates to (%.1f, %.1f, %.1f).", player.getName(), x, y, z));
                }
            } else {
                player.sendMessage(plugin.getMessage("not_holding_mace"));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessage("invalid_usage", Map.of("usage", "/bm-tpcoordinates <x> <y> <z>")));
        }
        return true;
    }

    private boolean handleChangeViewCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getMessage("invalid_usage", Map.of("usage", "/bm-changeview <chat|actionbar|title>")));
            return true;
        }
        String newMode = args[0].toLowerCase();
        if (!VIEW_MODES.contains(newMode)) {
            sender.sendMessage(plugin.getMessage("invalid_mode"));
            return true;
        }
        plugin.getConfig().set("message_display_mode", newMode);
        plugin.saveConfig();
        sender.sendMessage(plugin.getMessage("view_mode_changed", Map.of("mode", newMode)));

        plugin.getLoggerService().log(String.format("'%s' changed message display mode to %s.", sender.getName(), newMode));
        return true;
    }

    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage(plugin.getMessage("command_help"));
        return true;
    }

    private boolean handleSettingsCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("must_be_player"));
            return true;
        }

        plugin.getSettingsGUI().open(player);
        plugin.getLoggerService().log(String.format("Player '%s' opened the settings GUI.", player.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            return Collections.emptyList();
        }

        return switch (command.getName().toLowerCase()) {
            case "bm-setlanguage" -> completeLanguage(args);
            case "bm-tpcoordinates" -> (sender instanceof Player p) ? completeCoordinates(p, args) : Collections.emptyList();
            case "bm-changeview" -> completeViewMode(args);
            default -> Collections.emptyList();
        };
    }

    private List<String> completeLanguage(String[] args) {
        if (args.length == 1) {
            return filterAndCollect(plugin.getLanguageManager().getAvailableLanguages(), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> completeCoordinates(Player player, String[] args) {
        Location loc = player.getLocation();
        return switch (args.length) {
            case 1 -> List.of(String.valueOf(loc.getBlockX()));
            case 2 -> List.of(String.valueOf(loc.getBlockY()));
            case 3 -> List.of(String.valueOf(loc.getBlockZ()));
            default -> Collections.emptyList();
        };
    }

    private List<String> completeViewMode(String[] args) {
        if (args.length == 1) {
            return filterAndCollect(VIEW_MODES, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filterAndCollect(List<String> source, String input) {
        return source.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}