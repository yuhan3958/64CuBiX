package me.cubix.phys;

public final class AABBUtil {
    public static AABB playerAabb(PlayerPhysicsState p) {
        return new AABB(
                p.pos.x - p.halfW, p.pos.y,            p.pos.z - p.halfW,
                p.pos.x + p.halfW, p.pos.y + p.height, p.pos.z + p.halfW
        );
    }
    private AABBUtil() {}
}