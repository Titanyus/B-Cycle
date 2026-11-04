package org.oscim.layers.tile.vector.labeling;

import android.os.SystemClock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.bucket.TextItem;

/** Small worker-owned transition history; glyphs stay in native text atlases, alpha runs on GL. */
final class BCycleLabelFades {
    static final int DURATION=240;
    private final LinkedHashMap<String,State> states=new LinkedHashMap<>();
    private long generation;
    static float value(float from,float to,long start,int duration,long now) {
        float t=duration==0 ? 1 : Math.max(0,Math.min(1,(now-start)/(float)duration));
        t=t*t*(3-2*t);return from+(to-from)*t;
    }
    private static final class State {
        final TextItem item=new TextItem();
        float from,to;long start,seen;double wx,wy,scale;int duration,age;
        float opacity(long now) { return value(from,to,start,duration,now); }
        void target(float next,long now,int milliseconds) {
            if(next==to) return;
            from=opacity(now);to=next;start=now;duration=milliseconds;
        }
    }
    private static String identity(TextItem item,double wx,double wy) {
        String family=BCycleLabelPolicy.place(item.text) ? "place" : item.text.style;
        return family+'\u0000'+item.label+'\u0000'+Math.round((wx-Math.floor(wx))*32768)+":"+Math.round(wy*32768);
    }
    int visibleAge(TextItem item,double wx,double wy,int otherwise) {
        State state=states.get(identity(item,wx,wy));
        return state!=null && state.opacity(SystemClock.uptimeMillis())>.005f ? state.age : otherwise;
    }
    void clear() { states.clear(); }
    void build(BCycleLabelTask task,Label labels,int width,int height) {
        long now=SystemClock.uptimeMillis();
        int duration=android.animation.ValueAnimator.areAnimatorsEnabled() ? DURATION : 0;
        generation++;task.epoch=now;task.fadeUntil=now;task.textLayer.begin();
        MapPosition pos=task.pos;double world=Tile.SIZE*pos.scale;
        for(Label label=labels;label!=null;label=(Label)label.next) {
            double wx=pos.x+label.x/world,wy=pos.y+label.y/world;
            wx=wx-Math.floor(wx);
            String id=identity(label,wx,wy);
            State state=states.get(id);
            if(state==null) { state=new State();states.put(id,state); }
            state.target(1,now,duration);state.age=label.active;state.seen=generation;state.wx=wx;state.wy=wy;state.scale=pos.scale;
            TextItem item=state.item;
            item.label=label.label;item.text=label.text;item.width=label.width;item.length=label.length;
            item.x=label.x;item.y=label.y;item.x1=label.x1;item.y1=label.y1;item.x2=label.x2;item.y2=label.y2;
        }
        Iterator<State> iterator=states.values().iterator();
        while(iterator.hasNext()) {
            State state=iterator.next();
            if(state.seen!=generation) state.target(0,now,duration);
            if(state.to==0 && state.opacity(now)<=0) { iterator.remove();continue; }
            double dx=state.wx-pos.x;dx-=Math.floor(dx+.5);
            float x=(float)(dx*world),y=(float)((state.wy-pos.y)*world);
            if(Math.abs(x)>width+Tile.SIZE || Math.abs(y)>height+Tile.SIZE) { iterator.remove();continue; }
            TextItem old=state.item,item=TextItem.pool.get();
            float ratio=(float)(pos.scale/state.scale);
            item.label=old.label;item.text=old.text;item.width=old.width;item.length=old.length;
            item.x=x;item.y=y;item.x1=x+(old.x1-old.x)*ratio;item.y1=y+(old.y1-old.y)*ratio;
            item.x2=x+(old.x2-old.x)*ratio;item.y2=y+(old.y2-old.y)*ratio;
            task.textLayer.addText(item);
            boolean transitioning=now<state.start+state.duration;
            task.textLayer.fade(item,transitioning ? state.from : state.to,state.to,
                    (int)Math.max(-1000,state.start-now),transitioning ? state.duration : 0);
            if(transitioning) task.fadeUntil=Math.max(task.fadeUntil,state.start+state.duration);
        }
        // A fast pan cannot leave an unbounded trail of outgoing names.
        while(states.size()>1024) states.remove(states.keySet().iterator().next());
    }
}
