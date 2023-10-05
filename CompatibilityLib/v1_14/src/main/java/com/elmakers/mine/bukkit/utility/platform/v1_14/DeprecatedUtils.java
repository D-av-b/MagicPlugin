package com.elmakers.mine.bukkit.utility.platform.v1_14;

import org.bukkit.block.Skull;
import org.bukkit.map.MapView;

import com.elmakers.mine.bukkit.utility.platform.Platform;

public class DeprecatedUtils extends com.elmakers.mine.bukkit.utility.platform.legacy.DeprecatedUtils {

    public DeprecatedUtils(Platform platform) {
        super(platform);
    }

    @Override
    public void setSkullType(Skull skullBlock, short skullType) {
    }

    @Override
    public short getSkullType(Skull skullBlock) {
        return 0;
    }

    @Override
    public short getMapId(MapView mapView) {
        // MapView id is now an int- we probably should update our own code
        // and change this to an int
        return (short)mapView.getId();
    }
}
