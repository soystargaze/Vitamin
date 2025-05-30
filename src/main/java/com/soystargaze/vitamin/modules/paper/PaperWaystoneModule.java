package com.soystargaze.vitamin.modules.paper;

import com.soystargaze.vitamin.database.DatabaseHandler;
import com.soystargaze.vitamin.utils.text.modern.ModernTranslationHandler;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.Particle.DustOptions;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class PaperWaystoneModule implements Listener {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<Location, Waystone> waystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingWaystone> pendingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Waystone> renamingWaystones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> playerTeleportLocations = new ConcurrentHashMap<>();

    private final boolean onlyCreatorCanBreak;
    private final long autoCreateTime;
    private final double autoCreateDistanceSquared;
    private final String defaultWaystoneName;
    private final int teleportDelay;
    private final boolean cancelTeleportOnMove;
    private final int particleViewDistance;
    private final int holoRefreshRate;
    private final int defaultWaystoneLimit;
    private final boolean enableParticles;
    private final boolean enableSounds;
    private final List<String> restrictedWorlds;

    // Constantes para el item custom
    private static final String WAYSTONE_CORE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjJkNWQ5ZDQxZDE5MTdmODZkODIyZDEzZDI2YWEwN2VlNmRlZmVjY2RhMWM5ODg1MDVhYjg2NGFmZTk4YTYwNSJ9fX0=";
    private static final String WAYSTONE_CORE_NAME = "<gold><bold>Waystone Core";
    private static final String WAYSTONE_CORE_LORE = "<gray>Un núcleo mágico que permite crear waystones. <blue>Colócalo en el suelo para activar.";
    private static final String WAYSTONE_CORE_IDENTIFIER = "vitamin_waystone";

    // NamespacedKey para persistent data
    private final NamespacedKey waystoneCoreKey;

    public PaperWaystoneModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.waystoneCoreKey = new NamespacedKey(plugin, "vitamin_id");

        this.onlyCreatorCanBreak = plugin.getConfig().getBoolean("waystone.only_creator_can_break", false);
        this.autoCreateTime = plugin.getConfig().getLong("waystone.auto_create_time", 30000L);
        this.autoCreateDistanceSquared = plugin.getConfig().getDouble("waystone.auto_create_distance_squared", 100.0);
        this.defaultWaystoneName = plugin.getConfig().getString("waystone.default_name", "Waystone");
        this.teleportDelay = plugin.getConfig().getInt("waystone.teleport_delay", 3);
        this.cancelTeleportOnMove = plugin.getConfig().getBoolean("waystone.cancel_teleport_on_move", true);
        this.particleViewDistance = plugin.getConfig().getInt("waystone.particle_view_distance", 32);
        this.holoRefreshRate = plugin.getConfig().getInt("waystone.hologram_refresh_rate", 100);
        this.defaultWaystoneLimit = plugin.getConfig().getInt("waystone.default_limit", 5);
        this.enableParticles = plugin.getConfig().getBoolean("waystone.enable_particles", true);
        this.enableSounds = plugin.getConfig().getBoolean("waystone.enable_sounds", true);
        this.restrictedWorlds = plugin.getConfig().getStringList("waystone.restricted_worlds");

        registerWaystoneCoreRecipe();
        loadWaystones();
        startOptimizedTasks();
    }

    // Método para crear el item custom Waystone Core usando el estilo de VitaminConstants
    private ItemStack createWaystoneCore() {
        ItemStack head = getSkull(WAYSTONE_CORE_TEXTURE);
        ItemMeta meta = head.getItemMeta();

        if (meta != null) {
            Component displayName = MiniMessage.miniMessage()
                    .deserialize(WAYSTONE_CORE_NAME)
                    .decoration(TextDecoration.ITALIC, false);

            List<Component> loreComponents = List.of(
                    MiniMessage.miniMessage()
                            .deserialize(WAYSTONE_CORE_LORE)
                            .decoration(TextDecoration.ITALIC, false)
            );

            LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
            String legacyName = legacy.serialize(displayName);
            List<String> legacyLore = loreComponents.stream()
                    .map(legacy::serialize)
                    .collect(Collectors.toList());

            meta.setDisplayName(legacyName);
            meta.setLore(legacyLore);

            // Agregar NBT para identificación usando el mismo patrón que LumenItems
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(waystoneCoreKey, PersistentDataType.STRING, WAYSTONE_CORE_IDENTIFIER);

            head.setItemMeta(meta);
        }

        return head;
    }

    // Método para crear skull con textura (igual que en LumenItems)
    private ItemStack getSkull(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            try {
                String json = new String(
                        Base64.getDecoder().decode(texture),
                        StandardCharsets.UTF_8
                );
                int i = json.indexOf("\"url\":\"") + 7;
                int j = json.indexOf('"', i);
                String skinUrl = json.substring(i, j);

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), null);
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URI(skinUrl).toURL());
                profile.setTextures(textures);

                meta.setOwnerProfile(profile);
                head.setItemMeta(meta);
            } catch (Exception e) {
                plugin.getLogger().warning("Error aplicando textura al waystone core: " + e.getMessage());
            }
        }

        return head;
    }

    // Método para verificar si un item es un Waystone Core
    private boolean isWaystoneCore(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(waystoneCoreKey, PersistentDataType.STRING) &&
                WAYSTONE_CORE_IDENTIFIER.equals(container.get(waystoneCoreKey, PersistentDataType.STRING));
    }

    // Método para devolver el item al jugador de forma segura
    private void returnWaystoneCoreToPlayer(Player player, Location location) {
        ItemStack waystoneCoreItem = createWaystoneCore();

        // Intentar devolver al inventario primero
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(waystoneCoreItem);
        } else {
            // Si no hay espacio, dropearlo en la ubicación del jugador
            location.getWorld().dropItemNaturally(player.getLocation(), waystoneCoreItem);
            player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.item_returned_dropped"));
        }
    }

    // Registrar la receta del Waystone Core
    private void registerWaystoneCoreRecipe() {
        ItemStack result = createWaystoneCore();

        NamespacedKey recipeKey = new NamespacedKey(plugin, "waystone_core_recipe");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        recipe.shape(
                "EDE",
                "DND",
                "EDE"
        );

        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHER_STAR);

        try {
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("Receta del Waystone Core registrada correctamente");
        } catch (Exception e) {
            plugin.getLogger().warning("Error registrando receta del waystone core: " + e.getMessage());
        }
    }

    private Component processColorCodes(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Component.text(input == null ? "" : input);
        }

        if (hasValidMiniMessageTags(input)) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse MiniMessage in waystone name, falling back to legacy codes: " + input);
            }
        }

        String processed = input.replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(processed);
    }

    private String convertToMiniMessageFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String processed = input.replace('&', '§');

        if (hasValidMiniMessageTags(processed)) {
            return processed;
        }

        processed = processed
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>")
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>")
                .replace("§r", "<reset>");

        return processed;
    }

    private boolean hasValidMiniMessageTags(String text) {
        return text.contains("<") && text.contains(">") &&
                (text.contains("color:") || text.contains("gradient:") ||
                        text.contains("rainbow") || text.contains("bold") ||
                        text.contains("italic") || text.contains("underlined") ||
                        text.contains("strikethrough") || text.contains("obfuscated") ||
                        text.contains("red") || text.contains("green") || text.contains("blue") ||
                        text.contains("yellow") || text.contains("purple") || text.contains("aqua") ||
                        text.contains("white") || text.contains("black") || text.contains("gray") ||
                        text.contains("dark_red") || text.contains("dark_green") || text.contains("dark_blue") ||
                        text.contains("dark_aqua") || text.contains("dark_purple") || text.contains("gold") ||
                        text.contains("dark_gray") || text.contains("light_purple"));
    }

    private void startOptimizedTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<Map.Entry<Location, Waystone>> waystoneList = new ArrayList<>(waystones.entrySet());

            for (Map.Entry<Location, Waystone> entry : waystoneList) {
                Location loc = entry.getKey();
                Waystone waystone = entry.getValue();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!isValidWaystoneStructure(loc, waystone)) {
                        removeWaystoneEntities(waystone);
                        waystones.remove(loc);
                        removeWaystone(waystone);
                    } else {
                        TextDisplay hologram = waystone.getHologram();
                        if (hologram == null || hologram.isDead()) {
                            TextDisplay newHologram = createHologram(loc, waystone.getName());
                            waystone.setHologram(newHologram);
                        }

                        ArmorStand armorStand = waystone.getArmorStand();
                        if (armorStand == null || armorStand.isDead()) {
                            ArmorStand newArmorStand = createWaystoneArmorStand(loc);
                            waystone.setArmorStand(newArmorStand);
                        }

                        if (waystone.getBlockDisplays().isEmpty() ||
                                waystone.getBlockDisplays().stream().anyMatch(bd -> bd == null || bd.isDead())) {
                            waystone.getBlockDisplays().forEach(bd -> {
                                if (bd != null && !bd.isDead()) bd.remove();
                            });
                            waystone.getBlockDisplays().clear();
                            createWaystoneBlockDisplays(loc, waystone);
                        }

                        if (enableParticles && Math.random() < 0.05 && hasPlayersNearby(loc)) {
                            spawnAmbientParticles(loc);
                        }
                    }
                });
            }
        }, holoRefreshRate, holoRefreshRate);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> playerTeleportLocations.forEach((playerId, targetLocation) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && targetLocation != null) {
                if (enableParticles) {
                    spawnTeleportParticles(player.getLocation(), targetLocation);
                }
            }
        }), 2L, 2L);
    }

    private void removeWaystoneEntities(Waystone waystone) {
        TextDisplay hologram = waystone.getHologram();
        if (hologram != null) {
            hologram.remove();
            waystone.setHologram(null);
        }

        ArmorStand armorStand = waystone.getArmorStand();
        if (armorStand != null) {
            armorStand.remove();
            waystone.setArmorStand(null);
        }

        waystone.getBlockDisplays().forEach(bd -> {
            if (bd != null && !bd.isDead()) {
                bd.remove();
            }
        });
        waystone.getBlockDisplays().clear();
    }

    private boolean canCreateWaystone(Player player) {
        UUID playerId = player.getUniqueId();

        if (player.hasPermission("vitamin.module.waystone.limit.infinite")) {
            return true;
        }

        int limit = getPlayerWaystoneLimit(player);

        long playerWaystones = waystones.values().stream()
                .filter(waystone -> waystone.getCreator().equals(playerId))
                .count();

        return playerWaystones < limit;
    }

    private int getPlayerWaystoneLimit(Player player) {
        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission("vitamin.module.waystone.limit." + i)) {
                return i;
            }
        }

        return defaultWaystoneLimit;
    }

    private boolean isValidWaystoneStructure(Location loc, Waystone waystone) {
        ArmorStand armorStand = waystone.getArmorStand();
        if (armorStand != null && !armorStand.isDead()) {
            return armorStand.getLocation().distanceSquared(loc) <= 4;
        }
        return false;
    }

    private boolean hasPlayersNearby(Location location) {
        return !location.getWorld().getNearbyPlayers(location, particleViewDistance).isEmpty();
    }

    private void spawnAmbientParticles(Location location) {
        Location particleLocation = location.clone().add(0.5, 2.0, 0.5);
        location.getWorld().spawnParticle(
                Particle.WITCH,
                particleLocation,
                2,
                0.15, 0.15, 0.15,
                0.01);
    }

    private void spawnTeleportParticles(Location playerLoc, Location targetLoc) {
        if (hasPlayersNearby(playerLoc)) {
            playerLoc.getWorld().spawnParticle(
                    Particle.PORTAL,
                    playerLoc.clone().add(0, 1, 0),
                    15,
                    0.4, 0.8, 0.4,
                    0.08);
        }

        if (hasPlayersNearby(targetLoc)) {
            DustOptions dustOptions = new DustOptions(Color.fromRGB(55, 166, 229), 0.8f);
            targetLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    targetLoc.clone().add(0.5, 1.5, 0.5),
                    8,
                    0.4, 0.8, 0.4,
                    0,
                    dustOptions);
        }
    }

    private void playWaystoneActivateSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
            location.getWorld().playSound(location, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 0.5f, 1.5f);
        }
    }

    private void playWaystoneDeactivateSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
    }

    private void playWaystoneBreakSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.8f);
            location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0f, 0.5f);
        }
    }

    private void playWaystoneCreateSound(Location location) {
        if (enableSounds) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 1.5f);
            location.getWorld().playSound(location, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void spawnCreationParticles(Location location) {
        if (enableParticles) {
            Location particleLocation = location.clone().add(0.5, 1.5, 0.5);
            location.getWorld().spawnParticle(
                    Particle.END_ROD,
                    particleLocation,
                    40,
                    0.5, 0.8, 0.5,
                    0.08);

            location.getWorld().spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    particleLocation,
                    25,
                    0.25, 0.4, 0.25,
                    0.25);
        }
    }

    private void playTeleportBeginSound(Player player) {
        if (enableSounds) {
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }
    }

    private void playTeleportSound(Player player, Location destinationLocation) {
        if (enableSounds) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            destinationLocation.getWorld().playSound(destinationLocation, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void spawnTeleportCompleteParticles(Location location) {
        if (enableParticles) {
            location.getWorld().spawnParticle(
                    Particle.FLASH,
                    location.clone().add(0, 1, 0),
                    1,
                    0.05, 0.05, 0.05,
                    0);

            location.getWorld().spawnParticle(
                    Particle.PORTAL,
                    location.clone().add(0, 1, 0),
                    35,
                    0.4, 0.8, 0.4,
                    0.4);
        }
    }

    private void loadWaystones() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseHandler.WaystoneData> dataList = DatabaseHandler.loadWaystones();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (DatabaseHandler.WaystoneData data : dataList) {
                    Location loc = data.location();
                    Waystone waystone = new Waystone(data.id(), loc, data.name(), data.creator());
                    waystone.setRegisteredPlayers(DatabaseHandler.getRegisteredPlayers(data.id()));
                    waystones.put(loc, waystone);

                    ArmorStand armorStand = createWaystoneArmorStand(loc);
                    waystone.setArmorStand(armorStand);

                    createWaystoneBlockDisplays(loc, waystone);

                    TextDisplay hologram = createHologram(loc, data.name());
                    waystone.setHologram(hologram);
                }
                plugin.getLogger().info("Cargados " + waystones.size() + " waystones desde la base de datos");
            });
        });
    }

    private ArmorStand createWaystoneArmorStand(Location loc) {
        ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(
                loc.clone().add(0.5, 0, 0.5), EntityType.ARMOR_STAND);

        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setInvulnerable(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setCustomName("waystone_entity");
        armorStand.setCustomNameVisible(false);

        return armorStand;
    }

    private void createWaystoneBlockDisplays(Location loc, Waystone waystone) {
        List<BlockDisplay> displays = new ArrayList<>();

        displays.add(createBlockDisplay(loc, Material.CHISELED_STONE_BRICKS, 1f, 0.25f, 1f, 0f, 0f, 0f));
        displays.add(createBlockDisplay(loc, Material.STONE_BRICKS, 0.875f, 0.125f, 0.875f, 0.0625f, 0.25f, 0.0625f));
        displays.add(createBlockDisplay(loc, Material.STONE, 0.625f, 1f, 0.625f, 0.1875f, 0.4375f, 0.1875f));
        displays.add(createBlockDisplay(loc, Material.SMOOTH_QUARTZ, 0.75f, 0.125f, 0.75f, 0.125f, 0.3125f, 0.125f));

        displays.add(createBlockDisplay(loc, Material.SMOOTH_QUARTZ, 0.75f, 0.125f, 0.75f, 0.125f, 1.4375f, 0.125f));
        displays.add(createBlockDisplay(loc, Material.STONE_BRICKS, 0.875f, 0.125f, 0.875f, 0.0625f, 1.5f, 0.0625f));
        displays.add(createBlockDisplay(loc, Material.CHISELED_STONE_BRICKS, 1f, 0.1875f, 1f, 0f, 1.625f, 0f));
        displays.add(createBlockDisplay(loc, Material.SMOOTH_QUARTZ, 0.75f, 0.125f, 0.75f, 0.125f, 1.75f, 0.125f));

        displays.add(createBlockDisplay(loc, Material.STONE, 0.625f, 0.125f, 0.625f, 0.1875f, 1.8125f, 0.1875f));

        waystone.setBlockDisplays(displays);
    }

    private BlockDisplay createBlockDisplay(Location baseLoc, Material material,
                                            float scaleX, float scaleY, float scaleZ,
                                            float offsetX, float offsetY, float offsetZ) {
        BlockDisplay blockDisplay = (BlockDisplay) baseLoc.getWorld().spawnEntity(
                baseLoc.clone().add(offsetX, offsetY, offsetZ), EntityType.BLOCK_DISPLAY);

        blockDisplay.setBlock(material.createBlockData());

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scaleX, scaleY, scaleZ),
                new AxisAngle4f(0, 0, 0, 1)
        );

        blockDisplay.setTransformation(transformation);
        blockDisplay.setBrightness(new Display.Brightness(15, 15));
        blockDisplay.addScoreboardTag("vitaminwaystone");

        return blockDisplay;
    }

    private TextDisplay createHologram(Location loc, String name) {
        TextDisplay hologram = (TextDisplay) loc.getWorld().spawnEntity(
                loc.clone().add(0.5, 2.5, 0.5), EntityType.TEXT_DISPLAY);

        Component nameComponent = processColorCodes(name);
        hologram.text(nameComponent);

        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setSeeThrough(true);
        hologram.setShadowed(false);
        hologram.setBrightness(new Display.Brightness(15, 15));

        return hologram;
    }

    private void saveWaystone(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseHandler.WaystoneData data = new DatabaseHandler.WaystoneData(
                    waystone.getId(), waystone.getLocation(), waystone.getName(), waystone.getCreator());
            int id = DatabaseHandler.saveWaystone(data);
            waystone.setId(id);
            for (UUID playerId : waystone.getRegisteredPlayers()) {
                DatabaseHandler.registerPlayerToWaystone(id, playerId);
            }
        });
    }

    private void removeWaystone(Waystone waystone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                DatabaseHandler.removeWaystone(waystone.getId())
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        // Verificar si es un Waystone Core
        if (isWaystoneCore(item)) {
            plugin.getLogger().info("Waystone Core block place detectado para jugador: " + player.getName());

            event.setCancelled(true); // Cancelar la colocación del bloque

            if (!player.hasPermission("vitamin.module.waystone") ||
                    !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
                returnWaystoneCoreToPlayer(player, event.getBlock().getLocation());
                return;
            }

            Location blockLocation = event.getBlock().getLocation();

            // Verificar mundo restringido
            if (restrictedWorlds.contains(blockLocation.getWorld().getName())) {
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.restricted_world"));
                returnWaystoneCoreToPlayer(player, blockLocation);
                return;
            }

            // Verificar que no hay otro waystone en la misma ubicación
            if (waystones.containsKey(blockLocation)) {
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.already_exists"));
                returnWaystoneCoreToPlayer(player, blockLocation);
                return;
            }

            // Verificar límites de waystone
            if (!canCreateWaystone(player)) {
                int limit = getPlayerWaystoneLimit(player);
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.limit_reached", String.valueOf(limit)));
                returnWaystoneCoreToPlayer(player, blockLocation);
                return;
            }

            // Remover el item de la mano del jugador
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }

            // Iniciar el proceso de creación
            UUID playerId = player.getUniqueId();
            pendingWaystones.put(playerId, new PendingWaystone(blockLocation, System.currentTimeMillis()));
            playWaystoneActivateSound(blockLocation);
            player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.enter_new_name"));

            plugin.getLogger().info("Proceso de creación de waystone iniciado para: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (renamingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            Component messageComponent = event.message();
            String newName = PlainTextComponentSerializer.plainText().serialize(messageComponent).trim();
            Waystone waystone = renamingWaystones.remove(playerId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(waystone.getLocation()) && waystone.getHologram() != null) {
                    Component nameComponent = processColorCodes(newName);
                    waystone.getHologram().text(nameComponent);
                    waystone.setName(newName);

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                            DatabaseHandler.updateWaystoneName(waystone.getId(), newName)
                    );

                    if (enableSounds) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.8f);
                    }
                    if (enableParticles) {
                        Location waystoneLoc = waystone.getLocation();
                        waystoneLoc.getWorld().spawnParticle(
                                Particle.ENCHANT,
                                waystoneLoc.clone().add(0.5, 2.5, 0.5),
                                15,
                                0.4, 0.4, 0.4,
                                0.8);
                    }

                    String miniMessageFormattedName = convertToMiniMessageFormat(newName);
                    player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.renamed", miniMessageFormattedName));
                } else {
                    player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.renaming_canceled"));
                }
            });
            return;
        }

        if (pendingWaystones.containsKey(playerId)) {
            event.setCancelled(true);
            PendingWaystone pending = pendingWaystones.get(playerId);
            Location loc = pending.location();

            if (!canCreateWaystone(player)) {
                int limit = getPlayerWaystoneLimit(player);
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.limit_reached", String.valueOf(limit)));
                pendingWaystones.remove(playerId);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    playWaystoneDeactivateSound(loc);
                    returnWaystoneCoreToPlayer(player, loc);
                });
                return;
            }

            Component messageComponent = event.message();
            String name = PlainTextComponentSerializer.plainText().serialize(messageComponent).trim();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (waystones.containsKey(loc)) {
                    player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.already_exists"));
                    playWaystoneDeactivateSound(loc);
                    returnWaystoneCoreToPlayer(player, loc);
                    return;
                }

                if (!canCreateWaystone(player)) {
                    int limit = getPlayerWaystoneLimit(player);
                    player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.limit_reached", String.valueOf(limit)));
                    playWaystoneDeactivateSound(loc);
                    returnWaystoneCoreToPlayer(player, loc);
                    return;
                }

                Waystone waystone = new Waystone(-1, loc, name, playerId);
                waystone.registerPlayer(playerId);

                ArmorStand armorStand = createWaystoneArmorStand(loc);
                waystone.setArmorStand(armorStand);

                createWaystoneBlockDisplays(loc, waystone);

                TextDisplay hologram = createHologram(loc, name);
                waystone.setHologram(hologram);

                waystones.put(loc, waystone);
                saveWaystone(waystone);

                playWaystoneCreateSound(loc);
                spawnCreationParticles(loc);

                String miniMessageFormattedName = convertToMiniMessageFormat(name);
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.created", miniMessageFormattedName));
            });
            pendingWaystones.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("vitamin.module.waystone") ||
                !DatabaseHandler.isModuleEnabledForPlayer(player.getUniqueId(), "module.waystone")) {
            return;
        }

        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand)) return;

        Waystone waystone = waystones.values().stream()
                .filter(ws -> ws.getArmorStand() != null && ws.getArmorStand().equals(entity))
                .findFirst().orElse(null);

        if (waystone == null) return;

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();

        if (player.isSneaking() && waystone.getCreator().equals(playerId)) {
            renamingWaystones.put(playerId, waystone);
            player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.enter_new_name_for_rename"));
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
            if (enableParticles) {
                Location waystoneLoc = waystone.getLocation();
                waystoneLoc.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        waystoneLoc.clone().add(0.5, 2.0, 0.5),
                        8,
                        0.3, 0.3, 0.3,
                        0.08);
            }
            return;
        }

        if (!waystone.isRegistered(playerId)) {
            waystone.registerPlayer(playerId);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    DatabaseHandler.registerPlayerToWaystone(waystone.getId(), playerId)
            );

            String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
            player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.registered", miniMessageFormattedName));

            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 1.2f);
            }
            if (enableParticles) {
                Location waystoneLoc = waystone.getLocation();
                waystoneLoc.getWorld().spawnParticle(
                        Particle.COMPOSTER,
                        waystoneLoc.clone().add(0.5, 1.8, 0.5),
                        12,
                        0.25, 0.25, 0.25,
                        0.08);
            }
        } else {
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 0.8f, 1.5f);
            }
        }

        openWaystoneInventory(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof ArmorStand armorStand)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!tool.getType().name().endsWith("_PICKAXE")) return;

        Waystone waystone = null;
        for (Map.Entry<Location, Waystone> entry : waystones.entrySet()) {
            if (entry.getValue().getArmorStand() != null &&
                    entry.getValue().getArmorStand().equals(armorStand)) {
                waystone = entry.getValue();
                break;
            }
        }

        if (waystone == null) return;

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        boolean isCreator = waystone.getCreator().equals(playerId);
        boolean isAdmin = player.hasPermission("vitamin.module.waystone.admin");
        boolean isOp = player.isOp();

        if (onlyCreatorCanBreak && !isCreator) {
            if (!(isAdmin && (isOp || waystone.isAdminCreated()))) {
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.only_creator_can_break"));
                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
                }
                return;
            }
        }

        if (waystone.isAdminCreated() && !(isOp || isCreator)) {
            player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.only_operator_or_creator_can_break"));
            if (enableSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
            return;
        }

        Location loc = waystone.getLocation();

        removeWaystoneEntities(waystone);
        waystones.remove(loc);
        removeWaystone(waystone);

        // Entregar el Waystone Core exacto al jugador
        ItemStack waystoneCoreItem = createWaystoneCore();
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(waystoneCoreItem);
        } else {
            loc.getWorld().dropItemNaturally(loc, waystoneCoreItem);
        }

        playWaystoneBreakSound(loc);
        if (enableParticles) {
            loc.getWorld().spawnParticle(
                    Particle.LAVA,
                    loc.clone().add(0.5, 1.5, 0.5),
                    15,
                    0.4, 0.8, 0.4,
                    0.15);
        }

        String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
        player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.destroyed", miniMessageFormattedName));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        Iterator<Map.Entry<UUID, PendingWaystone>> pendingIterator = pendingWaystones.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<UUID, PendingWaystone> entry = pendingIterator.next();
            Location baseLoc = entry.getValue().location();
            if (loc.equals(baseLoc)) {
                UUID playerId = entry.getKey();
                pendingIterator.remove();
                playWaystoneDeactivateSound(baseLoc);
                Player targetPlayer = Bukkit.getPlayer(playerId);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.creation_canceled"));
                    returnWaystoneCoreToPlayer(targetPlayer, baseLoc);
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (cancelTeleportOnMove && pendingTeleports.containsKey(playerId)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
                BukkitTask task = pendingTeleports.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
                playerTeleportLocations.remove(playerId);

                if (enableSounds) {
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
                if (enableParticles) {
                    player.getWorld().spawnParticle(
                            Particle.SMOKE,
                            player.getLocation().add(0, 1, 0),
                            15,
                            0.25, 0.4, 0.25,
                            0.04);
                }

                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.teleport_canceled"));
            }
            return;
        }

        if (!pendingWaystones.containsKey(playerId)) return;

        PendingWaystone pending = pendingWaystones.get(playerId);
        long timeElapsed = System.currentTimeMillis() - pending.creationTime();
        if (timeElapsed > autoCreateTime) {
            Location loc = pending.location();
            if (player.getLocation().distanceSquared(loc) > autoCreateDistanceSquared) {
                if (!canCreateWaystone(player)) {
                    player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.auto_creation_canceled_limit"));
                    pendingWaystones.remove(playerId);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        playWaystoneDeactivateSound(loc);
                        returnWaystoneCoreToPlayer(player, loc);
                    });
                    return;
                }

                Waystone waystone = new Waystone(-1, loc, defaultWaystoneName, playerId);
                waystone.registerPlayer(playerId);

                ArmorStand armorStand = createWaystoneArmorStand(loc);
                waystone.setArmorStand(armorStand);

                createWaystoneBlockDisplays(loc, waystone);

                TextDisplay hologram = createHologram(loc, defaultWaystoneName);
                waystone.setHologram(hologram);

                waystones.put(loc, waystone);
                saveWaystone(waystone);

                playWaystoneCreateSound(loc);
                spawnCreationParticles(loc);

                String miniMessageFormattedName = convertToMiniMessageFormat(defaultWaystoneName);
                player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.created_with_default", miniMessageFormattedName));
                pendingWaystones.remove(playerId);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Si el jugador se desconecta mientras está en proceso de crear waystone, devolver el item
        if (pendingWaystones.containsKey(playerId)) {
            PendingWaystone pending = pendingWaystones.remove(playerId);
            // El item se devolverá cuando el jugador se reconecte si es necesario
        }

        renamingWaystones.remove(playerId);
        playerTeleportLocations.remove(playerId);
        if (pendingTeleports.containsKey(playerId)) {
            pendingTeleports.get(playerId).cancel();
            pendingTeleports.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component titleComponent = ModernTranslationHandler.getComponent("waystone.inventory.title");
        Component viewTitleComponent = event.getView().title();

        if (!titleComponent.equals(viewTitleComponent)) return;

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        Component displayNameComponent = clickedItem.getItemMeta().displayName();
        if (displayNameComponent == null) return;

        String cleanDisplayName = PlainTextComponentSerializer.plainText()
                .serialize(displayNameComponent).trim();

        for (Waystone waystone : waystones.values()) {
            if (waystone.isRegistered(player.getUniqueId())) {
                Component processedComponent = processColorCodes(waystone.getName());
                String cleanWaystoneName = PlainTextComponentSerializer.plainText()
                        .serialize(processedComponent).trim();

                if (cleanWaystoneName.equals(cleanDisplayName)) {
                    player.closeInventory();
                    UUID playerId = player.getUniqueId();

                    if (pendingTeleports.containsKey(playerId)) {
                        pendingTeleports.get(playerId).cancel();
                        pendingTeleports.remove(playerId);
                        playerTeleportLocations.remove(playerId);
                    }

                    playTeleportBeginSound(player);
                    final Location destination = waystone.getLocation().clone().add(0.5, 0.2, 0.5);
                    playerTeleportLocations.put(playerId, destination);

                    BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Location originalLoc = player.getLocation();
                        playTeleportSound(player, destination);
                        player.teleport(destination);
                        spawnTeleportCompleteParticles(destination);
                        if (enableParticles) {
                            originalLoc.getWorld().spawnParticle(
                                    Particle.REVERSE_PORTAL,
                                    originalLoc.clone().add(0, 1, 0),
                                    30,
                                    0.3, 0.6, 0.3,
                                    0.08);
                        }

                        String miniMessageFormattedName = convertToMiniMessageFormat(waystone.getName());
                        player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.teleported", miniMessageFormattedName));

                        pendingTeleports.remove(playerId);
                        playerTeleportLocations.remove(playerId);
                    }, teleportDelay * 20L);

                    pendingTeleports.put(playerId, task);

                    for (int i = 1; i <= teleportDelay; i++) {
                        final int count = i;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (enableParticles) {
                                player.getWorld().spawnParticle(
                                        Particle.ELECTRIC_SPARK,
                                        player.getLocation().add(0, 1.8, 0),
                                        8,
                                        0.25, 0.25, 0.25,
                                        0);
                            }
                            if (enableSounds && count < teleportDelay) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.5f, 1.0f + (count * 0.1f));
                            }
                        }, i * 20L);
                    }

                    player.sendMessage(ModernTranslationHandler.getPlayerComponent("waystone.teleporting_in", String.valueOf(teleportDelay)));
                    break;
                }
            }
        }
    }

    private void openWaystoneInventory(Player player) {
        Component titleComponent = ModernTranslationHandler.getComponent("waystone.inventory.title");

        Inventory inv = Bukkit.createInventory(null, 27, titleComponent);

        for (Waystone waystone : waystones.values()) {
            if (waystone.isRegistered(player.getUniqueId())) {
                ItemStack item = new ItemStack(Material.LODESTONE);
                ItemMeta meta = item.getItemMeta();

                Component nameComponent = processColorCodes(waystone.getName())
                        .decoration(TextDecoration.ITALIC, false);
                meta.displayName(nameComponent);

                List<Component> lore = new ArrayList<>();

                Component locationComponent = ModernTranslationHandler.getComponent(
                        "waystone.inventory.item.location",
                        waystone.getLocation().getBlockX(),
                        waystone.getLocation().getBlockY(),
                        waystone.getLocation().getBlockZ()
                ).decoration(TextDecoration.ITALIC, false);

                Component clickComponent = ModernTranslationHandler.getComponent("waystone.inventory.item.click_to_teleport")
                        .decoration(TextDecoration.ITALIC, false);

                lore.add(locationComponent);
                lore.add(clickComponent);

                lore.add(Component.text("§0§k" + waystone.getName(), NamedTextColor.BLACK)
                        .decoration(TextDecoration.OBFUSCATED, true));

                meta.lore(lore);
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
    }

    private static class Waystone {
        private volatile int id;
        private final Location location;
        private volatile String name;
        private final UUID creator;
        private final Set<UUID> registeredPlayers = ConcurrentHashMap.newKeySet();
        private volatile TextDisplay hologram;
        private volatile ArmorStand armorStand;
        private volatile List<BlockDisplay> blockDisplays = new ArrayList<>();
        private final boolean isAdminCreated;

        public Waystone(int id, Location location, String name, UUID creator) {
            this.id = id;
            this.location = location;
            this.name = name;
            this.creator = creator;
            this.isAdminCreated = Bukkit.getPlayer(creator) != null &&
                    Objects.requireNonNull(Bukkit.getPlayer(creator)).hasPermission("vitamin.module.waystone.admin");
        }

        public void setId(int id) { this.id = id; }
        public int getId() { return id; }
        public void registerPlayer(UUID playerId) { registeredPlayers.add(playerId); }
        public boolean isRegistered(UUID playerId) { return registeredPlayers.contains(playerId); }
        public Location getLocation() { return location; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public UUID getCreator() { return creator; }
        public TextDisplay getHologram() { return hologram; }
        public void setHologram(TextDisplay hologram) { this.hologram = hologram; }
        public ArmorStand getArmorStand() { return armorStand; }
        public void setArmorStand(ArmorStand armorStand) { this.armorStand = armorStand; }
        public List<BlockDisplay> getBlockDisplays() { return blockDisplays; }
        public void setBlockDisplays(List<BlockDisplay> blockDisplays) { this.blockDisplays = blockDisplays; }
        public Set<UUID> getRegisteredPlayers() { return new HashSet<>(registeredPlayers); }
        public void setRegisteredPlayers(Set<UUID> players) {
            registeredPlayers.clear();
            registeredPlayers.addAll(players);
        }
        public boolean isAdminCreated() { return isAdminCreated; }
    }

    private record PendingWaystone(Location location, long creationTime) {}
}