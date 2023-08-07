package com.example.bluromatic.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.OUTPUT_PATH
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Worker to remove all local blurring files. */
class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        makeStatusNotification(
            applicationContext.getString(R.string.saving_image),
            applicationContext
        )

        // To simulate long operation as operation is happening very fast.
        delay(DELAY_TIME_MILLIS)

        // By default CoroutineWorker is running in Dispatchers.DEFAULT.
        return withContext(Dispatchers.IO) {
            try {
                val filesDir = File(applicationContext.filesDir, OUTPUT_PATH)

                filesDir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()

                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
    }
}
