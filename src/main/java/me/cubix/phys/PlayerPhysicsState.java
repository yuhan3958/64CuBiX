package me.cubix.phys;

import org.joml.Vector3f;

public final class PlayerPhysicsState {
    public final Vector3f pos = new Vector3f();
    public final Vector3f vel = new Vector3f();
    public boolean onGround = false;

    // 플레이어 히트박스(발바닥 기준): 폭 0.6, 키 1.8
    public float halfW = 0.3f;
    public float height = 1.8f;

    public static AABB playerAabb(PlayerPhysicsState p) {
        return new AABB(
                p.pos.x - p.halfW, p.pos.y,           p.pos.z - p.halfW,
                p.pos.x + p.halfW, p.pos.y + p.height, p.pos.z + p.halfW
        );
    }

    private static boolean isSolid(short id) {
        return id != me.cubix.world.block.BlockId.AIR
                && id != me.cubix.world.block.BlockId.WATER; // 물은 일단 비고체로
    }

    static int floorToBlock(float v) {
        return (int)Math.floor(v);
    }

    static int ceilToBlock(float v) {
        return (int)Math.ceil(v) - 1;
    }

}
