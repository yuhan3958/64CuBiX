package me.cubix.gfx;

import me.cubix.core.Window;
import me.cubix.gfx.mesh.ChunkMesh;
import me.cubix.gfx.model.BlockModels;
import me.cubix.gfx.model.ChunkBuilder;
import me.cubix.world.World;
import me.cubix.world.block.BlockId;
import me.cubix.world.chunk.Chunk;
import org.joml.Vector3f;

import java.util.HashMap;

import static org.lwjgl.opengl.GL33.*;

public final class Renderer3D {
    private final Window window;
    public Camera camera;
    private Shader shader;
    private TextureArray blockTextures;
    private BlockModels blockModels;
    private SelectionRenderer selectionRenderer;
    private final HashMap<Long, ChunkMesh> meshes = new HashMap<>();

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
        blockModels = BlockModels.load();
        blockTextures = new TextureArray(blockModels.texturePaths(), true);
        selectionRenderer = new SelectionRenderer();

        camera.position.set(0, 40, 0);
        camera.yaw = (float)Math.toRadians(45);
        camera.pitch = (float)Math.toRadians(-30);
    }

    public void render(float dt, World world) {
        render(dt, world, true, true);
    }

    public void render(float dt, World world, boolean updateCamera) {
        render(dt, world, updateCamera, updateCamera);
    }

    public void render(float dt, World world, boolean updateCamera, boolean showSelection) {
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_CULL_FACE);
        glClearColor(0.08f, 0.09f, 0.11f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (updateCamera) {
            camera.update(window, dt);
        }
        camera.rebuildMatrices(window);

        if (world == null) {
            return;
        }

        renderWorld(world);
        if (showSelection) {
            BlockHit hit = raycastBlock(world, 6.0f);
            if (hit != null) {
                selectionRenderer.renderBlockOutline(camera, hit.x, hit.y, hit.z);
            }
        }
    }

    public void cleanup() {
        for (ChunkMesh mesh : meshes.values()) mesh.cleanup();
        if (selectionRenderer != null) selectionRenderer.cleanup();
        if (blockTextures != null) blockTextures.cleanup();
        if (shader != null) shader.cleanup();
    }

    public void renderCrosshair(int width, int height) {
        if (selectionRenderer != null) selectionRenderer.renderCrosshair(width, height);
    }

    public static long key(int cx, int cy, int cz) {
        // Pack signed chunk coordinates into fixed-width fields for mesh cache keys.
        return (((long)cx) & 0x1FFFFFL) << 42
                | (((long)cy) & 0x1FFFFFL) << 21
                | (((long)cz) & 0x1FFFFFL);
    }

    private void renderWorld(World world) {
        shader.bind();
        shader.setMat4("uProj", camera.proj);
        shader.setMat4("uView", camera.view);

        blockTextures.bind(0);
        shader.setInt("uBlockTextures", 0);

        int s = Chunk.S;
        int r = 4;

        int camCx = (int)Math.floor(camera.position.x / s);
        int camCz = (int)Math.floor(camera.position.z / s);
        int camCy = (int)Math.floor(camera.position.y / s);

        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    int cx = camCx + dx;
                    int cy = camCy + dy;
                    int cz = camCz + dz;

                    Chunk chunk = world.getOrCreateChunk(cx, cy, cz);
                    if (chunk == null) continue;

                    ChunkMesh mesh = getOrBuildMesh(world, chunk, cx, cy, cz);

                    shader.setVec3("uChunkPos", new Vector3f(cx * s, cy * s, cz * s));
                    mesh.draw();
                }
            }
        }

        shader.unbind();
    }

    private ChunkMesh getOrBuildMesh(World world, Chunk chunk, int cx, int cy, int cz) {
        long k = key(cx, cy, cz);

        ChunkMesh mesh = meshes.get(k);
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
        int s = Chunk.S;
        // Worst case is every block face visible; hidden faces are skipped while building.
        int maxFaces = s * s * s * 6;
        ChunkBuilder builder = new ChunkBuilder(maxFaces);

        int baseX = cx * s;
        int baseY = cy * s;
        int baseZ = cz * s;

        for (int lz = 0; lz < s; lz++) {
            for (int ly = 0; ly < s; ly++) {
                for (int lx = 0; lx < s; lx++) {
                    int gx = baseX + lx;
                    int gy = baseY + ly;
                    int gz = baseZ + lz;

                    short id = world.getBlock(gx, gy, gz);
                    if (id == BlockId.AIR) continue;

                    BlockModels.BlockModel model = blockModels.modelFor(id);
                    if (model == null) continue;

                    addVisibleFaces(world, builder, model, gx, gy, gz, lx, ly, lz);
                }
            }
        }

        ChunkBuilder.Built built = builder.build();
        mesh.upload(built.verts(), built.inds());
    }

    private static void addVisibleFaces(
            World world,
            ChunkBuilder builder,
            BlockModels.BlockModel model,
            int gx, int gy, int gz,
            int lx, int ly, int lz
    ) {
        // Emit only faces adjacent to air, which keeps chunk meshes compact.
        if (world.getBlock(gx + 1, gy, gz) == BlockId.AIR) addFace(builder, model, ChunkBuilder.POS_X, lx, ly, lz);
        if (world.getBlock(gx - 1, gy, gz) == BlockId.AIR) addFace(builder, model, ChunkBuilder.NEG_X, lx, ly, lz);
        if (world.getBlock(gx, gy + 1, gz) == BlockId.AIR) addFace(builder, model, ChunkBuilder.POS_Y, lx, ly, lz);
        if (world.getBlock(gx, gy - 1, gz) == BlockId.AIR) addFace(builder, model, ChunkBuilder.NEG_Y, lx, ly, lz);
        if (world.getBlock(gx, gy, gz + 1) == BlockId.AIR) addFace(builder, model, ChunkBuilder.POS_Z, lx, ly, lz);
        if (world.getBlock(gx, gy, gz - 1) == BlockId.AIR) addFace(builder, model, ChunkBuilder.NEG_Z, lx, ly, lz);
    }

    private static void addFace(ChunkBuilder builder, BlockModels.BlockModel model, int face, int lx, int ly, int lz) {
        builder.addFace(model.layerForFace(face), face, lx, ly, lz);
    }

    private BlockHit raycastBlock(World world, float maxDistance) {
        Vector3f origin = camera.position;
        Vector3f direction = camera.front();
        float step = 0.05f;

        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;

        // Step through the view ray and report the first non-passable block.
        for (float d = 0f; d <= maxDistance; d += step) {
            int x = fastFloor(origin.x + direction.x * d);
            int y = fastFloor(origin.y + direction.y * d);
            int z = fastFloor(origin.z + direction.z * d);
            if (x == lastX && y == lastY && z == lastZ) continue;

            lastX = x;
            lastY = y;
            lastZ = z;

            short id = world.getBlock(x, y, z);
            if (id != BlockId.AIR && id != BlockId.WATER) {
                return new BlockHit(x, y, z);
            }
        }

        return null;
    }

    private static int fastFloor(float v) {
        int i = (int)v;
        return v < i ? i - 1 : i;
    }

    private record BlockHit(int x, int y, int z) {}
}
