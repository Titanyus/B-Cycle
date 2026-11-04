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
    public static boolean inlineShield(org.oscim.theme.styles.TextStyle text) {
        return text.style!=null && text.style.startsWith("highway-shield") && text.bitmap!=null;
    }
    // Derived from VTM TextBucket.prepare (2012 Hannes Janetzek, 2019 devemux86).
    @Override
    public void prepare() {
        int numIndices = 0;
        int offsetIndices = 0;

        int advanceY = 0;
        float x = 0;
        float y = 0;
        float xx, yy;

        TextureItem t = pool.get();
        textures = t;
        mCanvas.setBitmap(t.bitmap);

        for (TextItem it = labels; it != null; ) {

            float width = it.width + 2 * mFontPadX;
            float height = (int) (it.text.fontHeight) + 0.5f;

            boolean shield=inlineShield(it.text);
            if(shield) {
                width=Math.max(width,it.text.bitmap.getWidth());
                height=Math.max(height,it.text.bitmap.getHeight());
            }
            if (height > TEXTURE_HEIGHT)
                height = TEXTURE_HEIGHT;

            if (height > advanceY)
                advanceY = (int) height;

            if (x + width > TEXTURE_WIDTH) {
                x = 0;
                y += advanceY;
                advanceY = (int) (height + 0.5f);

                if (y + height > TEXTURE_HEIGHT) {
                    t.offset = offsetIndices;
                    t.indices = (numIndices - offsetIndices);
                    offsetIndices = numIndices;

                    t.next = pool.get();
                    t = t.next;

                    mCanvas.setBitmap(t.bitmap);

                    x = 0;
                    y = 0;
                    advanceY = (int) height;
                }
            }

            xx = x + mFontPadX;
            yy = y + height - it.text.fontDescent;

            // FIXME !!!
            if (width > TEXTURE_WIDTH)
                width = TEXTURE_WIDTH;

            if (it.text.bgFill != null)
                mCanvas.fillRectangle(x + mFontPadX, y + 1, width - 2 * mFontPadX, height, it.text.bgFill.getColor());

            if(shield) {
                // One atlas cell and one fade: the plate can never outlive its number.
                mCanvas.drawBitmap(it.text.bitmap,x+(width-it.text.bitmap.getWidth())/2,
                        y+(height-it.text.bitmap.getHeight())/2);
                xx=x+(width-it.width)/2;
                yy=y+(height+it.text.fontHeight)/2-it.text.fontDescent;
            }
            mCanvas.drawText(it.label, xx, yy, it.text.paint, it.text.stroke);

            while (it != null) {
                addItem(it, width, height, x, y);

                /* six indices to draw the four vertices */
                numIndices += TextureBucket.INDICES_PER_SPRITE;
                numVertices += 4;

                if (it.next == null
                        || (it.next.text != it.text)
                        || (it.next.label != it.label)) {
                    it = it.next;
                    break;
                }
                it = it.next;

            }
            x += width;
        }

        t.offset = offsetIndices;
        t.indices = (numIndices - offsetIndices);
    }

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
        static void enableAttributes(int pos,int tex,int fade) {
            // VTM tracks only slots 0/1. Drivers may assign any slot to each
            // of our three attributes; leave its cache empty and own all three.
            GLState.enableVertexArrays(-1,-1);
            gl.enableVertexAttribArray(pos);gl.enableVertexAttribArray(tex);gl.enableVertexAttribArray(fade);
        }
        static void disableAttributes(int pos,int tex,int fade) {
            gl.disableVertexAttribArray(pos);gl.disableVertexAttribArray(tex);gl.disableVertexAttribArray(fade);
        }
        private final int mv,proj,scale,coord,div,time,pos,tex,fade;
        public Drawer() {
            if(!create("bcycle_labels")) throw new IllegalStateException("Label shader unavailable");
            mv=getUniform("u_mv");proj=getUniform("u_proj");scale=getUniform("u_scale");coord=getUniform("u_coord_scale");
            div=getUniform("u_div");time=getUniform("u_time");
            pos=getAttrib("a_pos");tex=getAttrib("a_tex_coord");fade=getAttrib("a_fade");
        }
        public void draw(BCycleTextBucket bucket,BufferObject main,GLViewport viewport,float ratio,float elapsed) {
            if(bucket.fadeVbo==null) return;
            GLState.test(false,false);GLState.blend(true);useProgram();enableAttributes(pos,tex,fade);
            try {
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
            } finally {
                disableAttributes(pos,tex,fade);
            }
        }
    }
}
