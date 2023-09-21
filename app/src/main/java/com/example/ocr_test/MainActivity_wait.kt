package com.example.ocr_test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope


import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.log


class MainActivity_wait : AppCompatActivity() {
    val TAG: String = "OCR_TAG"

    lateinit var targetImage: ImageView
    lateinit var translationResult: TextView
    lateinit var image: Bitmap
    lateinit var result: FirebaseVisionText
//    lateinit var translator : FirebaseTranslateLanguage
//    val translator : FirebaseTranslateLanguage

//    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        targetImage = findViewById(R.id.image_view)
        translationResult = findViewById(R.id.translation_result)

        targetImage.setImageDrawable(getDrawable(R.drawable.vietnam_menu2))
        image = BitmapFactory.decodeResource(resources, R.drawable.vietnam_menu2)

        val firebaseImage = FirebaseVisionImage.fromBitmap(image)

        val detector = FirebaseVision.getInstance().cloudTextRecognizer




        lifecycleScope.launch {
            try {
                // Firebase ML Kit의 processImage 메서드를 호출하고 결과를 얻습니다.
                result = detector.processImage(firebaseImage).await()

                // 성공적으로 결과를 받았을 때
                translationResult.text = result.text

                var language = async { checkLanguage(result.text) }

                // language.await()를 통해 언어가 준비될 때까지 대기
                val detectedLanguage = language.await()

                if (!detectedLanguage.isNullOrEmpty()) {
                    Log.d(TAG, "Detected language: $detectedLanguage")

                    // makeTranslator 함수를 호출하고 언어 정보를 전달
                    val translator = makeTranslator(detectedLanguage)

                    if (translator != null) {
                        translateStart(result.text, translator)
                    }

                } else {
                    Log.e(TAG, "Detected language is null or empty")
                    // 처리할 오류 또는 기본 처리
                }



            } catch (e: Exception) {
                // 오류가 발생했을 때
                translationResult.text = e.message
                Log.e(TAG, "fail", e)
            }

        }


    }

    suspend fun checkLanguage(text: String): String? {

        val languageIdentifier = LanguageIdentification.getClient()

        try {
            val languageCode = languageIdentifier.identifyLanguage(text).await()
            if (languageCode != "und") {
                return languageCode
            } else {
                Log.d(TAG, "checkLanguage: und")
                return null // "und"는 언어를 식별할 수 없는 경우를 나타냅니다.
            }
        } catch (e: Exception) {
            // 예외 처리
            Log.d(TAG, "checkLanguage e: ${e.message}")
            return null
        }
    }

    suspend fun makeTranslator(language: String): Translator? {
        if (language != null) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(language)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()

            val translator = Translation.getClient(options)

            var conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            try {
                // 모델 다운로드를 대기하고 성공 시에만 Translator 객체 반환
                translator.downloadModelIfNeeded(conditions).await()
                Log.d(TAG, "download successful")
                return translator

            } catch (exception: Exception) {
                // 모델 다운로드 실패 시 예외 처리
                Log.e(TAG, "Model download failed: ${exception.message}")
                return null
            }
        } else {
            Log.e(TAG, "Invalid language code")
            // 잘못된 언어 코드 처리
            return null
        }
    }


    suspend fun translateStart(text : String, translator : Translator){

        translator.translate(text)
            .addOnSuccessListener { translatedText ->

                Log.d(TAG, "translateStart success: ${translatedText}")
                runOnUiThread{
                    translationResult.text = translatedText
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "translateStart e: ${exception.message}")
            }
    }


}

