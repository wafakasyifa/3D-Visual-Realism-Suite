package com.grafkom.viewing3d;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImageWrite;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11) - memperluas ThreeDViewing (Minggu 10).
 *
 * Kontrol:
 *   B = Phong <-> Blinn-Phong        F = Flat <-> Smooth shading
 *   Z = Z-Buffer on/off              C = Back-Face Culling on/off
 *   T = Tekstur on/off               N = Nearest <-> Bilinear+Mipmap
 *   S = Shadow Mapping on/off        [ / ] = bias bayangan /2 , x2
 *   1..4 = Sphere/Torus/Cube/House   W = Wireframe
 *   SPACE = putar objek              L = orbit cahaya
 *   Mouse drag / Panah = orbit kamera, Scroll = zoom, R = reset kamera
 *   P = screenshot (folder screenshots/)   H = bantuan   ESC = keluar
 */
public class VisualRealismApp {

    private long window;
    private int fbW = 1280, fbH = 800;

    private ShaderProgram sceneShader, depthShader;
    private final List<Mesh> models = new ArrayList<>();
    private final String[] modelNames = {"Sphere", "Torus", "Cube", "House"};
    private Mesh floor;
    private Texture2D modelTexture, floorTexture;
    private ShadowMapFBO shadowFbo;
    private final OrbitCamera camera = new OrbitCamera();

    // ---- state fitur
    private int modelIndex = 0;
    private boolean blinnPhong = true, smooth = true, depthTest = true, backFaceCull = true;
    private boolean useTexture = true, mipmapped = true, shadows = true, wireframe = false;
    private boolean spin = true, lightOrbit = false, screenshotRequested = false;
    private float shadowBias = 0.01f;
    private float spinAngle = 0f, lightAngle = 0.8f;

    // ---- mouse
    private boolean dragging = false;
    private double lastX, lastY;

    // ---- FPS
    private int frames = 0;
    private double fpsTimer = 0, fps = 0;

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    // ================================================================== INIT
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);   // wajib di macOS
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        window = glfwCreateWindow(1280, 800, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Gagal membuat window GLFW");

        glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            fbW = Math.max(1, width);
            fbH = Math.max(1, height);
        });
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) onKey(key);
        });
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = action == GLFW_PRESS;
                double[] x = new double[1], y = new double[1];
                glfwGetCursorPos(w, x, y);
                lastX = x[0];
                lastY = y[0];
            }
        });
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) camera.rotate((float) (-(x - lastX) * 0.4), (float) ((y - lastY) * 0.4));
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(window, (w, dx, dy) -> camera.zoom((float) dy));

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);   // vsync OFF agar FPS terukur untuk laporan
        glfwShowWindow(window);

        GL.createCapabilities();
        System.out.println("OpenGL : " + glGetString(GL_VERSION));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));

        int[] fbWidth = new int[1], fbHeight = new int[1];
        glfwGetFramebufferSize(window, fbWidth, fbHeight);
        fbW = Math.max(1, fbWidth[0]);
        fbH = Math.max(1, fbHeight[0]);

        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");

        models.add(new Mesh(MeshFactory.sphere(1.5f, 48, 64)));
        models.add(new Mesh(MeshFactory.torus(1.3f, 0.55f, 64, 32)));
        models.add(new Mesh(MeshFactory.cube(1.2f)));
        models.add(new Mesh(MeshFactory.house()));
        floor = new Mesh(MeshFactory.floor(10f, 8f));

        modelTexture = new Texture2D("/textures/brick.png");
        floorTexture = new Texture2D("/textures/checker.png");
        shadowFbo = new ShadowMapFBO();

        printHelp();
    }

    // ================================================================== INPUT
    private void onKey(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            case GLFW_KEY_B -> { blinnPhong = !blinnPhong; log("Shading model: " + (blinnPhong ? "Blinn-Phong" : "Phong")); }
            case GLFW_KEY_F -> { smooth = !smooth; log("Normal: " + (smooth ? "Smooth (per-vertex averaging)" : "Flat (per-face)")); }
            case GLFW_KEY_Z -> { depthTest = !depthTest; log("Z-Buffer Depth Test: " + onOff(depthTest)); }
            case GLFW_KEY_C -> { backFaceCull = !backFaceCull; log("Back-Face Culling: " + onOff(backFaceCull)); }
            case GLFW_KEY_T -> { useTexture = !useTexture; log("Tekstur: " + onOff(useTexture)); }
            case GLFW_KEY_N -> {
                mipmapped = !mipmapped;
                modelTexture.setMipmapped(mipmapped);
                floorTexture.setMipmapped(mipmapped);
                log("Filtering: " + (mipmapped ? "Bilinear + Mipmap" : "Nearest"));
            }
            case GLFW_KEY_S -> { shadows = !shadows; log("Shadow Mapping: " + onOff(shadows)); }
            case GLFW_KEY_LEFT_BRACKET -> { shadowBias = Math.max(1e-5f, shadowBias * 0.5f); log("Shadow bias = " + shadowBias); }
            case GLFW_KEY_RIGHT_BRACKET -> { shadowBias = Math.min(0.2f, shadowBias * 2f); log("Shadow bias = " + shadowBias); }
            case GLFW_KEY_W -> { wireframe = !wireframe; log("Wireframe: " + onOff(wireframe)); }
            case GLFW_KEY_SPACE -> spin = !spin;
            case GLFW_KEY_L -> lightOrbit = !lightOrbit;
            case GLFW_KEY_R -> camera.reset();
            case GLFW_KEY_P -> screenshotRequested = true;
            case GLFW_KEY_H -> printHelp();
            case GLFW_KEY_1 -> modelIndex = 0;
            case GLFW_KEY_2 -> modelIndex = 1;
            case GLFW_KEY_3 -> modelIndex = 2;
            case GLFW_KEY_4 -> modelIndex = 3;
            default -> { }
        }
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private static void log(String s) { System.out.println("[INFO] " + s); }

    private static void printHelp() {
        System.out.println("""
                ===== 3D Visual Realism Suite - Kontrol =====
                 B  : Phong <-> Blinn-Phong        F : Flat <-> Smooth
                 Z  : Z-Buffer on/off              C : Back-Face Culling on/off
                 T  : Tekstur on/off               N : Nearest <-> Bilinear+Mipmap
                 S  : Shadow Mapping on/off        [ ] : bias bayangan /2 , x2
                 1-4: Sphere/Torus/Cube/House      W : Wireframe
                 SPACE: putar objek   L: orbit cahaya   R: reset kamera
                 Mouse drag / Panah: orbit kamera   Scroll: zoom
                 P  : screenshot -> folder screenshots/     ESC: keluar
                =============================================""");
    }

    private void pollContinuousInput(float dt) {
        float speed = 70f * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) camera.rotate(-speed, 0);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.rotate(speed, 0);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) camera.rotate(0, speed);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) camera.rotate(0, -speed);
    }

    // ================================================================== LOOP
    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            glfwPollEvents();
            pollContinuousInput(dt);
            if (spin) spinAngle += 30f * dt;
            if (lightOrbit) lightAngle += 0.6f * dt;

            render();

            if (screenshotRequested) {
                saveScreenshot();
                screenshotRequested = false;
            }
            glfwSwapBuffers(window);
            updateFps(dt);
        }
    }

    private void updateFps(float dt) {
        frames++;
        fpsTimer += dt;
        if (fpsTimer >= 0.5) {
            fps = frames / fpsTimer;
            frames = 0;
            fpsTimer = 0;
            glfwSetWindowTitle(window, String.format(
                    "Visual Realism | FPS %.0f | %s | %s | %s | Z:%s Cull:%s | Tex:%s(%s) | Shadow:%s bias=%.5f",
                    fps, modelNames[modelIndex],
                    blinnPhong ? "Blinn-Phong" : "Phong",
                    smooth ? "Smooth" : "Flat",
                    onOff(depthTest), onOff(backFaceCull),
                    onOff(useTexture), mipmapped ? "Mipmap" : "Nearest",
                    onOff(shadows), shadowBias));
        }
    }

    // ================================================================== RENDER
    private void render() {
        Mesh model = models.get(modelIndex);
        Matrix4f modelMat = new Matrix4f()
                .translate(0f, model.getGroundOffset(), 0f)
                .rotateY((float) Math.toRadians(spinAngle));
        Matrix4f floorMat = new Matrix4f();

        // Cahaya: dianggap directional, bergerak melingkar bila 'L' aktif
        Vector3f lightPos = new Vector3f(8f * (float) Math.cos(lightAngle), 10f, 8f * (float) Math.sin(lightAngle));
        Matrix4f lightProj = new Matrix4f().ortho(-14f, 14f, -14f, 14f, 1f, 35f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ---------- PASS 1: Depth Pass (sudut pandang cahaya) ----------
        shadowFbo.bindForWriting();
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpace);
        depthShader.setMat4("model", floorMat);
        floor.draw(true);
        depthShader.setMat4("model", modelMat);
        model.draw(smooth);
        shadowFbo.unbind(fbW, fbH);

        // ---------- PASS 2: Lighting Pass (sudut pandang kamera) ----------
        glClearColor(0.16f, 0.18f, 0.22f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCull);
        glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);

        Matrix4f view = camera.getViewMatrix();
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0), fbW / (float) fbH, 0.1f, 100f);

        sceneShader.use();
        sceneShader.setMat4("view", view);
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", 1f, 1f, 1f);
        sceneShader.setBool("useTexture", useTexture);
        sceneShader.setBool("useBlinnPhong", blinnPhong);
        sceneShader.setBool("useShadow", shadows);
        sceneShader.setFloat("shadowBias", shadowBias);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureID());

        // Lantai dulu, lalu objek (urutan ini membuat efek Z-Buffer OFF terlihat jelas)
        floorTexture.bind(0);
        sceneShader.setMat4("model", floorMat);
        sceneShader.setMat3("normalMatrix", floorMat.normal(new Matrix3f()));
        sceneShader.setVec3("objectColor", 0.6f, 0.62f, 0.68f);
        floor.draw(true);

        modelTexture.bind(0);
        sceneShader.setMat4("model", modelMat);
        sceneShader.setMat3("normalMatrix", modelMat.normal(new Matrix3f()));
        sceneShader.setVec3("objectColor", 0.9f, 0.5f, 0.2f);
        model.draw(smooth);

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    // ================================================================== SCREENSHOT
    private void saveScreenshot() {
        try {
            Files.createDirectories(Path.of("screenshots"));
        } catch (IOException e) {
            System.err.println("Gagal membuat folder screenshots: " + e.getMessage());
            return;
        }
        ByteBuffer buf = BufferUtils.createByteBuffer(fbW * fbH * 3);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadBuffer(GL_BACK);
        glReadPixels(0, 0, fbW, fbH, GL_RGB, GL_UNSIGNED_BYTE, buf);
        STBImageWrite.stbi_flip_vertically_on_write(true);
        String name = String.format("screenshots/%s_%s_%s_Z%s_C%s_Tex%s_Sh%s_%d.png",
                modelNames[modelIndex],
                blinnPhong ? "Blinn" : "Phong",
                smooth ? "Smooth" : "Flat",
                depthTest ? "on" : "off", backFaceCull ? "on" : "off",
                useTexture ? (mipmapped ? "Mip" : "Nearest") : "off",
                shadows ? "on" : "off",
                System.currentTimeMillis() % 100000);
        boolean ok = STBImageWrite.stbi_write_png(name, fbW, fbH, 3, buf, fbW * 3);
        System.out.println(ok ? "[INFO] Screenshot disimpan: " + name : "[ERROR] Gagal menyimpan screenshot");
    }

    // ================================================================== CLEANUP
    private void cleanup() {
        // Resource GPU dibebaskan otomatis saat context dihancurkan; dispose eksplisit untuk kerapian.
        try {
            if (sceneShader != null) sceneShader.dispose();
            if (depthShader != null) depthShader.dispose();
            for (Mesh m : models) m.dispose();
            if (floor != null) floor.dispose();
            if (modelTexture != null) modelTexture.dispose();
            if (floorTexture != null) floorTexture.dispose();
            if (shadowFbo != null) shadowFbo.dispose();
        } catch (Exception ignored) { }
        if (window != NULL) glfwDestroyWindow(window);
        glfwTerminate();
    }
}
