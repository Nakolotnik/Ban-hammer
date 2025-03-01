package org.nakolotnik.banMace.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.nakolotnik.banMace.BanMace;

public class ItemPickupListener implements Listener {
    private final BanMace plugin;

    public ItemPickupListener(BanMace plugin) {
        this.plugin = plugin;
    }

    // Проверка при подборе предмета с земли
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "ban_mace");

        if (dataContainer.has(key, PersistentDataType.STRING)
                && "unique_ban_mace".equals(dataContainer.get(key, PersistentDataType.STRING))) {
            Player player = event.getPlayer();
            if (!canInteract(player)) {
                event.setCancelled(true);
                event.getItem().remove(); // Удаляем предмет с земли
                player.sendMessage(plugin.getMessage("item_removed_no_permission"));
            }
        }
    }

    // Проверка при взаимодействии с предметом (например, щелчок правой/левой кнопкой)
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "ban_mace");

        if (dataContainer.has(key, PersistentDataType.STRING)
                && "unique_ban_mace".equals(dataContainer.get(key, PersistentDataType.STRING))) {
            if (!canInteract(player)) {
                // Удаляем предмет из инвентаря
                player.getInventory().remove(item);
                player.sendMessage(plugin.getMessage("item_removed_no_permission"));
            }
        }
    }

    // Проверка при входе игрока – если в инвентаре есть булава, а прав нет, она удаляется.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!canInteract(player)) {
            removeBanMaceFromInventory(player);
        }
    }

    // Проверка при ударе (например, если игрок пытается использовать булаву в бою)
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            ItemStack item = damager.getInventory().getItemInMainHand();
            if (item == null || !item.hasItemMeta()) {
                return;
            }
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "ban_mace");

            if (container.has(key, PersistentDataType.STRING)
                    && "unique_ban_mace".equals(container.get(key, PersistentDataType.STRING))) {
                if (!canInteract(damager)) {
                    // Удаляем предмет из инвентаря
                    damager.getInventory().remove(item);
                    damager.sendMessage(plugin.getMessage("item_removed_no_permission"));
                }
            }
        }
    }

    // Проверка, имеет ли игрок необходимые права (OP или в whitelist)
    private boolean canInteract(Player player) {
        if (player.isOp()) {
            return true;
        }
        if (plugin.getConfig().getBoolean("whitelist.enabled")) {
            return plugin.getConfig().getStringList("whitelist.players").contains(player.getName());
        }
        return false;
    }

    // Проходим по инвентарю игрока и удаляем все экземпляры Ban Mace
    private void removeBanMaceFromInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.MACE && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                PersistentDataContainer container = meta.getPersistentDataContainer();
                NamespacedKey key = new NamespacedKey(plugin, "ban_mace");
                if (container.has(key, PersistentDataType.STRING)
                        && "unique_ban_mace".equals(container.get(key, PersistentDataType.STRING))) {
                    player.getInventory().setItem(slot, null);
                }
            }
        }
    }
}
