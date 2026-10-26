package de.bcycle.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Looper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.net.ConnectException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MaintenanceRepositoryTest {
    private class Device(base:Context):ContextWrapper(base) {
        private val id=UUID.randomUUID().toString()
        override fun getNoBackupFilesDir()=File(baseContext.cacheDir,id).apply { mkdirs() }
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=baseContext.getSharedPreferences("$id-$name",mode)
    }
    private fun tasks()=DreeveMaintenanceTask.decode(JSONObject(javaClass.getResource("/dreeve-maintenance.json")!!.readText()))
    private fun account(key:String="private-synthetic-key")=MaintenanceAccount(ActivityServiceConfig(address="https://dreeve.example/"),key)
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+5_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
        shadowOf(Looper.getMainLooper()).idle();assertTrue("Async maintenance result missing",condition())
    }
    @Test fun freshAutoRequestsCoalesceButManualAndUploadRefreshImmediately() {
        val c=Device(RuntimeEnvironment.getApplication());val count=AtomicInteger();val now=AtomicLong(1_790_000_000_000)
        MaintenanceRepository(c,{account()},{ _,_->count.incrementAndGet();MaintenancePayload(tasks(),mapOf("gear-bike-a" to "Bike")) },now::get).use { repo ->
            repo.refresh();await { repo.state.snapshot!=null && !repo.state.loading }
            repo.refresh();repo.refresh();Thread.sleep(20);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1,count.get())
            repo.refresh(MaintenanceRepository.Reason.MANUAL);await { count.get()==2 && !repo.state.loading }
            now.addAndGet(1)
            repo.refresh(MaintenanceRepository.Reason.UPLOAD);await { repo.state.snapshot?.fetchedAt==now.get() }
            assertEquals(3,count.get())
            val cache=File(c.noBackupFilesDir,"dreeve-maintenance.json").readText()
            assertFalse(cache.contains("private-synthetic-key"));assertFalse(cache.contains("https://dreeve.example/"))
        }
    }
    @Test fun processRestartAndOfflineKeepTheLastSuccessfulDataAndTime() {
        val c=Device(RuntimeEnvironment.getApplication());val now=AtomicLong(1_790_000_000_000)
        val data=tasks()
        MaintenanceRepository(c,{account()},{ _,_->MaintenancePayload(data,mapOf("gear-bike-a" to "E-Bike")) },now::get).use { repo ->
            repo.refresh();await { repo.state.snapshot!=null }
        }
        now.addAndGet(120000)
        MaintenanceRepository(c,{account()},{ _,_->throw ConnectException() },now::get).use { repo ->
            repo.refresh();await { repo.state.problem!=null && !repo.state.loading }
            assertEquals(R.string.service_unreachable,repo.state.problem)
            assertEquals(data,repo.state.snapshot!!.tasks)
            assertEquals(now.get()-120000,repo.state.snapshot!!.fetchedAt)
            assertEquals("E-Bike",repo.state.snapshot!!.gears["gear-bike-a"])
        }
        MaintenanceRepository(c,{account("other-key")},{ _,_->throw ConnectException() },now::get).use { repo ->
            repo.refresh();await { repo.state.problem!=null && !repo.state.loading };assertNull(repo.state.snapshot)
        }
    }
    @Test fun accountSwitchDiscardsLateOldResultAndSchedulesTheNewIdentity() {
        val c=Device(RuntimeEnvironment.getApplication());val who=AtomicReference(account("first-key"))
        val started=CountDownLatch(1);val release=CountDownLatch(1);val seen=mutableListOf<String>()
        MaintenanceRepository(c,who::get,{ owner,_->
            if(owner.key=="first-key") { started.countDown();assertTrue(release.await(3,TimeUnit.SECONDS)) }
            MaintenancePayload(tasks().map { it.copy(label=owner.key) },emptyMap())
        }).use { repo ->
            val subscription=repo.observe { it.snapshot?.tasks?.firstOrNull()?.label?.let(seen::add) }
            repo.refresh();assertTrue(started.await(3,TimeUnit.SECONDS))
            who.set(account("second-key"))
            c.getSharedPreferences("activity-service-dreeve",0).edit().putString("address","https://changed.example/").commit()
            shadowOf(Looper.getMainLooper()).idle();release.countDown()
            await { repo.state.snapshot?.tasks?.firstOrNull()?.label=="second-key" }
            assertFalse("First account must not reach the UI",seen.contains("first-key"));subscription.close()
        }
    }
    @Test fun uploadDuringAnInFlightOldReadCausesOneFollowupAndObserversCanDetach() {
        val c=Device(RuntimeEnvironment.getApplication());val count=AtomicInteger();val started=CountDownLatch(1);val release=CountDownLatch(1)
        MaintenanceRepository(c,{account()},{ _,_->
            if(count.incrementAndGet()==1) { started.countDown();assertTrue(release.await(3,TimeUnit.SECONDS)) }
            MaintenancePayload(tasks().map { it.copy(label=count.get().toString()) },emptyMap())
        }).use { repo ->
            var callbacks=0;val observation=repo.observe { callbacks++ }
            repo.refresh();assertTrue(started.await(3,TimeUnit.SECONDS))
            repeat(3) { repo.refresh(MaintenanceRepository.Reason.UPLOAD) }
            observation.close();val before=callbacks;release.countDown()
            await { repo.state.snapshot?.tasks?.firstOrNull()?.label=="2" && !repo.state.loading }
            assertEquals(2,count.get());assertEquals(before,callbacks)
        }
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun appForegroundRequestsMaintenanceAndQuickReopeningUsesTheSameCache() {
        val c=RuntimeEnvironment.getApplication();val count=AtomicInteger()
        MaintenanceRepository(c,{account()},{ _,_->count.incrementAndGet();MaintenancePayload(emptyList(),emptyMap()) }).use { repo ->
            val field=MaintenanceRepository::class.java.getDeclaredField("instance").apply { isAccessible=true }
            (field.get(null) as? MaintenanceRepository)?.close();field.set(null,repo)
            val runtime=RideRuntime.get(c)
            try {
                await { !runtime.mapState.busy }
                runtime.setVisible(true);await { repo.state.snapshot!=null };assertEquals(1,count.get())
                runtime.setVisible(false);runtime.setVisible(true)
                Thread.sleep(20);shadowOf(Looper.getMainLooper()).idle();assertEquals(1,count.get())
            } finally { runtime.setVisible(false);field.set(null,null) }
        }
    }
    @Test fun unfinishedUploadRefreshSurvivesProcessExitAndBypassesFreshCacheAfterRestart() {
        val c=Device(RuntimeEnvironment.getApplication());val now=AtomicLong(1_790_000_000_000)
        val started=CountDownLatch(1);val blocker=CountDownLatch(1);val count=AtomicInteger()
        MaintenanceRepository(c,{account()},{ _,_->
            if(count.incrementAndGet()>1) { started.countDown();blocker.await(3,TimeUnit.SECONDS) }
            MaintenancePayload(tasks(),emptyMap())
        },now::get).use { repo ->
            repo.refresh();await { repo.state.snapshot!=null }
            repo.refresh(MaintenanceRepository.Reason.UPLOAD);assertTrue(started.await(3,TimeUnit.SECONDS))
            repo.close()
        }
        now.incrementAndGet()
        val fetched=AtomicInteger()
        MaintenanceRepository(c,{account()},{ _,_->fetched.incrementAndGet();MaintenancePayload(tasks(),emptyMap()) },now::get).use { repo ->
            repo.refresh();await { repo.state.snapshot?.fetchedAt==now.get() };assertEquals(1,fetched.get())
        }
    }
    @Test fun missingConfigurationAndCorruptCacheNeverInventData() {
        val c=Device(RuntimeEnvironment.getApplication());File(c.noBackupFilesDir,"dreeve-maintenance.json").writeText("broken")
        MaintenanceRepository(c,{null},{ _,_->fail("Must not fetch");MaintenancePayload(emptyList(),emptyMap()) }).use { repo ->
            repo.refresh();await { repo.state.problem!=null };assertEquals(R.string.dreeve_import_config_missing,repo.state.problem);assertNull(repo.state.snapshot)
        }
        MaintenanceRepository(c,{account()},{ _,_->MaintenancePayload(emptyList(),emptyMap()) }).use { repo ->
            repo.refresh();await { repo.state.snapshot!=null };assertTrue(repo.state.snapshot!!.tasks.isEmpty())
        }
    }
}
