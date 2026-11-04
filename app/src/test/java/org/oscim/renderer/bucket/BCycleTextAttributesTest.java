package org.oscim.renderer.bucket;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.oscim.backend.GL;
import org.oscim.backend.GLAdapter;
import org.oscim.renderer.GLState;
import static org.junit.Assert.*;

public class BCycleTextAttributesTest {
    @Test public void everyDriverAttributeOrderDrawsAndRestoresNativeArrayState() {
        GL original=GLAdapter.gl;
        Set<Integer> enabled=new HashSet<>();
        GLAdapter.gl=(GL)Proxy.newProxyInstance(GL.class.getClassLoader(),new Class[]{GL.class},(proxy,method,args)->{
            if(method.getName().equals("enableVertexAttribArray")) enabled.add((Integer)args[0]);
            if(method.getName().equals("disableVertexAttribArray")) enabled.remove((Integer)args[0]);
            return null;
        });
        try {
            for(int[] order:new int[][]{{1,2,0},{0,1,2},{0,2,1},{1,0,2},{2,0,1},{2,1,0},{4,5,7}}) {
                GLState.enableVertexArrays(0,1);
                BCycleTextBucket.Drawer.enableAttributes(order[0],order[1],order[2]);
                assertTrue("Position missing",enabled.contains(order[0]));
                assertTrue("Texture coordinates missing: "+java.util.Arrays.toString(order),enabled.contains(order[1]));
                assertTrue("Fade missing",enabled.contains(order[2]));
                BCycleTextBucket.Drawer.disableAttributes(order[0],order[1],order[2]);
                assertTrue("Custom attributes leaked into following layers",enabled.isEmpty());
                GLState.enableVertexArrays(0,1);
                assertEquals(new HashSet<>(java.util.Arrays.asList(0,1)),enabled);
                GLState.enableVertexArrays(-1,-1);
            }
        } finally { GLState.enableVertexArrays(-1,-1);GLAdapter.gl=original; }
    }
}
