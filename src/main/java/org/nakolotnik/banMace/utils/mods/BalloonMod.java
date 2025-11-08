package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BalloonMod implements ModeHandler, Listener {

    private final Map<UUID, List<Entity>> activeBalloons = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> balloonUpdaters = new ConcurrentHashMap<>();
    private final Map<Entity, Double> balloonRandomYOffsets = new ConcurrentHashMap<>();
    private final Map<Entity, Boolean> balloonSizes = new ConcurrentHashMap<>();
    private final Set<UUID> protectedEntities = ConcurrentHashMap.newKeySet();

    @Override
    public String getModeName() {
        return "Balloon";
    }

    @Override
    public void execute(Player damager, Player target) {
        BanMace plugin = BanMace.getInstance();

        if (activeBalloons.containsKey(target.getUniqueId())) {
            damager.sendMessage("§cThis player is already flying on balloons.");
            return;
        }

        int levitationDuration = 5 * 20;
        int levitationAmplifier = 4;
        int slowFallDuration = 15 * 20;

        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationDuration, levitationAmplifier));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, levitationDuration + slowFallDuration, 0, false, false), true);

        attachVisualBalloons(target);

        new BukkitRunnable() {
            @Override
            public void run() {
                removeVisualBalloons(target.getUniqueId());
            }
        }.runTaskLater(plugin, levitationDuration + slowFallDuration);

        damager.sendMessage(plugin.getMessage("balloon_applied", Map.of("target", target.getName())));
        target.sendMessage(plugin.getMessage("balloon_received"));

        String actionDetails = "Launched into the air for " + (levitationDuration / 20) + " seconds.";
        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);
    }

    private void attachVisualBalloons(Player target) {
        List<Entity> balloonEntities = new ArrayList<>();
        UUID targetId = target.getUniqueId();
        BanMace plugin = BanMace.getInstance();
        Material[] colors = {Material.RED_WOOL, Material.YELLOW_WOOL, Material.BLUE_WOOL};
        Random random = new Random();

        for (Material color : colors) {
            boolean isSmall = random.nextBoolean();
            Parrot parrot = target.getWorld().spawn(target.getLocation().add(0, 3, 0), Parrot.class, p -> {
                p.setInvisible(true);
                p.setSilent(true);
                p.setInvulnerable(true);
                p.setLeashHolder(target);
                p.setAgeLock(true);
                p.setAI(false);
                p.setGravity(false);
                p.setCollidable(false);
                p.setRemoveWhenFarAway(false);
            });
            protectedEntities.add(parrot.getUniqueId());
            double randomYOffset = 2.0 + random.nextDouble() * 1.5;
            balloonRandomYOffsets.put(parrot, randomYOffset);
            balloonSizes.put(parrot, isSmall);

            double armorStandOffset = isSmall ? -0.75 : -1.3;
            ArmorStand as = target.getWorld().spawn(parrot.getLocation().add(0, armorStandOffset, 0), ArmorStand.class, a -> {
                a.setInvisible(true);
                a.setGravity(false);
                a.setMarker(true);
                a.setInvulnerable(true);
                a.getEquipment().setHelmet(new ItemStack(color));
                a.setSmall(isSmall);
                a.setCollidable(false);
                a.setRemoveWhenFarAway(false);
            });

            protectedEntities.add(as.getUniqueId());

            balloonEntities.add(parrot);
            balloonEntities.add(as);
        }

        activeBalloons.put(targetId, balloonEntities);

        BukkitTask updaterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline() || !activeBalloons.containsKey(targetId)) {
                    removeVisualBalloons(targetId);
                    cancel();
                    return;
                }

                List<Entity> entities = activeBalloons.get(targetId);
                Location targetHeadLocation = target.getEyeLocation();

                for (int i = 0; i < entities.size(); i += 2) {
                    Parrot parrot = (Parrot) entities.get(i);
                    ArmorStand as = (ArmorStand) entities.get(i + 1);

                    if (!parrot.isValid() || !as.isValid()) {
                        removeVisualBalloons(targetId);
                        cancel();
                        return;
                    }

                    if (!parrot.isLeashed() || parrot.getLeashHolder() != target) {
                        parrot.setLeashHolder(target);
                    }

                    Vector playerDirection = target.getLocation().getDirection().setY(0).normalize();
                    Vector sideDirection = playerDirection.clone().crossProduct(new Vector(0, 1, 0));

                    double angle = (2 * Math.PI / (entities.size() / 2.0)) * (i / 2.0) + (System.currentTimeMillis() / 2000.0);
                    double radius = 1.3;

                    double sideOffset = Math.cos(angle) * radius;
                    double fwdOffset = Math.sin(angle) * radius;

                    Vector finalOffset = sideDirection.multiply(sideOffset).add(playerDirection.multiply(fwdOffset));

                    double baseOffsetY = balloonRandomYOffsets.getOrDefault(parrot, 2.5);
                    double bobbing = Math.sin(System.currentTimeMillis() / 400.0 + (i * 2)) * 0.2;
                    double finalOffsetY = baseOffsetY + bobbing;

                    Location balloonTargetPos = targetHeadLocation.clone().add(finalOffset).add(0, finalOffsetY, 0);

                    parrot.teleport(balloonTargetPos);

                    boolean isSmall = balloonSizes.getOrDefault(parrot, false);
                    double armorStandOffset = isSmall ? -0.75 : -1.3;
                    as.teleport(parrot.getLocation().add(0, armorStandOffset, 0));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        balloonUpdaters.put(targetId, updaterTask);
    }

    private void removeVisualBalloons(UUID targetId) {
        BukkitTask updater = balloonUpdaters.remove(targetId);
        if (updater != null) {
            updater.cancel();
        }

        List<Entity> entities = activeBalloons.remove(targetId);
        if (entities != null) {
            for (Entity entity : entities) {
                balloonRandomYOffsets.remove(entity);
                balloonSizes.remove(entity);
                protectedEntities.remove(entity.getUniqueId());

                if (entity.isValid()) {
                    if (entity instanceof Parrot parrot && parrot.isLeashed()) {
                        parrot.setLeashHolder(null);
                    }
                    entity.remove();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (BanMace.isHoldingBanMace(damager) && BanMace.getCurrentMode() instanceof BalloonMod) {
            event.setCancelled(true);
            if (target.hasPermission("banmace.bypass")) {
                damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
                return;
            }
            execute(damager, target);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (protectedEntities.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeVisualBalloons(event.getPlayer().getUniqueId());
    }

    public void cleanup() {
        for (UUID playerId : new HashSet<>(activeBalloons.keySet())) {
            removeVisualBalloons(playerId);
        }
        activeBalloons.clear();
        balloonUpdaters.clear();
        balloonRandomYOffsets.clear();
        balloonSizes.clear();
        protectedEntities.clear();
    }
}