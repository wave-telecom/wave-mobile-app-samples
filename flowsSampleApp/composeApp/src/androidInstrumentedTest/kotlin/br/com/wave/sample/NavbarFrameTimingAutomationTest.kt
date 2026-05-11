package br.com.wave.sample

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavbarFrameTimingAutomationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val targetContext: Context = instrumentation.targetContext
    private val logTag = "WaveNavbarTimingTest"

    @Before
    fun setUp() {
        DebugStateRegistry.reset()
        dismissSystemDialogsIfAny()
    }

    @Test
    fun auto_click_login_and_capture_navbar_timing() {
        waitForText("Entrar", timeoutMs = 60_000)
        DebugStateRegistry.awaitNextSnapshot(1_000)?.let {
            Log.i(logTag, "drained pre-click snapshot=$it")
        }

        tapText("Entrar")

        val loginTapped = awaitSnapshotContaining("timing login_tapped", 10_000)
        val homeSnapshot = awaitSnapshotContaining("home|home", 60_000)
        val homeFirstFrame = awaitSnapshotContaining("timing home_first_frame", 30_000)
        val navbarFirstLayoutFrame = awaitSnapshotContaining("frames navbar_first_layout", 120_000)
        val navbarReady = awaitSnapshotContaining("timing navbar_ready", 120_000)
        val navbarReadyFrame = awaitSnapshotContaining("frames navbar_ready", 120_000)

        Log.i(logTag, "loginTapped=$loginTapped")
        Log.i(logTag, "homeSnapshot=$homeSnapshot")
        Log.i(logTag, "homeFirstFrame=$homeFirstFrame")
        Log.i(logTag, "navbarFirstLayoutFrame=$navbarFirstLayoutFrame")
        Log.i(logTag, "navbarReady=$navbarReady")
        Log.i(logTag, "navbarReadyFrame=$navbarReadyFrame")
    }

    private fun awaitSnapshotContaining(
        needle: String,
        timeoutMs: Long,
    ): String {
        val snapshot = awaitSnapshotMatching(timeoutMs) { it.contains(needle) }
        Log.i(logTag, "observed snapshot=$snapshot")
        return snapshot
    }

    private fun awaitSnapshotMatching(
        timeoutMs: Long,
        predicate: (String) -> Boolean,
    ): String {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()).coerceAtLeast(1)
            val snapshot = DebugStateRegistry.awaitNextSnapshot(remainingMs)
            if (snapshot != null && predicate(snapshot)) {
                return snapshot
            }
        }
        error("Timed out waiting for matching snapshot within ${timeoutMs}ms")
    }

    private fun dismissSystemDialogsIfAny() {
        val waitBtn = device.wait(Until.findObject(By.text("Wait")), 3_000)
        if (waitBtn != null) {
            Log.i(logTag, "dismissing ANR dialog")
            waitBtn.click()
            device.waitForIdle()
        }
    }

    private fun tapText(text: String) {
        val obj = device.wait(Until.findObject(By.text(text)), 10_000)
            ?: device.wait(Until.findObject(By.textContains(text)), 2_000)
        assertNotNull("Could not find text=$text", obj)
        obj!!.click()
        device.waitForIdle()
        instrumentation.waitForIdleSync()
    }

    private fun waitForText(text: String, timeoutMs: Long = 30_000) {
        val found = device.wait(Until.hasObject(By.text(text)), timeoutMs) ||
            device.wait(Until.hasObject(By.textContains(text)), 5_000)
        assertTrue("Timed out waiting for text=$text", found)
    }
}
