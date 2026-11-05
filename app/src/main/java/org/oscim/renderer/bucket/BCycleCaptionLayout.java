package org.oscim.renderer.bucket;

import org.oscim.theme.styles.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Two-line place captions, shared by atlas packing and collision placement; source names stay intact. */
public final class BCycleCaptionLayout {
    private static final class Key {
        final TextStyle style; final String name;
        Key(TextStyle style,String name) { this.style=style;this.name=name; }
        @Override public int hashCode() { return 31*System.identityHashCode(style)+name.hashCode(); }
        @Override public boolean equals(Object value) { return value instanceof Key && ((Key)value).style==style && ((Key)value).name.equals(name); }
    }
    private static final Map<Key,BCycleCaptionLayout> CACHE=new LinkedHashMap<Key,BCycleCaptionLayout>(256,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key,BCycleCaptionLayout> entry) { return size()>256; }
    };
    public final String first,second;
    public final float firstWidth,secondWidth,width,height,lineAdvance;
    private BCycleCaptionLayout(TextStyle style,String name) {
        String[] lines=split(style,name);
        first=lines[0];second=lines.length==2 ? lines[1] : null;
        firstWidth=style.paint.measureText(first);secondWidth=second==null ? 0 : style.paint.measureText(second);
        width=Math.max(firstWidth,secondWidth);
        lineAdvance=style.fontHeight*1.12f;
        height=style.fontHeight+(second==null ? 0 : lineAdvance);
    }
    public static synchronized BCycleCaptionLayout get(TextStyle style,String name) {
        Key key=new Key(style,name);BCycleCaptionLayout layout=CACHE.get(key);
        if(layout==null) { layout=new BCycleCaptionLayout(style,name);CACHE.put(key,layout); }
        return layout;
    }
    public static boolean place(TextStyle style) {
        return style.caption && style.style!=null && (style.style.equals("place-other") || style.style.equals("place-village") || style.style.equals("place-town") || style.style.startsWith("place-city"));
    }
    public static float padding(TextStyle style) {
        return style.style!=null && style.style.startsWith("place-") && style.stroke!=null ? (float)Math.ceil(style.stroke.getStrokeWidth()/2)+1 : 0;
    }
    public static float height(TextItem item) { return (place(item.text) ? get(item.text,item.label).height : item.text.fontHeight)+2*padding(item.text); }
    public static float width(TextItem item) { return place(item.text) ? get(item.text,item.label).width+2*padding(item.text) : item.width; }
    private static String[] split(TextStyle style,String name) {
        if(!place(style)) return new String[]{name};
        String text=name.trim().replaceAll("\\s+"," ");
        if(text.length()<16 || !text.contains(" ")) return new String[]{name};
        String[] words=text.split(" ");
        int cut=-1;
        for(int i=1;i<words.length-1;i++) {
            String word=words[i].toLowerCase(Locale.ROOT);
            if(word.equals("an") || word.equals("am") || word.equals("bei") || word.equals("im") || word.equals("in") || word.equals("unter") || word.equals("ob") || word.equals("auf") || word.equals("vor") || word.equals("hinter") || word.equals("über")) { cut=i;break; }
        }
        if(cut<0) {
            float best=Float.MAX_VALUE;
            for(int i=1;i<words.length;i++) {
                float a=style.paint.measureText(join(words,0,i)),b=style.paint.measureText(join(words,i,words.length));
                float score=Math.max(a,b);
                if(score<best) { best=score;cut=i; }
            }
        }
        return new String[]{join(words,0,cut),join(words,cut,words.length)};
    }
    private static String join(String[] words,int first,int end) {
        StringBuilder result=new StringBuilder();
        for(int i=first;i<end;i++) { if(i>first) result.append(' ');result.append(words[i]); }
        return result.toString();
    }
}
