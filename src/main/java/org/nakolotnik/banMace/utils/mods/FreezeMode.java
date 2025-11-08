package org.nakolotnik.banMace.utils.mods;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.nakolotnik.banMace.BanMace;
import org.nakolotnik.banMace.ModeHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeMode implements ModeHandler, Listener {

    private final Map<UUID, Set<BlockDisplay>> frozenPlayerIceBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> frozenPlayerTasks = new ConcurrentHashMap<>();
    private final int freezeDuration = 20;

    @Override
    public void execute(Player damager, Player target) {
        BanMace plugin = BanMace.getInstance();

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeDuration * 20, 9));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, freezeDuration * 20, 0));

        createIceEffect(target);

        damager.sendMessage(plugin.getMessage("freeze_applied", Map.of("target", target.getName())));
        target.sendMessage(plugin.getMessage("freeze_received"));

        String actionDetails = "Duration: " + freezeDuration + " seconds.";
        plugin.getLoggerService().logMaceAction(damager, target, getModeName(), actionDetails);

        scheduleIceRemoval(target);
    }

    private void createIceEffect(Player player) {
        UUID playerId = player.getUniqueId();
        removeIceEffect(playerId);

        Location playerLoc = player.getLocation();
        Set<BlockDisplay> iceBlocks = new HashSet<>();

        double radius = 2.0;
        int blockCount = 16;
        for (int i = 0; i < blockCount; i++) {
            double angle = (2 * Math.PI * i) / blockCount;
            double x = playerLoc.getX() + Math.cos(angle) * radius;
            double y = playerLoc.getY() - 0.5;
            double z = playerLoc.getZ() + Math.sin(angle) * radius;
            iceBlocks.add(createIceBlock(new Location(player.getWorld(), x, y, z), i));
        }

        iceBlocks.add(createCenterIceBlock(playerLoc.clone().add(0, -0.3, 0)));
        createDecorativeIce(player, iceBlocks);
        frozenPlayerIceBlocks.put(playerId, iceBlocks);

        player.getWorld().playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
        spawnFreezeParticles(player);
    }

    private BlockDisplay createIceBlock(Location loc, int index) {
        BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class);
        display.setBlock(Material.ICE.createBlockData());
        float scale = 0.8f + (index % 3) * 0.2f;
        float rotationY = (float) Math.toRadians(index * 22.5);
        Transformation transformation = new Transformation(new Vector3f(), new AxisAngle4f(rotationY, 0, 1, 0), new Vector3f(scale, 0.3f, scale), new AxisAngle4f());
        display.setTransformation(transformation);
        display.setInterpolationDuration(10);
        display.setBrightness(new Display.Brightness(15, 15));
        return display;
    }

    private BlockDisplay createCenterIceBlock(Location loc) {
        BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class);
        display.setBlock(Material.PACKED_ICE.createBlockData());
        Transformation transformation = new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 0.2f, 1.5f), new AxisAngle4f());
        display.setTransformation(transformation);
        display.setInterpolationDuration(15);
        display.setBrightness(new Display.Brightness(15, 15));
        return display;
    }

    private void createDecorativeIce(Player player, Set<BlockDisplay> iceBlocks) {
        Location playerLoc = player.getLocation();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 1.0 + random.nextDouble() * 1.5;
            double height = random.nextDouble() * 0.5;
            Location crystalLoc = new Location(player.getWorld(), playerLoc.getX() + Math.cos(angle) * distance, playerLoc.getY() + height, playerLoc.getZ() + Math.sin(angle) * distance);

            BlockDisplay crystal = crystalLoc.getWorld().spawn(crystalLoc, BlockDisplay.class);
            crystal.setBlock(Material.BLUE_ICE.createBlockData());

            float crystalScale = 0.3f + random.nextFloat() * 0.4f;
            Transformation transformation = new Transformation(new Vector3f(), new AxisAngle4f((float) Math.toRadians(random.nextInt(360)), 0, 1, 0), new Vector3f(crystalScale, crystalScale * 2, crystalScale), new AxisAngle4f());
            crystal.setTransformation(transformation);
            crystal.setInterpolationDelay(5 + i * 2);
            crystal.setInterpolationDuration(20);
            crystal.setBrightness(new Display.Brightness(15, 15));
            iceBlocks.add(crystal);
        }
    }

    private void spawnFreezeParticles(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || !frozenPlayerIceBlocks.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
                ticks++;
            }
        }.runTaskTimer(BanMace.getInstance(), 0, 5);
    }

    private void scheduleIceRemoval(Player player) {
        UUID playerId = player.getUniqueId();
        if (frozenPlayerTasks.containsKey(playerId)) {
            frozenPlayerTasks.get(playerId).cancel();
        }
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                removeIceEffect(playerId);
                frozenPlayerTasks.remove(playerId);
            }
        };
        frozenPlayerTasks.put(playerId, task);
        task.runTaskLater(BanMace.getInstance(), freezeDuration * 20L);
    }

    private void removeIceEffect(UUID playerId) {
        Set<BlockDisplay> iceBlocks = frozenPlayerIceBlocks.remove(playerId);
        if (iceBlocks != null) {
            for (BlockDisplay block : iceBlocks) {
                if (block != null && !block.isDead()) {
                    Transformation shrink = new Transformation(block.getTransformation().getTranslation(), block.getTransformation().getLeftRotation(), new Vector3f(0.1f), block.getTransformation().getRightRotation());
                    block.setInterpolationDuration(20);
                    block.setTransformation(shrink);
                    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!block.isDead()) block.remove();
                        }
                    }.runTaskLater(BanMace.getInstance(), 20);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (frozenPlayerIceBlocks.containsKey(event.getPlayer().getUniqueId())) {
            if (event.hasChangedPosition()) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getModeName() {
        return "Freeze";
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (BanMace.isHoldingBanMace(damager) && BanMace.getCurrentMode() instanceof FreezeMode) {
            if (target.hasPermission("banmace.bypass")) {
                damager.sendMessage(BanMace.getInstance().getMessage("cannot_use_on_player", Map.of("player", target.getName())));
                return;
            }
            execute(damager, target);
            event.setCancelled(true);
        }
    }

    public void removeAllIceEffects() {
        for (UUID playerId : frozenPlayerIceBlocks.keySet()) {
            removeIceEffect(playerId);
        }
        frozenPlayerTasks.values().forEach(BukkitRunnable::cancel);
        frozenPlayerTasks.clear();
    }
}