package com.example.ocr_test


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.log


class Functions : AppCompatActivity() {
    val TAG : String = "OCR_TAG"

    lateinit var targetImage : ImageView
    lateinit var translationResult : TextView
    lateinit var image : Bitmap
    //    lateinit var result : FirebaseVisionText
    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        targetImage = findViewById(R.id.image_view)
        translationResult = findViewById(R.id.translation_result)

        targetImage.setImageDrawable(getDrawable(R.drawable.vietnam_menu2))
        image = BitmapFactory.decodeResource(resources, R.drawable.vietnam_menu2)

//        val firebaseImage = FirebaseVisionImage.fromBitmap(image)
//
//        val detector = FirebaseVision.getInstance().cloudTextRecognizer

        image = scaleBitmapDown(image, 640)

        // Convert bitmap to base64 encoded string
        val byteArrayOutputStream = ByteArrayOutputStream()

        runOnUiThread {
            image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        }


        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)


        functions = Firebase.functions

        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        val image = JsonObject()
        image.add("content", JsonPrimitive(base64encoded))
        request.add("image", image)

        // Add features to the request
        val feature = JsonObject()
        feature.add("type", JsonPrimitive("TEXT_DETECTION"))

        // Alternatively, for DOCUMENT_TEXT_DETECTION:
        feature.add("type", JsonPrimitive("DOCUMENT_TEXT_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)

        val imageContext = JsonObject()
        val languageHints = JsonArray()
        languageHints.add("th")
        languageHints.add("vi")
        imageContext.add("languageHints", languageHints)
        request.add("imageContext", imageContext)

        lifecycleScope.launch {
            annotateImage(request.toString())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        // Task failed with an exception
                        // ...
                        Log.d(TAG, "!task.isSuccessful: ")
                    } else {
                        // 성공적으로 결과를 받았을 때
                        Log.d(TAG, "success up: ")
                        val annotation = task.result!!.asJsonArray[0]
                            .asJsonObject["fullTextAnnotation"]
                            .asJsonObject

                        var something =  String.format("%n%s", annotation["text"].asString)
                        Log.d(TAG, "Complete annotation: " + something)

                        System.out.format("%nComplete annotation:")
                        System.out.format("%n%s", annotation["text"].asString)

                        Log.d(TAG, "success1")
                    }
                }
        }


//        lifecycleScope.launch {
//            try {
//                // Firebase ML Kit의 processImage 메서드를 호출하고 결과를 얻습니다.
//                result = detector.processImage(firebaseImage).await()
//
//                // 성공적으로 결과를 받았을 때
//                translationResult.text = result.text
//
//                Log.d(TAG, "success")
//
//            } catch (e: Exception) {
//                // 오류가 발생했을 때
//                translationResult.text = e.message
//                Log.e(TAG, "fail", e)
//            }
//
//        }




    }


    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                Log.d(TAG, "annotateImage: 1")
                Log.d(TAG, "annotateImage: " + task.isSuccessful)
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }
}

