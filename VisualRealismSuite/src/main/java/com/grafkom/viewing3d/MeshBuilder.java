package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshBuilder {
    private record PosKey(int x, int y, int z) {
        static PosKey of(Vector3f p) {
            return new PosKey(Math.round(p.x * 10000f), Math.round(p.y * 10000f), Math.round(p.z * 10000f));
        }
    }

    private final List<Vector3f> pos = new ArrayList<>();
    private final List<Vector3f> hint = new ArrayList<>();   // normal analitik, hanya untuk menentukan arah luar
    private final List<Vector2f> uv = new ArrayList<>();
    private final List<int[]> tris = new ArrayList<>();

    public int addVertex(Vector3f p, Vector3f outwardHint, Vector2f t) {
        pos.add(p);
        hint.add(outwardHint);
        uv.add(t);
        return pos.size() - 1;
    }

    public void addTriangle(int a, int b, int c) { tris.add(new int[]{a, b, c}); }

    public void addQuad(int a, int b, int c, int d) {
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    private static Vector3f safeNormalize(Vector3f v, Vector3f fallback) {
        Vector3f r = new Vector3f(v);
        if (r.lengthSquared() < 1e-12f) r.set(fallback);
        if (r.lengthSquared() < 1e-12f) r.set(0f, 1f, 0f);
        return r.normalize();
    }

    public MeshData build(float groundOffset) {
        int triCount = tris.size();
        Vector3f[] faceN = new Vector3f[triCount];
        Map<PosKey, Vector3f> accum = new HashMap<>();

        for (int t = 0; t < triCount; t++) {
            int[] tri = tris.get(t);
            Vector3f a = pos.get(tri[0]), b = pos.get(tri[1]), c = pos.get(tri[2]);
            Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            Vector3f h = new Vector3f(hint.get(tri[0])).add(hint.get(tri[1])).add(hint.get(tri[2]));
            // Pastikan winding CCW menghadap ke luar (penting untuk Back-Face Culling)
            if (n.dot(h) < 0f) {
                int tmp = tri[1]; tri[1] = tri[2]; tri[2] = tmp;
                n.negate();
            }
            faceN[t] = n;   // tidak dinormalisasi -> otomatis berbobot luas (area-weighted)
            for (int k = 0; k < 3; k++) {
                accum.merge(PosKey.of(pos.get(tri[k])), new Vector3f(n), (x, y) -> x.add(y));
            }
        }

        float[] flat = new float[triCount * 3 * 8];
        float[] smooth = new float[triCount * 3 * 8];
        int o = 0;
        for (int t = 0; t < triCount; t++) {
            int[] tri = tris.get(t);
            Vector3f avgHint = new Vector3f(hint.get(tri[0])).add(hint.get(tri[1])).add(hint.get(tri[2]));
            Vector3f fn = safeNormalize(faceN[t], avgHint);
            for (int k = 0; k < 3; k++) {
                int idx = tri[k];
                Vector3f p = pos.get(idx);
                Vector3f sn = safeNormalize(accum.get(PosKey.of(p)), hint.get(idx));
                Vector2f tc = uv.get(idx);
                put(flat, o, p, fn, tc);
                put(smooth, o, p, sn, tc);
                o += 8;
            }
        }
        return new MeshData(flat, smooth, groundOffset);
    }

    private static void put(float[] arr, int o, Vector3f p, Vector3f n, Vector2f t) {
        arr[o] = p.x;     arr[o + 1] = p.y; arr[o + 2] = p.z;
        arr[o + 3] = n.x; arr[o + 4] = n.y; arr[o + 5] = n.z;
        arr[o + 6] = t.x; arr[o + 7] = t.y;
    }
}
