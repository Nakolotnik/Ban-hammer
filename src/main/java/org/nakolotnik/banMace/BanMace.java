package org.nakolotnik.banMace;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public final class BanMace extends JavaPlugin implements Listener {
    private static final Material HAMMER_MATERIAL = Material.NETHERITE_AXE;
    private static final String HAMMER_NAME = "§cBan Mace";
    private ItemStack exampleHammer;
    private String currentLanguage;
    private FileConfiguration messages;
    private YamlConfiguration locale;
    private enum Mode { SPAWN, BED, BAN, KICK}
    private Mode currentMode = Mode.SPAWN;
    private String toggleKey;
    private final Map<UUID, Integer> hammerUsageStats = new HashMap<>();
    private static final NamespacedKey MODE_KEY = new NamespacedKey("banmace", "mode");

    private String getMessage(String key) {
        String message = messages.getString(key, "Message not found: " + key);
        if (message.startsWith("Message not found:")) {
            getLogger().warning("Missing message for key: " + key);
        }
        return message;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLocale();
        currentLanguage = getConfig().getString("banmace.language", "en");
        loadMessages(currentLanguage);
        getLogger().info(getMessage("mace_received")); // Пример использования локализации
        currentMode = Mode.valueOf(getConfig().getString("banmace.mode", "SPAWN"));
        toggleKey = getConfig().getString("banmace.toggle_key", "RIGHT_CLICK_BLOCK");
        getServer().getPluginManager().registerEvents(this, this);
        createExampleHammer();

        if (this.getCommand("banmace") != null) {
            Objects.requireNonNull(this.getCommand("banmace")).setExecutor(this);
            Objects.requireNonNull(this.getCommand("banmace")).setTabCompleter(this);
        } else {
            getLogger().severe(getMessage("command_not_found"));
        }

        if (this.getCommand("setlanguage") != null) {
            Objects.requireNonNull(this.getCommand("setlanguage")).setExecutor(this);
            Objects.requireNonNull(this.getCommand("setlanguage")).setTabCompleter(this);
        }
    }

    private void loadLocale() {
        String language = getConfig().getString("banmace.language", "en");
        loadMessages(language);
        File localeFile = new File(getDataFolder(), "messages_" + language + ".yml");
        if (!localeFile.exists()) {
            saveResource("messages_" + language + ".yml", false);
        }
        locale = YamlConfiguration.loadConfiguration(localeFile);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("setlanguage")) {
            if (args.length == 1) {
                return Arrays.asList("en", "ru");
            }
        }
        return null;
    }


    @Override
    public void onDisable() {
        getConfig().set("banmace.language", currentLanguage);
        getConfig().set("banmace.mode", currentMode.name());
        getConfig().set("banmace.toggle_key", toggleKey);
        saveConfig();
    }


    private void updateLore(ItemMeta meta, Mode mode) {
        String modeName = meta.getPersistentDataContainer().get(MODE_KEY, PersistentDataType.STRING);
        Mode effectiveMode = modeName != null ? Mode.valueOf(modeName) : Mode.SPAWN;

        String lore = switch (effectiveMode) {
            case SPAWN -> getMessage("mode_spawn");
            case BED -> getMessage("mode_bed");
            case BAN -> getMessage("mode_ban");
            case KICK -> getMessage("mode_kick");
        };
        meta.setLore(Collections.singletonList(lore));
    }



    private void createExampleHammer() {
        exampleHammer = createHammer(currentMode);
    }

    private ItemStack createHammer(Mode mode) {
        ItemStack hammer = new ItemStack(HAMMER_MATERIAL);
        ItemMeta meta = hammer.getItemMeta();
        if (meta != null) {
            String displayName = HAMMER_NAME + " (" + mode.name() + ")";
            meta.setDisplayName(displayName);
            updateLore(meta, mode);

            int customModelData = getConfig().getInt("item_customization.modes." + mode.name().toLowerCase(), 0);
            meta.setCustomModelData(customModelData);

            meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(MODE_KEY, PersistentDataType.STRING, mode.name());

            hammer.setItemMeta(meta);
        }
        return hammer;
    }






    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();

            ItemStack itemInHand = damager.getInventory().getItemInMainHand();
            if (isBanMace(itemInHand) && damager.hasPermission("banmace.use")) {
                event.setCancelled(true);
                logHammerUsage(damager, target);

                switch (currentMode) {
                    case SPAWN:
                    case BED:
                        handleTeleport(damager, target);
                        break;
                    case BAN:
                        handleBan(damager, target);
                        break;
                    case KICK:
                        handleKick(damager, target);
                        break;
                }
            }
        }
    }

    private void handleTeleport(Player damager, Player target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                playTeleportEffects(target);
                Location teleportLocation;
                boolean noBedMessageSent = false;

                if (currentMode == Mode.BED) {
                    Location bedLocation = target.getBedSpawnLocation();
                    if (bedLocation != null) {
                        teleportLocation = bedLocation;
                    } else {
                        teleportLocation = target.getWorld().getSpawnLocation();
                        damager.sendMessage(getMessage("player_no_bed_teleported_spawn").replace("%player%", target.getName()));
                        noBedMessageSent = true;
                    }
                } else {
                    teleportLocation = target.getWorld().getSpawnLocation();
                }
                if (target.getWorld().getEnvironment() != World.Environment.NORMAL) {
                    World overworld = Bukkit.getWorlds().get(0);
                    if (overworld != null) {
                        teleportLocation = teleportLocation.clone();
                        teleportLocation.setWorld(overworld);
                    }
                }
                target.teleport(teleportLocation);
                if (!noBedMessageSent) {
                    damager.sendMessage(getMessage("player_teleported_spawn").replace("%player%", target.getName()));
                    target.sendMessage(getMessage("player_teleported_spawn").replace("%player%", target.getName()));
                }
                playTeleportEffects(target);
            }
        }.runTaskLater(this, 1L);
    }


    private void handleBan(Player damager, Player target) {
        int banDuration = getConfig().getInt("banmace_modes.ban_duration", 60);
        target.kickPlayer(getMessage("ban").replace("%player%", target.getName()));
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), getMessage("ban").replace("%player%", target.getName()), new Date(System.currentTimeMillis() + banDuration * 1000L), damager.getName());
    }

    private void handleKick(Player damager, Player target) {
        target.kickPlayer(getMessage("kick").replace("%player%", target.getName()));
    }



    private void playTeleportEffects(Player target) {
        Particle particle = Particle.valueOf(getConfig().getString("teleport_effect.particle", "PORTAL"));
        int count = getConfig().getInt("teleport_effect.count", 100);
        double offset = getConfig().getDouble("teleport_effect.offset", 0.5);
        float speed = (float) getConfig().getDouble("teleport_effect.speed", 0.2);

        target.getWorld().spawnParticle(particle, target.getLocation(), count, offset, offset, offset, speed);
        Sound sound = Sound.valueOf(getConfig().getString("teleport_sound", "ENTITY_ENDERMAN_TELEPORT"));
        target.getWorld().playSound(target.getLocation(), sound, 1.0f, 1.0f);
    }

    private void logHammerUsage(Player damager, Player target) {
        hammerUsageStats.put(damager.getUniqueId(), hammerUsageStats.getOrDefault(damager.getUniqueId(), 0) + 1);
        getLogger().info(getMessage("hammer_used")
                .replace("%damager%", damager.getName())
                .replace("%target%", target.getName())
                .replace("%mode%", currentMode.name()));
    }

    private boolean isBanMace(ItemStack item) {
        if (item == null || item.getType() != HAMMER_MATERIAL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String displayName = meta.getDisplayName();
        if (!HAMMER_NAME.equals(displayName.split(" \\(")[0])) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(MODE_KEY, PersistentDataType.STRING);
    }


    private void cycleMode(Player player) {
        currentMode = Mode.values()[(currentMode.ordinal() + 1) % Mode.values().length];
        updateHammerInInventory(player);
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand != null && itemInHand.getType() == HAMMER_MATERIAL) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null) {
                updateLore(meta, currentMode);
                String displayName = HAMMER_NAME + " (" + currentMode.name() + ")";
                meta.setDisplayName(displayName);

                // Update the mode in PersistentDataContainer
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(MODE_KEY, PersistentDataType.STRING, currentMode.name());

                int customModelData = getConfig().getInt("item_customization.modes." + currentMode.name().toLowerCase(), 0);
                meta.setCustomModelData(customModelData);

                itemInHand.setItemMeta(meta);
                player.getInventory().setItemInMainHand(itemInHand);
            }
        }
        player.sendMessage(getMessage("mode_switched").replace("%mode%", currentMode.name()));

        playCustomEffects(player, "mode_switch");
    }


    private Inventory createModeSelectionGUI() {
        Inventory gui = Bukkit.createInventory(null, 9, "Select Mode");

        for (Mode mode : Mode.values()) {
            ItemStack item = new ItemStack(Material.NETHERITE_AXE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c" + mode.name());
                meta.setLore(Collections.singletonList(getMessage("mode_" + mode.name().toLowerCase())));
                item.setItemMeta(meta);
            }
            gui.addItem(item);
        }

        return gui;
    }

    private void playCustomEffects(Player player, String effectType) {
        Particle particle;
        Sound sound;

        switch (effectType) {
            case "mode_switch":
                particle = Particle.valueOf(getConfig().getString("item_customization.additional_effects.mode_switch_particle", "SPELL_WITCH"));
                sound = Sound.valueOf(getConfig().getString("item_customization.additional_effects.mode_switch_sound", "BLOCK_NOTE_BLOCK_PLING"));
                break;
            default:
                return;
        }

        int count = 50;
        double offset = 0.5;
        float speed = 0.1f;

        player.getWorld().spawnParticle(particle, player.getLocation(), count, offset, offset, offset, speed);

        player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action;

        try {
            action = Action.valueOf(toggleKey);
        } catch (IllegalArgumentException e) {
            getLogger().severe(getMessage("unknown_toggle_action").replace("%toggleKey%", toggleKey));
            return;
        }

        if (event.getHand() == EquipmentSlot.HAND && event.getAction() == action) {
            if (isBanMace(player.getInventory().getItemInMainHand())) {
                cycleMode(player);
            }
        }
    }

    private void updateHammerInInventory(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (isBanMace(itemInHand)) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null) {
                updateLore(meta, currentMode);
                String displayName = HAMMER_NAME + " (" + currentMode.name() + ")";
                meta.setDisplayName(displayName);

                int customModelData = getConfig().getInt("item_customization.modes." + currentMode.name().toLowerCase(), 0);
                meta.setCustomModelData(customModelData);

                itemInHand.setItemMeta(meta);
                player.getInventory().setItemInMainHand(itemInHand);
            }
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Select Mode")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String modeName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            try {
                Mode selectedMode = Mode.valueOf(modeName);
                currentMode = selectedMode;
                updateHammerInInventory(player);
                player.sendMessage(getMessage("mode_switched").replace("%mode%", currentMode.name()));
                player.closeInventory();
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid mode selected!");
            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("banmace") && sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("banmace.give")) {
                player.getInventory().addItem(exampleHammer);
                player.sendMessage(getMessage("mace_received"));
            } else {
                player.sendMessage(getMessage("no_permission"));
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("selectmode") && sender instanceof Player) {
            Player player = (Player) sender;
            Inventory modeSelectionGUI = createModeSelectionGUI();
            player.openInventory(modeSelectionGUI);
            return true;
        }

        if (command.getName().equalsIgnoreCase("setlanguage")) {
            if (!(sender instanceof Player) || sender.hasPermission("banmace.setlanguage")) {
                if (args.length == 1) {
                    String language = args[0].toLowerCase();
                    if (language.equals("en") || language.equals("ru")) {
                        loadMessages(language);
                        sender.sendMessage(getMessage("language_set").replace("%language%", language));
                    } else {
                        sender.sendMessage(getMessage("invalid_language"));
                    }
                } else {
                    sender.sendMessage(getMessage("language_usage"));
                }
            } else {
                sender.sendMessage(getMessage("no_permission"));
            }
            return true;
        }

        return false;
    }


    private void loadMessages(String language) {
        String fileName = "messages_" + language + ".yml";
        File languageFile = new File(getDataFolder(), fileName);

        if (!languageFile.exists()) {
            saveResource(fileName, false);
        }

        messages = YamlConfiguration.loadConfiguration(languageFile);
        currentLanguage = language;
        getLogger().info("Loaded language: " + language);
    }



    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (isBanMace(item) && !player.hasPermission("banmace.give")) {
            event.setCancelled(true);
            event.getItem().remove();
            player.sendMessage(getMessage("item_removed_no_permission"));
        }
    }
}
