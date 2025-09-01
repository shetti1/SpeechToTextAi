package com.example.speechtotextleapllm

import ai.liquid.leap.LeapClient
import ai.liquid.leap.ModelRunner
import android.util.Log

object ModelProvider {
    var modelRunner: ModelRunner? = null

    suspend fun getModel(): ModelRunner {
        if (modelRunner == null) {
            modelRunner = LeapClient.loadModel("/data/local/tmp/leap/model.bundle")
            Log.d("PRS", "getModel: Model loaded")
        }

        return modelRunner!!
    }
}