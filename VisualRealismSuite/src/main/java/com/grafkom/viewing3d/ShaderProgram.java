package com.grafkom.viewing3d;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, ResourceUtil.readText(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, ResourceUtil.readText(fragmentPath), fragmentPath);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Link shader gagal: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String source, String name) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Compile shader gagal (" + name + "):\n" + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void use() { glUseProgram(programId); }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int v)         { glUniform1i(loc(name), v); }
    public void setBool(String name, boolean v)    { glUniform1i(loc(name), v ? 1 : 0); }
    public void setFloat(String name, float v)     { glUniform1f(loc(name), v); }
    public void setVec3(String name, Vector3f v)   { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }
    public void setMat4(String name, Matrix4f m)   { glUniformMatrix4fv(loc(name), false, m.get(new float[16])); }
    public void setMat3(String name, Matrix3f m)   { glUniformMatrix3fv(loc(name), false, m.get(new float[9])); }

    public void dispose() { glDeleteProgram(programId); }
}
