package org.nakolotnik.banMace.utils;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nakolotnik.banMace.BanMace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SettingsGUI implements Listener {

    private final BanMace plugin;

    private String getMainGuiTitle() { return plugin.getMessage("gui.title.main"); }
    private String getParticleGuiTitle() { return plugin.getMessage("gui.title.particle"); }
    private String getSoundGuiTitle() { return plugin.getMessage("gui.title.sound"); }
    private String getItemGuiTitle() { return plugin.getMessage("gui.title.item"); }
    private String getConfirmGuiTitle() { return plugin.getMessage("gui.title.confirm"); }

    private final Map<UUID, PlayerSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerInputMap = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> pendingAnimations = new ConcurrentHashMap<>();

    private final List<Material> ALLOWED_MATERIALS;
    private final List<Particle> particleList;
    private final List<Sound> soundList;

    private static final int[] BORDER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};

    public SettingsGUI(BanMace plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        ALLOWED_MATERIALS = Arrays.stream(Material.values())
                .filter(this::isAllowedMaterial)
                .sorted(Comparator.comparing(m -> {
                    NamespacedKey key = Registry.MATERIAL.getKey(m);
                    return key != null ? key.value() : "zzz";
                }))
                .collect(Collectors.toList());

        particleList = Registry.PARTICLE_TYPE.stream().filter(Objects::nonNull).collect(Collectors.toList());
        soundList = Registry.SOUNDS.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isAllowedMaterial(Material material) {
        NamespacedKey key = Registry.MATERIAL.getKey(material);
        if (key == null || !material.isItem()) return false;
        String keyStr = key.value();
        return (keyStr.endsWith("_sword") || keyStr.endsWith("_axe") ||
                keyStr.endsWith("_pickaxe") || keyStr.endsWith("_shovel") ||
                keyStr.endsWith("_hoe") || material == Material.MACE ||
                material == Material.TRIDENT || material == Material.STICK ||
                material == Material.BLAZE_ROD) && !keyStr.startsWith("wooden_axe");
    }

    private static class PlayerSession {
        private int particlePage = 0, soundPage = 0, itemPage = 0;
        private String confirmAction = null;
    }

    private PlayerSession getOrCreateSession(UUID uuid) {
        return playerSessions.computeIfAbsent(uuid, k -> new PlayerSession());
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, getMainGuiTitle());
        initializeMainItems(gui, player);
        player.openInventory(gui);
        startInventoryAnimation(player, gui);
    }

    private void startInventoryAnimation(Player player, Inventory gui) {
        BukkitRunnable animation = new BukkitRunnable() {
            private int ticks = 0;
            @Override
            public void run() {
                if (ticks > 20 || !player.getOpenInventory().getTopInventory().equals(gui)) {
                    cancel();
                    return;
                }
                if (ticks % 10 == 0) {
                    ItemStack animBorder = createGuiItem(
                            ticks % 20 == 0 ? Material.ORANGE_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE,
                            "§8"
                    );
                    gui.setItem(0, animBorder);
                    gui.setItem(8, animBorder);
                    gui.setItem(45, animBorder);
                    gui.setItem(53, animBorder);
                }
                ticks++;
            }
        };
        animation.runTaskTimer(plugin, 0L, 2L);
        pendingAnimations.put(player.getUniqueId(), animation);
    }

    private void initializeMainItems(Inventory gui, Player player) {
        gui.clear();
        fillBorders(gui);

        String currentLang = plugin.getLanguageManager().getCurrentLanguage();
        String currentView = plugin.getConfig().getString("message_display_mode", "chat");
        boolean whitelistOn = plugin.getConfig().getBoolean("whitelist.enabled");
        boolean updatesOn = plugin.getConfig().getBoolean("check_for_updates");
        String currentParticle = plugin.getConfig().getString("teleport_effect.particle", "portal");
        String currentSound = plugin.getConfig().getString("teleport_sound", "entity.enderman.teleport");
        String webhookUrl = plugin.getConfig().getString("logging.discord.webhook_url", "");
        boolean discordLogOn = plugin.getConfig().getBoolean("logging.discord.enabled");

        String statusEnabled = plugin.getMessage("gui.status.enabled");
        String statusDisabled = plugin.getMessage("gui.status.disabled");
        String webhookConfigured = plugin.getMessage("gui.webhook.configured");
        String webhookNotConfigured = plugin.getMessage("gui.webhook.not_configured");

        List<String> availableLangs = plugin.getLanguageManager().getAvailableLanguages();
        String formattedLangs = availableLangs.stream()
                .map(lang -> (lang.equals(currentLang) ? "§b§l" : "§b") + lang.toUpperCase())
                .collect(Collectors.joining(" §8| "));

        gui.setItem(10, createGuiItem(Material.ENCHANTED_BOOK, plugin.getMessage("gui.main.language.title"),
                plugin.getMessage("gui.main.lore.current", Map.of("value", currentLang.toUpperCase())),
                plugin.getMessage("gui.main.language.lore_1"),
                plugin.getMessage("gui.main.language.lore_available_title"),
                plugin.getMessage("gui.main.language.lore_languages_format", Map.of("languages", formattedLangs)),
                plugin.getMessage("gui.main.language.lore_4"),
                "",
                plugin.getMessage("gui.main.lore.click_to_cycle")));

        gui.setItem(11, createGuiItem(Material.OAK_SIGN, plugin.getMessage("gui.main.display_mode.title"),
                plugin.getMessage("gui.main.lore.current", Map.of("value", currentView.toUpperCase())),
                plugin.getMessage("gui.main.display_mode.lore_1"),
                plugin.getMessage("gui.main.display_mode.lore_2"),
                plugin.getMessage("gui.main.display_mode.lore_3"),
                plugin.getMessage("gui.main.display_mode.lore_4"),
                plugin.getMessage("gui.main.display_mode.lore_5"),
                "",
                plugin.getMessage("gui.main.lore.click_to_cycle")));

        gui.setItem(12, createGuiItem(whitelistOn ? Material.LIME_WOOL : Material.RED_WOOL, plugin.getMessage("gui.main.whitelist.title"),
                plugin.getMessage("gui.main.lore.status", Map.of("status", whitelistOn ? statusEnabled : statusDisabled)), "",
                plugin.getMessage("gui.main.whitelist.lore_1"),
                plugin.getMessage("gui.main.whitelist.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_toggle")));

        gui.setItem(13, createGuiItem(updatesOn ? Material.LIME_DYE : Material.GRAY_DYE, plugin.getMessage("gui.main.update_checker.title"),
                plugin.getMessage("gui.main.lore.status", Map.of("status", updatesOn ? statusEnabled : statusDisabled)), "",
                plugin.getMessage("gui.main.update_checker.lore_1"),
                plugin.getMessage("gui.main.update_checker.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_toggle")));

        gui.setItem(15, createGuiItem(BanMace.getBanMaceMaterial(), plugin.getMessage("gui.main.mace_item.title"),
                plugin.getMessage("gui.main.lore.current", Map.of("value", formatMaterialName(BanMace.getBanMaceMaterial()))), "",
                plugin.getMessage("gui.main.mace_item.lore_1"),
                plugin.getMessage("gui.main.mace_item.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_open")));

        gui.setItem(16, createGuiItem(Material.NETHER_STAR, plugin.getMessage("gui.main.teleport_effects.title"),
                plugin.getMessage("gui.main.lore.particle", Map.of("value", formatEnumName(currentParticle))), "",
                plugin.getMessage("gui.main.teleport_effects.lore_1"),
                plugin.getMessage("gui.main.teleport_effects.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_browse"),
                plugin.getMessage("gui.main.lore.right_click_preview")));

        gui.setItem(19, createGuiItem(Material.NOTE_BLOCK, plugin.getMessage("gui.main.sound_effects.title"),
                plugin.getMessage("gui.main.lore.sound", Map.of("value", formatEnumName(currentSound))), "",
                plugin.getMessage("gui.main.sound_effects.lore_1"),
                plugin.getMessage("gui.main.sound_effects.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_browse"),
                plugin.getMessage("gui.main.lore.right_click_preview")));

        gui.setItem(20, createGuiItem(Material.WRITABLE_BOOK, plugin.getMessage("gui.main.discord_integration.title"),
                plugin.getMessage("gui.main.lore.webhook", Map.of("status", webhookUrl.isEmpty() ? webhookNotConfigured : webhookConfigured)), "",
                plugin.getMessage("gui.main.discord_integration.lore_1"),
                plugin.getMessage("gui.main.discord_integration.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_configure")));

        gui.setItem(21, createGuiItem(discordLogOn ? Material.LIME_DYE : Material.GRAY_DYE, plugin.getMessage("gui.main.discord_logging.title"),
                plugin.getMessage("gui.main.lore.status", Map.of("status", discordLogOn ? statusEnabled : statusDisabled)), "",
                plugin.getMessage("gui.main.discord_logging.lore_1"),
                plugin.getMessage("gui.main.discord_logging.lore_2"), "",
                plugin.getMessage("gui.main.lore.click_to_toggle")));

        gui.setItem(49, createGuiItem(Material.BARRIER, plugin.getMessage("gui.main.close.title"),
                plugin.getMessage("gui.main.close.lore_1"), "",
                plugin.getMessage("gui.main.lore.click_to_close")));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(getMainGuiTitle()) && !title.equals(getParticleGuiTitle()) &&
                !title.equals(getSoundGuiTitle()) && !title.equals(getItemGuiTitle()) && !title.equals(getConfirmGuiTitle())) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        PlayerSession session = getOrCreateSession(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        if (title.equals(getMainGuiTitle())) handleMainGUIClick(event, player, session);
        else if (title.equals(getParticleGuiTitle())) handleParticleGUIClick(event, player, session);
        else if (title.equals(getSoundGuiTitle())) handleSoundGUIClick(event, player, session);
        else if (title.equals(getItemGuiTitle())) handleItemGUIClick(event, player, session);
        else if (title.equals(getConfirmGuiTitle())) handleConfirmationGUIClick(event, player, session);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BukkitRunnable animation = pendingAnimations.remove(uuid);
        if (animation != null) {
            animation.cancel();
        }
    }

    private void handleMainGUIClick(InventoryClickEvent event, Player player, PlayerSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        int slot = event.getSlot();
        switch (slot) {
            case 10:
                cycleLanguage(player);
                return;
            case 11:
                cycleDisplayMode(player);
                break;
            case 12:
                session.confirmAction = "toggle_whitelist";
                openConfirmationGUI(player, "toggle_whitelist", plugin.getMessage("gui.main.whitelist.title"));
                return;
            case 13:
                toggleBoolean(player, "check_for_updates", plugin.getMessage("gui.main.update_checker.title"));
                break;
            case 15:
                openItemMenu(player, session, 0);
                return;
            case 16:
                if (event.isRightClick()) previewCurrentParticle(player);
                else openParticleMenu(player, session, 0);
                return;
            case 19:
                if (event.isRightClick()) previewCurrentSound(player);
                else openSoundMenu(player, session, 0);
                return;
            case 20:
                session.confirmAction = "edit_webhook";
                openConfirmationGUI(player, "edit_webhook", plugin.getMessage("gui.main.discord_integration.title"));
                return;
            case 21:
                toggleBoolean(player, "logging.discord.enabled", plugin.getMessage("gui.main.discord_logging.title"));
                break;
            case 49:
                player.closeInventory();
                player.sendMessage(plugin.getMessage("gui.message.settings_saved"));
                return;
            default: return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTitle().equals(getMainGuiTitle())) {
                initializeMainItems(event.getInventory(), player);
            }
        });
    }

    private void cycleLanguage(Player player) {
        LanguageManager langManager = plugin.getLanguageManager();
        List<String> languages = langManager.getAvailableLanguages();
        if (languages.isEmpty()) {
            player.sendMessage(plugin.getMessage("no_languages_available"));
            return;
        }
        String currentLang = langManager.getCurrentLanguage();
        int currentIndex = languages.indexOf(currentLang);
        String newLang = languages.get((currentIndex + 1) % languages.size());

        plugin.getConfig().set("language", newLang);
        plugin.saveConfig();

        langManager.loadMessages(newLang);

        player.sendMessage(plugin.getMessage("gui.message.language_changed", Map.of("language", newLang.toUpperCase())));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);

        open(player);
    }

    private void previewCurrentParticle(Player player) {
        String particleKey = plugin.getConfig().getString("teleport_effect.particle", "portal");
        Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.fromString(particleKey));
        if (particle != null) {
            spawnParticleSafely(player, particle, player.getLocation().add(0, 1, 0));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            player.sendMessage(plugin.getMessage("gui.message.preview_particle", Map.of("particle", formatEnumName(particleKey))));
        }
    }

    private void previewCurrentSound(Player player) {
        String soundKey = plugin.getConfig().getString("teleport_sound", "entity.enderman.teleport");
        Sound sound = Registry.SOUNDS.get(NamespacedKey.fromString(soundKey));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            player.sendMessage(plugin.getMessage("gui.message.preview_sound", Map.of("sound", formatEnumName(soundKey))));
        }
    }

    private void spawnParticleSafely(Player player, Particle particle, Location location) {
        try {
            if (particle.getDataType() == Void.class) {
                player.getWorld().spawnParticle(particle, location, 30, 0.5, 0.5, 0.5, 0.1);
            } else if (particle.getDataType() == Particle.DustOptions.class) {
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1.0F);
                player.getWorld().spawnParticle(particle, location, 30, 0.5, 0.5, 0.5, dustOptions);
            } else if (particle.getDataType() == Particle.DustTransition.class) {
                Particle.DustTransition dustTransition = new Particle.DustTransition(Color.RED, Color.YELLOW, 1.0F);
                player.getWorld().spawnParticle(particle, location, 30, 0.5, 0.5, 0.5, dustTransition);
            } else if (particle.getDataType() == ItemStack.class) {
                player.getWorld().spawnParticle(particle, location, 30, 0.5, 0.5, 0.5, new ItemStack(Material.STONE));
            } else {
                player.getWorld().spawnParticle(particle, location, 30, 0.5, 0.5, 0.5, 0.1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not spawn preview for particle " + particle.name() + ": " + e.getMessage());
            player.sendMessage(plugin.getMessage("gui.message.preview_failed", Map.of("particle", particle.name())));
        }
    }

    private void openItemMenu(Player player, PlayerSession session, int page) {
        session.itemPage = page;
        Inventory gui = Bukkit.createInventory(null, 54, getItemGuiTitle());
        int itemsPerPage = 36;
        int startIndex = page * itemsPerPage;
        fillSelectionBorders(gui);

        int slot = 9;
        for (int i = 0; i < itemsPerPage && startIndex + i < ALLOWED_MATERIALS.size(); i++) {
            Material material = ALLOWED_MATERIALS.get(startIndex + i);
            gui.setItem(slot++, createGuiItem(material, "§c🔨 " + formatMaterialName(material), "§7Click to select this item."));
        }
        addNavigationButtons(gui, page, ALLOWED_MATERIALS.size(), itemsPerPage);
        player.openInventory(gui);
    }

    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            if (material == Material.ENCHANTED_BOOK || material == Material.NETHER_STAR ||
                    material == Material.LIME_WOOL || material == Material.LIME_DYE) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorders(Inventory gui) {
        ItemStack borderItem = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        ItemStack accentItem = createGuiItem(Material.ORANGE_STAINED_GLASS_PANE, "§8");
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, (slot == 0 || slot == 8 || slot == 45 || slot == 53) ? accentItem : borderItem);
        }
    }

    private void fillSelectionBorders(Inventory gui) {
        ItemStack borderItem = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < 9; i++) gui.setItem(i, borderItem);
        for (int i = 45; i < 54; i++) gui.setItem(i, borderItem);
    }

    private void handleParticleGUIClick(InventoryClickEvent event, Player player, PlayerSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        String displayName = meta.getDisplayName();

        if (clickedItem.getType() == Material.ARROW) {
            if (displayName.equals(plugin.getMessage("gui.selection.page_next"))) openParticleMenu(player, session, session.particlePage + 1);
            else if (displayName.equals(plugin.getMessage("gui.selection.page_prev"))) openParticleMenu(player, session, session.particlePage - 1);
        } else if (clickedItem.getType() == Material.BARRIER) {
            open(player);
        } else if (clickedItem.getType() == Material.GLOWSTONE_DUST || clickedItem.getType() == Material.GLOWSTONE) {
            String particleKey = ChatColor.stripColor(displayName).replace("✨ ", "").trim();
            if (event.isLeftClick()) {
                plugin.getConfig().set("teleport_effect.particle", particleKey);
                plugin.saveConfig();
                player.sendMessage(plugin.getMessage("gui.message.particle_set", Map.of("particle", formatEnumName(particleKey))));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 20L);
            } else if (event.isRightClick()) {
                Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.fromString(particleKey));
                if (particle != null) {
                    spawnParticleSafely(player, particle, player.getLocation().add(0, 1, 0));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                    player.sendMessage(plugin.getMessage("gui.message.preview_particle", Map.of("particle", formatEnumName(particleKey))));
                }
            }
        }
    }

    private void handleSoundGUIClick(InventoryClickEvent event, Player player, PlayerSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        String displayName = meta.getDisplayName();

        if (clickedItem.getType() == Material.ARROW) {
            if (displayName.equals(plugin.getMessage("gui.selection.page_next"))) openSoundMenu(player, session, session.soundPage + 1);
            else if (displayName.equals(plugin.getMessage("gui.selection.page_prev"))) openSoundMenu(player, session, session.soundPage - 1);
        } else if (clickedItem.getType() == Material.BARRIER) {
            open(player);
        } else if (clickedItem.getType() == Material.NOTE_BLOCK || clickedItem.getType() == Material.JUKEBOX) {
            String soundKey = ChatColor.stripColor(displayName).replace("🔊 ", "").trim();
            Sound sound = Registry.SOUNDS.get(NamespacedKey.fromString(soundKey));
            if (sound == null) return;
            if (event.isLeftClick()) {
                plugin.getConfig().set("teleport_sound", soundKey);
                plugin.saveConfig();
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                player.sendMessage(plugin.getMessage("gui.message.sound_set", Map.of("sound", formatEnumName(soundKey))));
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 20L);
            } else if (event.isRightClick()) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                player.sendMessage(plugin.getMessage("gui.message.preview_sound", Map.of("sound", formatEnumName(soundKey))));
            }
        }
    }

    private void handleItemGUIClick(InventoryClickEvent event, Player player, PlayerSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        String displayName = meta.getDisplayName();

        if (clickedItem.getType() == Material.ARROW) {
            if (displayName.equals(plugin.getMessage("gui.selection.page_next"))) openItemMenu(player, session, session.itemPage + 1);
            else if (displayName.equals(plugin.getMessage("gui.selection.page_prev"))) openItemMenu(player, session, session.itemPage - 1);
        } else if (clickedItem.getType() == Material.BARRIER) {
            open(player);
        } else if (ALLOWED_MATERIALS.contains(clickedItem.getType())) {
            Material selectedMaterial = clickedItem.getType();
            Material oldMaterial = BanMace.getBanMaceMaterial();

            if (oldMaterial == selectedMaterial) {
                player.sendMessage(plugin.getMessage("gui.message.item_already_set"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
                return;
            }

            NamespacedKey key = Registry.MATERIAL.getKey(selectedMaterial);
            if (key != null) {
                plugin.getConfig().set("item_customization.item_material", key.value());
                plugin.saveConfig();
                plugin.reloadBanMaceMaterial();

                int updatedCount = replaceExistingMaces(oldMaterial, selectedMaterial);

                player.sendMessage(plugin.getMessage("gui.message.item_set", Map.of("item", formatMaterialName(selectedMaterial))));
                if (updatedCount > 0) {
                    player.sendMessage(plugin.getMessage("gui.message.maces_updated_globally", Map.of("count", String.valueOf(updatedCount))));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.2f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 20L);
            }
        }
    }

    private int replaceExistingMaces(Material oldMaterial, Material newMaterial) {
        int updatedCount = 0;
        NamespacedKey maceKey = new NamespacedKey(plugin, "ban_mace");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Inventory inventory = onlinePlayer.getInventory();
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);

                if (item == null || item.getType() != oldMaterial || !item.hasItemMeta()) {
                    continue;
                }

                ItemMeta meta = item.getItemMeta();
                if (meta.getPersistentDataContainer().has(maceKey, PersistentDataType.STRING)) {
                    ItemStack newMace = new ItemStack(newMaterial);
                    newMace.setItemMeta(meta);

                    inventory.setItem(i, newMace);
                    updatedCount++;

                    onlinePlayer.sendMessage(plugin.getMessage("gui.message.mace_updated"));
                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.5f);
                }
            }
        }
        return updatedCount;
    }

    private void handleConfirmationGUIClick(InventoryClickEvent event, Player player, PlayerSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String actionKey = session.confirmAction;
        if (actionKey == null) {
            open(player);
            return;
        }

        if (clickedItem.getType() == Material.LIME_WOOL) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            switch (actionKey) {
                case "toggle_whitelist":
                    toggleBoolean(player, "whitelist.enabled", plugin.getMessage("gui.main.whitelist.title"));
                    open(player);
                    break;
                case "edit_webhook":
                    promptForInput(player, "edit_webhook", plugin.getMessage("gui.prompt.webhook"));
                    break;
                default:
                    player.sendMessage(plugin.getMessage("unknown_action", Map.of("action", actionKey)));
                    open(player);
                    break;
            }
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            player.sendMessage(plugin.getMessage("gui.message.operation_cancelled"));
            open(player);
        }
        session.confirmAction = null;
    }

    private void cycleDisplayMode(Player player) {
        List<String> modes = Arrays.asList("chat", "actionbar", "title");
        String currentMode = plugin.getConfig().getString("message_display_mode", "chat");
        int currentIndex = modes.indexOf(currentMode);
        String newMode = modes.get((currentIndex + 1) % modes.size());
        plugin.getConfig().set("message_display_mode", newMode);
        plugin.saveConfig();
        player.sendMessage(plugin.getMessage("gui.message.display_mode_changed", Map.of("mode", newMode)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
        switch (newMode) {
            case "actionbar": player.sendActionBar(plugin.getMessage("gui.message.example_actionbar")); break;
            case "title": player.sendTitle(plugin.getMessage("gui.message.example_title_main"), plugin.getMessage("gui.message.example_title_sub"), 10, 40, 10); break;
            case "chat": player.sendMessage(plugin.getMessage("gui.message.example_chat")); break;
        }
    }

    private void openConfirmationGUI(Player player, String actionKey, String actionName) {
        PlayerSession session = getOrCreateSession(player.getUniqueId());
        session.confirmAction = actionKey;
        Inventory gui = Bukkit.createInventory(null, 27, getConfirmGuiTitle());
        gui.setItem(13, createGuiItem(Material.PAPER, plugin.getMessage("gui.confirm.title"),
                plugin.getMessage("gui.confirm.action", Map.of("action", actionName)), "",
                plugin.getMessage("gui.confirm.question"),
                plugin.getMessage("gui.confirm.warning"), "",
                plugin.getMessage("gui.confirm.choose")));
        gui.setItem(11, createGuiItem(Material.LIME_WOOL, plugin.getMessage("gui.confirm.button_yes_title"),
                plugin.getMessage("gui.confirm.button_yes_lore", Map.of("action", actionName))));
        gui.setItem(15, createGuiItem(Material.RED_WOOL, plugin.getMessage("gui.confirm.button_no_title"),
                plugin.getMessage("gui.confirm.button_no_lore")));
        fillConfirmationBorders(gui);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
    }

    private void fillConfirmationBorders(Inventory gui) {
        ItemStack borderItem = createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
            gui.setItem(i + 18, borderItem);
        }
        gui.setItem(9, borderItem);
        gui.setItem(17, borderItem);
    }

    private void toggleBoolean(Player player, String configPath, String settingName) {
        boolean currentValue = plugin.getConfig().getBoolean(configPath);
        boolean newValue = !currentValue;
        plugin.getConfig().set(configPath, newValue);
        plugin.saveConfig();
        String status = newValue ? plugin.getMessage("gui.status.enabled") : plugin.getMessage("gui.status.disabled");
        player.sendMessage(plugin.getMessage("gui.message.setting_toggled", Map.of("setting", settingName, "status", status)));
        player.playSound(player.getLocation(), newValue ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, newValue ? 1.5f : 0.8f);
        if (configPath.equals("logging.discord.enabled") && newValue) {
            String webhookUrl = plugin.getConfig().getString("logging.discord.webhook_url", "");
            if (webhookUrl.isEmpty()) {
                player.sendMessage(plugin.getMessage("gui.message.webhook_not_set_warning"));
            }
        }
    }

    private void openParticleMenu(Player player, PlayerSession session, int page) {
        session.particlePage = Math.max(0, Math.min(page, (particleList.size() - 1) / 36));
        Inventory gui = Bukkit.createInventory(null, 54, getParticleGuiTitle());
        int itemsPerPage = 36;
        int startIndex = session.particlePage * itemsPerPage;
        fillSelectionBorders(gui);
        int slot = 9;
        for (int i = 0; i < itemsPerPage && startIndex + i < particleList.size(); i++) {
            Particle particle = particleList.get(startIndex + i);
            NamespacedKey particleKey = Registry.PARTICLE_TYPE.getKey(particle);
            if (particleKey != null) {
                String particleName = particleKey.value();
                String currentParticle = plugin.getConfig().getString("teleport_effect.particle", "portal");
                boolean isSelected = particleName.equals(currentParticle);
                gui.setItem(slot++, createGuiItem(isSelected ? Material.GLOWSTONE : Material.GLOWSTONE_DUST, (isSelected ? "§a§l✓ " : "§d") + "✨ " + particleName, isSelected ? plugin.getMessage("gui.selection.currently_selected") : plugin.getMessage("gui.selection.click_to_select"), plugin.getMessage("gui.main.lore.right_click_preview")));
            }
        }
        addNavigationButtons(gui, session.particlePage, particleList.size(), itemsPerPage);
        player.openInventory(gui);
    }

    private void openSoundMenu(Player player, PlayerSession session, int page) {
        session.soundPage = Math.max(0, Math.min(page, (soundList.size() - 1) / 36));
        Inventory gui = Bukkit.createInventory(null, 54, getSoundGuiTitle());
        int itemsPerPage = 36;
        int startIndex = session.soundPage * itemsPerPage;
        fillSelectionBorders(gui);
        int slot = 9;
        for (int i = 0; i < itemsPerPage && startIndex + i < soundList.size(); i++) {
            Sound sound = soundList.get(startIndex + i);
            NamespacedKey soundKey = Registry.SOUNDS.getKey(sound);
            if (soundKey != null) {
                String soundName = soundKey.value();
                String currentSound = plugin.getConfig().getString("teleport_sound", "entity.enderman.teleport");
                boolean isSelected = soundName.equals(currentSound);
                gui.setItem(slot++, createGuiItem(isSelected ? Material.JUKEBOX : Material.NOTE_BLOCK, (isSelected ? "§a§l✓ " : "§b") + "🔊 " + soundName, isSelected ? plugin.getMessage("gui.selection.currently_selected") : plugin.getMessage("gui.selection.click_to_select"), plugin.getMessage("gui.main.lore.right_click_preview")));
            }
        }
        addNavigationButtons(gui, session.soundPage, soundList.size(), itemsPerPage);
        player.openInventory(gui);
    }

    private void addNavigationButtons(Inventory gui, int page, int totalItems, int itemsPerPage) {
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (page > 0) gui.setItem(45, createGuiItem(Material.ARROW, plugin.getMessage("gui.selection.page_prev")));
        if ((page + 1) * itemsPerPage < totalItems) gui.setItem(53, createGuiItem(Material.ARROW, plugin.getMessage("gui.selection.page_next")));
        gui.setItem(49, createGuiItem(Material.PAPER, plugin.getMessage("gui.selection.page_info_title"),
                plugin.getMessage("gui.selection.page_info_current", Map.of("current", String.valueOf(page + 1))),
                plugin.getMessage("gui.selection.page_info_total", Map.of("total", String.valueOf(maxPages))),
                plugin.getMessage("gui.selection.page_info_items", Map.of("items", String.valueOf(totalItems)))));
        gui.setItem(46, createGuiItem(Material.BARRIER, plugin.getMessage("gui.selection.back_button")));
    }

    private void promptForInput(Player player, String key, String prompt) {
        playerInputMap.put(player.getUniqueId(), key);
        player.closeInventory();
        String border = "§8" + "=".repeat(50);
        player.sendMessage(border);
        player.sendMessage(prompt);
        player.sendMessage(border);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!playerInputMap.containsKey(uuid)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        String settingKey = playerInputMap.remove(uuid);

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getMessage("gui.message.operation_cancelled"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            Bukkit.getScheduler().runTask(plugin, () -> open(player));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if ("edit_webhook".equals(settingKey)) handleWebhookInput(player, input);
            else player.sendMessage(plugin.getMessage("unknown_input_type", Map.of("type", settingKey)));
            open(player);
        });
    }

    private void handleWebhookInput(Player player, String input) {
        if (input.startsWith("https://discord.com/api/webhooks/") ||
                input.startsWith("https://discordapp.com/api/webhooks/")) {
            if (input.split("/").length >= 7) {
                plugin.getConfig().set("logging.discord.webhook_url", input);
                plugin.saveConfig();
                if (plugin.getLoggerService() != null) plugin.getLoggerService().reloadWebhook();
                player.sendMessage(plugin.getMessage("gui.message.webhook_success"));
                player.sendMessage(plugin.getMessage("gui.message.webhook_url_display", Map.of("url", input.length() > 50 ? input.substring(0, 50) + "..." : input)));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            } else {
                player.sendMessage(plugin.getMessage("gui.message.webhook_invalid_format"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            }
        } else {
            player.sendMessage(plugin.getMessage("gui.message.webhook_invalid_url"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
        }
    }

    private String formatMaterialName(Material material) {
        NamespacedKey key = Registry.MATERIAL.getKey(material);
        if (key == null) return "Unknown Material";
        String[] words = key.value().replace("minecraft:", "").replace("_", " ").split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return builder.toString().trim();
    }

    private String formatEnumName(String enumName) {
        return enumName.toLowerCase(Locale.ROOT).replace("_", " ").replace(".", " ");
    }
}