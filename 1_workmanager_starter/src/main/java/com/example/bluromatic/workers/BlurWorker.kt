package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class BlurWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        makeStatusNotification(
            applicationContext.getString(R.string.blurring_image),
            applicationContext
        )

        // To simulate long operation as blurring is happening very fast.
        delay(DELAY_TIME_MILLIS)

        val blurLevel = 1

        // By default CoroutineWorker is running in Dispatchers.DEFAULT.
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.android_cupcake
                )

                val blurredBitmap = blurBitmap(bitmap, blurLevel)

                val outputUri = writeBitmapToFile(applicationContext, blurredBitmap)

                makeStatusNotification(
                    "Blurred image stored: $outputUri",
                    applicationContext
                )

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, applicationContext.getString(R.string.error_applying_blur), e)

                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "BlurWorker"
    }
}