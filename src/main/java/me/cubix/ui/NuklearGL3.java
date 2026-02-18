package me.cubix.ui;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL33.*;

public final class NuklearGL3 {

    private final long window;

    private NkContext ctx;
    private NkBuffer cmds;
    private NkDrawNullTexture nullTex;
    private NkFontAtlas atlas;
    private int fontTex;

    // GL objects
    private int vao, vbo, ebo;
    private int prog;
    private int vertSh, fragSh;

    private int uniformTex;
    private int uniformProj;

    // buffers
    private ByteBuffer vertices;
    private ByteBuffer elements;

    // config
    private NkConvertConfig convertConfig;

    // input: char callback
    private GLFWCharCallback charCallback;
    private GLFWScrollCallback scrollCallback;

    // size
    private int fbWidth, fbHeight;

    public NuklearGL3(long window) {
        this.window = window;
    }

    public NkContext ctx() { return ctx; }

    //backSpace
    private boolean backDown = false;

    private boolean backFiredOnce = false;

    private double backPressedAt = 0.0;

    private double backLastRepeatAt = 0.0;

    //Left
    private boolean LeftDown = false;

    private boolean LeftFiredOnce = false;

    private double LeftPressedAt = 0.0;

    private double LeftLastRepeatAt = 0.0;

    //Right
    private boolean RightDown = false;

    private boolean RightFiredOnce = false;

    private double RightPressedAt = 0.0;

    private double RightLastRepeatAt = 0.0;

    public void init() {
        ctx = NkContext.create();
        cmds = NkBuffer.create();
        nullTex = NkDrawNullTexture.create();
        atlas = NkFontAtlas.create();

        nk_init(ctx, ALLOCATOR, null);
        nk_buffer_init(cmds, ALLOCATOR, 4 * 1024);

        // Clipboard (optional but nice)
        ctx.clip().copy((handle, text, len) -> {
            if (len == 0) return;
            ByteBuffer str = MemoryUtil.memByteBuffer(text, len);
            byte[] bytes = new byte[len];
            str.get(bytes);
            glfwSetClipboardString(window, new String(bytes));
        });
        ctx.clip().paste((handle, edit) -> {
            String text = glfwGetClipboardString(window);
            if (text != null) {
                nk_textedit_paste(NkTextEdit.create(edit), text);
            }
        });

        try {
            setupFont();
            setupGL();
            setupCallbacks();
            setupConvertConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ByteBuffer loadResource(String path) throws IOException {
        try (var in = NuklearGL3.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new FileNotFoundException("Resource not found: " + path);
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = org.lwjgl.system.MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            return buf;
        }
    }

    private void setupFont() throws IOException {
        nk_font_atlas_init(atlas, ALLOCATOR);
        nk_font_atlas_begin(atlas);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NkFontConfig cfg = NkFontConfig.malloc(stack);
            nk_font_config(18.0f, cfg);
            cfg.range(Objects.requireNonNull(nk_font_korean_glyph_ranges()));

            ByteBuffer ttf = loadResource("font/NotoSansKR-VariableFont_wght.ttf");
            NkFont font = nk_font_atlas_add_from_memory(atlas, ttf, 18.0f, cfg);
            if (font == null) throw new IllegalStateException("Font load failed: returned null");

            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            ByteBuffer img = nk_font_atlas_bake(atlas, w, h, NK_FONT_ATLAS_RGBA32);

            fontTex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, fontTex);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, img);

            NkHandle fontHandle = NkHandle.malloc(stack);
            nk_handle_id(fontTex, fontHandle);
            nk_font_atlas_end(atlas, fontHandle, nullTex);

            nk_style_set_font(ctx, font.handle());

            org.lwjgl.system.MemoryUtil.memFree(ttf);
        }
    }


    private void setupGL() {
        // shaders
        String vertex =
                "#version 330 core\n" +
                        "layout(location=0) in vec2 Position;\n" +
                        "layout(location=1) in vec2 TexCoord;\n" +
                        "layout(location=2) in vec4 Color;\n" +
                        "uniform mat4 ProjMtx;\n" +
                        "out vec2 Frag_UV;\n" +
                        "out vec4 Frag_Color;\n" +
                        "void main(){\n" +
                        "  Frag_UV = TexCoord;\n" +
                        "  Frag_Color = Color;\n" +
                        "  gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" +
                        "}\n";

        String fragment =
                "#version 330 core\n" +
                        "in vec2 Frag_UV;\n" +
                        "in vec4 Frag_Color;\n" +
                        "uniform sampler2D Texture;\n" +
                        "out vec4 Out_Color;\n" +
                        "void main(){\n" +
                        "  Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                        "}\n";

        vertSh = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertSh, vertex);
        glCompileShader(vertSh);
        if (glGetShaderi(vertSh, GL_COMPILE_STATUS) == GL_FALSE)
            throw new IllegalStateException("UI vertex shader:\n" + glGetShaderInfoLog(vertSh));

        fragSh = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragSh, fragment);
        glCompileShader(fragSh);
        if (glGetShaderi(fragSh, GL_COMPILE_STATUS) == GL_FALSE)
            throw new IllegalStateException("UI fragment shader:\n" + glGetShaderInfoLog(fragSh));

        prog = glCreateProgram();
        glAttachShader(prog, vertSh);
        glAttachShader(prog, fragSh);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE)
            throw new IllegalStateException("UI program:\n" + glGetProgramInfoLog(prog));

        uniformTex = glGetUniformLocation(prog, "Texture");
        uniformProj = glGetUniformLocation(prog, "ProjMtx");

        // buffers
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        int vsz = 4 * 1024 * 1024;    // 4MB vertex
        int esz = 2 * 1024 * 1024;    // 2MB element
        glBufferData(GL_ARRAY_BUFFER, vsz, GL_STREAM_DRAW);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, esz, GL_STREAM_DRAW);

        // Vertex layout = NkDrawVertex {float2 pos, float2 uv, ubyte4 col}
        int stride = 2 * 4 + 2 * 4 + 4; // 20 bytes
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 8);
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, stride, 16);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        vertices = MemoryUtil.memAlloc(vsz);
        elements = MemoryUtil.memAlloc(esz);
    }

    private void setupCallbacks() {
        charCallback = new GLFWCharCallback() {
            @Override public void invoke(long window, int codepoint) {
                nk_input_unicode(ctx, codepoint);
            }
        };
        glfwSetCharCallback(window, charCallback);

        scrollCallback = new GLFWScrollCallback() {
            @Override public void invoke(long window, double xoff, double yoff) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    NkVec2 v = NkVec2.malloc(stack);
                    nk_vec2((float)xoff, (float)yoff, v);
                    nk_input_scroll(ctx, v);
                }
            }
        };
    }

    private void setupConvertConfig() {
        convertConfig = NkConvertConfig.calloc();

        NkDrawVertexLayoutElement.Buffer vertexLayout = NkDrawVertexLayoutElement.calloc(4);
        vertexLayout.get(0).attribute(NK_VERTEX_POSITION).format(NK_FORMAT_FLOAT).offset(0);
        vertexLayout.get(1).attribute(NK_VERTEX_TEXCOORD).format(NK_FORMAT_FLOAT).offset(8);
        vertexLayout.get(2).attribute(NK_VERTEX_COLOR).format(NK_FORMAT_R8G8B8A8).offset(16);
        vertexLayout.get(3).attribute(NK_VERTEX_ATTRIBUTE_COUNT).format(NK_FORMAT_COUNT).offset(0);
        convertConfig.vertex_layout(vertexLayout);
        convertConfig.vertex_size(20);
        convertConfig.vertex_alignment(4);
        convertConfig.tex_null().set(nullTex);

        // AA off로 시작 (성능/선명함). 나중에 켜도 됨
        convertConfig.circle_segment_count(22);
        convertConfig.curve_segment_count(22);
        convertConfig.arc_segment_count(22);
        convertConfig.global_alpha(1.0f);
        convertConfig.shape_AA(NK_ANTI_ALIASING_OFF);
        convertConfig.line_AA(NK_ANTI_ALIASING_OFF);
    }

    public void beginInput() {
        nk_input_begin(ctx);
    }

    public void endInput() {
        // mouse + keyboard 상태를 여기서 ctx에 넣기
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer mx = stack.mallocDouble(1);
            DoubleBuffer my = stack.mallocDouble(1);
            glfwGetCursorPos(window, mx, my);

            int x = (int) mx.get(0);
            int y = (int) my.get(0);

            nk_input_motion(ctx, x, y);
            nk_input_button(ctx, NK_BUTTON_LEFT, x, y, glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS);
            nk_input_button(ctx, NK_BUTTON_RIGHT, x, y, glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS);
            nk_input_button(ctx, NK_BUTTON_MIDDLE, x, y, glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS);
        }

        nk_input_key(ctx, NK_KEY_ENTER, glfwGetKey(window, GLFW_KEY_ENTER) == GLFW_PRESS);
        nk_input_key(ctx, NK_KEY_TAB, glfwGetKey(window, GLFW_KEY_TAB) == GLFW_PRESS);
        handleBackspaceRepeat();
        nk_input_key(ctx, NK_KEY_DEL, glfwGetKey(window, GLFW_KEY_DELETE) == GLFW_PRESS);
        handleLeftRepeat();
        handleRightRepeat();
        nk_input_key(ctx, NK_KEY_UP, glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS);
        nk_input_key(ctx, NK_KEY_DOWN, glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS);

        boolean ctrl = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS ||
                glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS;

        nk_input_key(ctx, NK_KEY_COPY, ctrl && glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS);
        nk_input_key(ctx, NK_KEY_PASTE, ctrl && glfwGetKey(window, GLFW_KEY_V) == GLFW_PRESS);
        nk_input_key(ctx, NK_KEY_CUT, ctrl && glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS);

        nk_input_end(ctx);
    }

    public void beginDraw(int width, int height) {
        this.fbWidth = width;
        this.fbHeight = height;
    }

    public void endDraw() {
        render();
        nk_clear(ctx);
    }

    private void render() {
        // Convert Nuklear draw commands to buffers
        NkBuffer vbuf = NkBuffer.create();
        NkBuffer ebuf = NkBuffer.create();
        nk_buffer_init_fixed(vbuf, vertices);
        nk_buffer_init_fixed(ebuf, elements);

        nk_convert(ctx, cmds, vbuf, ebuf, convertConfig);

        // setup state
        int lastProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int lastTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int lastArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int lastElementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int lastVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int lastBlendSrc = glGetInteger(GL_BLEND_SRC);
        int lastBlendDst = glGetInteger(GL_BLEND_DST);
        int lastBlendEq = glGetInteger(GL_BLEND_EQUATION_RGB);
        boolean lastBlend = glIsEnabled(GL_BLEND);
        boolean lastCull = glIsEnabled(GL_CULL_FACE);
        boolean lastDepth = glIsEnabled(GL_DEPTH_TEST);
        boolean lastScissor = glIsEnabled(GL_SCISSOR_TEST);

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_SCISSOR_TEST);

        glUseProgram(prog);
        glUniform1i(uniformTex, 0);

        Matrix4f ortho = new Matrix4f().setOrtho2D(0, fbWidth, fbHeight, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            ortho.get(fb);
            glUniformMatrix4fv(uniformProj, false, fb);
        }

        glViewport(0, 0, fbWidth, fbHeight);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        // upload buffers
        int vsize = (int) nk_buffer_total(vbuf);
        int esize = (int) nk_buffer_total(ebuf);

        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices.position(0).limit(vsize));
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, elements.position(0).limit(esize));

        // Draw
        long offset = 0;

        for (NkDrawCommand cmd = nk__draw_begin(ctx, cmds);
             cmd != null;
             cmd = nk__draw_next(cmd, cmds, ctx)) {

            if (cmd.elem_count() == 0) continue;

            glBindTexture(GL_TEXTURE_2D, cmd.texture().id());

            NkRect clip = cmd.clip_rect();
            int scX = (int) clip.x();
            int scY = fbHeight - (int)(clip.y() + clip.h());
            int scW = (int) clip.w();
            int scH = (int) clip.h();
            if (scW < 0) scW = 0;
            if (scH < 0) scH = 0;

            glScissor(scX, scY, scW, scH);
            glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);

            offset += (long) cmd.elem_count() * 2L;
        }
        nk__draw_end(ctx, cmds);

        // restore state
        if (!lastBlend) glDisable(GL_BLEND);
        glBlendFunc(lastBlendSrc, lastBlendDst);
        glBlendEquation(lastBlendEq);
        if (lastCull) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
        if (lastDepth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (lastScissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);

        glUseProgram(lastProgram);
        glBindTexture(GL_TEXTURE_2D, lastTexture);
        glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, lastElementBuffer);
        glBindVertexArray(lastVertexArray);

        nk_buffer_clear(cmds);
        nk_buffer_free(vbuf);
        nk_buffer_free(ebuf);
    }

    public void cleanup() {
        if (charCallback != null) charCallback.free();
        if (scrollCallback != null) scrollCallback.free();

        nk_font_atlas_clear(atlas);
        nk_buffer_free(cmds);
        nk_free(ctx);

        if (fontTex != 0) glDeleteTextures(fontTex);

        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);

        glDeleteProgram(prog);
        glDeleteShader(vertSh);
        glDeleteShader(fragSh);

        MemoryUtil.memFree(vertices);
        MemoryUtil.memFree(elements);

        convertConfig.free();
        atlas.free();
        nullTex.free();
        cmds.free();
        ctx.free();
    }

    private static final NkAllocator ALLOCATOR = NkAllocator.create()
            .alloc((handle, old, size) -> MemoryUtil.nmemAllocChecked(size))
            .mfree((handle, ptr) -> MemoryUtil.nmemFree(ptr));

    private void handleBackspaceRepeat() {
        boolean down = glfwGetKey(window, GLFW_KEY_BACKSPACE) == GLFW_PRESS;
        double t = glfwGetTime();

        final double initialDelay = 0.35;   // 길게 누를 때 첫 반복까지 기다림(초)
        final double repeatInterval = 0.03; // 반복 간격(초) -> 0.03이면 초당 약 33자

        if (down) {
            if (!backDown) {
                // 딱 1번만 삭제
                backDown = true;
                backFiredOnce = true;
                backPressedAt = t;
                backLastRepeatAt = t;

                nk_input_key(ctx, NK_KEY_BACKSPACE, true);
                nk_input_key(ctx, NK_KEY_BACKSPACE, false);
                return;
            }

            // initialDelay 이후부터 repeatInterval마다 1번씩만 삭제
            if ((t - backPressedAt) >= initialDelay) {
                if ((t - backLastRepeatAt) >= repeatInterval) {
                    backLastRepeatAt = t;
                    nk_input_key(ctx, NK_KEY_BACKSPACE, true);
                    nk_input_key(ctx, NK_KEY_BACKSPACE, false);
                }
            }
        } else {
            backDown = false;
            backFiredOnce = false;
        }
    }

    private void handleLeftRepeat() {
        boolean down = glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS;
        double t = glfwGetTime();

        final double initialDelay = 0.35;   // 길게 누를 때 첫 반복까지 기다림(초)
        final double repeatInterval = 0.03; // 반복 간격(초) -> 0.03이면 초당 약 33자

        if (down) {
            if (!LeftDown) {
                // 딱 1번만 삭제
                LeftDown = true;
                LeftFiredOnce = true;
                LeftPressedAt = t;
                LeftLastRepeatAt = t;

                nk_input_key(ctx, NK_KEY_LEFT, true);
                nk_input_key(ctx, NK_KEY_LEFT, false);
                return;
            }

            // initialDelay 이후부터 repeatInterval마다 1번씩만 삭제
            if ((t - LeftPressedAt) >= initialDelay) {
                if ((t - LeftLastRepeatAt) >= repeatInterval) {
                    LeftLastRepeatAt = t;
                    nk_input_key(ctx, NK_KEY_LEFT, true);
                    nk_input_key(ctx, NK_KEY_LEFT, false);
                }
            }
        } else {
            LeftDown = false;
            LeftFiredOnce = false;
        }
    }

    private void handleRightRepeat() {
        boolean down = glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS;
        double t = glfwGetTime();

        final double initialDelay = 0.35;
        final double repeatInterval = 0.03;

        if (down) {
            if (!RightDown) {
                RightDown = true;
                RightFiredOnce = true;
                RightPressedAt = t;
                RightLastRepeatAt = t;

                nk_input_key(ctx, NK_KEY_RIGHT, true);
                nk_input_key(ctx, NK_KEY_RIGHT, false);
                return;
            }

            if ((t - RightPressedAt) >= initialDelay) {
                if ((t - RightLastRepeatAt) >= repeatInterval) {
                    RightLastRepeatAt = t;
                    nk_input_key(ctx, NK_KEY_RIGHT, true);
                    nk_input_key(ctx, NK_KEY_RIGHT, false);
                }
            }
        } else {
            RightDown = false;
            RightFiredOnce = false;
        }
    }
}