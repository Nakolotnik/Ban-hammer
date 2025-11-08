package org.nakolotnik.banMace;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.nakolotnik.banMace.utils.*;
import org.nakolotnik.banMace.utils.mods.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BanMace extends JavaPlugin {
    private static ModeHandler currentMode;
    private static BanMace instance;
    private LanguageManager languageManager;
    private static Material banMaceMaterial;
    private SettingsGUI settingsGUI;
    private LoggerService loggerService;
    private FreezeMode freezeModeInstance;
    private BalloonMod balloonModInstance;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        loggerService = new LoggerService(this);
        languageManager = new LanguageManager(this);
        settingsGUI = new SettingsGUI(this);
        freezeModeInstance = new FreezeMode();
        balloonModInstance = new BalloonMod();

        languageManager.checkAndLoadLanguageFiles();
        reloadAllConfigs();
        getServer().getPluginManager().registerEvents(new RadialMenuManager(this), this);

        try {
            BanMaceMode initialMode = BanMaceMode.valueOf(getConfig().getString("active_mode", "SPAWN"));
            setMode(null, initialMode.getHandler());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid mode in config: " + getConfig().getString("active_mode", "SPAWN") + ". Defaulting to SPAWN mode.");
            setMode(null, new SpawnMode());
        }

        Listener[] listeners = { new SpawnMode(), new BedMode(), new BanMode(),
                new KickMode(), new ModeSwitcher(), new ItemPickupListener(this),
                freezeModeInstance, new TeleportToMode(), balloonModInstance };
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        CommandHandler commandHandler = new CommandHandler(this);
        String[] commands = {"bm-give", "bm-setlanguage", "bm-changeview", "bm-tpcoordinates", "bm-help", "bm-settings"};
        for (String cmdName : commands) {
            var command = getCommand(cmdName);
            if (command != null) {
                command.setExecutor(commandHandler);
                command.setTabCompleter(commandHandler);
            } else {
                getLogger().warning("Command '" + cmdName + "' not found in plugin.yml! It will not work.");
            }
        }
        if (getConfig().getBoolean("check_for_updates", true)) {
            new UpdateChecker(this).checkForUpdates();
        }
    }

    public void reloadAllConfigs() {
        reloadConfig();
        reloadBanMaceMaterial();
        languageManager.loadMessages(getConfig().getString("language", "en"));
        if (loggerService != null) loggerService.reloadWebhook();
    }

    public void reloadBanMaceMaterial() {
        String materialName = getConfig().getString("item_customization.item_material", "mace");
        Material newMaterial = getMaterialFromString(materialName);
        if (newMaterial != null) banMaceMaterial = newMaterial;
        else {
            getLogger().warning("Invalid material in config.yml: '" + materialName + "'. Defaulting to MACE.");
            banMaceMaterial = Material.MACE;
        }
    }

    @SuppressWarnings("deprecation")
    private Material getMaterialFromString(String materialString) {
        if (materialString == null || materialString.isEmpty()) return null;
        NamespacedKey key = NamespacedKey.fromString(materialString.toLowerCase(Locale.ROOT));
        if (key != null) {
            Material material = Registry.MATERIAL.get(key);
            if (material != null) return material;
        }
        try { return Material.valueOf(materialString.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    public SettingsGUI getSettingsGUI() { return settingsGUI; }
    public LoggerService getLoggerService() { return loggerService; }
    public static Material getBanMaceMaterial() { return banMaceMaterial; }
    public static ModeHandler getCurrentMode() { return currentMode; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public static BanMace getInstance() { return instance; }

    public static void setMode(Player player, ModeHandler mode) {
        currentMode = mode;
        BanMace plugin = getInstance();
        for (BanMaceMode banMaceMode : BanMaceMode.values()) {
            if (banMaceMode.getHandler().getClass().equals(mode.getClass())) {
                plugin.getConfig().set("active_mode", banMaceMode.name());
                plugin.saveConfig();
                if (player != null && isHoldingBanMace(player)) {
                    plugin.updateMaceItem(player.getInventory().getItemInMainHand(), banMaceMode);
                }
                break;
            }
        }
    }

    public String getMessage(String key, Map<String, String> placeholders) { return languageManager.getMessage(key, placeholders); }
    public String getMessage(String key) { return languageManager.getMessage(key, null); }

    @Override
    public void onDisable() {
        if (freezeModeInstance != null) {
            freezeModeInstance.removeAllIceEffects();
            getLogger().info("Cleared all active freeze effects.");
        }
        if (balloonModInstance != null) {
            balloonModInstance.cleanup();
            getLogger().info("Cleared all active balloon effects.");
        }
        instance = null;
    }

    public ItemStack createBanMace() {
        ItemStack mace = new ItemStack(getBanMaceMaterial());
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "ban_mace"), PersistentDataType.STRING, "unique_ban_mace");
            mace.setItemMeta(meta);

            try {
                BanMaceMode initialMode = BanMaceMode.valueOf(getConfig().getString("active_mode", "SPAWN"));
                updateMaceItem(mace, initialMode);
            } catch (IllegalArgumentException e) {
                updateMaceItem(mace, BanMaceMode.SPAWN);
            }
        }
        return mace;
    }

    public void updateMaceItem(ItemStack mace, BanMaceMode mode) {
        if (mace == null || mace.getItemMeta() == null) return;
        ItemMeta meta = mace.getItemMeta();

        String translatedModeName = getMessage("mode_" + mode.name().toLowerCase());
        String nameColor = getConfig().getString("item_customization.name_color", "§7");
        String baseName = getConfig().getString("item_customization.bm-name", "Ban Mace");
        meta.setDisplayName(String.format("%s%s §7(§e%s§7)", nameColor, baseName, translatedModeName));

        List<String> newLore = new ArrayList<>(getConfig().getStringList("item_customization.lore"));
        List<String> modeLore = languageManager.getMessageList("lore." + mode.name().toLowerCase());
        if (modeLore != null && !modeLore.isEmpty()) {
            newLore.add("");
            newLore.addAll(modeLore);
        }
        meta.setLore(newLore);

        mace.setItemMeta(meta);
    }

    public enum BanMaceMode {
        SPAWN(new SpawnMode()), BED(new BedMode()), BAN(new BanMode()), KICK(new KickMode()), FREEZE(new FreezeMode()), TELEPORT_TO(new TeleportToMode()), BALLOON(new BalloonMod());
        private final ModeHandler handler;
        BanMaceMode(ModeHandler handler) { this.handler = handler; }
        public ModeHandler getHandler() { return handler; }
    }

    public void playTeleportEffects(Player target) {
        Particle particle = getParticleFromString(getConfig().getString("teleport_effect.particle", "portal"));
        Sound sound = getSoundFromString(getConfig().getString("teleport_sound", "entity.enderman.teleport"));

        if (particle != null) {
            int count = getConfig().getInt("teleport_effect.count", 100);
            double offset = getConfig().getDouble("teleport_effect.offset", 0.5);
            float speed = (float) getConfig().getDouble("teleport_effect.speed", 0.2);
            target.getWorld().spawnParticle(particle, target.getLocation().add(0, 1, 0), count, offset, offset, offset, speed);
        } else {
            getLogger().warning("Invalid particle in config: " + getConfig().getString("teleport_effect.particle"));
        }
        if (sound != null) {
            target.getWorld().playSound(target.getLocation(), sound, 1.0f, 1.0f);
        } else {
            getLogger().warning("Invalid sound in config: " + getConfig().getString("teleport_sound"));
        }
    }

    @SuppressWarnings("deprecation")
    public Sound getSoundFromString(String soundString) {
        if (soundString == null || soundString.isEmpty()) return null;
        NamespacedKey key = NamespacedKey.fromString(soundString.toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) return sound;
        }
        try { return Sound.valueOf(soundString.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    @SuppressWarnings("deprecation")
    public Particle getParticleFromString(String particleString) {
        if (particleString == null || particleString.isEmpty()) return null;
        NamespacedKey key = NamespacedKey.fromString(particleString.toLowerCase(Locale.ROOT));
        if (key != null) {
            Particle particle = Registry.PARTICLE_TYPE.get(key);
            if (particle != null) return particle;
        }
        try { return Particle.valueOf(particleString.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    public void displayMessage(Player player, String message) {
        switch (getConfig().getString("message_display_mode", "chat").toLowerCase()) {
            case "actionbar" -> player.sendActionBar(message);
            case "title" -> player.sendTitle("", message, 10, 70, 20);
            default -> player.sendMessage(message);
        }
    }

    public static boolean isHoldingBanMace(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != getBanMaceMaterial() || !item.hasItemMeta()) return false;
        return "unique_ban_mace".equals(item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(getInstance(), "ban_mace"), PersistentDataType.STRING));
    }
}