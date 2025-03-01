package org.nakolotnik.banMace;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.nakolotnik.banMace.utils.*;
import org.nakolotnik.banMace.utils.mods.*;

import java.util.*;

public final class BanMace extends JavaPlugin {
    private static ModeHandler currentMode;
    private static BanMace instance;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Инициализация менеджера языков
        languageManager = new LanguageManager(this);
        languageManager.checkAndLoadLanguageFiles();
        languageManager.loadMessages(getConfig().getString("language", "en"));

        String lastActiveMode = getConfig().getString("active_mode", "SPAWN");
        try {
            BanMaceMode initialMode = BanMaceMode.valueOf(lastActiveMode);
            setMode(initialMode.getHandler());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid mode in config: " + lastActiveMode + ". Defaulting to SPAWN mode.");
            setMode(new SpawnMode());
        }

        Listener[] listeners = new Listener[] {
                new SpawnMode(),
                new BedMode(),
                new BanMode(),
                new KickMode(),
                new ModeSwitcher(),
                new ItemPickupListener(this),
                new FreezeMode(),
                new TeleportToMode()
        };
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        CommandHandler commandHandler = new CommandHandler(this);
        String[] commands = {"bm-give", "bm-setlanguage", "bm-changeview", "bm-tpcoordinates", "bm-help"};
        for (String cmd : commands) {
            Objects.requireNonNull(getCommand(cmd)).setExecutor(commandHandler);
        }

        if (getConfig().getBoolean("check_for_updates", true)) {
            getLogger().info("Checking for updates...");
            new UpdateChecker(this).checkForUpdates();
        } else {
            getLogger().info("Update checking is disabled in the config.");
        }
    }



    public static void setMode(ModeHandler mode) {
        currentMode = mode;

        BanMaceMode selectedMode = null;
        for (BanMaceMode banMaceMode : BanMaceMode.values()) {
            if (banMaceMode.getHandler().getClass().equals(mode.getClass())) {
                selectedMode = banMaceMode;
                break;
            }
        }

        if (selectedMode != null) {
            BanMace.getInstance().getConfig().set("active_mode", selectedMode.name());
            BanMace.getInstance().saveConfig();
        }
    }


    public static ModeHandler getCurrentMode() {
        return currentMode;
    }

    public String getMessage(String key) {
        return languageManager.getMessage(key, null);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        return languageManager.getMessage(key, placeholders);
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static BanMace getInstance() {
        return instance;
    }

    ItemStack createBanMace() {
        String nameColor = getConfig().getString("item_customization.name_color", "§7");
        String name = getConfig().getString("item_customization.bm-name", "Ban Mace");
        List<String> lore = getConfig().getStringList("item_customization.lore");

        ItemStack mace = new ItemStack(Material.MACE);
        var meta = mace.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nameColor + name);

            if (!lore.isEmpty()) {
                List<String> formattedLore = new ArrayList<>();
                for (String line : lore) {
                    formattedLore.add(line);
                }
                meta.setLore(formattedLore);
            }

            meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);

            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(this, "ban_mace");
            dataContainer.set(key, PersistentDataType.STRING, "unique_ban_mace");

            mace.setItemMeta(meta);
        }
        return mace;
    }


    public enum BanMaceMode {
        SPAWN(new SpawnMode()),
        BED(new BedMode()),
        BAN(new BanMode()),
        KICK(new KickMode()),
        FREEZE(new FreezeMode()),
        TELEPORT(new TeleportToMode());

        private final ModeHandler handler;

        BanMaceMode(ModeHandler handler) {
            this.handler = handler;
        }

        public ModeHandler getHandler() {
            return handler;
        }
    }

    public void playTeleportEffects(Player target) {
        Particle particle = Particle.valueOf(getConfig().getString("teleport_effect.particle", "PORTAL"));
        int count = getConfig().getInt("teleport_effect.count", 100);
        double offset = getConfig().getDouble("teleport_effect.offset", 0.5);
        float speed = (float) getConfig().getDouble("teleport_effect.speed", 0.2);

        target.getWorld().spawnParticle(particle, target.getLocation(), count, offset, offset, offset, speed);
        Sound sound = Sound.valueOf(getConfig().getString("teleport_sound", "ENTITY_ENDERMAN_TELEPORT"));
        target.getWorld().playSound(target.getLocation(), sound, 1.0f, 1.0f);
    }

    public void displayMessage(Player player, String message) {
        String mode = getConfig().getString("message_display_mode", "chat");

        switch (mode.toLowerCase()) {
            case "actionbar":
                player.sendActionBar(message);
                break;
            case "title":
                player.sendTitle("", message, 10, 70, 20);
                break;
            case "chat":
            default:
                player.sendMessage(message);
                break;
        }
    }
    public static boolean isHoldingBanMace(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) {
            return false;
        }

        var meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey maceKey = new NamespacedKey(BanMace.getInstance(), "ban_mace");

        return "unique_ban_mace".equals(container.get(maceKey, PersistentDataType.STRING));
    }
}
