package de.bcycle.app;

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.oscim.backend.GLAdapter;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class BicycleGlContextsTest {
    private static class Context extends EGLContext { public GL getGL(){ return null; } }
    @Test public void mapsShareNamesAndSerializeFramesWithIndependentViewportAndCleanAttributes() throws Exception {
        List<EGLContext> shares=new ArrayList<>();List<Context> made=new ArrayList<>();
        EGL10 egl=(EGL10)Proxy.newProxyInstance(EGL10.class.getClassLoader(),new Class[]{EGL10.class},(p,m,a)->{
            if(m.getName().equals("eglCreateContext")){ shares.add((EGLContext)a[2]);java.lang.reflect.Field field=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");field.setAccessible(true);
                Context c=(Context)field.get(null).getClass().getMethod("allocateInstance",Class.class).invoke(field.get(null),Context.class);made.add(c);return c; }
            if(m.getReturnType()==boolean.class) return true;
            if(m.getReturnType()==int.class) return 0;
            return null;
        });
        EGLDisplay display=null;EGLConfig config=null;
        EGLContext dashboard=BicycleGlContexts.FACTORY.createContext(egl,display,config);
        EGLContext details=BicycleGlContexts.FACTORY.createContext(egl,display,config);
        assertSame(EGL10.EGL_NO_CONTEXT,shares.get(0));assertSame(dashboard,shares.get(1));
        org.oscim.backend.GL original=GLAdapter.gl;
        List<String> calls=Collections.synchronizedList(new ArrayList<>());
        GLAdapter.gl=(org.oscim.backend.GL)Proxy.newProxyInstance(org.oscim.backend.GL.class.getClassLoader(),new Class[]{org.oscim.backend.GL.class},(p,m,a)->{
            if(m.getName().equals("viewport")) calls.add("viewport:"+a[2]+"x"+a[3]);
            if(m.getName().equals("disableVertexAttribArray")) calls.add("attribute:"+a[0]);
            if(m.getName().equals("finish")) calls.add("finish");return null;
        });
        try {
            AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger(),initialized=new AtomicInteger();
            GLSurfaceView.Renderer delegate=new GLSurfaceView.Renderer(){
                public void onSurfaceCreated(GL10 gl,EGLConfig c){initialized.incrementAndGet();}
                public void onSurfaceChanged(GL10 gl,int w,int h){}
                public void onDrawFrame(GL10 gl){ int n=active.incrementAndGet();maximum.accumulateAndGet(n,Math::max);try{Thread.sleep(10);}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{active.decrementAndGet();} }
            };
            GLSurfaceView.Renderer a=BicycleGlContexts.wrap(delegate,new Object()),b=BicycleGlContexts.wrap(delegate,new Object());
            a.onSurfaceCreated(null,config);b.onSurfaceCreated(null,config);a.onSurfaceCreated(null,config);assertEquals(1,initialized.get());
            a.onSurfaceChanged(null,300,180);b.onSurfaceChanged(null,1080,2100);calls.clear();
            a.onDrawFrame(null);b.onDrawFrame(null);a.onDrawFrame(null);
            assertEquals(Arrays.asList("viewport:300x180","viewport:1080x2100","viewport:300x180"),calls.stream().filter(s->s.startsWith("viewport")).collect(java.util.stream.Collectors.toList()));
            assertEquals(3,calls.stream().filter(s->s.equals("attribute:7")).count());
            assertEquals(3,calls.stream().filter(s->s.equals("finish")).count());
            ExecutorService threads=Executors.newFixedThreadPool(2);
            try { Future<?> x=threads.submit(()->a.onDrawFrame(null)),y=threads.submit(()->b.onDrawFrame(null));x.get(2,TimeUnit.SECONDS);y.get(2,TimeUnit.SECONDS);assertEquals(1,maximum.get()); }finally{threads.shutdownNow();}
            BicycleGlContexts.FACTORY.destroyContext(egl,display,details);
            EGLContext fullscreen=BicycleGlContexts.FACTORY.createContext(egl,display,config);
            assertSame(dashboard,shares.get(2));BicycleGlContexts.FACTORY.destroyContext(egl,display,fullscreen);
        } finally { BicycleGlContexts.FACTORY.destroyContext(egl,display,dashboard);GLAdapter.gl=original; }
    }
}
