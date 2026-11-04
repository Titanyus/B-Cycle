/*
 * Copyright 2012 Hannes Janetzek
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

// B|CYCLE adaptation (2026-11-04), derived from Mapsforge VTM 0.25.0.
// Renamed local labeling classes; modifications and upstream references:
// docs/vtm-label-stability.md. Original LGPL notice retained above.
package org.oscim.layers.tile.vector.labeling;

import org.oscim.layers.tile.vector.labeling.BCycleLabelLayer.Worker;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.RenderBucket;
import org.oscim.renderer.bucket.TextureBucket;
import org.oscim.renderer.bucket.BCycleTextBucket;
import org.oscim.renderer.MapRenderer;
import android.os.SystemClock;

class BCycleTextRenderer extends BucketRenderer {
    static final boolean dbg = false;

    private final Worker mWorker;

    public BCycleTextRenderer(Worker worker) {
        mWorker = worker;
    }

    private long epoch,fadeUntil;
    private BCycleTextBucket.Drawer textDrawer;
    @Override public boolean setup() { textDrawer=new BCycleTextBucket.Drawer();return true; }

    public synchronized void release() {
        buckets.clear();setReady(false);
        if(textDrawer!=null) {
            GLState.useProgram(0);org.oscim.backend.GLAdapter.gl.deleteProgram(textDrawer.program);textDrawer=null;
        }
    }

    @Override
    public synchronized void update(GLViewport v) {

        BCycleLabelTask t;
        synchronized (mWorker) {
            t = mWorker.poll();
            if (t == null) {
                if (!mWorker.isRunning()) {
                    mWorker.submit(50);
                }
                return;
            }
            buckets.clear();
        }

        // set new TextLayer to be uploaded and rendered
        buckets.set(t.layers);
        mMapPosition = t.pos;
        epoch=t.epoch;fadeUntil=t.fadeUntil;
        compile();
    }

    @Override
    public synchronized void render(GLViewport v) {
        GLState.test(false, false);
        //Debug.draw(pos, layers);

        buckets.vbo.bind();

        float scale = (float) (v.pos.scale / mMapPosition.scale);

        setMatrix(v, false);

        for (RenderBucket l = buckets.get(); l != null; ) {
            buckets.vbo.bind();
            if(l instanceof BCycleTextBucket) {
                textDrawer.draw((BCycleTextBucket)l,buckets.vbo,v,scale,SystemClock.uptimeMillis()-epoch);l=l.next;
            } else l=TextureBucket.Renderer.draw(l,v,scale);
        }
        if(SystemClock.uptimeMillis()<fadeUntil) MapRenderer.animate();
    }

}
