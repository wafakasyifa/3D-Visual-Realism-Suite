package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL33.*;

public class Mesh {
    private final int flatVao, flatVbo, smoothVao, smoothVbo;
    private final int vertexCount;
    private final float groundOffset;

    public Mesh(MeshData data) {
        vertexCount = data.flat().length / 8;
        groundOffset = data.groundOffset();
        int[] f = upload(data.flat());
        int[] s = upload(data.smooth());
        flatVao = f[0];   flatVbo = f[1];
        smoothVao = s[0]; smoothVbo = s[1];
    }

    private static int[] upload(float[] data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);                       // aPos
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);         // aNormal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);         // aTexCoords
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? smoothVao : flatVao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public float getGroundOffset() { return groundOffset; }

    public void dispose() {
        glDeleteVertexArrays(flatVao);
        glDeleteVertexArrays(smoothVao);
        glDeleteBuffers(flatVbo);
        glDeleteBuffers(smoothVbo);
    }
}
