package org.lwjgl.util.glu;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** LWJGL's Project (GLU helpers); the browser runtime's GLU does the work. */
public class Project {
    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        GLU.gluPerspective(fovy, aspect, zNear, zFar);
    }

    public static void gluLookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz,
            float upx, float upy, float upz) {
        GLU.gluLookAt(eyex, eyey, eyez, centerx, centery, centerz, upx, upy, upz);
    }

    public static boolean gluUnProject(float winx, float winy, float winz, FloatBuffer modelMatrix,
            FloatBuffer projMatrix, IntBuffer viewport, FloatBuffer objPos) {
        return GLU.gluUnProject(winx, winy, winz, modelMatrix, projMatrix, viewport, objPos);
    }

    public static boolean gluProject(float objx, float objy, float objz, FloatBuffer modelMatrix,
            FloatBuffer projMatrix, IntBuffer viewport, FloatBuffer winPos) {
        return GLU.gluProject(objx, objy, objz, modelMatrix, projMatrix, viewport, winPos);
    }
}
