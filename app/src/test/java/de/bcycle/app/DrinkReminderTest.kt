package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class DrinkReminderTest {
    private val on=DrinkReminderConfig(true,10)
    @Test fun onlyRecordingTriggersAndEachIntervalShowsAtMostOnce() {
        val timer=DrinkReminder()
        assertFalse(timer.update(false,99999999,on,true))
        assertFalse(timer.update(true,0,on,true))
        assertFalse(timer.update(true,599999,on,true))
        assertTrue(timer.update(true,600000,on,true))
        assertFalse(timer.update(true,600000,on,true))
        assertFalse(timer.update(true,1199999,on,true))
        assertTrue(timer.update(true,1200000,on,true))
        assertFalse(timer.update(false,1200010,on,true))
        assertFalse(timer.update(true,0,on,true))
        assertTrue(timer.update(true,600000,on,true))
    }
    @Test fun hiddenDashboardDefersOneCueAndSkippingIntervalsDoesNotQueueMessages() {
        val timer=DrinkReminder()
        timer.update(true,0,on,false)
        assertFalse(timer.update(true,1900000,on,false))
        assertTrue(timer.update(true,1900001,on,true))
        assertFalse(timer.update(true,1900002,on,true))
        assertFalse(timer.update(true,2399999,on,true))
        assertTrue(timer.update(true,2400000,on,true))
    }
    @Test fun changingIntervalDisablingAndStartingNewRideResetSchedule() {
        val timer=DrinkReminder();timer.update(true,0,on,true)
        assertFalse(timer.update(true,500000,DrinkReminderConfig(true,20),true))
        assertFalse(timer.update(true,1699999,DrinkReminderConfig(true,20),true))
        assertTrue(timer.update(true,1700000,DrinkReminderConfig(true,20),true))
        assertFalse(timer.update(true,1700001,DrinkReminderConfig(false,20),true))
        assertFalse(timer.update(true,1800000,on,true))
        assertFalse(timer.update(true,100,on,true))
        assertTrue(timer.update(true,600100,on,true))
        timer.reset();assertFalse(timer.update(true,600100,on,true))
    }
}
