package com.othman.ocrtranslate

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var translateBtn: Button
    private lateinit var ocrBtn: Button
    private lateinit var pickBtn: Button

    private var detectedText: String = ""
    private var imageBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
        // ضع مفتاح Google Translate API هنا إذا أردت ترجمة فعلية
        private const val GOOGLE_TRANSLATE_API_KEY = "YOUR_API_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.resultText)
        translateBtn = findViewById(R.id.translateBtn)
        ocrBtn = findViewById(R.id.ocrBtn)
        pickBtn = findViewById(R.id.pickBtn)

        pickBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        ocrBtn.setOnClickListener {
            imageBitmap?.let { bitmap ->
                runOCR(bitmap)
            }
        }

        translateBtn.setOnClickListener {
            if (detectedText.isNotEmpty()) {
                translateText(detectedText)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    val uri = data.data
                    val inputStream: InputStream? = uri?.let { contentResolver.openInputStream(it) }
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imageBitmap = bitmap
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun runOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                detectedText = visionText.text
                resultText.text = detectedText
            }
            .addOnFailureListener { e ->
                resultText.text = "لم يتم التعرف على نص!"
            }
    }

    private fun translateText(text: String) {
        resultText.text = "جارٍ الترجمة..."
        thread {
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val urlStr =
                    "https://translation.googleapis.com/language/translate/v2?key=$GOOGLE_TRANSLATE_API_KEY&q=$encodedText&target=ar"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val stream = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(stream)
                    val translated =
                        json.getJSONObject("data").getJSONArray("translations").getJSONObject(0)
                            .getString("translatedText")
                    runOnUiThread {
                        resultText.text = translated
                    }
                } else {
                    runOnUiThread {
                        resultText.text = "فشل الترجمة (تأكد من مفتاح API)"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    resultText.text = "خطأ في الترجمة: ${e.message}"
                }
            }
        }
    }
}