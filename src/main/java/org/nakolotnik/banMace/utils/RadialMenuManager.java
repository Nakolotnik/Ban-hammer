package org.nakolotnik.banMace.utils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.BanMace.BanMaceMode;

import java.util.*;
import java.util.stream.Collectors;

public class RadialMenuManager implements Listener {

    private final BanMace plugin;
    private final Map<UUID, List<Entity>> activeMenus = new HashMap<>();
    private final Map<UUID, BanMaceMode> selectedModes = new HashMap<>();
    private final Map<UUID, BukkitTask> menuUpdateTasks = new HashMap<>();
    private final Map<UUID, Location> menuCenters = new HashMap<>();
    private final Map<UUID, Integer> animationTicks = new HashMap<>();
    private final Map<UUID, Boolean> isAnimating = new HashMap<>();
    private final Map<UUID, Integer> spawnOrder = new HashMap<>();
    private final Set<Material> iconMaterials;

    public RadialMenuManager(BanMace plugin) {
        this.plugin = plugin;
        this.iconMaterials = Arrays.stream(BanMaceMode.values())
                .map(this::getIconForMode)
                .collect(Collectors.toSet());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (!BanMace.isHoldingBanMace(player)) return;

        event.setCancelled(true);

        if (!canUseRadialMenu(player)) {
            player.sendMessage(plugin.getMessage("item_removed_no_permission"));
            player.getInventory().remove(player.getInventory().getItemInMainHand());
            return;
        }

        if (activeMenus.containsKey(player.getUniqueId())) {
            closeMenu(player, true);
        } else {
            openMenu(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        closeMenu(event.getPlayer(), false);
    }

    private void openMenu(Player player) {
        if (!canUseRadialMenu(player)) {
            player.sendMessage(plugin.getMessage("item_removed_no_permission"));
            player.getInventory().remove(player.getInventory().getItemInMainHand());
            return;
        }

        List<BanMaceMode> modes = Arrays.asList(BanMaceMode.values());
        if (modes.isEmpty()) return;

        Location center = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(4.0));
        menuCenters.put(player.getUniqueId(), center);

        List<Entity> spawnedEntities = new ArrayList<>();
        activeMenus.put(player.getUniqueId(), spawnedEntities);
        animationTicks.put(player.getUniqueId(), 0);
        isAnimating.put(player.getUniqueId(), true);
        spawnOrder.put(player.getUniqueId(), 0);

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeMenus.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                if (!canUseRadialMenu(player)) {
                    closeMenu(player, false);
                    player.sendMessage(plugin.getMessage("item_removed_no_permission"));
                    player.getInventory().remove(player.getInventory().getItemInMainHand());
                    this.cancel();
                    return;
                }

                if (isAnimating.get(player.getUniqueId())) {
                    animateSpawn(player, modes);
                } else {
                    updateSelection(player);
                    animateMenu(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        menuUpdateTasks.put(player.getUniqueId(), task);
    }

    private void animateSpawn(Player player, List<BanMaceMode> modes) {
        UUID uuid = player.getUniqueId();
        int currentOrder = spawnOrder.get(uuid);
        int tick = animationTicks.get(uuid);

        if (tick % 4 == 0 && currentOrder < modes.size()) {
            spawnMenuItem(player, modes.get(currentOrder), currentOrder, modes.size());
            spawnOrder.put(uuid, currentOrder + 1);

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f + (currentOrder * 0.1f));
        }

        if (currentOrder >= modes.size()) {
            isAnimating.put(uuid, false);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
        }

        animationTicks.put(uuid, tick + 1);
    }

    private void spawnMenuItem(Player player, BanMaceMode mode, int index, int totalModes) {
        UUID uuid = player.getUniqueId();
        Location center = menuCenters.get(uuid);
        if (center == null) return;

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector sideVector = new Vector(0, 1, 0).crossProduct(direction).normalize();
        Vector upVector = direction.crossProduct(sideVector).normalize();

        double angle = (2 * Math.PI / totalModes) * index - (Math.PI / 2);
        double radius = 2.2;

        Vector offset = sideVector.clone().multiply(radius * Math.cos(angle))
                .add(upVector.clone().multiply(radius * Math.sin(angle)));
        Location iconLocation = center.clone().add(offset);

        final Material iconMaterial = getIconForMode(mode);
        final ItemStack iconStack = new ItemStack(iconMaterial);
        final String modeDisplayName = plugin.getMessage("mode_" + mode.name().toLowerCase());

        final ItemDisplay iconDisplay = iconLocation.getWorld().spawn(iconLocation, ItemDisplay.class, display -> {
            display.setItemStack(iconStack);
            Transformation transformation = display.getTransformation();
            transformation.getScale().set(0.1f, 0.1f, 0.1f);
            display.setTransformation(transformation);
        });

        ArmorStand labelStand = iconLocation.getWorld().spawn(iconLocation.clone().add(0, 0.5, 0), ArmorStand.class, as -> {
            as.setGravity(false);
            as.setVisible(false);
            as.setMarker(true);
            as.setCustomName(ChatColor.WHITE + modeDisplayName);
            as.setCustomNameVisible(false);
            as.addPassenger(iconDisplay);
        });

        List<Entity> spawnedEntities = activeMenus.get(uuid);
        spawnedEntities.add(iconDisplay);
        spawnedEntities.add(labelStand);

        iconLocation.getWorld().spawnParticle(Particle.POOF, iconLocation, 8, 0.2, 0.2, 0.2, 0.1);
        iconLocation.getWorld().spawnParticle(Particle.ENCHANT, iconLocation, 5, 0.3, 0.3, 0.3, 0.05);

        new BukkitRunnable() {
            private int animTicks = 0;
            private final int maxTicks = 10;

            @Override
            public void run() {
                if (!iconDisplay.isValid() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                animTicks++;
                float progress = (float) animTicks / maxTicks;

                float scale;
                if (progress <= 0.7f) {
                    scale = 0.1f + (0.8f * progress / 0.7f);
                } else {
                    float bounceProgress = (progress - 0.7f) / 0.3f;
                    scale = 0.9f - (0.3f * bounceProgress);
                }

                Transformation t = iconDisplay.getTransformation();
                t.getScale().set(scale, scale, scale);
                iconDisplay.setTransformation(t);

                if (animTicks >= maxTicks) {
                    labelStand.setCustomNameVisible(true);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void animateMenu(Player player) {
        UUID uuid = player.getUniqueId();
        int ticks = animationTicks.getOrDefault(uuid, 0);
        Location center = menuCenters.get(uuid);
        if (center == null) return;

        if (ticks % 6 == 0) {
            center.getWorld().spawnParticle(Particle.ENCHANT, center, 5, 0.3, 0.3, 0.3, 0.05);
        }

        if (ticks % 8 == 0) {
            double angle = (ticks * 0.1) % (2 * Math.PI);
            Vector offset = new Vector(Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(offset), 1, 0, 0, 0, 0);
        }

        animationTicks.put(uuid, ticks + 1);
    }

    private void updateSelection(Player player) {
        ArmorStand closestStand = null;
        double minAngle = Double.MAX_VALUE;

        List<Entity> menuEntities = activeMenus.get(player.getUniqueId());
        if (menuEntities == null) return;

        for (Entity entity : menuEntities) {
            if (entity instanceof ArmorStand stand) {
                Vector toStand = stand.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
                double angle = player.getEyeLocation().getDirection().angle(toStand);
                if (angle < minAngle) {
                    minAngle = angle;
                    closestStand = stand;
                }
            }
        }

        unhighlightAll(player);

        double selectionThreshold = 0.3;
        if (closestStand != null && minAngle < selectionThreshold) {
            highlight(closestStand, player);
            String customName = ChatColor.stripColor(closestStand.getCustomName());
            for (BanMaceMode mode : BanMaceMode.values()) {
                if (customName.equals(plugin.getMessage("mode_" + mode.name().toLowerCase()))) {
                    if (selectedModes.get(player.getUniqueId()) != mode) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                    }
                    selectedModes.put(player.getUniqueId(), mode);
                    break;
                }
            }
        } else {
            selectedModes.remove(player.getUniqueId());
        }
    }

    private void highlight(ArmorStand stand, Player player) {
        stand.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + ChatColor.stripColor(stand.getCustomName()));
        stand.getPassengers().stream()
                .filter(e -> e instanceof ItemDisplay)
                .map(e -> (ItemDisplay) e)
                .findFirst()
                .ifPresent(iconDisplay -> {
                    Transformation t = iconDisplay.getTransformation();
                    t.getScale().set(0.9f, 0.9f, 0.9f);
                    iconDisplay.setTransformation(t);
                    iconDisplay.setGlowing(true);
                    Location loc = iconDisplay.getLocation();
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.2, 0.2, 0.2, 0);
                });
    }

    private void unhighlightAll(Player player) {
        List<Entity> menuEntities = activeMenus.get(player.getUniqueId());
        if (menuEntities == null) return;

        for (Entity entity : menuEntities) {
            if (entity instanceof ArmorStand stand) {
                stand.setCustomName(ChatColor.WHITE + ChatColor.stripColor(stand.getCustomName()));
            } else if (entity instanceof ItemDisplay display) {
                if (iconMaterials.contains(display.getItemStack().getType())) {
                    Transformation t = display.getTransformation();
                    t.getScale().set(0.6f, 0.6f, 0.6f);
                    display.setTransformation(t);
                    display.setGlowing(false);
                }
            }
        }
    }

    public void closeMenu(Player player, boolean confirm) {
        UUID uuid = player.getUniqueId();
        if (!activeMenus.containsKey(uuid)) return;

        if (menuUpdateTasks.containsKey(uuid)) {
            menuUpdateTasks.get(uuid).cancel();
            menuUpdateTasks.remove(uuid);
        }

        Location center = menuCenters.get(uuid);
        if (center != null) {
            center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(Particle.POOF, center, 15, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
        }

        List<Entity> entities = activeMenus.get(uuid);
        if (entities != null) {
            for (Entity entity : entities) {
                if (entity instanceof ItemDisplay display) {
                    new BukkitRunnable() {
                        private int ticks = 0;
                        @Override
                        public void run() {
                            if (!display.isValid() || ticks >= 5) {
                                display.remove();
                                this.cancel();
                                return;
                            }
                            Transformation t = display.getTransformation();
                            float scale = 0.6f * (1.0f - (ticks / 5.0f));
                            t.getScale().set(scale, scale, scale);
                            display.setTransformation(t);
                            ticks++;
                        }
                    }.runTaskTimer(plugin, 1L, 1L);
                } else {
                    entity.remove();
                }
            }
        }

        activeMenus.remove(uuid);
        menuCenters.remove(uuid);
        animationTicks.remove(uuid);
        isAnimating.remove(uuid);
        spawnOrder.remove(uuid);

        if (confirm) {
            BanMaceMode selected = selectedModes.get(uuid);
            if (selected != null) {
                BanMace.setMode(player, selected.getHandler());
                String modeName = plugin.getMessage("mode_" + selected.name().toLowerCase());
                String message = plugin.getMessage("mode_switched", Map.of("mode", modeName));
                plugin.displayMessage(player, message);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        }
        selectedModes.remove(uuid);
    }

    private Material getIconForMode(BanMaceMode mode) {
        return switch (mode) {
            case SPAWN -> Material.COMPASS;
            case BED -> Material.RED_BED;
            case BAN -> Material.TNT;
            case KICK -> Material.IRON_DOOR;
            case FREEZE -> Material.ICE;
            case TELEPORT_TO -> Material.ENDER_PEARL;
            case BALLOON -> Material.STRING;
            default -> Material.BARRIER;
        };
    }

    private boolean canUseRadialMenu(Player player) {
        if (player.isOp()) {
            return true;
        }
        if (plugin.getConfig().getBoolean("whitelist.enabled")) {
            return plugin.getConfig().getStringList("whitelist.players").contains(player.getName());
        }
        return false;
    }
}