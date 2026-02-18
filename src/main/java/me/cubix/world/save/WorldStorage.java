package me.cubix.world.save;

import me.cubix.world.World;
import me.cubix.world.WorldInfo;
import me.cubix.world.chunk.Chunk;
import me.cubix.world.chunk.ChunkCodecRLE;
import me.cubix.world.chunk.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class WorldStorage {
    public static Path chunkDir(WorldInfo info) {
        return info.dir().resolve("chunks");
    }

    public static Path chunkFile(WorldInfo info, int cx, int cy, int cz) {
        return chunkDir(info).resolve("c." + cx + "." + cy + "." + cz + ".bin");
    }

    public static void saveDirtyChunks(World world) throws IOException {
        System.out.println("[SAVE] saveDirtyChunks start");
        WorldInfo info = world.info();
        int total = world.chunksView().size();
        int dirty = 0;
        for (var e : world.chunksView().entrySet()) if (e.getValue().isDirty()) dirty++;
        System.out.println("[SAVE] chunks total=" + total + ", dirty=" + dirty);
        for (Map.Entry<ChunkPos, Chunk> e : world.chunksView().entrySet()) {
            ChunkPos p = e.getKey();
            Chunk c = e.getValue();
            if (!c.isDirty()) continue;

            Path file = chunkFile(info, p.x(), p.y(), p.z());
            ChunkCodecRLE.save(file, p.x(), p.y(), p.z(), c);
            c.clearDirty();
        }
    }

    public static Chunk loadChunkIfExists(WorldInfo info, int cx, int cy, int cz) throws IOException {
        Path file = chunkFile(info, cx, cy, cz);
        if (!java.nio.file.Files.exists(file)) return null;
        return ChunkCodecRLE.load(file, cx, cy, cz);
    }

    private WorldStorage() {}
}