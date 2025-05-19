package com.soystargaze.vitamin.modules.core;

import com.soystargaze.vitamin.config.ConfigHandler;
import com.soystargaze.vitamin.database.DatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ArmorTrimModule implements Listener {

    private final Set<UUID> provokedPlayers = new HashSet<>();
    private final ArmorTrimManager trimManager;

    public ArmorTrimModule() {
        trimManager = new ArmorTrimManager();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    trimManager.updatePlayerTrims(player);
                    trimManager.applyEffects(player);
                }
            }
        }.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Vitamin")), 0L, 20L);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Piglin && event.getTarget() instanceof Player player) {
            if (!player.hasPermission("vitamin.module.armor_trim") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.armor_trim")) {
                return;
            }
            if (!provokedPlayers.contains(player.getUniqueId()) && trimManager.hasGoldArmorTrim(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof Piglin) {
            provokedPlayers.add(player.getUniqueId());
        }
        trimManager.handleDamage(event);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        trimManager.handleSneak(event.getPlayer(), event.isSneaking());
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        trimManager.handleExpChange(event);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        trimManager.handleInventoryOpen(event);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        trimManager.handleGenericDamage(event);
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        trimManager.handleEnchantItem(event);
    }
}

class ArmorTrimManager {
    private final Map<UUID, Map<TrimMaterial, Integer>> playerTrimCounts = new HashMap<>();
    private final Map<TrimMaterial, List<Effect>> trimEffects = new HashMap<>();

    private static final Map<String, TrimMaterial> MATERIAL_MAP = new HashMap<>();

    static {
        MATERIAL_MAP.put("COPPER", TrimMaterial.COPPER);
        MATERIAL_MAP.put("IRON", TrimMaterial.IRON);
        MATERIAL_MAP.put("REDSTONE", TrimMaterial.REDSTONE);
        MATERIAL_MAP.put("EMERALD", TrimMaterial.EMERALD);
        MATERIAL_MAP.put("NETHERITE", TrimMaterial.NETHERITE);
        MATERIAL_MAP.put("LAPIS", TrimMaterial.LAPIS);
        MATERIAL_MAP.put("QUARTZ", TrimMaterial.QUARTZ);
        MATERIAL_MAP.put("DIAMOND", TrimMaterial.DIAMOND);
        MATERIAL_MAP.put("AMETHYST", TrimMaterial.AMETHYST);
        MATERIAL_MAP.put("GOLD", TrimMaterial.GOLD);
    }

    public ArmorTrimManager() {
        FileConfiguration config = ConfigHandler.getConfig();
        for (String materialKey : MATERIAL_MAP.keySet()) {
            if (config.getBoolean("armor_trims." + materialKey + ".enabled", true)) {
                List<Effect> effects = new ArrayList<>();
                switch (materialKey) {
                    case "COPPER":
                        if (config.getBoolean("armor_trims." + materialKey + ".haste_on_sneak.enabled", true)) {
                            int durationSeconds = config.getInt("armor_trims." + materialKey + ".haste_on_sneak.duration", 15);
                            int cooldownSeconds = config.getInt("armor_trims." + materialKey + ".haste_on_sneak.cooldown", 60);
                            effects.add(new HasteOnSneakEffect(durationSeconds * 20, cooldownSeconds));
                        }
                        break;
                    case "IRON":
                        if (config.getBoolean("armor_trims." + materialKey + ".magnet.enabled", true)) {
                            double baseRadius = config.getDouble("armor_trims." + materialKey + ".magnet.base_radius", 1.0);
                            effects.add(new MagnetEffect(baseRadius));
                        }
                        break;
                    case "REDSTONE":
                        if (config.getBoolean("armor_trims." + materialKey + ".speed_on_sneak.enabled", true)) {
                            double speedBoost = config.getDouble("armor_trims." + materialKey + ".speed_on_sneak.speed_boost", 0.05);
                            int durationSeconds = config.getInt("armor_trims." + materialKey + ".speed_on_sneak.duration", 5);
                            effects.add(new SpeedOnSneakEffect(speedBoost, durationSeconds));
                        }
                        break;
                    case "EMERALD":
                        effects.add(new HeroOfTheVillageEffect());
                        break;
                    case "NETHERITE":
                        if (config.getBoolean("armor_trims." + materialKey + ".fire_resistance.enabled", true)) {
                            int durationSeconds = config.getInt("armor_trims." + materialKey + ".fire_resistance.duration", 10);
                            int cooldownSeconds = config.getInt("armor_trims." + materialKey + ".fire_resistance.cooldown", 30);
                            effects.add(new FireResistanceEffect(durationSeconds, cooldownSeconds));
                        }
                        if (config.getBoolean("armor_trims." + materialKey + ".knockback_resistance.enabled", true)) {
                            effects.add(new KnockbackResistanceEffect());
                        }
                        break;
                    case "LAPIS":
                        if (config.getBoolean("armor_trims." + materialKey + ".extra_exp.enabled", true)) {
                            double extraPerPiece = config.getDouble("armor_trims." + materialKey + ".extra_exp.extra_per_piece", 0.10);
                            effects.add(new ExtraExpEffect(extraPerPiece));
                        }
                        if (config.getBoolean("armor_trims." + materialKey + ".enchant_exp_refund.enabled", true)) {
                            double probabilityPerPiece = config.getDouble("armor_trims." + materialKey + ".enchant_exp_refund.probability_per_piece", 0.05);
                            double refundPercentage = config.getDouble("armor_trims." + materialKey + ".enchant_exp_refund.refund_percentage", 0.15);
                            effects.add(new EnchantExpRefundEffect(probabilityPerPiece, refundPercentage));
                        }
                        break;
                    case "QUARTZ":
                        if (config.getBoolean("armor_trims." + materialKey + ".night_vision_on_sneak.enabled", true)) {
                            effects.add(new NightVisionOnSneakEffect());
                        }
                        break;
                    case "DIAMOND":
                        if (config.getBoolean("armor_trims." + materialKey + ".armor_boost.enabled", true)) {
                            double reductionPerPiece = config.getDouble("armor_trims." + materialKey + ".armor_boost.reduction_per_piece", 0.08);
                            effects.add(new ArmorBoostEffect(reductionPerPiece));
                        }
                        break;
                    case "AMETHYST":
                        if (config.getBoolean("armor_trims." + materialKey + ".regeneration_on_damage.enabled", true)) {
                            int durationSeconds = config.getInt("armor_trims." + materialKey + ".regeneration_on_damage.duration", 8);
                            int cooldownSeconds = config.getInt("armor_trims." + materialKey + ".regeneration_on_damage.cooldown", 15);
                            double radius = config.getDouble("armor_trims." + materialKey + ".regeneration_on_damage.radius", 5.0);
                            effects.add(new RegenerationOnDamageEffect(durationSeconds, cooldownSeconds, radius));
                        }
                        break;
                    // No effects defined for GOLD by default, handled in hasGoldArmorTrim
                }
                if (!effects.isEmpty()) {
                    trimEffects.put(MATERIAL_MAP.get(materialKey), effects);
                }
            }
        }
    }

    public void updatePlayerTrims(Player player) {
        Map<TrimMaterial, Integer> counts = new HashMap<>();
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()) {
                ItemMeta meta = armor.getItemMeta();
                if (meta instanceof ArmorMeta armorMeta) {
                    ArmorTrim trim = armorMeta.getTrim();
                    if (trim != null) {
                        counts.put(trim.getMaterial(), counts.getOrDefault(trim.getMaterial(), 0) + 1);
                    }
                }
            }
        }
        playerTrimCounts.put(player.getUniqueId(), counts);
    }

    public void applyEffects(Player player) {
        Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
        for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
            List<Effect> effects = trimEffects.get(entry.getKey());
            if (effects != null) {
                for (Effect effect : effects) {
                    effect.apply(player, entry.getValue());
                }
            }
        }
    }

    public boolean hasGoldArmorTrim(Player player) {
        FileConfiguration config = ConfigHandler.getConfig();
        if (!config.getBoolean("armor_trims.GOLD.enabled", true)) {
            return false;
        }
        Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
        return counts.getOrDefault(TrimMaterial.GOLD, 0) > 0;
    }

    public void handleSneak(Player player, boolean isSneaking) {
        Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
        for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
            List<Effect> effects = trimEffects.get(entry.getKey());
            if (effects != null) {
                for (Effect effect : effects) {
                    if (effect instanceof SneakListenerEffect) {
                        ((SneakListenerEffect) effect).onSneak(player, entry.getValue(), isSneaking);
                    }
                }
            }
        }
    }

    public void handleDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
            for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
                List<Effect> effects = trimEffects.get(entry.getKey());
                if (effects != null) {
                    for (Effect effect : effects) {
                        if (effect instanceof DamageListenerEffect) {
                            ((DamageListenerEffect) effect).onDamage(event, entry.getValue());
                        }
                    }
                }
            }
        }
    }

    public void handleExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
        for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
            List<Effect> effects = trimEffects.get(entry.getKey());
            if (effects != null) {
                for (Effect effect : effects) {
                    if (effect instanceof ExpListenerEffect) {
                        ((ExpListenerEffect) effect).onExpChange(event, entry.getValue());
                    }
                }
            }
        }
    }

    public void handleInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory() instanceof MerchantInventory) {
            Player player = (Player) event.getPlayer();
            Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
            for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
                List<Effect> effects = trimEffects.get(entry.getKey());
                if (effects != null) {
                    for (Effect effect : effects) {
                        if (effect instanceof InventoryListenerEffect) {
                            ((InventoryListenerEffect) effect).onInventoryOpen(event, entry.getValue());
                        }
                    }
                }
            }
        }
    }

    public void handleGenericDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
            for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
                List<Effect> effects = trimEffects.get(entry.getKey());
                if (effects != null) {
                    for (Effect effect : effects) {
                        if (effect instanceof GenericDamageListenerEffect) {
                            ((GenericDamageListenerEffect) effect).onGenericDamage(event, entry.getValue());
                        }
                    }
                }
            }
        }
    }

    public void handleEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Map<TrimMaterial, Integer> counts = playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>());
        for (Map.Entry<TrimMaterial, Integer> entry : counts.entrySet()) {
            List<Effect> effects = trimEffects.get(entry.getKey());
            if (effects != null) {
                for (Effect effect : effects) {
                    if (effect instanceof EnchantListenerEffect) {
                        ((EnchantListenerEffect) effect).onEnchant(event, entry.getValue());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public int getTrimStack(Player player, TrimMaterial material) {
        return playerTrimCounts.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(material, 0);
    }
}

interface Effect {
    void apply(Player player, int stack);
}

interface SneakListenerEffect extends Effect {
    void onSneak(Player player, int stack, boolean isSneaking);
}

interface DamageListenerEffect extends Effect {
    void onDamage(EntityDamageByEntityEvent event, int stack);
}

interface ExpListenerEffect extends Effect {
    void onExpChange(PlayerExpChangeEvent event, int stack);
}

interface InventoryListenerEffect extends Effect {
    void onInventoryOpen(InventoryOpenEvent event, int stack);
}

interface GenericDamageListenerEffect extends Effect {
    void onGenericDamage(EntityDamageEvent event, int stack);
}

interface EnchantListenerEffect extends Effect {
    void onEnchant(EnchantItemEvent event, int stack);
}

class HasteOnSneakEffect implements SneakListenerEffect {
    private final int duration; // ticks
    private final int cooldown; // seconds
    private final Map<UUID, Long> lastActivatedMap = new HashMap<>();

    public HasteOnSneakEffect(int duration, int cooldown) {
        this.duration = duration;
        this.cooldown = cooldown;
    }

    @Override
    public void onSneak(Player player, int stack, boolean isSneaking) {
        if (isSneaking) {
            long now = System.currentTimeMillis();
            UUID uuid = player.getUniqueId();
            Long lastActivated = lastActivatedMap.get(uuid);
            if (lastActivated == null || now - lastActivated >= cooldown * 1000L) {
                int level = stack - 1; // Levels (0 for lvl 1, 1 for lvl 2, etc.)
                if (level >= 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, level, false, false));
                    lastActivatedMap.put(uuid, now);
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10 * stack, 0.5, 0.5, 0.5, 1);
                }
            }
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class HeroOfTheVillageEffect implements InventoryListenerEffect {
    @Override
    public void onInventoryOpen(InventoryOpenEvent event, int stack) {
        if (event.getInventory() instanceof MerchantInventory) {
            Player player = (Player) event.getPlayer();
            int level = Math.min(stack, 5);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 600, level - 1, false, false));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10 * stack, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class EnchantExpRefundEffect implements EnchantListenerEffect {
    private final double probabilityPerPiece;
    private final double refundPercentage;

    public EnchantExpRefundEffect(double probabilityPerPiece, double refundPercentage) {
        this.probabilityPerPiece = probabilityPerPiece;
        this.refundPercentage = refundPercentage;
    }

    @Override
    public void onEnchant(EnchantItemEvent event, int stack) {
        double probability = probabilityPerPiece * stack;
        if (Math.random() < probability) {
            int expSpent = event.getExpLevelCost() * 30;
            int refund = (int) (expSpent * refundPercentage);
            event.getEnchanter().giveExp(refund);
            Player player = event.getEnchanter();
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 15 * stack, 0.5, 0.5, 0.5, 1);
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class MagnetEffect implements Effect {
    private final double baseRadius;

    public MagnetEffect(double baseRadius) {
        this.baseRadius = baseRadius;
    }

    @Override
    public void apply(Player player, int stack) {
        double radius = baseRadius * stack;
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof Item) {
                Vector direction = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                entity.setVelocity(direction.multiply(0.1));
                Location itemLoc = entity.getLocation();
                Location playerLoc = player.getLocation();
                Vector particleDir = playerLoc.toVector().subtract(itemLoc.toVector()).normalize();
                for (double i = 0; i < itemLoc.distance(playerLoc); i += 0.5) {
                    Location particleLoc = itemLoc.clone().add(particleDir.clone().multiply(i));
                    player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}

class SpeedOnSneakEffect implements SneakListenerEffect {
    private final double speedBoost;
    private final int duration;

    public SpeedOnSneakEffect(double speedBoost, int duration) {
        this.speedBoost = speedBoost;
        this.duration = duration;
    }

    @Override
    public void onSneak(Player player, int stack, boolean isSneaking) {
        if (isSneaking) {
            int amplifier = (int) (speedBoost * stack * 10);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, amplifier));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 10 * stack, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class FireResistanceEffect implements Effect {
    private final int duration;
    private final int cooldown;
    private final Map<UUID, Long> lastAppliedMap = new HashMap<>();

    public FireResistanceEffect(int duration, int cooldown) {
        this.duration = duration;
        this.cooldown = cooldown;
    }

    @Override
    public void apply(Player player, int stack) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Long lastApplied = lastAppliedMap.get(uuid);
        if (lastApplied == null || now - lastApplied >= cooldown * 1000L) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration * 20 * stack, 0));
            lastAppliedMap.put(uuid, now);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 10 * stack, 0.5, 0.5, 0.5, 0.1);
        }
    }
}

class KnockbackResistanceEffect implements DamageListenerEffect {
    @Override
    public void onDamage(EntityDamageByEntityEvent event, int stack) {
        if (event.getEntity() instanceof Player player) {
            Vector knockback = event.getDamager().getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            player.setVelocity(knockback.multiply(0.5 / stack));
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 5 * stack, 0.5, 0.5, 0.5, 0, Material.NETHERITE_BLOCK.createBlockData());
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class ExtraExpEffect implements ExpListenerEffect {
    private final double extraPerPiece;

    public ExtraExpEffect(double extraPerPiece) {
        this.extraPerPiece = extraPerPiece;
    }

    @Override
    public void onExpChange(PlayerExpChangeEvent event, int stack) {
        double extra = extraPerPiece * stack;
        int additionalExp = (int) (event.getAmount() * extra);
        event.setAmount(event.getAmount() + additionalExp);
        Player player = event.getPlayer();
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 10 * stack, 0.5, 0.5, 0.5, 1);
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class NightVisionOnSneakEffect implements SneakListenerEffect {
    @Override
    public void onSneak(Player player, int stack, boolean isSneaking) {
        if (isSneaking) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 10 * stack, 0.5, 0.5, 0.5, 0.1);
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class ArmorBoostEffect implements GenericDamageListenerEffect {
    private final double reductionPerPiece;

    public ArmorBoostEffect(double reductionPerPiece) {
        this.reductionPerPiece = reductionPerPiece;
    }

    @Override
    public void onGenericDamage(EntityDamageEvent event, int stack) {
        double reduction = reductionPerPiece * stack;
        event.setDamage(event.getDamage() * (1 - reduction));
        Player player = (Player) event.getEntity();
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 10 * stack, 0.5, 0.5, 0.5, 0, Material.DIAMOND_BLOCK.createBlockData());
    }

    @Override
    public void apply(Player player, int stack) {
    }
}

class RegenerationOnDamageEffect implements DamageListenerEffect {
    private final int duration;
    private final int cooldown;
    private final double radius;
    private final Map<UUID, Long> lastAppliedMap = new HashMap<>();

    public RegenerationOnDamageEffect(int duration, int cooldown, double radius) {
        this.duration = duration;
        this.cooldown = cooldown;
        this.radius = radius;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event, int stack) {
        if (event.getEntity() instanceof Player player) {
            long now = System.currentTimeMillis();
            UUID uuid = player.getUniqueId();
            Long lastApplied = lastAppliedMap.get(uuid);
            if (lastApplied == null || now - lastApplied >= cooldown * 1000L) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration * 20 * stack, 0));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5 * stack, 0.5, 0.5, 0.5, 0.1);

                for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
                    if (nearby instanceof Player ally) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration * 20 * stack, 0));
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1, 0), 5 * stack, 0.5, 0.5, 0.5, 0.1);
                        ally.getWorld().spawnParticle(Particle.SONIC_BOOM, ally.getLocation(), stack);
                    }
                }
                lastAppliedMap.put(uuid, now);
            }
        }
    }

    @Override
    public void apply(Player player, int stack) {
    }
}