package de.bcycle.app

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class BackupWorkTest {
    @Test fun foregroundProtectionSurvivesScreenOffAndEndsOnlyAfterLastOperation() {
        val c=RuntimeEnvironment.getApplication()
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        val first=BackupWork.begin(c);val second=BackupWork.begin(c)
        val service=Robolectric.buildService(BackupWorkService::class.java).create().startCommand(0,1)
        try {
            val wake=ShadowPowerManager.getLatestWakeLock()
            assertTrue(wake.isHeld);assertTrue(BackupWork.active)
            assertNotNull(shadowOf(service.get()).lastForegroundNotification)
            shadowOf(c.getSystemService(Context.POWER_SERVICE) as PowerManager).setIsInteractive(false)
            host.pause().stop()
            assertTrue(wake.isHeld);assertTrue(BackupWork.active)
            first.close();first.close();assertTrue(BackupWork.active);assertTrue(wake.isHeld)
            second.close();assertFalse(BackupWork.active)
            // A new operation can arrive before the queued idle-stop is handled.
            val next=BackupWork.begin(c)
            try {
                shadowOf(android.os.Looper.getMainLooper()).idle()
                assertTrue(BackupWork.active);assertTrue(wake.isHeld)
            } finally { next.close() }
        } finally { first.close();second.close();service.destroy();host.destroy() }
        assertFalse(ShadowPowerManager.getLatestWakeLock().isHeld)
    }
    @Test fun realWorkerContinuesWithHiddenActivityAndAlwaysReleasesItsLease() {
        val c=RuntimeEnvironment.getApplication();val host=Robolectric.buildActivity(Activity::class.java).setup()
        val entered=CountDownLatch(1);val finish=CountDownLatch(1);val done=CountDownLatch(1)
        BackupWork.start(c,"test-backup") {
            try { entered.countDown();check(finish.await(5,TimeUnit.SECONDS)) } finally { done.countDown() }
        }
        try {
            assertTrue(entered.await(5,TimeUnit.SECONDS));host.pause().stop()
            assertTrue(BackupWork.active);finish.countDown();assertTrue(done.await(5,TimeUnit.SECONDS))
            val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
            while(BackupWork.active && System.nanoTime()<end) Thread.yield()
            assertFalse(BackupWork.active)
        } finally { finish.countDown();host.destroy() }
    }
    @Test fun rejectedForegroundStartDoesNotRunUnprotectedOrLeaveAnActiveLease() {
        val c=object:android.content.ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getApplicationContext():Context=this
            override fun startForegroundService(intent:android.content.Intent):android.content.ComponentName?=throw IllegalStateException("rejected")
        }
        var ran=false;var failure:Exception?=null
        BackupWork.start(c,"rejected",failed={ failure=it }) { ran=true }
        assertFalse(ran);assertNotNull(failure);assertFalse(BackupWork.active)
    }
    @Test fun actualMapStateBackupAndRestoreFinishAfterTheActivityStops() {
        val base=RuntimeEnvironment.getApplication()
        val folder=java.nio.file.Files.createTempDirectory(base.cacheDir.toPath(),"backup-screen-off-").toFile()
        val c=object:android.content.ContextWrapper(base) {
            override fun getFilesDir()=java.io.File(folder,"files").apply { mkdirs() }
            override fun getCacheDir()=java.io.File(folder,"cache").apply { mkdirs() }
        }
        val state=MapState(c) {}
        fun await(done:()->Boolean) {
            val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
            while(!done() && System.nanoTime()<until) { shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5) }
            assertTrue(done())
        }
        await { !state.busy }
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        val entered=CountDownLatch(1);val release=CountDownLatch(1)
        val parent=FileBackupDirectory(java.io.File(folder,"backup").apply { mkdirs() })
        val slow=object:BackupDirectory by parent {
            override fun child(name:String,create:Boolean):BackupDirectory {
                val child=parent.child(name,create)
                return object:BackupDirectory by child {
                    override fun write(name:String,source:java.io.File) {
                        entered.countDown();check(release.await(5,TimeUnit.SECONDS));child.write(name,source)
                    }
                }
            }
        }
        try {
            var result:Result<RideBackupResult>?=null
            assertTrue(state.transferBackup(slow,false,true,{}, { result=it }))
            assertTrue(entered.await(5,TimeUnit.SECONDS));host.pause().stop()
            shadowOf(c.getSystemService(PowerManager::class.java)).setIsInteractive(false)
            assertTrue(state.busy);assertTrue(BackupWork.active)
            release.countDown();await { result!=null && !state.busy }
            val exported=result!!.getOrThrow();assertFalse(BackupWork.active)
            result=null
            assertTrue(state.transferBackup(parent.child(exported.folder,false),true,true,{}, { result=it }))
            await { result!=null && !state.busy }
            assertTrue(result!!.isSuccess);assertFalse(BackupWork.active)
        } finally { release.countDown();host.destroy();folder.deleteRecursively() }
    }
    @Test @Config(sdk=[35]) fun systemTimeoutInterruptsWorkerAndReleasesWakeLock() {
        val c=RuntimeEnvironment.getApplication();val lease=BackupWork.begin(c)
        val waiting=CountDownLatch(1);val interrupted=CountDownLatch(1)
        val thread=Thread {
            lease.use {
                try { it.enter();waiting.countDown();CountDownLatch(1).await() }
                catch(_:InterruptedException) { interrupted.countDown() }
            }
        }
        val service=Robolectric.buildService(BackupWorkService::class.java).create().startCommand(0,1)
        try {
            thread.start();assertTrue(waiting.await(5,TimeUnit.SECONDS))
            service.get().onTimeout(1,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            assertTrue(interrupted.await(5,TimeUnit.SECONDS));thread.join(5000);assertFalse(BackupWork.active)
        } finally { lease.close();thread.interrupt();service.destroy() }
        assertFalse(ShadowPowerManager.getLatestWakeLock().isHeld)
    }
}
