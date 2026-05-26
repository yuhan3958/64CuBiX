package me.cubix.gameplay;

import me.cubix.world.World;
import me.cubix.world.block.BlockId;
import org.joml.Vector3f;

public final class Player {
    private static final float EPS = 1e-5f;

    public final Vector3f vel = new Vector3f();
    public final Vector3f pos = new Vector3f();

    public float gravity = 100f;
    public float gravityGrowth = 80f;
    public float maxGravity = 300f;
    public float jumpSpeed = 15.45f;
    public float waterMoveMultiplier = 0.35f;
    public float waterRiseSpeed = 4.0f;
    public float waterGravityMultiplier = 0.35f;
    public float airborneTime = 0f;
    public float halfWidth = 0.30f;

    public boolean onGround = false;

    public void moveAndCollide(World world, Vector3f delta, float height) {
        if (world == null) {
            pos.add(delta);
            onGround = false;
            return;
        }

        onGround = false;

        if (delta.x != 0f) {
            pos.x = moveAxis(world, pos.x, pos.y, pos.z, delta.x, 0, height);
        }

        if (delta.z != 0f) {
            pos.z = moveAxis(world, pos.x, pos.y, pos.z, delta.z, 2, height);
        }

        float oldY = pos.y;
        if (delta.y != 0f) {
            pos.y = moveAxis(world, pos.x, pos.y, pos.z, delta.y, 1, height);
        }

        if (delta.y < 0f) {
            float attemptedY = oldY + delta.y;
            if (pos.y > attemptedY + EPS) {
                onGround = true;
            }
        }
    }

    public boolean isInWater(World world, float height) {
        if (world == null) return false;

        float minX = pos.x - halfWidth;
        float maxX = pos.x + halfWidth;
        float minY = pos.y;
        float maxY = pos.y + height;
        float minZ = pos.z - halfWidth;
        float maxZ = pos.z + halfWidth;

        int bx0 = fastFloor(minX);
        int bx1 = fastFloor(maxX - EPS);
        int by0 = fastFloor(minY);
        int by1 = fastFloor(maxY - EPS);
        int bz0 = fastFloor(minZ);
        int bz1 = fastFloor(maxZ - EPS);

        for (int bx = bx0; bx <= bx1; bx++) {
            for (int by = by0; by <= by1; by++) {
                for (int bz = bz0; bz <= bz1; bz++) {
                    if (world.getBlock(bx, by, bz) == BlockId.WATER) return true;
                }
            }
        }
        return false;
    }

    private float moveAxis(World world, float x, float y, float z, float d, int axis, float height) {
        if (d == 0f) return axis == 0 ? x : axis == 1 ? y : z;

        float nx = x, ny = y, nz = z;
        if (axis == 0) nx += d;
        if (axis == 1) ny += d;
        if (axis == 2) nz += d;

        float minX = nx - halfWidth;
        float maxX = nx + halfWidth;
        float minY = ny;
        float maxY = ny + height;
        float minZ = nz - halfWidth;
        float maxZ = nz + halfWidth;

        int bx0 = fastFloor(minX);
        int bx1 = fastFloor(maxX - EPS);
        int by0 = fastFloor(minY);
        int by1 = fastFloor(maxY - EPS);
        int bz0 = fastFloor(minZ);
        int bz1 = fastFloor(maxZ - EPS);

        if (axis == 0) {
            if (d > 0f) {
                float allowedX = nx;
                for (int bx = bx0; bx <= bx1; bx++) {
                    for (int by = by0; by <= by1; by++) {
                        for (int bz = bz0; bz <= bz1; bz++) {
                            if (!isSolid(world, bx, by, bz)) continue;
                            float stop = bx - halfWidth - EPS;
                            if (stop < allowedX) allowedX = stop;
                        }
                    }
                }
                return allowedX;
            }

            float allowedX = nx;
            for (int bx = bx0; bx <= bx1; bx++) {
                for (int by = by0; by <= by1; by++) {
                    for (int bz = bz0; bz <= bz1; bz++) {
                        if (!isSolid(world, bx, by, bz)) continue;
                        float stop = bx + 1 + halfWidth + EPS;
                        if (stop > allowedX) allowedX = stop;
                    }
                }
            }
            return allowedX;
        }

        if (axis == 2) {
            if (d > 0f) {
                float allowedZ = nz;
                for (int bx = bx0; bx <= bx1; bx++) {
                    for (int by = by0; by <= by1; by++) {
                        for (int bz = bz0; bz <= bz1; bz++) {
                            if (!isSolid(world, bx, by, bz)) continue;
                            float stop = bz - halfWidth - EPS;
                            if (stop < allowedZ) allowedZ = stop;
                        }
                    }
                }
                return allowedZ;
            }

            float allowedZ = nz;
            for (int bx = bx0; bx <= bx1; bx++) {
                for (int by = by0; by <= by1; by++) {
                    for (int bz = bz0; bz <= bz1; bz++) {
                        if (!isSolid(world, bx, by, bz)) continue;
                        float stop = bz + 1 + halfWidth + EPS;
                        if (stop > allowedZ) allowedZ = stop;
                    }
                }
            }
            return allowedZ;
        }

        if (d > 0f) {
            float allowedY = ny;
            for (int bx = bx0; bx <= bx1; bx++) {
                for (int by = by0; by <= by1; by++) {
                    for (int bz = bz0; bz <= bz1; bz++) {
                        if (!isSolid(world, bx, by, bz)) continue;
                        float stop = by - height - EPS;
                        if (stop < allowedY) allowedY = stop;
                    }
                }
            }
            return allowedY;
        }

        float allowedY = ny;
        for (int bx = bx0; bx <= bx1; bx++) {
            for (int by = by0; by <= by1; by++) {
                for (int bz = bz0; bz <= bz1; bz++) {
                    if (!isSolid(world, bx, by, bz)) continue;
                    float stop = by + 1 + EPS;
                    if (stop > allowedY) allowedY = stop;
                }
            }
        }
        return allowedY;
    }

    private boolean isSolid(World world, int x, int y, int z) {
        short id = world.getBlock(x, y, z);
        return id != BlockId.AIR && id != BlockId.WATER;
    }

    private static int fastFloor(float v) {
        int i = (int)v;
        return v < i ? i - 1 : i;
    }
}
