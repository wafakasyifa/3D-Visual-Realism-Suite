package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class OrbitCamera {
    private float yaw, pitch, distance;
    private final Vector3f target = new Vector3f(0f, 1.2f, 0f);

    public OrbitCamera() { reset(); }

    public void reset() { yaw = 35f; pitch = 28f; distance = 15f; }

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(3f, Math.min(89f, pitch + dPitch));
    }

    public void zoom(float delta) {
        distance = Math.max(4f, Math.min(40f, distance - delta));
    }

    public Vector3f getPosition() {
        float y = (float) Math.toRadians(yaw);
        float p = (float) Math.toRadians(pitch);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(p) * Math.sin(y)),
                target.y + distance * (float) Math.sin(p),
                target.z + distance * (float) (Math.cos(p) * Math.cos(y)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }
}
