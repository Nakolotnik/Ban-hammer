package org.nakolotnik.banMace.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!isBanMace(item)) {
            return;
        }

        Player player = event.getPlayer();
        if (!canInteract(player)) {
            event.setCancelled(true);
            event.getItem().remove();
            player.sendMessage(plugin.getMessage("item_removed_no_permission"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isBanMace(item)) {
            return;
        }

        if (!canInteract(player)) {
            event.setCancelled(true);
            player.getInventory().remove(item);
            player.sendMessage(plugin.getMessage("item_removed_no_permission"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        if (isBanMace(mainHand) || isBanMace(offHand)) {
            if (!canInteract(player)) {
                event.setCancelled(true);
                if (isBanMace(mainHand)) {
                    player.getInventory().remove(mainHand);
                }
                if (isBanMace(offHand)) {
                    player.getInventory().remove(offHand);
                }
                player.sendMessage(plugin.getMessage("item_removed_no_permission"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (isBanMace(clickedItem) || isBanMace(cursorItem)) {
            if (!canInteract(player)) {
                event.setCancelled(true);
                if (isBanMace(clickedItem)) {
                    event.setCurrentItem(null);
                }
                if (isBanMace(cursorItem)) {
                    event.setCursor(null);
                }
                player.sendMessage(plugin.getMessage("item_removed_no_permission"));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!canInteract(player)) {
            removeBanMaceFromInventory(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        ItemStack item = damager.getInventory().getItemInMainHand();
        if (!isBanMace(item)) {
            return;
        }

        if (!canInteract(damager)) {
            event.setCancelled(true);
            damager.getInventory().remove(item);
            damager.sendMessage(plugin.getMessage("item_removed_no_permission"));
        }
    }

    private boolean isBanMace(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "ban_mace");

        return dataContainer.has(key, PersistentDataType.STRING)
                && "unique_ban_mace".equals(dataContainer.get(key, PersistentDataType.STRING));
    }

    private boolean canInteract(Player player) {
        if (player.isOp()) {
            return true;
        }
        if (plugin.getConfig().getBoolean("whitelist.enabled")) {
            return plugin.getConfig().getStringList("whitelist.players").contains(player.getName());
        }
        return false;
    }

    private void removeBanMaceFromInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isBanMace(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }
}