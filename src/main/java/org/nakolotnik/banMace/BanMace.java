package org.nakolotnik.banMace;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class BanMace extends JavaPlugin implements Listener {
    private static final Material HAMMER_MATERIAL = Material.NETHERITE_AXE;
    private static final String HAMMER_NAME = "§cBan Mace";
    private ItemStack exampleHammer;

    private enum Mode { SPAWN, BED, BAN, KICK, FREEZE }
    private Mode currentMode = Mode.SPAWN;
    private String toggleKey;

    private final Map<UUID, Integer> hammerUsageStats = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Загружены сообщения: " + getConfig().getString("banmace.messages.mace_received"));
        currentMode = Mode.valueOf(getConfig().getString("banmace.mode", "SPAWN"));
        toggleKey = getConfig().getString("banmace.toggle_key", "RIGHT_CLICK_BLOCK");
        getServer().getPluginManager().registerEvents(this, this);
        createExampleHammer();
        if (this.getCommand("banmace") != null) {
            Objects.requireNonNull(this.getCommand("banmace")).setExecutor(this);
            Objects.requireNonNull(this.getCommand("banmace")).setTabCompleter(this);
        } else {
            getLogger().severe("Команда /banmace не была найдена. Проверьте plugin.yml!");
        }
    }


    @Override
    public void onDisable() {
        getConfig().set("banmace.mode", currentMode.name());
        getConfig().set("banmace.toggle_key", toggleKey);
        saveConfig();
    }

    private void createExampleHammer() {
        exampleHammer = createHammer(currentMode);
    }

    private ItemStack createHammer(Mode mode) {
        ItemStack hammer = new ItemStack(HAMMER_MATERIAL);
        ItemMeta meta = hammer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HAMMER_NAME);
            updateLore(meta, mode);
            meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            hammer.setItemMeta(meta);
        }
        return hammer;
    }

    private void updateLore(ItemMeta meta, Mode mode) {
        String lore = switch (mode) {
            case SPAWN -> "§7Телепортирует игрока на точку спавна";
            case BED -> "§7Телепортирует игрока к его кровати (или на спавн)";
            case BAN -> "§7Временно банит игрока";
            case KICK -> "§7Кикает игрока с сервера";
            case FREEZE -> "§7Замораживает игрока на месте";
            default -> "§7Неизвестный режим";
        };
        meta.setLore(Collections.singletonList(lore));
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
                    case FREEZE:
                        handleFreeze(damager, target);
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
                World world = target.getWorld();
                Location teleportLocation;
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    teleportLocation = world.getSpawnLocation();
                } else {
                    World mainWorld = Bukkit.getWorlds().stream()
                            .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                            .findFirst()
                            .orElse(null);

                    if (mainWorld != null) {
                        target.teleport(mainWorld.getSpawnLocation());
                        damager.sendMessage(getMessage("player_teleported_spawn").replace("%player%", target.getName()));
                    } else {
                        damager.sendMessage("§cОсновной мир не найден. Невозможно телепортировать игрока.");
                    }
                    return;
                }

                target.teleport(teleportLocation);
                damager.sendMessage(getMessage("player_teleported_spawn").replace("%player%", target.getName()));
                target.sendMessage(getMessage("player_teleported_spawn").replace("%player%", target.getName()));
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            // Проверка на изменение координат по Y (прыжок)
            if (event.getFrom().getY() < event.getTo().getY()) {
                // Блокируем движение вверх
                event.setTo(event.getFrom());
            }
        }
    }
    private final Set<UUID> frozenPlayers = new HashSet<>();

    private void handleFreeze(Player damager, Player target) {
        int freezeDuration = getConfig().getInt("banmace_modes.freeze_duration", 10);
        target.sendMessage(getMessage("freeze").replace("%player%", target.getName()).replace("%time%", String.valueOf(freezeDuration)));

        // Добавляем игрока в список замороженных
        frozenPlayers.add(target.getUniqueId());

        // Устанавливаем скорость передвижения в 0
        target.setWalkSpeed(0);
        target.setFlySpeed(0);

        // Отменяем заморозку через заданное время
        new BukkitRunnable() {
            @Override
            public void run() {
                // Восстанавливаем скорости передвижения
                target.setWalkSpeed(0.2f);
                target.setFlySpeed(0.1f);

                // Убираем игрока из списка замороженных
                frozenPlayers.remove(target.getUniqueId());
            }
        }.runTaskLater(this, freezeDuration * 20L);
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
        getLogger().info(damager.getName() + " использовал ban mace на " + target.getName() + " в режиме " + currentMode);
    }

    private String getMessage(String key) {
        String message = getConfig().getString("banmace.messages." + key, "Сообщение не найдено: " + key);
        if (message.startsWith("Сообщение не найдено:")) {
            getLogger().warning("Отсутствует сообщение для ключа: " + key);
        }
        return message;
    }



    private boolean isBanMace(ItemStack item) {
        return item != null && item.getType() == HAMMER_MATERIAL && HAMMER_NAME.equals(item.getItemMeta().getDisplayName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action;
        try {
            action = Action.valueOf(toggleKey);
        } catch (IllegalArgumentException e) {
            getLogger().severe("Неизвестное действие для toggle_key в конфигурации: " + toggleKey);
            return;
        }

        if (event.getAction() == action) {
            if (isBanMace(player.getInventory().getItemInMainHand())) {
                cycleMode(player);
            }
        }
    }


    private void cycleMode(Player player) {
        currentMode = Mode.values()[(currentMode.ordinal() + 1) % Mode.values().length];
        ItemMeta meta = exampleHammer.getItemMeta();
        if (meta != null) {
            updateLore(meta, currentMode);
            exampleHammer.setItemMeta(meta);
        }
        player.sendMessage("§aБулова бана теперь в режиме: §c" + currentMode);
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
        return false;
    }
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (isBanMace(item) && !player.hasPermission("banmace.give")) {
            event.setCancelled(true);
            event.getItem().remove();
            player.sendMessage("§cВы не имеете права на использование этого предмета и он был удален.");
        }
    }
}
