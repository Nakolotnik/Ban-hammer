package org.nakolotnik;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;

public final class banhammer extends JavaPlugin implements Listener {
    private static final Material HAMMER_MATERIAL = Material.NETHERITE_AXE;
    private static final String HAMMER_NAME = "§cBanHammer";
    private ItemStack exampleHammer;

    private enum Mode { SPAWN, BED }
    private Mode currentMode = Mode.SPAWN;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        createExampleHammer();
        if (this.getCommand("banhammer") != null) {
            this.getCommand("banhammer").setExecutor(this);
            this.getCommand("banhammer").setTabCompleter(this);
        } else {
            getLogger().severe("Команда /banhammer не была найдена. Проверьте plugin.yml!");
        }
    }

    private void createExampleHammer() {
        exampleHammer = new ItemStack(HAMMER_MATERIAL);
        ItemMeta meta = exampleHammer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HAMMER_NAME);
            updateLore(meta);
            meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            exampleHammer.setItemMeta(meta);
        }
    }

    private ItemStack createHammerWithMode(Mode mode) {
        ItemStack hammer = new ItemStack(HAMMER_MATERIAL);
        ItemMeta meta = hammer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HAMMER_NAME);
            updateLore(meta);
            meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            hammer.setItemMeta(meta);
        }
        return hammer;
    }

    private void updateLore(ItemMeta meta) {
        if (currentMode == Mode.SPAWN) {
            meta.setLore(Collections.singletonList("§7Тот, кто ударит этим молотом, телепортирует игрока на точку спавна"));
        } else if (currentMode == Mode.BED) {
            meta.setLore(Collections.singletonList("§7Тот, кто ударит этим молотом, телепортирует игрока к его кровати"));
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();

            ItemStack itemInHand = damager.getInventory().getItemInMainHand();
            if (isBanHammer(itemInHand) && damager.hasPermission("banhammer.use")) {
                event.setCancelled(true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (currentMode == Mode.SPAWN) {
                            target.teleport(target.getWorld().getSpawnLocation());
                            damager.sendMessage("§aИгрок " + target.getName() + " был телепортирован на точку спавна!");
                        } else if (currentMode == Mode.BED) {
                            target.teleport(target.getBedSpawnLocation() != null ? target.getBedSpawnLocation() : target.getWorld().getSpawnLocation());
                            damager.sendMessage("§aИгрок " + target.getName() + " был телепортирован к своей кровати!");
                        }
                        target.sendMessage("§cВас телепортировали " + (currentMode == Mode.SPAWN ? "на точку спавна!" : "к вашей кровати!"));
                    }
                }.runTaskLater(this, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isBanHammer(player.getInventory().getItemInMainHand()) && player.hasPermission("banhammer.use")) {
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                toggleMode(player);
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();

        if (isBanHammer(item)) {
            Player player = event.getPlayer();
            if (!player.hasPermission("banhammer.use")) {
                event.setCancelled(true);
                event.getItem().remove();
                player.sendMessage("§cВы не имеете прав для использования BanHammer! Он был уничтожен.");
            }
        }
    }

    private boolean isBanHammer(ItemStack item) {
        if (item == null || item.getType() != HAMMER_MATERIAL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return HAMMER_NAME.equals(meta.getDisplayName()) && exampleHammer.getItemMeta().getEnchants().equals(meta.getEnchants());
    }

    private void toggleMode(Player player) {
        if (currentMode == Mode.SPAWN) {
            currentMode = Mode.BED;
        } else {
            currentMode = Mode.SPAWN;
        }

        ItemMeta meta = exampleHammer.getItemMeta();
        if (meta != null) {
            updateLore(meta);
            ItemStack updatedHammer = createHammerWithMode(currentMode);
            player.getInventory().getItemInMainHand().setItemMeta(updatedHammer.getItemMeta());
        }

        player.sendMessage("§eРежим BanHammer переключен на: §6" + (currentMode == Mode.SPAWN ? "ТП на спавн" : "ТП к кровати"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("banhammer")) {
                if (player.hasPermission("banhammer.give")) {
                    player.getInventory().addItem(createHammerWithMode(currentMode));
                    player.sendMessage("§aВы получили BanHammer!");
                    return true;
                } else {
                    player.sendMessage("§cУ вас нет прав для использования этой команды.");
                    return true;
                }
            }
        } else if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("Эту команду может использовать только игрок.");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void onDisable() {
    }
}
