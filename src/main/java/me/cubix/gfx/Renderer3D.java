package me.cubix.gfx;

import me.cubix.core.Window;

import static org.lwjgl.opengl.GL33.*;

public final class Renderer3D {
    private final Window window;
    private Camera camera;
    private Shader shader;
    private Texture atlas;
    private me.cubix.gfx.mesh.ChunkMesh testMesh;


    public Renderer3D(Window window) {
        this.window = window;
    }

    public void init() {
        glViewport(0, 0, window.width(), window.height());
        glEnable(GL_DEPTH_TEST);

        camera = new Camera();
        camera.setPerspective((float)Math.toRadians(70.0f),
                (float)window.width() / (float)window.height(),
                0.1f, 2000f);

        shader = new Shader("/shaders/block.vert", "/shaders/block.frag");


        try {
            atlas = new Texture("/textures/atlas.png", true);
        } catch (RuntimeException e) {
            atlas = new Texture("/textures/missing.png", true);
        }


        testMesh = new me.cubix.gfx.mesh.ChunkMesh();

        // 테스트 메쉬: 1x1x1 큐브(좌표 0..1), 타일 0번 사용
        var built = me.cubix.gfx.test.TestCube.build(0, 1024, 64);
        testMesh.upload(built.verts(), built.inds());
    }

    public void render(float dt, me.cubix.world.World world) {
        glClearColor(0.08f, 0.09f, 0.11f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();
        shader.setMat4("uProj", camera.proj());
        shader.setMat4("uView", camera.view());

        atlas.bind(0);
        shader.setInt("uAtlas", 0);

        // 큐브를 월드 원점 근처에 두기
        shader.setVec3("uChunkPos", new org.joml.Vector3f(0, 0, 0));
        testMesh.draw();

        shader.unbind();
    }

    public void cleanup() {
        if (testMesh != null) testMesh.cleanup();
        if (atlas != null) atlas.cleanup();
        if (shader != null) shader.cleanup();
    }

}