package me.cubix.world.chunk;

public final class Chunk {

    private boolean dirty = true; // 생성 직후는 저장 대상
    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clearDirty() { dirty = false; }


    public static final int S = 16;
    private final short[] blocks = new short[S * S * S]; // y-major든 뭐든 일단 고정

    private static int idx(int x, int y, int z) {
        return (y * S + z) * S + x;
    }

    public short get(int x, int y, int z) {
        return blocks[idx(x,y,z)];
    }

    public void set(int x, int y, int z, short id) {
        blocks[idx(x,y,z)] = id;
    }

    public short[] raw() { return blocks; }
}

