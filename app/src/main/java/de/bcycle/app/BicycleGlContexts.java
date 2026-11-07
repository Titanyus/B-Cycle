package de.bcycle.app;

import org.oscim.renderer.GLState;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.BCycleGlReset;

import android.opengl.GLSurfaceView;
import org.oscim.backend.GLAdapter;
import org.oscim.map.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL10;

/** VTM 0.25 keeps buffer pools, quad IDs, shaders and GL-state caches process-wide.
 * Every Android map must therefore use one EGL share group and serialize GPU access.
 * No map surface is detached/reparented by this adapter. */
public final class BicycleGlContexts {
    private BicycleGlContexts() {}
    private static final Object DRAW = new Object();
    private static final Object EGL = new Object();
    private static final List<EGLContext> contexts = new ArrayList<>();
    private static final WeakHashMap<Map,Integer> backgrounds = new WeakHashMap<>();
    private static Object lastOwner;
    private static boolean initialized;
    public static final GLSurfaceView.EGLContextFactory FACTORY = new GLSurfaceView.EGLContextFactory() {
        @Override public EGLContext createContext(EGL10 egl,EGLDisplay display,EGLConfig config) {
            synchronized(DRAW) { synchronized(EGL) {
                EGLContext share=contexts.isEmpty() ? EGL10.EGL_NO_CONTEXT : contexts.get(contexts.size()-1);
                EGLContext next=egl.eglCreateContext(display,config,share,new int[]{0x3098,2,EGL10.EGL_NONE});
                if(next==null || next==EGL10.EGL_NO_CONTEXT) throw new IllegalStateException("Cannot create shared VTM EGL context: "+egl.eglGetError());
                contexts.add(next);return next;
            } }
        }
        @Override public void destroyContext(EGL10 egl,EGLDisplay display,EGLContext context) {
            synchronized(DRAW) { synchronized(EGL) {
                contexts.remove(context);
                if(!egl.eglDestroyContext(display,context)) throw new IllegalStateException("Cannot destroy VTM EGL context: "+egl.eglGetError());
                lastOwner=null;
                if(contexts.isEmpty()) initialized=false;
            } }
        }
    };
    public static void background(Map map,int color) { synchronized(DRAW) { synchronized(backgrounds) { backgrounds.put(map,color); } MapRenderer.setBackgroundColor(color); } }
    private static void state(Object owner) {
        if(lastOwner==owner) return;
        // GLES2 guarantees at least eight attributes. Native VTM caches only its first two;
        // our label-fade attribute must not leak from another map either.
        BCycleGlReset.reset();
        lastOwner=owner;
    }
    private static void complete() {
        boolean shared;
        synchronized(EGL) { shared=contexts.size()>1; }
        // A CPU lock alone does not order shared texture/buffer access on separate GPU queues.
        if(shared) GLAdapter.gl.finish();
    }
    public static GLSurfaceView.Renderer wrap(final GLSurfaceView.Renderer renderer,final Object owner) {
        return new GLSurfaceView.Renderer() {
            private int width,height;
            @Override public void onSurfaceCreated(GL10 gl,EGLConfig config) {
                synchronized(DRAW) {
                    // These VTM initializers create global shader programs and quad buffers.
                    // Shared objects survive another surface being attached; initialize once per group.
                    if(!initialized) { renderer.onSurfaceCreated(gl,config);initialized=true; }
                    lastOwner=null;complete();
                }
            }
            @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
                synchronized(DRAW) { state(owner);width=w;height=h;renderer.onSurfaceChanged(gl,w,h);lastOwner=null;complete(); }
            }
            @Override public void onDrawFrame(GL10 gl) {
                synchronized(DRAW) {
                    state(owner);GLState.viewport(width,height);
                    Integer color;
                    synchronized(backgrounds) { color=backgrounds.get(owner); }
                    MapRenderer.setBackgroundColor(color==null ? 0xffcccccc : color);
                    try { renderer.onDrawFrame(gl); } finally { complete(); }
                }
            }
        };
    }
    public static Runnable task(final Object owner,final Runnable runnable) {
        return () -> { synchronized(DRAW) {
            EGL10 egl=(EGL10)EGLContext.getEGL();
            boolean current=egl.eglGetCurrentContext()!=EGL10.EGL_NO_CONTEXT && GLAdapter.gl!=null;
            if(current) state(owner);
            try { runnable.run(); } finally { if(current) complete(); }
        } };
    }
}
