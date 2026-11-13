package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class HSRManager {

    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);      // piksel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);       // buang polygon belakang
            glFrontFace(GL_CCW);       // counter-clockwise = tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}
