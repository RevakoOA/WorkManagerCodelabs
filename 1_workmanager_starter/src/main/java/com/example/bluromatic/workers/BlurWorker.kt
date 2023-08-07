package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
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

        val imageUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)

        // By default CoroutineWorker is running in Dispatchers.DEFAULT.
        return withContext(Dispatchers.IO) {
            try {
                require(imageUri.isNullOrBlank().not()) {
                    val errorMessage = applicationContext.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }

                val resolver = applicationContext.contentResolver
                val bitmap =
                    BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(imageUri)))

                val blurredBitmap = if (blurLevel == 0) {
                    bitmap
                } else {
                    blurBitmap(bitmap, blurLevel)
                }

                val outputUri = writeBitmapToFile(applicationContext, blurredBitmap)

                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
                Result.success(outputData)
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