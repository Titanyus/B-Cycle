package de.bcycle.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors

/** Private, bounded photo copies: no dependency on an expiring document URI. */
internal object BikePhotos {
    val worker=Executors.newSingleThreadExecutor { Thread(it,"Bicycle-bike-photo") }
    fun file(c:Context,slot:Int):File { require(slot >= 0);return File(c.filesDir,"bike-photos/bike-$slot.webp") }
    fun import(c:Context,slot:Int,uri:Uri) {
        val input=File.createTempFile("bike-photo-", ".input",c.cacheDir)
        try {
            c.contentResolver.openInputStream(uri).use { source ->
                require(source!=null)
                input.outputStream().use { output ->
                    val buffer=ByteArray(16384);var total=0
                    while(true) { val n=source.read(buffer);if(n<0) break;total+=n;require(total<=25*1024*1024);output.write(buffer,0,n) }
                }
            }
            val bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(input)) { decoder,info,_ ->
                val scale=1200.0/maxOf(info.size.width,info.size.height).coerceAtLeast(1200)
                decoder.setTargetSize((info.size.width*scale).toInt().coerceAtLeast(1),(info.size.height*scale).toInt().coerceAtLeast(1))
                decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            }
            try {
                val target=file(c,slot);target.parentFile!!.mkdirs();val atomic=AtomicFile(target)
                val stream=atomic.startWrite()
                try { check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY,85,stream));atomic.finishWrite(stream) }
                catch(e:Exception) { atomic.failWrite(stream);throw e }
            } finally { bitmap.recycle() }
        } finally { input.delete() }
    }
    fun delete(c:Context,slot:Int) { AtomicFile(file(c,slot)).delete() }
}
internal class BikePhotoView(context:Context,private val themes:ThemeManager,private val slot:Int,choose:()->Unit):android.widget.FrameLayout(context) {
    private val image=android.widget.ImageView(context).apply { visibility=GONE;scaleType=android.widget.ImageView.ScaleType.CENTER_CROP;contentDescription=context.getString(R.string.bike_photo) }
    private val select=BicycleComponents.action(context,themes,context.getString(R.string.bike_photo_choose),choose).apply { tag="bike_photo_choose" }
    private val remove=BicycleComponents.icon(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.bike_photo_remove)) {
        BikePhotos.worker.execute { BikePhotos.delete(context.applicationContext,slot);post { refresh() } }
    }.apply { tag="bike_photo_remove";visibility=GONE }
    private var generation=0
    init {
        tag="bike_photo";clipToOutline=true
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY,BicycleDesign.inputRadius)
        addView(image,LayoutParams(-1,-1));addView(select,LayoutParams(-2,-2,android.view.Gravity.CENTER))
        BicycleComponents.background(remove,themes,ThemeRole.TERTIARY,BicycleDesign.inputRadius)
        addView(remove,LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize),android.view.Gravity.TOP or android.view.Gravity.END))
        BicycleDialogStyle.actionIcon(select,themes,BicycleIcon.Import)
        image.setOnClickListener { choose() }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();refresh() }
    override fun onDetachedFromWindow() { generation++;super.onDetachedFromWindow() }
    fun refresh() {
        val token=++generation
        BikePhotos.worker.execute {
            val bitmap=runCatching { val file=BikePhotos.file(context,slot);if(file.isFile) android.graphics.BitmapFactory.decodeFile(file.path) else null }.getOrNull()
            post {
                if(token!=generation || !isAttachedToWindow) { bitmap?.recycle();return@post }
                layoutParams?.let { it.height=BicycleDesign.dp(context,if(bitmap==null) BicycleDesign.bikePhotoEmptyHeight else BicycleDesign.bikePhotoHeight);layoutParams=it }
                image.setImageBitmap(bitmap);image.visibility=if(bitmap!=null) VISIBLE else GONE
                select.visibility=if(bitmap==null) VISIBLE else GONE;remove.visibility=if(bitmap!=null) VISIBLE else GONE
            }
        }
    }
}
