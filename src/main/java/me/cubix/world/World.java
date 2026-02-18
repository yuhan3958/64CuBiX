package me.cubix.world;

import me.cubix.world.chunk.Chunk;
import me.cubix.world.chunk.ChunkPos;
import me.cubix.world.save.WorldStorage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class World {
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final WorldGen gen;
    private final WorldInfo info;

    public World(long seed, WorldInfo info) {
        this.gen = new WorldGen(seed);
        this.info = info;
    }

    public short getBlock(int x, int y, int z) {
        Chunk c = getOrCreateChunk(floorDiv(x, Chunk.S), floorDiv(y, Chunk.S), floorDiv(z, Chunk.S));
        int lx = floorMod(x, Chunk.S);
        int ly = floorMod(y, Chunk.S);
        int lz = floorMod(z, Chunk.S);
        return c.get(lx, ly, lz);
    }

    public void setBlock(int x, int y, int z, short id) {
        Chunk c = getOrCreateChunk(floorDiv(x, Chunk.S), floorDiv(y, Chunk.S), floorDiv(z, Chunk.S));
        c.set(floorMod(x, Chunk.S), floorMod(y, Chunk.S), floorMod(z, Chunk.S), id);
        c.markDirty();
    }

    public Chunk getOrCreateChunk(int cx, int cy, int cz) {
        ChunkPos p = new ChunkPos(cx, cy, cz);
        Chunk c = chunks.get(p);
        if (c != null) return c;

        try {
            Chunk loaded = WorldStorage.loadChunkIfExists(info, cx, cy, cz);
            if (loaded != null) {
                chunks.put(p, loaded);
                return loaded;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        c = new Chunk();
        gen.generateChunk(cx, cy, cz, c);
        chunks.put(p, c);
        return c;
    }


    private static int floorDiv(int a, int b) { return Math.floorDiv(a, b); }
    private static int floorMod(int a, int b) { return Math.floorMod(a, b); }

    public WorldInfo info() { return info; }

    public Map<ChunkPos, Chunk> chunksView() { return java.util.Collections.unmodifiableMap(chunks); }

    public int chunkSize() {
        return Chunk.S;
    }
}

