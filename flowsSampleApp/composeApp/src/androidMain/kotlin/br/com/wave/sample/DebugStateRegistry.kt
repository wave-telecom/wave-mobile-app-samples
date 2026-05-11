package br.com.wave.sample

import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val DEBUG_STATE_TAG = "WaveSdkSampleTest"

object DebugStateRegistry {
    private val latestSnapshot = AtomicReference<String?>(null)
    private val snapshots = LinkedBlockingQueue<String>()

    fun report(snapshot: String) {
        val previous = latestSnapshot.getAndSet(snapshot)
        if (previous == snapshot) {
            return
        }

        snapshots.offer(snapshot)
        Log.i(DEBUG_STATE_TAG, "snapshot=$snapshot")
    }

    fun awaitNextSnapshot(timeoutMs: Long): String? = snapshots.poll(timeoutMs, TimeUnit.MILLISECONDS)

    fun reset() {
        latestSnapshot.set(null)
        snapshots.clear()
    }
}
