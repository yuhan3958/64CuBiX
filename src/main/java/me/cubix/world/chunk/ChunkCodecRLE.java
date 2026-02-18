package me.cubix.world.chunk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChunkCodecRLE {
    private static final int MAGIC = 0x43554258; // "CUBX"
    private static final int VERSION = 1;

    public static void save(Path file, int cx, int cy, int cz, Chunk c) throws IOException {
        Files.createDirectories(file.getParent());
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(cx);
            out.writeInt(cy);
            out.writeInt(cz);

            // payload: (short id, short run)
            int total = Chunk.S * Chunk.S * Chunk.S;
            int i = 0;
            while (i < total) {
                short v = c.raw()[i];
                int run = 1;
                while (i + run < total && run < 0xFFFF && c.raw()[i + run] == v) run++;
                out.writeShort(v);
                out.writeShort(run);
                i += run;
            }
        }
    }

    public static Chunk load(Path file, int expectedCx, int expectedCy, int expectedCz) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC) throw new IOException("Bad chunk magic");
            int ver = in.readInt();
            if (ver != VERSION) throw new IOException("Unsupported chunk ver: " + ver);

            int cx = in.readInt(), cy = in.readInt(), cz = in.readInt();
            if (cx != expectedCx || cy != expectedCy || cz != expectedCz)
                throw new IOException("Chunk coord mismatch");

            Chunk c = new Chunk();
            int total = Chunk.S * Chunk.S * Chunk.S;
            int i = 0;
            short[] raw = c.raw();
            while (i < total) {
                short id = in.readShort();
                int run = in.readUnsignedShort();
                for (int k = 0; k < run && i < total; k++) raw[i++] = id;
            }
            c.clearDirty();
            return c;
        }
    }

    private ChunkCodecRLE() {}
}
