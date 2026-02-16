// me.cubix.gfx.model.BlockTiles.java
package me.cubix.gfx.model;

import me.cubix.world.block.BlockId;

public final class BlockTiles {
    public static int tileFor(short id) {
        return switch (id) {
            case BlockId.STONE -> 0;
            case BlockId.DIRT  -> 1;
            case BlockId.GRASS -> 2;
            case BlockId.WATER -> 3;
            default -> -1; // AIR 등
        };
    }
    private BlockTiles() {}
}
