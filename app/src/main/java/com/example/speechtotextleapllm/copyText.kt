//
//class SpeechToTextActivity : AppCompatActivity() {
//
//    private lateinit var speakButton: Button
//    private lateinit var statusTextView: TextView
//    private lateinit var resultTextView: TextView
//
//    private var speechRecognizer: SpeechRecognizer? = null
//    private var isListening = false
//    private var finalizeByApp = false
//    private var committedText = ""
//
//    private val RECORD_AUDIO_REQUEST_CODE = 101
//
//    // Custom pause detection variables
//    private val PAUSE_TIMEOUT = 4000L // 4 seconds of silence to trigger end of sentence
//    private val handler = Handler(Looper.getMainLooper())
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.speech_main)
//
//        speakButton = findViewById(R.id.speakButton)
//        statusTextView = findViewById(R.id.statusTextView)
//        resultTextView = findViewById(R.id.resultTextView)
//
//        if (checkPermission()) {
//            setupSpeechRecognizer()
//        } else {
//            requestPermission()
//        }
//
//        speakButton.setOnClickListener {
//            if (isListening) {
//                stopListening()
//            } else {
//                startListening()
//            }
//        }
//    }
//
//    private fun setupSpeechRecognizer() {
//        if (SpeechRecognizer.isRecognitionAvailable(this)) {
//            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//            speechRecognizer?.setRecognitionListener(recognitionListener)
//        } else {
//            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
//            speakButton.isEnabled = false
//        }
//    }
//
//    private val recognitionListener = object : RecognitionListener {
//        override fun onReadyForSpeech(params: Bundle?) {
//            statusTextView.text = "Listening..."
//            Log.d("SpeechRecognizer", "onReadyForSpeech")
//        }
//
//        override fun onBeginningOfSpeech() {
//            statusTextView.text = "Speak now..."
//            Log.d("SpeechRecognizer", "onBeginningOfSpeech")
//            resetPauseTimer()
//        }
//
//        override fun onRmsChanged(rmsdB: Float) {
//            // Optional: for visualizing audio level
//        }
//
//        override fun onBufferReceived(buffer: ByteArray?) {}
//
//        override fun onEndOfSpeech() {
//            statusTextView.text = "Processing..."
//            Log.d("SpeechRecognizer", "onEndOfSpeech")
//            handler.removeCallbacks(pauseRunnable) // Stop custom timer when engine ends speech
//            // Note: The isListening state is now managed by our custom timer
//        }
//
//        override fun onError(error: Int) {
//            val errorMessage = getErrorText(error)
//            statusTextView.text = "Error: $errorMessage"
//            Log.e("SpeechRecognizer", "onError: $errorMessage ($error)")
//
//            handler.removeCallbacks(pauseRunnable) // Stop custom timer on error
//
//            if (finalizeByApp) {
//                isListening = false
//                speakButton.text = "Start Listening"
//                statusTextView.text = "Recognition finished."
//            } else {
//                when (error) {
//                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
//                        // Engine stopped due to silence but not by us; continue listening
//                        committedText = resultTextView.text?.toString().orEmpty()
//                        statusTextView.text = "Listening..."
//                        restartListening()
//                    }
//                    else -> {
//                        isListening = false
//                        speakButton.text = "Start Listening"
//                    }
//                }
//            }
//        }
//
//        override fun onResults(results: Bundle?) {
//            handler.removeCallbacks(pauseRunnable) // Stop custom timer when final result is received
//            // Capture what the UI was showing (committed + latest partial) before we alter it
//            val preText = resultTextView.text?.toString().orEmpty()
//
//            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//            if (!matches.isNullOrEmpty()) {
//                val recognizedText = matches[0]
//                committedText = if (committedText.isBlank()) recognizedText else "$committedText $recognizedText"
//                resultTextView.text = committedText
//            }
//            Log.d("SpeechRecognizer", "onResults: $matches")
//
//            if (finalizeByApp) {
//                // True end requested by our 4s timer or user
//                isListening = false
//                speakButton.text = "Start Listening"
//                statusTextView.text = "Recognition finished."
//            } else {
//                // Engine ended early (e.g., brief pause). Auto-restart to continue the session.
//                // If the UI had more complete text from partials, keep that instead of losing it.
//                if (preText.length > committedText.length) {
//                    committedText = preText
//                    resultTextView.text = committedText
//                }
//                statusTextView.text = "Listening..."
//                restartListening()
//            }
//        }
//
//        override fun onPartialResults(partialResults: Bundle?) {
//            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//            if (!matches.isNullOrEmpty()) {
//                val partial = matches[0]
//                val display = if (committedText.isBlank()) partial else "$committedText $partial"
//                resultTextView.text = display
//            }
//            Log.d("TAG", "onPartialResults: custom pause")
//            resetPauseTimer() // Reset the custom timer on every new partial result
//        }
//
//        override fun onEvent(eventType: Int, params: Bundle?) {}
//    }
//
//    // Runnable that triggers when a long pause is detected
//    private val pauseRunnable = Runnable {
//        Log.d("CustomPause", "Long pause detected. Stopping listening.")
//        finalizeByApp = true
//        speechRecognizer?.stopListening()
//    }
//
//    // Resets the custom pause timer
//    private fun resetPauseTimer() {
//        handler.removeCallbacks(pauseRunnable)
//        handler.postDelayed(pauseRunnable, PAUSE_TIMEOUT)
//    }
//
//    private fun createRecognizerIntent(): Intent {
//        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
//            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
//            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//
//            // Make engine much less likely to auto-finish; we will decide via custom 4s timer.
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0L)
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
//            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000L)
//        }
//    }
//
//    private fun restartListening() {
//        val recognizerIntent = createRecognizerIntent()
//        speechRecognizer?.startListening(recognizerIntent)
//        isListening = true
//        // Do not clear existing text or change button label on auto-restart
//        statusTextView.text = "Listening..."
//        resetPauseTimer()
//    }
//
//    private fun startListening() {
//        finalizeByApp = false
//        val recognizerIntent = createRecognizerIntent()
//        speechRecognizer?.startListening(recognizerIntent)
//        isListening = true
//        speakButton.text = "Stop Listening"
//        committedText = ""
//        resultTextView.text = ""
//        statusTextView.text = "Initializing recognition..."
//        resetPauseTimer()
//    }
//
//    private fun stopListening() {
//        finalizeByApp = true
//        speechRecognizer?.stopListening()
//        handler.removeCallbacks(pauseRunnable) // Stop the custom timer
//        isListening = false
//        speakButton.text = "Start Listening"
//        statusTextView.text = "Stopped."
//    }
//
//    private fun getErrorText(errorCode: Int): String {
//        return when (errorCode) {
//            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
//            SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
//            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
//            SpeechRecognizer.ERROR_NETWORK -> "Network error"
//            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
//            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
//            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service is busy"
//            SpeechRecognizer.ERROR_SERVER -> "Server-side error"
//            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
//            else -> "Unknown error"
//        }
//    }
//
//    private fun checkPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun requestPermission() {
//        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
//                setupSpeechRecognizer()
//            } else {
//                Toast.makeText(this, "Permission Denied. The app cannot function without it.", Toast.LENGTH_LONG).show()
//                speakButton.isEnabled = false
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        handler.removeCallbacks(pauseRunnable)
//        speechRecognizer?.destroy()
//    }
//}