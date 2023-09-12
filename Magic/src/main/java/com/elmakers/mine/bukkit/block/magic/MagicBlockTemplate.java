package com.elmakers.mine.bukkit.block.magic;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.elmakers.mine.bukkit.api.effect.EffectPlayer;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.magic.Mage;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.warp.MagicWarpDescription;
import com.google.common.base.Preconditions;

public class MagicBlockTemplate {
    @Nonnull
    private final MageController controller;
    @Nonnull
    private final ConfigurationSection configuration;
    @Nonnull
    private final String key;
    @Nullable
    private String name;
    @Nullable
    private String description;
    private int interval;
    @Nullable
    private Spawner spawner;
    @Nullable
    private Caster caster;
    @Nullable
    private InteractCaster interactCaster;
    @Nullable
    private Collection<EffectPlayer> effects;
    @Nullable
    private MagicWarpDescription portalWarp;
    @Nullable
    private String portalSpell;

    private final int playerRange;
    private final int minPlayers;
    private final Integer minTimeOfDay;
    private final Integer maxTimeOfDay;
    private final Integer minPhaseOfMoon;
    private final Integer maxPhaseOfMoon;
    private final boolean alwaysActive;
    private final boolean removeWhenBroken;
    private final boolean remove;
    private final String dropWhenRemoved;

    public MagicBlockTemplate(@Nonnull MageController controller, @Nonnull String key, @Nonnull ConfigurationSection configuration) {
        this.key = Preconditions.checkNotNull(key);
        this.controller = controller;
        this.configuration = configuration;
        name = configuration.getString("name");
        description = configuration.getString("description");
        interval = configuration.getInt("interval", 0);
        alwaysActive = configuration.getBoolean("always_active", false);
        removeWhenBroken = configuration.getBoolean("remove_when_broken", false);
        remove = configuration.getBoolean("remove", false);
        dropWhenRemoved = configuration.getString("drop_when_removed");
        portalSpell = configuration.getString("portal_spell");
        String portalWarpKey = configuration.getString("portal_warp");
        if (portalWarpKey != null) {
            portalWarp = new MagicWarpDescription(controller, portalWarpKey, configuration.getBoolean("warp_maintain_direction", true));
            Collection<EffectPlayer> warpEffects = null;
            if (configuration.isList("warp_effects")) {
                warpEffects = controller.loadEffects(configuration, "warp_effects");
            } else {
                String effectKey = configuration.getString("warp_effects");
                if (effectKey != null) {
                    warpEffects = controller.getEffects(effectKey);
                    if (warpEffects.isEmpty()) {
                        warpEffects = null;
                    }
                }
            }
            portalWarp.setPermission(configuration.getString("permission"));
            portalWarp.setEffects(warpEffects);
        }
        if (configuration.isList("effects")) {
            effects = controller.loadEffects(configuration, "effects");
        } else {
            String effectKey = configuration.getString("effects");
            if (effectKey != null) {
                effects = controller.getEffects(effectKey);
                if (effects.isEmpty()) {
                    effects = null;
                }
            }
        }

        if (configuration.contains("spawn")) {
            spawner = new Spawner(controller, this, configuration.getConfigurationSection("spawn"));
        }

        if (configuration.contains("cast")) {
            caster = new Caster(this, configuration.getConfigurationSection("cast"));
        }

        if (configuration.contains("interact")) {
            interactCaster = new InteractCaster(this, controller, configuration.getConfigurationSection("interact"));
        }

        // Common parameters
        playerRange = configuration.getInt("player_range", 64);
        minPlayers = configuration.getInt("min_players", 0);
        minTimeOfDay = parseTime(configuration, "min_time", controller.getLogger());
        maxTimeOfDay = parseTime(configuration, "max_time", controller.getLogger());
        if (configuration.contains("moon_phase")) {
            minPhaseOfMoon = maxPhaseOfMoon = parseMoonPhase(configuration, "moon_phase", controller.getLogger());
        } else {
            minPhaseOfMoon = parseMoonPhase(configuration, "min_moon_phase", controller.getLogger());
            maxPhaseOfMoon = parseMoonPhase(configuration, "max_moon_phase", controller.getLogger());
        }
    }

    @Nullable
    private Integer parseTime(ConfigurationSection configuration, String key, Logger log) {
        return ConfigurationUtils.parseTime(configuration.getString(key), log, "magic block");
    }

    @Nullable
    private Integer parseMoonPhase(ConfigurationSection configuration, String key, Logger log) {
        return ConfigurationUtils.parseMoonPhase(configuration.getString(key), log, "magic block");
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    @Nonnull
    public String getName() {
        String name = this.name;
        return name == null ? key : name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public int getInterval() {
        return interval;
    }

    public boolean isAlwaysActive() {
        return alwaysActive;
    }

    public boolean interact(MagicBlock instance, Player player) {
        if (interactCaster == null) {
            return false;
        }

        Mage mage = instance.getMage();
        return interactCaster.onInteract(mage, instance.getLocation(), player);
    }

    public void tick(MagicBlock instance) {
        boolean isActive = checkActive(instance.getLocation());
        boolean firstActivate = false;
        if (isActive) {
            if (!instance.isActive()) {
                firstActivate = true;
                instance.activate();
            }
        } else {
            if (instance.isActive()) {
                instance.deactivate();
            }
            return;
        }

        if (spawner != null) {
            if (instance.getTimeToNextSpawn() <= 0) {
                instance.spawn();
            }
            instance.checkEntities();
        }

        if (caster != null && (caster.isRecast() || firstActivate)) {
            Mage mage = instance.getMage();
            caster.cast(mage, instance.getLocation());
        }
    }

    public MagicBlockTemplate getVariant(ConfigurationSection parameters) {
        ConfigurationSection mergedConfiguration = ConfigurationUtils.cloneConfiguration(configuration);
        mergedConfiguration = ConfigurationUtils.addConfigurations(mergedConfiguration, parameters);
        return new MagicBlockTemplate(controller, key, mergedConfiguration);
    }

    @Nullable
    public Collection<EffectPlayer> getEffects() {
        return effects;
    }

    private boolean checkRange(Integer min, Integer max, int value) {
        if (min != null) {
            if (max != null && max < min) {
                if (value < min && value > max) {
                    return false;
                }
            } else {
                if (value < min) {
                    return false;
                }
            }
        }
        if (max != null && value > max && (min == null || min <= max)) {
            return false;
        }

        return true;
    }

    public boolean checkActive(Location location) {
        if (!checkRange(minTimeOfDay, maxTimeOfDay, (int)location.getWorld().getTime())) {
            return false;
        }
        if (!checkRange(minPhaseOfMoon, maxPhaseOfMoon, (int)((location.getWorld().getFullTime() / 24000) % 8))) {
            return false;
        }

        if (minPlayers >= 0 && playerRange > 0) {
            int playerCount = 0;
            int rangeSquared = playerRange * playerRange;
            List<Player> players = location.getWorld().getPlayers();
            for (Player player : players) {
                if (player.getLocation().distanceSquared(location) <= rangeSquared) {
                    playerCount++;
                }
            }

            if (playerCount < minPlayers) {
                return false;
            }
        }

        return true;
    }

    public boolean isUndoAll() {
        return caster != null && caster.isUndoAll();
    }

    @Nullable
    public Spawner getSpawner() {
        return spawner;
    }

    public ConfigurationSection getConfiguration() {
        return configuration;
    }

    public boolean removeWhenBroken() {
        return removeWhenBroken;
    }

    public boolean shouldRemove() {
        return remove;
    }

    public String getDropWhenRemoved() {
        return dropWhenRemoved;
    }

    public MagicWarpDescription getPortalWarp() {
        return portalWarp;
    }

    public String getPortalSpell() {
        return portalSpell;
    }
}
