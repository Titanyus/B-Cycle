package de.bcycle.app;

import android.opengl.GLSurfaceView;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** UI tests have no Android EGL driver. Exercise the GL adapter separately with a recording GL. */
@Implements(GLSurfaceView.class)
public class ShadowHeadlessGlSurfaceView extends org.robolectric.shadows.ShadowGLSurfaceView {
    private int renderMode=GLSurfaceView.RENDERMODE_WHEN_DIRTY;
    @Implementation protected void setRenderer(GLSurfaceView.Renderer renderer) {}
    @Implementation protected void setRenderMode(int mode) { renderMode=mode; }
    @Implementation protected int getRenderMode() { return renderMode; }
    @Implementation protected void requestRender() {}
    @Implementation protected void queueEvent(Runnable action) {}
}
