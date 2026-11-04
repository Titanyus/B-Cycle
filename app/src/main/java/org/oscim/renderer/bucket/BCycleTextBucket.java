/*
 * Copyright 2012, 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
// B|CYCLE 2026-11-04: TextureBucket draw adaptation with separate fade attributes.
// Native six-short glyph vertices/UV packing are unchanged; see docs/vtm-label-stability.md.
package org.oscim.renderer.bucket;

import java.nio.ShortBuffer;
import java.util.IdentityHashMap;
import org.oscim.backend.GL;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import static org.oscim.backend.GLAdapter.gl;

/** Keep VTM glyph packing/geometry; a separate small VBO supplies animation metadata. */
public final class BCycleTextBucket extends TextBucket {
    private final IdentityHashMap<TextItem,short[]> fades=new IdentityHashMap<>();
    private final VertexData metadata=new VertexData();
    private BufferObject fadeVbo;
    public void begin() { fades.clear();metadata.dispose(); }
    public void fade(TextItem item,float from,float to,int start,int duration) {
        fades.put(item,new short[]{(short)Math.round(from*1000),(short)Math.round(to*1000),
                (short)Math.max(-1000,Math.min(1000,start)),(short)duration,0,0});
    }
    public void prepared() { fades.clear();clearLabels(); }
    @Override protected void addItem(TextItem item,float width,float height,float x,float y) {
        super.addItem(item,width,height,x,y);
        short[] fade=fades.get(item);
        if(fade==null) fade=new short[]{1000,1000,0,0,-100,0};
        for(int i=0;i<4;i++) metadata.add(fade[0],fade[1],fade[2],fade[3],fade[4],fade[5]);
    }
    @Override protected void compile(ShortBuffer vertices,ShortBuffer indices) {
        super.compile(vertices,indices);
        int size=metadata.countSize();
        if(size==0) return;
        ShortBuffer data=MapRenderer.getShortBuffer(size);metadata.compile(data);
        if(fadeVbo==null) fadeVbo=BufferObject.get(GL.ARRAY_BUFFER,size*2);
        fadeVbo.loadBufferData(data.flip(),size*2);
    }
    @Override public void clear() {
        super.clear();fades.clear();metadata.dispose();fadeVbo=BufferObject.release(fadeVbo);
    }
    /** Per-map/context shader, never shares a GL program name with another MapView. */
    public static final class Drawer extends GLShader {
        private final int mv,proj,scale,coord,div,time,pos,tex,fade;
        public Drawer() {
            if(!create("bcycle_labels")) throw new IllegalStateException("Label shader unavailable");
            mv=getUniform("u_mv");proj=getUniform("u_proj");scale=getUniform("u_scale");coord=getUniform("u_coord_scale");
            div=getUniform("u_div");time=getUniform("u_time");
            pos=getAttrib("a_pos");tex=getAttrib("a_tex_coord");fade=getAttrib("a_fade");
        }
        public void draw(BCycleTextBucket bucket,BufferObject main,GLViewport viewport,float ratio,float elapsed) {
            if(bucket.fadeVbo==null) return;
            GLState.test(false,false);GLState.blend(true);useProgram();GLState.enableVertexArrays(pos,tex);
            gl.enableVertexAttribArray(fade);
            gl.uniform1f(scale,bucket.fixed ? 1/ratio : 1);gl.uniform1f(coord,MapRenderer.COORD_SCALE);
            gl.uniform1f(time,Math.min(1000,elapsed));
            viewport.proj.setAsUniform(proj);viewport.mvp.setAsUniform(mv);MapRenderer.bindQuadIndicesVBO();
            for(TextureItem texture=bucket.textures;texture!=null;texture=texture.next) {
                gl.uniform2f(div,1f/(texture.width*MapRenderer.COORD_SCALE),1f/(texture.height*MapRenderer.COORD_SCALE));texture.bind();
                for(int i=0;i<texture.indices;i+=MapRenderer.MAX_INDICES) {
                    int offset=(texture.offset+i)*8;
                    main.bind();
                    gl.vertexAttribPointer(pos,4,GL.SHORT,false,12,bucket.vertexOffset+offset);
                    gl.vertexAttribPointer(tex,2,GL.SHORT,false,12,bucket.vertexOffset+offset+8);
                    bucket.fadeVbo.bind();
                    gl.vertexAttribPointer(fade,4,GL.SHORT,false,12,offset);
                    gl.drawElements(GL.TRIANGLES,Math.min(texture.indices-i,MapRenderer.MAX_INDICES),GL.UNSIGNED_SHORT,0);
                }
            }
            gl.disableVertexAttribArray(fade);
        }
    }
}
