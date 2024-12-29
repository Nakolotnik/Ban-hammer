package org.nakolotnik.banMace.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.nakolotnik.banMace.BanMace;

public class ItemPickupListener implements Listener {
    private final BanMace plugin;

    public ItemPickupListener(BanMace plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        var item = event.getItem().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "ban_mace");

            if (dataContainer.has(key, PersistentDataType.STRING) &&
                    "unique_ban_mace".equals(dataContainer.get(key, PersistentDataType.STRING))) {
                Player player = event.getPlayer();
                if (!canInteract(player)) {
                    event.setCancelled(true);
                    event.getItem().remove();
                    player.sendMessage(plugin.getMessage("item_removed_no_permission"));
                }
            }
        }
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
}
