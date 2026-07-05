package me.cubix.phys;

public record AABB(float minX, float minY, float minZ,
                   float maxX, float maxY, float maxZ) {

    public AABB moved(float dx, float dy, float dz) {
        return new AABB(minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz);
    }
}