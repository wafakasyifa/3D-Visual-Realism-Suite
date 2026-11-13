package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

public final class MeshFactory {
    private MeshFactory() {}

    public static MeshData sphere(float r, int stacks, int slices) {
        MeshBuilder mb = new MeshBuilder();
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * j / slices;
                float x = (float) (Math.sin(phi) * Math.cos(theta));
                float y = (float) Math.cos(phi);
                float z = (float) (Math.sin(phi) * Math.sin(theta));
                mb.addVertex(new Vector3f(x * r, y * r, z * r), new Vector3f(x, y, z),
                        new Vector2f((float) j / slices * 2f, 1f - (float) i / stacks));
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j;
                int b = a + slices + 1;
                mb.addQuad(a, b, b + 1, a + 1);
            }
        }
        return mb.build(r);
    }

    public static MeshData torus(float bigR, float smallR, int rings, int sides) {
        MeshBuilder mb = new MeshBuilder();
        for (int i = 0; i <= rings; i++) {
            double th = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double ph = 2.0 * Math.PI * j / sides;
                float cx = (float) (Math.cos(ph) * Math.cos(th));
                float cy = (float) Math.sin(ph);
                float cz = (float) (Math.cos(ph) * Math.sin(th));
                Vector3f p = new Vector3f(
                        (float) ((bigR + smallR * Math.cos(ph)) * Math.cos(th)),
                        (float) (smallR * Math.sin(ph)),
                        (float) ((bigR + smallR * Math.cos(ph)) * Math.sin(th)));
                mb.addVertex(p, new Vector3f(cx, cy, cz),
                        new Vector2f((float) i / rings * 4f, (float) j / sides));
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = i * (sides + 1) + j;
                int b = a + sides + 1;
                mb.addQuad(a, b, b + 1, a + 1);
            }
        }
        return mb.build(smallR);
    }

    public static MeshData cube(float halfSize) {
        MeshBuilder mb = new MeshBuilder();
        addBox(mb, new Vector3f(0, 0, 0), new Vector3f(halfSize, halfSize, halfSize));
        return mb.build(halfSize);
    }

    /** Rumah sederhana: badan (box) + atap prisma segitiga. Dasar di y = 0. */
    public static MeshData house() {
        MeshBuilder mb = new MeshBuilder();
        addBox(mb, new Vector3f(0f, 0.9f, 0f), new Vector3f(1.2f, 0.9f, 0.9f));

        float ex = 1.4f, ez = 1.0f, ey = 1.81f, ry = 2.8f;   // overhang atap
        Vector3f nL = new Vector3f(-1f, 1.4f, 0f).normalize();
        Vector3f nR = new Vector3f(1f, 1.4f, 0f).normalize();
        // lereng kiri
        int a = mb.addVertex(new Vector3f(-ex, ey, -ez), nL, new Vector2f(0, 0));
        int b = mb.addVertex(new Vector3f(-ex, ey, ez), nL, new Vector2f(2, 0));
        int c = mb.addVertex(new Vector3f(0, ry, ez), nL, new Vector2f(2, 1));
        int d = mb.addVertex(new Vector3f(0, ry, -ez), nL, new Vector2f(0, 1));
        mb.addQuad(a, b, c, d);
        // lereng kanan
        a = mb.addVertex(new Vector3f(ex, ey, ez), nR, new Vector2f(0, 0));
        b = mb.addVertex(new Vector3f(ex, ey, -ez), nR, new Vector2f(2, 0));
        c = mb.addVertex(new Vector3f(0, ry, -ez), nR, new Vector2f(2, 1));
        d = mb.addVertex(new Vector3f(0, ry, ez), nR, new Vector2f(0, 1));
        mb.addQuad(a, b, c, d);
        // gable depan (+z) dan belakang (-z)
        Vector3f nF = new Vector3f(0, 0, 1), nB = new Vector3f(0, 0, -1);
        a = mb.addVertex(new Vector3f(-ex, ey, ez), nF, new Vector2f(0, 0));
        b = mb.addVertex(new Vector3f(ex, ey, ez), nF, new Vector2f(1, 0));
        c = mb.addVertex(new Vector3f(0, ry, ez), nF, new Vector2f(0.5f, 1));
        mb.addTriangle(a, b, c);
        a = mb.addVertex(new Vector3f(-ex, ey, -ez), nB, new Vector2f(0, 0));
        b = mb.addVertex(new Vector3f(ex, ey, -ez), nB, new Vector2f(1, 0));
        c = mb.addVertex(new Vector3f(0, ry, -ez), nB, new Vector2f(0.5f, 1));
        mb.addTriangle(a, b, c);
        // alas atap (menghadap bawah)
        Vector3f nD = new Vector3f(0, -1, 0);
        a = mb.addVertex(new Vector3f(-ex, ey, -ez), nD, new Vector2f(0, 0));
        b = mb.addVertex(new Vector3f(ex, ey, -ez), nD, new Vector2f(1, 0));
        c = mb.addVertex(new Vector3f(ex, ey, ez), nD, new Vector2f(1, 1));
        d = mb.addVertex(new Vector3f(-ex, ey, ez), nD, new Vector2f(0, 1));
        mb.addQuad(a, b, c, d);
        return mb.build(0f);
    }

    /** Bidang lantai (ground plane) pada y = 0 dengan UV berulang. */
    public static MeshData floor(float half, float uvRepeat) {
        MeshBuilder mb = new MeshBuilder();
        Vector3f up = new Vector3f(0, 1, 0);
        int a = mb.addVertex(new Vector3f(-half, 0, -half), up, new Vector2f(0, 0));
        int b = mb.addVertex(new Vector3f(-half, 0, half), up, new Vector2f(0, uvRepeat));
        int c = mb.addVertex(new Vector3f(half, 0, half), up, new Vector2f(uvRepeat, uvRepeat));
        int d = mb.addVertex(new Vector3f(half, 0, -half), up, new Vector2f(uvRepeat, 0));
        mb.addQuad(a, b, c, d);
        return mb.build(0f);
    }

    // ---------------------------------------------------------------- helper
    private static final Vector3f[] AXES = {
            new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
            new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
            new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)
    };

    private static float ext(Vector3f axis, Vector3f half) {
        return Math.abs(axis.x) * half.x + Math.abs(axis.y) * half.y + Math.abs(axis.z) * half.z;
    }

    /** Box sumbu-sejajar: 6 face x 4 vertex (normal & UV per-face). */
    private static void addBox(MeshBuilder mb, Vector3f center, Vector3f half) {
        float[][] s = {{-1, -1, 0, 0}, {1, -1, 1, 0}, {1, 1, 1, 1}, {-1, 1, 0, 1}};
        for (Vector3f n : AXES) {
            Vector3f t = Math.abs(n.y) > 0.5f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            Vector3f b = new Vector3f(n).cross(t);
            float en = ext(n, half), et = ext(t, half), eb = ext(b, half);
            int[] idx = new int[4];
            for (int k = 0; k < 4; k++) {
                Vector3f p = new Vector3f(center)
                        .add(new Vector3f(n).mul(en))
                        .add(new Vector3f(t).mul(s[k][0] * et))
                        .add(new Vector3f(b).mul(s[k][1] * eb));
                idx[k] = mb.addVertex(p, new Vector3f(n), new Vector2f(s[k][2], s[k][3]));
            }
            mb.addQuad(idx[0], idx[1], idx[2], idx[3]);
        }
    }
}
