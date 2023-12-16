package com.elmakers.mine.bukkit.action.builtin;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.effect.SoundEffect;

public class PlaySoundAction extends BaseSpellAction
{
    private SoundEffect sound;
    private int radius;
    private boolean countAsCast;

    @Override
    public SpellResult perform(CastContext context)
    {
        if (sound == null) {
            return SpellResult.FAIL;
        }
        sound.setRange(radius);
        if (radius == 0) {
            Entity entity = context.getTargetEntity();
            if (entity == null || !(entity instanceof Player)) {
                return SpellResult.NO_TARGET;
            }
            sound.play(context.getPlugin(), context.getController().getLogger(), entity);
            return countAsCast ? SpellResult.CAST : SpellResult.NO_ACTION;
        }
        Location location = context.getTargetLocation();
        if (location == null) {
            location = context.getLocation();
        }
        if (location == null) {
            return SpellResult.NO_TARGET;
        }
        sound.play(context.getPlugin(), context.getLogger(), location, context.getObservers());
        return countAsCast ? SpellResult.CAST : SpellResult.NO_ACTION;
    }

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        sound = new SoundEffect(parameters.getString("sound"));
        sound.setPitch((float)parameters.getDouble("pitch", sound.getPitch()));
        sound.setPitch((float)parameters.getDouble("sound_pitch", sound.getPitch()));
        sound.setVolume((float)parameters.getDouble("volume", sound.getVolume()));
        sound.setVolume((float)parameters.getDouble("sound_volume", sound.getVolume()));
        radius = parameters.getInt("radius", 32);
        countAsCast = parameters.getBoolean("effects_count_as_cast", false);
        countAsCast = parameters.getBoolean("sounds_count_as_cast", countAsCast);
    }
}
