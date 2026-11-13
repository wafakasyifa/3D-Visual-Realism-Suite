package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ResourceUtil {
    private ResourceUtil() {}

    public static byte[] readBytes(String path) {
        try (InputStream in = ResourceUtil.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Resource tidak ditemukan: " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca resource: " + path, e);
        }
    }

    public static String readText(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    public static ByteBuffer readToDirectBuffer(String path) {
        byte[] bytes = readBytes(path);
        ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
        buf.put(bytes).flip();
        return buf;
    }
}
