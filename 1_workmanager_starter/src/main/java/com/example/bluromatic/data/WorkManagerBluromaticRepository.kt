/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluromatic.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.getImageUri
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

class WorkManagerBluromaticRepository(private val context: Context) : BluromaticRepository {

    private val workManager = WorkManager.getInstance(context)

    override val outputWorkInfo: Flow<WorkInfo?> = MutableStateFlow(null)

    /**
     * Create the WorkRequests to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     *
     * @return url to permanent image or null if something has failed.
     */
    override suspend fun applyBlur(blurLevel: Int): String? {
        val storeImageWorker = OneTimeWorkRequestBuilder<SaveImageToFileWorker>().build()
        workManager
            // Default way to create worker request
            .beginWith(OneTimeWorkRequest.Companion.from(CleanupWorker::class.java))
            .then(let {
                val imageUri = context.getImageUri()
                val blurInputData = createInputDataForWorkRequest(blurLevel, imageUri)

                // Another way to create worker request from extensions library
                OneTimeWorkRequestBuilder<BlurWorker>()
                    .setInputData(blurInputData)
                    .build()
            })
            .then(storeImageWorker)
            .enqueue()

        return GlobalScope.async {
            val workInfo = workManager.getWorkInfoByIdLiveData(storeImageWorker.id).asFlow()
                .filter { workInfo -> workInfo.state.isFinished }.take(1).toList()[0]
            return@async workInfo.outputData.getString(KEY_IMAGE_URI)
        }.await()
    }

    /**
     * Cancel any ongoing WorkRequests
     * */
    override fun cancelWork() {}

    /**
     * Creates the input data bundle which includes the blur level to
     * update the amount of blur to be applied and the Uri to operate on
     * @return Data which contains the Image Uri as a String and blur level as an Integer
     */
    private fun createInputDataForWorkRequest(blurLevel: Int, imageUri: Uri): Data = workDataOf(
        KEY_IMAGE_URI to imageUri.toString(), KEY_BLUR_LEVEL to blurLevel
    )
}
