package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Worker to save image to a permanent file. */
class SaveImageToFileWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override suspend fun doWork(): Result {
        makeStatusNotification(
            applicationContext.getString(R.string.saving_image),
            applicationContext
        )

        // To simulate long operation as operation is happening very fast.
        delay(DELAY_TIME_MILLIS)

        // By default CoroutineWorker is running in Dispatchers.DEFAULT.
        val resolver = applicationContext.contentResolver
        return withContext(Dispatchers.IO) {
            try {
                val localImage = inputData.getString(KEY_IMAGE_URI)
                val imageBitmap = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(localImage))
                )

                val imageUrl = MediaStore.Images.Media.insertImage(
                    resolver, imageBitmap, title, dateFormatter.format(Date())
                )

                if (imageUrl.isNullOrBlank()) {
                    Log.e(TAG, applicationContext.getString(R.string.writing_to_mediaStore_failed))
                    Result.failure()
                } else {
                    val outputData = workDataOf(KEY_IMAGE_URI to imageUrl)
                    Result.success(outputData)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "SaveImageToFileWorker"
    }
}
