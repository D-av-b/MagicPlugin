package com.elmakers.mine.bukkit.utility.platform.modern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;

import com.elmakers.mine.bukkit.utility.platform.Platform;

public class ModernDeprecatedUtils extends com.elmakers.mine.bukkit.utility.platform.v1_16.DeprecatedUtils {
    public ModernDeprecatedUtils(Platform platform) {
        super(platform);
    }

    @Override
    public void setTypeAndData(Block block, Material material, byte data, boolean applyPhysics) {
        block.setType(material, applyPhysics);
    }

    @Override
    public void setSkullType(Skull skullBlock, short skullType) {
    }

    @Override
    public short getSkullType(Skull skullBlock) {
        return 0;
    }
}
