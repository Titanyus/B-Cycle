package org.oscim.renderer;

/** Public bridge to VTM's package-local state reset; contains no Android/EGL dependencies. */
public final class BCycleGlReset {
    private BCycleGlReset() {}
    public static void reset() {
        GLState.init();
        for(int i=0;i<8;i++) org.oscim.backend.GLAdapter.gl.disableVertexAttribArray(i);
    }
}
