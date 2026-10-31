package de.bcycle.app

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.View
import android.widget.Spinner
import com.garmin.fit.FitDecoder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingSourcesTest {
    private val app get()=RuntimeEnvironment.getApplication()
    @Test fun fallbackUsesOnlyFreshCompatibleReadingsAndReturnsToPreferredSource() {
        val preferences=RecordingSources(mapOf(RecordingQuantity.SPEED to RecordingSource.BIKE))
        val gps=SourceReading(4.0,6000);val bike=SourceReading(5.0,0)
        assertEquals(RecordingSource.GPS,chooseReading(RecordingQuantity.SPEED,preferences,mapOf(RecordingSource.BIKE to bike,RecordingSource.GPS to gps),6000)!!.source)
        assertEquals(RecordingSource.BIKE,chooseReading(RecordingQuantity.SPEED,preferences,mapOf(RecordingSource.BIKE to bike.copy(atMs=6500),RecordingSource.GPS to gps),6500)!!.source)
        assertNull(chooseReading(RecordingQuantity.SPEED,preferences,mapOf(RecordingSource.GPS to gps),12000))
        assertNull(chooseReading(RecordingQuantity.SPEED,preferences,mapOf(RecordingSource.BAROMETER to gps),6000))
        assertThrows(IllegalArgumentException::class.java) { RecordingSources.decode("{\"SPEED\":\"BAROMETER\"}") }
    }
    @Test fun distanceCountersSwitchWithoutImportingAccumulatedInactiveDistanceOrResettingTheRide() {
        val distance=SourceDistance()
        fun reading(source:RecordingSource,value:Double)=ChosenReading(source,SourceReading(value,0))
        assertEquals(0.0,distance.add(reading(RecordingSource.BIKE,1000.0)),0.0)
        assertEquals(10.0,distance.add(reading(RecordingSource.BIKE,1010.0)),0.0)
        assertEquals(10.0,distance.add(reading(RecordingSource.GPS,600.0)),0.0)
        assertEquals(15.0,distance.add(reading(RecordingSource.GPS,605.0)),0.0)
        assertEquals(15.0,distance.add(reading(RecordingSource.BIKE,1400.0)),0.0)
        assertEquals(20.0,distance.add(reading(RecordingSource.BIKE,1405.0)),0.0)
        assertEquals(20.0,distance.add(null),0.0)
        assertEquals(20.0,distance.add(reading(RecordingSource.BIKE,1800.0)),0.0)
        assertEquals(20.0,distance.add(reading(RecordingSource.BIKE,0.0)),0.0)
    }
    @Test fun smartphoneBarometerRequiresCalibrationAndNeverInventsAnMslReference() {
        val manager=app.getSystemService(SensorManager::class.java)
        val sensor=ShadowSensor.newInstance(Sensor.TYPE_PRESSURE);shadowOf(manager).addSensor(sensor)
        val barometer=PhoneBarometer(app);assertTrue(barometer.available);barometer.start()
        fun pressure(value:Float) {
            val event=ShadowSensorManager.createSensorEvent(1,Sensor.TYPE_PRESSURE)
            event.sensor=sensor;event.timestamp=SystemClock.elapsedRealtimeNanos();event.values[0]=value
            barometer.onSensorChanged(event)
        }
        pressure(1000f);assertNull(barometer.reading(null))
        val now=SystemClock.elapsedRealtime()
        assertEquals(500.0,barometer.reading(SourceReading(500.0,now,false))!!.value,.001)
        assertFalse(barometer.reading(null)!!.seaLevel)
        pressure(999f);assertTrue(barometer.reading(null)!!.value>500)
        SystemClock.sleep(6000);assertNull(barometer.reading(null))
        barometer.stop();pressure(998f);assertNull(barometer.reading(SourceReading(500.0,SystemClock.elapsedRealtime(),true)))
    }
    @Test fun allQuantitiesShowSourceDropdownsButOnlyMultipleConfiguredSourcesAreEditableAndBackedUp() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=host.get();val prefs=activity.getSharedPreferences("bike",0);val profiles=BikeProfileStore(activity,prefs)
            val profile=profiles.current().copy(bikeAddress="00:11:22:33:44:55",gpsEnabled=true)
            val sources=RecordingSources.defaults(profile.recording,false).copy(preferred=mapOf(RecordingQuantity.SPEED to RecordingSource.GPS,RecordingQuantity.HEIGHT to RecordingSource.BAROMETER))
            profiles.save(profile.copy(recording=profile.recording.copy(sourcePreferences=sources)))
            val themes=ThemeManager(prefs)
            var editable=true;val changes=mutableListOf<RecordingSources>()
            val view=RecordingSourcesView(activity,themes,profile,true,sources,{editable},{changes+=it})
            activity.setContentView(view)
            assertNotNull(view.findViewWithTag<Spinner>("record_source_choice_SPEED"))
            assertNotNull(view.findViewWithTag<Spinner>("record_source_choice_HEIGHT"))
            val power=view.findViewWithTag<Spinner>("record_source_choice_POWER")
            assertNotNull(power);assertFalse(power.isEnabled)
            assertEquals(activity.getString(RecordingSource.BIKE.label),power.selectedItem)
            val heart=view.findViewWithTag<Spinner>("record_source_choice_HEART")
            assertFalse(heart.isEnabled);assertEquals(activity.getString(R.string.source_none),heart.selectedItem)
            assertTrue(RecordingQuantity.entries.all { view.findViewWithTag<Spinner>("record_source_choice_${it.name}")!=null })
            shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(changes.isEmpty())
            val speed=view.findViewWithTag<Spinner>("record_source_choice_SPEED")
            speed.setSelection(0);shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(RecordingSource.BIKE,changes.last().preferred[RecordingQuantity.SPEED])
            editable=false;view.refreshLock();assertFalse(speed.isEnabled)
            speed.setSelection(1);shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(0,speed.selectedItemPosition);assertEquals(1,changes.size)
            editable=true;view.refreshLock();assertTrue(speed.isEnabled);assertFalse(power.isEnabled);assertFalse(heart.isEnabled)
            val other=profiles.add()!!;profiles.select(other.slot)
            profiles.save(profiles.current().copy(recording=profile.recording.copy(sourcePreferences=sources)))
            profiles.select(profile.slot)
            view.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST))
            view.layout(0,0,400,view.measuredHeight)
            val bitmap=android.graphics.Bitmap.createBitmap(400,view.measuredHeight,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File("build/reports/bikes/recording-sources.png").apply { parentFile!!.mkdirs();outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } };bitmap.recycle()
            val backup=SettingsBackup(activity,themes);val bytes=backup.export(setOf(BackupSection.PROFILE,BackupSection.RECORDING))
            prefs.edit().clear().commit();val doc=backup.decode(bytes);backup.apply(backup.plan(doc,doc.sections.keys))
            assertTrue(BikeProfileStore(activity,prefs).all().all { it.recording.sourcePreferences==sources })
            assertEquals(sources,RecordingSources.decode(prefs.getString(RecordingSources.KEY,"")!!))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun runtimeUsesGpsPreferenceAndBikeFallbackConsistentlyForSampleGraphAndPerformance() {
        shadowOf(app).grantPermissions(android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_SCAN)
        val runtime=RideRuntime.get(app)
        runtime.setGpsEnabled(true)
        runtime.saveConfig(RecordingConfig(setOf("GPS_SPEED","BIKE_DISTANCE"),"GPS_SPEED",sourcePreferences=
            RecordingSources(mapOf(RecordingQuantity.SPEED to RecordingSource.GPS))))
        val phase=runtime.connection.javaClass.getDeclaredField("phase").apply { isAccessible=true }
        phase.set(runtime.connection,phase.type.enumConstants!!.single { it.toString()=="LIVE" })
        runtime.beginRecording()
        fun bike() {
            runtime.onSamples(listOf(McspSample(LiveMetric.SPEED.address,1800,true))) // 18 km/h = 5 m/s
            runtime.onData(LiveData(speedHundredths=1800),true)
        }
        fun sample():RideSample=RideRuntime::class.java.getDeclaredMethod("sample").apply { isAccessible=true }.invoke(runtime) as RideSample
        fun verify(speed:Double) {
            val point=sample()
            assertEquals(speed,point.values["PRIMARY_SPEED"]!!,.001)
            assertEquals(speed*3.6,runtime.currentSpeedKmh!!,.001)
            val config=RecordingConfig(setOf("GPS_SPEED",PrimaryRecording.FLAG),"GPS_SPEED")
            assertEquals(speed*3.6,graphPoint(point,config,false,false).values[0]!!,.001)
            val field=RideRuntime::class.java.getDeclaredField("latestPerformance").apply { isAccessible=true }
            @Suppress("UNCHECKED_CAST") val events=field.get(runtime) as Map<PerformanceChannel,PerformanceEvent>
            assertEquals(speed,events.getValue(PerformanceChannel.SPEED).value!!,.001)
        }
        bike();verify(5.0) // Preferred GPS has not supplied a fix: use bike.
        val now=SystemClock.elapsedRealtime()
        assertTrue(runtime.ride.gps.accept(GpsFix(now,50.0,8.0,3.0,3.0,.2),now))
        RideRuntime::class.java.getDeclaredMethod("captureGpsPerformance").apply { isAccessible=true }.invoke(runtime)
        verify(3.0)
        SystemClock.sleep(6000);bike();verify(5.0) // GPS expired, even though it remains the configured preference.
        runtime.setGpsEnabled(false)
        assertEquals(5.0,sample().values["PRIMARY_SPEED"]!!,.001)
    }
    @Test fun trainerHeartPreferenceFallsBackWithoutOverwritingTheIndependentGattsTimestamp() {
        val runtime=RideRuntime.get(app);assertTrue(runtime.changeBikeType(BikeType.INDOOR))
        runtime.saveConfig(runtime.config.copy(sourcePreferences=RecordingSources(mapOf(RecordingQuantity.HEART to RecordingSource.TRAINER))))
        fun set(target:Any,name:String,value:Any) { target.javaClass.getDeclaredField(name).apply { isAccessible=true }.set(target,value) }
        set(runtime.heart,"live",true);set(runtime,"pulse",PulseValues(bpm=80))
        val start=SystemClock.elapsedRealtime();set(runtime,"heartAt",start)
        set(runtime.trainer,"subscribed",true)
        fun trainer(bpm:Double) {
            val frame=TrainerMeasurement(mapOf(TrainerField.HEART to bpm));val now=SystemClock.elapsedRealtime()
            runtime.trainer.readings.apply(frame,now);runtime.onTrainerMeasurement(frame,now)
        }
        trainer(100.0)
        assertEquals(100,runtime.pulseValues.bpm);assertEquals(100.0,runtime.trainingInputs().heart!!,.001)
        SystemClock.sleep(6000)
        set(runtime,"pulse",PulseValues(bpm=90));set(runtime,"heartAt",SystemClock.elapsedRealtime())
        assertEquals(90,runtime.pulseValues.bpm)
        SystemClock.sleep(1000);trainer(110.0)
        assertEquals(110,runtime.pulseValues.bpm)
        assertEquals(SystemClock.elapsedRealtime(),runtime.trainingInputs().heartAt)
        val rawAt=RideRuntime::class.java.getDeclaredField("heartAt").apply { isAccessible=true }.get(runtime)
        assertEquals(start+6000,rawAt) // Trainer receipt never refreshes a stale GATT sample.
    }
    @Test fun primaryFitValuesAndGraphsUseResolvedSourcesWhileRawDeveloperDataAndOldJournalsStayIndependent() {
        val root=File(app.cacheDir,"primary-test").apply { mkdirs() }
        val config=RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER","GPS_ALTITUDE",PrimaryRecording.FLAG))
        val start=1_790_000_000_000L
        val samples=(0..2).map { index -> RideSample(start+index*1000,index*1000L,values=mapOf(
            "SPEED" to 10.0,"BIKE_DISTANCE" to 999.0,"RIDER_POWER" to 250.0,"GPS_ALTITUDE" to 450.0,
            "PRIMARY_SPEED" to 4.0,"PRIMARY_DISTANCE" to index*4.0,"PRIMARY_POWER" to 200.0,"PRIMARY_HEIGHT" to 500.0,"PRIMARY_HEIGHT_MSL" to 1.0)) }
        val journal=File(root,"primary.ride");RideJournal(journal,start,config).use { writer->samples.forEach(writer::append) }
        val file=File(root,"primary.fit");FitWriter.encode(journal,file)
        val fit=file.inputStream().use { FitDecoder().decode(it) };val record=fit.recordMesgs.last()
        assertEquals(4.0,record.speed.toDouble(),.001);assertEquals(8.0,record.distance.toDouble(),.001)
        assertEquals(200,record.power.toInt());assertEquals(500.0,record.enhancedAltitude.toDouble(),.001)
        assertEquals(10.0,(record.developerFields.single { it.name=="SPEED" }.getValue(0) as Number).toDouble(),0.0)
        val graph=graphPoint(samples.last(),config,false,false)
        assertEquals(8.0,graph.distanceM!!,0.0);assertEquals(14.4,graph.values[0]!!,.001);assertEquals(200.0,graph.values[2]!!,0.0)
        val legacy=File(root,"legacy.ride");RideJournal(legacy,start,config.copy(fields=config.fields-PrimaryRecording.FLAG)).use { writer->samples.forEach(writer::append) }
        FitWriter.encode(legacy,file)
        assertEquals(10.0,file.inputStream().use { FitDecoder().decode(it).recordMesgs.last().speed.toDouble() },.001)
    }
    @Test fun ownFitKeepsBikeDistanceMovingTimeAndChartsDuringLongGpsOutage() {
        val root=java.nio.file.Files.createTempDirectory("own-fit-gps-outage").toFile()
        try {
            val start=1_790_000_000_000L
            val config=RecordingConfig(setOf("GPS_POSITION","SPEED","BIKE_DISTANCE","RIDER_POWER","CADENCE","GPS_ALTITUDE",PrimaryRecording.FLAG))
            val journal=File(root,"ride.ride")
            RideJournal(journal,start,config).use { writer ->
                for(i in 0..60) {
                    val hasGps=i<5 || i>45
                    writer.append(RideSample(start+i*1000,i*1000L,
                        latitude=if(hasGps) 50.0+i*.00001 else null,longitude=if(hasGps) 8.0 else null,
                        values=mapOf("PRIMARY_SPEED" to 10.0,"PRIMARY_DISTANCE" to i*10.0,
                            "PRIMARY_POWER" to 200.0,"PRIMARY_CADENCE" to 80.0,
                            "PRIMARY_HEIGHT" to 500.0+i,"PRIMARY_HEIGHT_MSL" to 1.0)))
                }
            }
            val file=File(root,"ride.fit");FitWriter.encode(journal,file)
            val read=FitRead.read(file)
            assertEquals(600.0,read.details.distanceM!!,.001)
            assertEquals(60000L,read.details.movingMs);assertEquals(2,read.route!!.segments)
            val analysis=RouteAnalysisRead.read(file,ManagedRoute("id","Test",false,"FIT",read.details,true))
            assertEquals(60.0,analysis.metrics.getValue(AnalysisMetric.MOVING),.001)
            assertEquals(36.0,analysis.metrics.getValue(AnalysisMetric.AVG_SPEED),.001)
            for(chart in listOf(AnalysisChart.CADENCE,AnalysisChart.ELEVATION)) {
                val series=analysis.series.single { it.chart==chart }
                assertEquals(61,series.points.count { it.value!=null })
                assertTrue(RouteChartCurve.prepare(series,analysis.distanceAxis,analysis.axisEnd,300f).any { it.size>30 })
            }
        } finally { root.deleteRecursively() }
    }

}
