package me.cubix.gfx;

import me.cubix.core.Window;
import me.cubix.gfx.mesh.ChunkMesh;
import me.cubix.world.World;
import me.cubix.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public final class Renderer3D {
    private final Window window;
    public Camera camera;
    private Shader shader;
    private Texture atlas;
    private me.cubix.gfx.mesh.ChunkMesh testMesh;
    private final java.util.HashMap<Long, me.cubix.gfx.mesh.ChunkMesh> chunkMeshes = new java.util.HashMap<>();



    public Renderer3D(Window window) {
        this.window = window;
    }

    public void init() {
        glViewport(0, 0, window.width(), window.height());
        glEnable(GL_DEPTH_TEST);

        camera = new Camera();
        camera.setPerspective((float)Math.toRadians(70.0f),
                (float)window.width() / (float)window.height(),
                0.1f);

        shader = new Shader("/shaders/block.vert", "/shaders/block.frag");

        camera.position.set(0, 40, 0);
        camera.yaw = (float)Math.toRadians(45);
        camera.pitch = (float)Math.toRadians(-30);


        try {
            atlas = new Texture("/textures/atlas.png", true);
        } catch (RuntimeException e) {
            atlas = new Texture("/textures/missing.png", true);
        }


        ;
    }

    public void render(float dt, me.cubix.world.World world) {
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_CULL_FACE); // 일단 꺼서 와인딩 문제 배제
        glClearColor(0.08f, 0.09f, 0.11f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        camera.update(window, dt);
        camera.rebuildMatrices(window);

        shader.bind();
        shader.setMat4("uProj", camera.proj);
        shader.setMat4("uView", camera.view);

        atlas.bind(0);
        shader.setInt("uAtlas", 0);

        if (world == null) {
            renderBackground(dt); // 선택사항 (아래 함수)
            return;
        } else {
            renderWorld(dt, world);
        }

        shader.unbind();
    }

    private void renderBackground(float dt) {
        shader.unbind();
    }

    public void cleanup() {
        if (testMesh != null) testMesh.cleanup();
        if (atlas != null) atlas.cleanup();
        if (shader != null) shader.cleanup();
    }

    public final java.util.HashMap<Long, me.cubix.gfx.mesh.ChunkMesh> meshes = new java.util.HashMap<>();

    public static long key(int cx, int cy, int cz) {
        // 간단 패킹(범위 작다고 가정). 음수 가능하면 bias 주거나 21비트씩 쓰는 방식으로 바꾸면 됨
        return (((long)cx) & 0x1FFFFFL) << 42
                | (((long)cy) & 0x1FFFFFL) << 21
                | (((long)cz) & 0x1FFFFFL);
    }

    private final Map<Long, ChunkMesh> meshMap = new HashMap<>();
    private static final int RENDER_DISTANCE = 8; // 청크 반경(8이면 17x17)

    private void renderWorld(float dt, me.cubix.world.World world) {
        shader.bind();
        shader.setMat4("uProj", camera.proj);
        shader.setMat4("uView", camera.view);

        atlas.bind(0);
        shader.setInt("uAtlas", 0);

        int S = me.cubix.world.chunk.Chunk.S;
        int r = 4;

        int camCx = (int)Math.floor(camera.position.x / S);
        int camCz = (int)Math.floor(camera.position.z / S);
        int camCy = (int)Math.floor(camera.position.y / S); // 일단 0층만

        System.out.println("world render tick");
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    int cx = camCx + dx;
                    int cy = camCy + dy;
                    int cz = camCz + dz;

                    var chunk = world.getOrCreateChunk(cx, cy, cz);
                    if (chunk == null) continue;

                    var mesh = getOrBuildMesh(world, chunk, cx, cy, cz);

                    shader.setVec3("uChunkPos", new org.joml.Vector3f(cx * S, cy * S, cz * S));
                    mesh.draw();
                }
            }
        }

        shader.unbind();
    }


    private ChunkMesh getOrBuildMesh(World world, Chunk chunk, int cx, int cy, int cz) {
        long k = key(cx, cy, cz);

        var mesh = meshes.get(k);
        if (mesh == null) {
            mesh = new ChunkMesh();
            meshes.put(k, mesh);
            rebuildChunkMesh(world, mesh, cx, cy, cz);
            chunk.clearDirty();
            return mesh;
        }

        if (chunk.isDirty()) {
            rebuildChunkMesh(world, mesh, cx, cy, cz);
            chunk.clearDirty();
        }
        return mesh;
    }


    private void rebuildChunkMesh(World world, ChunkMesh mesh, int cx, int cy, int cz) {

        int S = me.cubix.world.chunk.Chunk.S;
        int atlasSize = 1024;
        int tileSize  = 64;

        // 최악의 경우 모든 블록이 노출: S*S*S*6 faces
        // 근데 너무 크게 잡으면 메모리 큼 → 일단 “겉면 예상치”로 대충
        int maxFaces = S * S * 6 * 2; // 임시(충분히 크게)
        var b = new me.cubix.gfx.model.ChunkBuilder(atlasSize, tileSize, maxFaces);

        int baseX = cx * S;
        int baseY = cy * S;
        int baseZ = cz * S;

        for (int lz = 0; lz < S; lz++)
            for (int ly = 0; ly < S; ly++)
                for (int lx = 0; lx < S; lx++) {
                    int gx = baseX + lx;
                    int gy = baseY + ly;
                    int gz = baseZ + lz;

                    short id = world.getBlock(gx, gy, gz);
                    if (id == me.cubix.world.block.BlockId.AIR) continue;

                    int tile = me.cubix.gfx.model.BlockTiles.tileFor(id);

                    // 이웃이 AIR면 그 면은 보인다
                    if (world.getBlock(gx+1, gy, gz) == me.cubix.world.block.BlockId.AIR) b.addFace(tile, me.cubix.gfx.model.ChunkBuilder.POS_X, lx, ly, lz);
                    if (world.getBlock(gx-1, gy, gz) == me.cubix.world.block.BlockId.AIR) b.addFace(tile, me.cubix.gfx.model.ChunkBuilder.NEG_X, lx, ly, lz);
                    if (world.getBlock(gx, gy+1, gz) == me.cubix.world.block.BlockId.AIR) b.addFace(tile, me.cubix.gfx.model.ChunkBuilder.POS_Y, lx, ly, lz);
                    if (world.getBlock(gx, gy-1, gz) == me.cubix.world.block.BlockId.AIR) b.addFace(tile, me.cubix.gfx.model.ChunkBuilder.NEG_Y, lx, ly, lz);
                    if (world.getBlock(gx, gy, gz+1) == me.cubix.world.block.BlockId.AIR) b.addFace(tile, me.cubix.gfx.model.ChunkBuilder.POS_Z, lx, ly, lz);
                    if (world.getBlock(gx, gy, gz-1) == me.cubix.world.block.BlockId.AIR) b.addFace(tile, me.cubix.gfx.model.ChunkBuilder.NEG_Z, lx, ly, lz);
                }

        var built = b.build();
        mesh.upload(built.verts(), built.inds());
    }
}