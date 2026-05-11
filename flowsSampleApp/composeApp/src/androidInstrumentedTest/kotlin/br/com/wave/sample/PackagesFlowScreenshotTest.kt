package br.com.wave.sample

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackagesFlowScreenshotTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val targetContext: Context = instrumentation.targetContext
    private val logTag = "WavePackagesTest"

    @Before
    fun setUp() {
        DebugStateRegistry.reset()
        dismissSystemDialogsIfAny()
    }

    @Test
    fun capturePackagesStatesOneByOne() {
        // Dismiss any ANR / system dialogs that appear on emulator boot
        dismissSystemDialogsIfAny()

        waitForText("Entrar", timeoutMs = 60_000)
        tapTextOrNull("Entrar") || error("Could not click login button")

        // SDK onReady can take a while on first launch; wait up to 120s
        awaitAndLogSnapshot("home|home", 120_000)

        tapAnyText("Paquetes", "Packages", "paquetes", "packages")

        captureSnapshotsUntilIdle(
            timeoutMs = 15_000,
            screenshotPrefix = "packages",
        )
    }

    private fun captureSnapshotsUntilIdle(
        timeoutMs: Long,
        screenshotPrefix: String,
    ) {
        var index = 1
        while (true) {
            val snapshot = DebugStateRegistry.awaitNextSnapshot(timeoutMs) ?: break
            if (!snapshot.contains("|packages") && !snapshot.contains("packages|")) {
                Log.i(logTag, "skipping snapshot=$snapshot")
                continue
            }

            val screenshotFile = screenshotFile(
                prefix = screenshotPrefix,
                index = index,
                snapshot = snapshot,
            )
            assertTrue("Screenshot capture failed for $snapshot", device.takeScreenshot(screenshotFile))
            Log.i(logTag, "saved screenshot for snapshot=$snapshot path=${screenshotFile.absolutePath}")
            index++
        }
    }

    private fun dismissSystemDialogsIfAny() {
        // Dismiss ANR dialogs ("Wait" button) that appear on slow emulator boot
        val waitBtn = device.wait(Until.findObject(By.text("Wait")), 3_000)
        if (waitBtn != null) {
            Log.i(logTag, "dismissing ANR dialog")
            waitBtn.click()
            device.waitForIdle()
        }
        // Also try "Close app" -> we prefer "Wait"
        val closeBtn = device.wait(Until.findObject(By.text("Close app")), 1_000)
        if (closeBtn != null) {
            // tap Wait if still present, otherwise ignore
            val wait2 = device.findObject(By.text("Wait"))
            wait2?.click()
        }
    }

    private fun awaitAndLogSnapshot(expectedSnapshot: String, timeoutMs: Long) {
        val snapshot = DebugStateRegistry.awaitNextSnapshot(timeoutMs)
        assertNotNull("Timed out waiting for $expectedSnapshot", snapshot)
        Log.i(logTag, "observed snapshot=$snapshot")
    }

    private fun tapAnyText(vararg candidates: String) {
        for (candidate in candidates) {
            if (tapTextOrNull(candidate)) {
                return
            }
        }
        throw AssertionError("Could not find any of: ${candidates.joinToString()}")
    }

    private fun tapTextOrNull(text: String): Boolean {
        val obj = device.wait(Until.findObject(By.text(text)), 2_000)
            ?: device.wait(Until.findObject(By.textContains(text)), 1_000)
        if (obj != null) {
            obj.click()
            device.waitForIdle()
            instrumentation.waitForIdleSync()
            return true
        }
        return false
    }

    private fun waitForText(text: String, timeoutMs: Long = 30_000) {
        val found = device.wait(Until.hasObject(By.text(text)), timeoutMs) ||
            device.wait(Until.hasObject(By.textContains(text)), 5_000)
        assertTrue("Timed out waiting for text=$text", found)
    }

    private fun screenshotFile(
        prefix: String,
        index: Int,
        snapshot: String,
    ): File {
        val dir = File(targetContext.cacheDir, "packages-flow-screenshots").apply { mkdirs() }
        val sanitized = snapshot
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return File(dir, "%02d-%s-%s.png".format(index, prefix, sanitized))
    }
}
