package com.elmakers.mine.bukkit.api.effect;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.elmakers.mine.bukkit.api.magic.MageController;

public interface EffectContext {
    @Nonnull
    String getName();
    @Nullable
    Entity getEntity();
    @Nullable
    LivingEntity getLivingEntity();
    @Nullable
    Location getEyeLocation();
    @Nullable
    Location getLocation();
    void setLocation(Location location);
    @Nullable
    Entity getTargetEntity();
    @Nullable
    Location getTargetLocation();
    @Nonnull
    MageController getController();
    @Nullable
    Location getCastLocation();
    @Nullable
    Location getWandLocation();
    void clearSourceLocation();

    void cancelEffects();
    void trackEffects(EffectPlayer player);
    Collection<EffectPlay> getCurrentEffects();

    Color getEffectColor();
    String getEffectParticle();
}
