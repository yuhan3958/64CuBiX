package me.cubix.phys;

import me.cubix.world.World;

public final class CollisionResolver {

    public static void stepPlayer(World world, PlayerPhysicsState p, float dt) {
        p.vel.y -= 30.0f * dt;

        float dx = p.vel.x * dt;
        float dy = p.vel.y * dt;
        float dz = p.vel.z * dt;

        // Resolve each axis separately to preserve wall sliding.
        p.onGround = false;
        if (dx != 0) dx = collideAxis(world, p, dx, 0, 0);
        p.pos.x += dx;
        if (dy != 0) dy = collideAxis(world, p, 0, dy, 0);
        p.pos.y += dy;
        if (dz != 0) dz = collideAxis(world, p, 0, 0, dz);
        p.pos.z += dz;
    }

    private static float collideAxis(World world, PlayerPhysicsState p, float dx, float dy, float dz) {
        AABB box = AABBUtil.playerAabb(p);
        AABB moved = box.moved(dx, dy, dz);

        int minX = (int)Math.floor(moved.minX());
        int maxX = (int)Math.ceil(moved.maxX()) - 1;
        int minY = (int)Math.floor(moved.minY());
        int maxY = (int)Math.ceil(moved.maxY()) - 1;
        int minZ = (int)Math.floor(moved.minZ());
        int maxZ = (int)Math.ceil(moved.maxZ()) - 1;

        float allowedDx = dx, allowedDy = dy, allowedDz = dz;

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    short id = world.getBlock(x, y, z);
                    if (!isSolid(id)) continue;

                    if (intersects(moved, x, y, z)) {
                        if (dx != 0) {
                            if (dx > 0) allowedDx = Math.min(allowedDx, x - box.maxX());
                            else        allowedDx = Math.max(allowedDx, (x + 1) - box.minX());
                            moved = box.moved(allowedDx, 0, 0);
                            p.vel.x = 0;
                        } else if (dy != 0) {
                            if (dy > 0) allowedDy = Math.min(allowedDy, y - box.maxY());
                            else {
                                allowedDy = Math.max(allowedDy, (y + 1) - box.minY());
                                p.onGround = true;
                            }
                            moved = box.moved(0, allowedDy, 0);
                            p.vel.y = 0;
                        } else if (dz != 0) {
                            if (dz > 0) allowedDz = Math.min(allowedDz, z - box.maxZ());
                            else        allowedDz = Math.max(allowedDz, (z + 1) - box.minZ());
                            moved = box.moved(0, 0, allowedDz);
                            p.vel.z = 0;
                        }
                    }
                }
            }
        }

        if (dx != 0) return allowedDx;
        if (dy != 0) return allowedDy;
        return allowedDz;
    }

    private static boolean intersects(AABB a, int bx, int by, int bz) {
        float bMinX = bx, bMaxX = bx + 1;
        float bMinY = by, bMaxY = by + 1;
        float bMinZ = bz, bMaxZ = bz + 1;

        return a.maxX() > bMinX && a.minX() < bMaxX
                && a.maxY() > bMinY && a.minY() < bMaxY
                && a.maxZ() > bMinZ && a.minZ() < bMaxZ;
    }

    private static boolean isSolid(short id) {
        return id != me.cubix.world.block.BlockId.AIR
                && id != me.cubix.world.block.BlockId.WATER;
    }

    private CollisionResolver() {}
}
