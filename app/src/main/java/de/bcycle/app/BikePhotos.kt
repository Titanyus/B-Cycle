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
    fun migrateIdentities(c:Context) {
        val root=File(c.filesDir,"bike-photos")
        root.listFiles().orEmpty().forEach { source ->
            val slot=Regex("bike-([0-9]+)\\.webp").matchEntire(source.name)?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            val id=BikeIdentities.id(c.getSharedPreferences("bike",0),slot)
            val target=File(root,"by-id-$id.webp")
            java.nio.file.Files.move(source.toPath(),target.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }
    fun file(c:Context,slot:Int):File {
        require(slot>=0)
        val local=File(c.filesDir,"bike-photos/bike-$slot.webp")
        val id=BikeIdentities.id(c.getSharedPreferences("bike",0),slot)
        val portable=File(c.filesDir,"bike-photos/by-id-$id.webp")
        return if(!local.isFile && portable.isFile) portable else local
    }
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
internal class BikePhotoView(context:Context,private val themes:ThemeManager,private val slot:Int,type:BikeType,choose:()->Unit):android.widget.FrameLayout(context) {
    private val image=android.widget.ImageView(context).apply { visibility=GONE;scaleType=android.widget.ImageView.ScaleType.CENTER_CROP;contentDescription=context.getString(R.string.bike_photo) }
    private val select=object:android.widget.ImageView(context) {
        override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
            super.onSizeChanged(w,h,oldw,oldh)
            val size=BicycleDesign.dp(context,BicycleDesign.bikePhotoPlaceholderIconSize)
            val x=((w-size)/2).coerceAtLeast(0);val y=((h-size)/2).coerceAtLeast(0)
            setPadding(x,y,x,y)
        }
    }.apply {
        tag="bike_photo_choose";scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
        isClickable=true;isFocusable=true;contentDescription=context.getString(R.string.bike_photo_choose)
        setOnClickListener { choose() }
        BicycleComponents.bindTheme(this,themes) { tokens ->
            background=BicycleComponents.controlBackground(context,tokens)
            setImageDrawable(BicycleIcons.drawable(context,BikeTypeIcons.forType(type),tokens.secondaryTextColor).apply {
                val size=BicycleDesign.dp(context,BicycleDesign.bikePhotoPlaceholderIconSize);setBounds(0,0,size,size)
            })
        }
    }
    private val remove=BicycleComponents.icon(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.bike_photo_remove)) {
        BikePhotos.worker.execute { BikePhotos.delete(context.applicationContext,slot);post { refresh() } }
    }.apply { tag="bike_photo_remove";visibility=GONE }
    private var generation=0
    init {
        tag="bike_photo";clipToOutline=true
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY,BicycleDesign.inputRadius)
        addView(image,LayoutParams(-1,-1));addView(select,LayoutParams(-1,-1))
        addView(remove,LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize),android.view.Gravity.TOP or android.view.Gravity.END))
        image.setOnClickListener { choose() }
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val width=android.view.View.MeasureSpec.getSize(widthMeasureSpec)
        val height=(width/BicycleDesign.bikePhotoAspectRatio).toInt()
        super.onMeasure(widthMeasureSpec,android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY))
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();refresh() }
    override fun onDetachedFromWindow() { generation++;super.onDetachedFromWindow() }
    fun refresh() {
        val token=++generation
        BikePhotos.worker.execute {
            val bitmap=runCatching { val file=BikePhotos.file(context,slot);if(file.isFile) android.graphics.BitmapFactory.decodeFile(file.path) else null }.getOrNull()
            post {
                if(token!=generation || !isAttachedToWindow) { bitmap?.recycle();return@post }
                image.setImageBitmap(bitmap);image.visibility=if(bitmap!=null) VISIBLE else GONE
                select.visibility=if(bitmap==null) VISIBLE else GONE;remove.visibility=if(bitmap!=null) VISIBLE else GONE
            }
        }
    }
}
