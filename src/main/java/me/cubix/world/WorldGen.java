package me.cubix.world;

import me.cubix.world.block.BlockId;
import me.cubix.world.chunk.Chunk;

public final class WorldGen {
    private final long seed;

    public WorldGen(long seed) {
        this.seed = seed;
    }

    public void generateChunk(int cx, int cy, int cz, Chunk out) {
        int baseX = cx * Chunk.S;
        int baseY = cy * Chunk.S;
        int baseZ = cz * Chunk.S;

        for (int ly = 0; ly < Chunk.S; ly++) {
            int y = baseY + ly;
            for (int lz = 0; lz < Chunk.S; lz++) {
                int z = baseZ + lz;
                for (int lx = 0; lx < Chunk.S; lx++) {
                    int x = baseX + lx;

                    short id = sampleBlock(x, y, z);
                    out.set(lx, ly, lz, id);
                }
            }
        }
    }

    private short sampleBlock(int x, int y, int z) {
        // 지표 높이(2D FBM)
        double h = 24.0 + 18.0 * fbm2(x * 0.01, z * 0.01, 5);
        int height = (int)Math.floor(h);

        int sea = 22;

        if (y > height) {
            return (y <= sea) ? BlockId.WATER : BlockId.AIR;
        }

        // 지표 아래 층 구성
        if (y == height) return BlockId.GRASS;
        if (y >= height - 3) return BlockId.DIRT;
        return BlockId.STONE;
    }

    // -------- noise ----------
    private double fbm2(double x, double z, int oct) {
        double sum = 0, amp = 1, freq = 1;
        double norm = 0;
        for (int i = 0; i < oct; i++) {
            sum += amp * valueNoise2(x * freq, z * freq);
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum / norm; // 0..1 근처
    }

    // 0..1
    private double valueNoise2(double x, double z) {
        int x0 = (int)Math.floor(x);
        int z0 = (int)Math.floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = x - x0;
        double tz = z - z0;

        double v00 = hash01(x0, z0);
        double v10 = hash01(x1, z0);
        double v01 = hash01(x0, z1);
        double v11 = hash01(x1, z1);

        double a = lerp(v00, v10, smooth(tx));
        double b = lerp(v01, v11, smooth(tx));
        return lerp(a, b, smooth(tz));
    }

    private double hash01(int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        // 0..1
        return ((h >>> 11) & ((1L << 53) - 1)) / (double)(1L << 53);
    }

    private static double smooth(double t) {
        // smoothstep
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}

