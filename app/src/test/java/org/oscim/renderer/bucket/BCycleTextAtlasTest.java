package org.oscim.renderer.bucket;

import de.bcycle.app.BicycleMapView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscim.theme.styles.TextStyle;
import org.oscim.renderer.MapRenderer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BCycleTextAtlasTest {
    private static short[] vertices(BCycleTextBucket bucket) {
        int size=bucket.vertexItems.countSize();short[] out=new short[size];int offset=0;
        for(VertexData.Chunk chunk=bucket.vertexItems.head();chunk!=null;chunk=chunk.next) {
            System.arraycopy(chunk.vertices,0,out,offset,chunk.used);offset+=chunk.used;
        }
        return out;
    }
    private static TextItem item(String name,TextStyle style) { return new TextItem().set(100,100,name,style); }
    private static void bounded(BCycleTextBucket bucket) {
        short[] v=vertices(bucket);
        for(TextureItem texture=bucket.textures;texture!=null;texture=texture.next) {
            int start=texture.offset/6*24,end=start+texture.indices/6*24;
            for(int i=start;i<end;i+=6) {
                assertTrue("u inside its atlas: "+v[i+4],v[i+4]>=0 && v[i+4]<=texture.width*MapRenderer.COORD_SCALE);
                assertTrue("v inside its atlas: "+v[i+5],v[i+5]>=0 && v[i+5]<=texture.height*MapRenderer.COORD_SCALE);
            }
        }
    }
    @Test public void tallerCityMustMoveToNewAtlasEvenWhenItFitsTheRemainingRowWidth() {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        try {
            for(String name:new String[]{"Günzburg","Dillingen an der Donau"}) {
                BCycleTextBucket packed=new BCycleTextBucket(),single=new BCycleTextBucket();
                TextStyle small=TextStyle.builder().style("test-filler").fontSize(10).isCaption(true).build();small.fontHeight=15.5f;
                TextStyle city=TextStyle.builder().style("place-city").fontSize(18).isCaption(true).build();
                try {
                    TextItem first=null,last=null;
                    for(int i=0;i<16;i++) {
                        TextItem next=item("i"+i,small);next.width=i<15 ? TextureBucket.TEXTURE_WIDTH-2 : 60;
                        if(first==null) first=next;else last.next=next;last=next;
                    }
                    TextItem caption=item(name,city);last.next=caption;packed.setLabels(first);packed.prepare();
                    bounded(packed);
                    assertNotNull("A taller last-row caption needs a new page",packed.textures.next);
                    single.setLabels(item(name,city));single.prepare();
                    int[] expected=single.textures.bitmap.getPixels(),actual=packed.textures.next.bitmap.getPixels();
                    int width=(int)Math.ceil(BCycleCaptionLayout.get(city,name).width)+2;
                    int height=(int)Math.ceil(BCycleCaptionLayout.get(city,name).height);
                    for(int y=0;y<height;y++) for(int x=0;x<width;x++)
                        assertEquals("Complete glyphs on new atlas page",expected[y*TextureBucket.TEXTURE_WIDTH+x],actual[y*TextureBucket.TEXTURE_WIDTH+x]);
                    assertEquals(16*6,packed.textures.indices);assertEquals(16*6,packed.textures.next.offset);assertEquals(6,packed.textures.next.indices);
                } finally { packed.clear();single.clear(); }
            }
        } finally { view.disposeMap(); }
    }    @Test public void largeTwoLineCaptionGetsAnAtlasPageTallEnoughForItsActualGlyphs() {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());BCycleTextBucket bucket=new BCycleTextBucket();
        try {
            TextStyle city=TextStyle.builder().style("place-city").fontSize(140).strokeWidth(2.4f).strokeColor(0xffffffff).isCaption(true).build();
            bucket.setLabels(item("Dillingen an der Donau",city));bucket.prepare();bounded(bucket);
            TextureItem page=bucket.textures;while(page.indices==0) page=page.next;
            assertTrue("Large two-line labels must not be clamped to the standard256px page",page.height>256);
            int[] pixels=page.bitmap.getPixels();int lower=0;
            for(int y=256;y<page.height;y++) for(int x=0;x<page.width;x++) if((pixels[y*page.width+x]>>>24)>64) lower++;
            assertTrue("Second line really exists below the old atlas boundary",lower>100);
            assertTrue(page.width<=2048 && page.height<=1024);
        } finally { bucket.clear();view.disposeMap(); }
    }

}
