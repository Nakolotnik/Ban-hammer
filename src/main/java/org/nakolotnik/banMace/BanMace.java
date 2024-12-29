package org.nakolotnik.banMace;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
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
        setMode(new SpawnMode());

        instance = this;
        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.checkAndLoadLanguageFiles();
        languageManager.loadMessages(getConfig().getString("language", "en"));

        getServer().getPluginManager().registerEvents(new SpawnMode(), this);
        getServer().getPluginManager().registerEvents(new BedMode(), this);
        getServer().getPluginManager().registerEvents(new BanMode(), this);
        getServer().getPluginManager().registerEvents(new KickMode(), this);
        getServer().getPluginManager().registerEvents(new ModeSwitcher(), this);
        getServer().getPluginManager().registerEvents(new ItemPickupListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeMode(), this);

        CommandHandler commandHandler = new CommandHandler(this);
        Objects.requireNonNull(getCommand("bm-give")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("bm-setlanguage")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("bm-changeview")).setExecutor(commandHandler);

        if (getConfig().getBoolean("check_for_updates", true)) {
            getLogger().info("Checking for updates...");
            new UpdateChecker(this).checkForUpdates();
        } else {
            getLogger().info("Update checking is disabled in the config.");
        }
    }

    public static void setMode(ModeHandler mode) {
        currentMode = mode;
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

        ItemStack mace = new ItemStack(Material.NETHERITE_AXE);
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
        FREEZE(new FreezeMode());

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

        if (item == null || item.getType() != Material.NETHERITE_AXE || !item.hasItemMeta()) {
            return false;
        }

        var meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey maceKey = new NamespacedKey(BanMace.getInstance(), "ban_mace");

        return "unique_ban_mace".equals(container.get(maceKey, PersistentDataType.STRING));
    }

}
