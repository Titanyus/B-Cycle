package org.oscim.layers.tile.vector.labeling;

import org.oscim.theme.styles.TextStyle;

/** Cohorts use VTM's 256-pixel zoom (MapLibre zoom + 1), independent of tile arrival. */
final class BCycleLabelPolicy {
    static boolean place(TextStyle text) { return text.style != null && text.style.startsWith("place-"); }
    static int importance(TextStyle text) { return place(text) || text.priority>=10000 ? text.priority/100 : 1000+text.priority; }
    static float minimumZoom(TextStyle text) {
        if (!place(text)) return -1;
        int importance=importance(text);
        if(importance<20) return 0;
        if(importance<30) return 6.25f;
        if(importance<40) return 9.25f;
        if(importance<50) return 11.25f;
        if(importance<60) return 13.25f;
        if(importance<70) return 15.25f;
        return 16.25f;
    }
    static boolean eligible(TextStyle text,double zoom,boolean retained) {
        return !place(text) || zoom >= minimumZoom(text)+(retained ? -.8 : .2);
    }
    static float horizontalGap(TextStyle text) { return place(text) ? Math.max(32,text.fontHeight*3) : 5; }
    static float verticalGap(TextStyle text) { return place(text) ? Math.max(20,text.fontHeight*1.5f) : 5; }
}
