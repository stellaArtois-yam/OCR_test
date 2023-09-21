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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    val TAG: String = "OCR_TAG"

    lateinit var targetImage: ImageView
    lateinit var translationResult: TextView
    lateinit var image: Bitmap
    lateinit var result: FirebaseVisionText
//    lateinit var result: Text


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        targetImage = findViewById(R.id.image_view)
        translationResult = findViewById(R.id.translation_result)

        targetImage.setImageDrawable(getDrawable(R.drawable.vietnam_menu5))
        image = BitmapFactory.decodeResource(resources, R.drawable.vietnam_menu5)

        val firebaseImage = FirebaseVisionImage.fromBitmap(image)

        val detector = FirebaseVision.getInstance().cloudTextRecognizer

//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//        val recognitionImage = InputImage.fromBitmap(image, 0)



        lifecycleScope.launch {
            try {
                // Firebase ML Kit의 processImage 메서드를 호출하고 결과를 얻습니다.
                result = detector.processImage(firebaseImage).await()
//                result = recognizer.process(recognitionImage).await(

                // 성공적으로 결과를 받았을 때
                translationResult.text = result.text

                for(block in result.textBlocks){
                    val blockText = block.text
                    Log.d(TAG, "requestServer blockText: $blockText")

                    for(line in block.lines) {
                        val lineText = line.text
                        Log.d(TAG, "requestServer lineText: $lineText")
                    }
//                        val lineFrame = line.boundingBox
//                        Log.d(TAG, "lineFrame: $lineFrame")
//
//
//                        for(element in line.elements){
//                            val elementText = element.text
//                            Log.d(TAG, "elementText: $elementText")
//
//                            val elementFrame = element.boundingBox
//                            Log.d(TAG, "elementFrame: $elementFrame")
//
////
//                        }
//                    }
                }

//                recognizedText(image)

                var language = async { checkLanguage(result.text) }

                // language.await()를 통해 언어가 준비될 때까지 대기
                val detectedLanguage = language.await()

                if (!detectedLanguage.isNullOrEmpty()) {
                    Log.d(TAG, "Detected language: $detectedLanguage")

                    requestServer(result.text, detectedLanguage)

                    // makeTranslator 함수를 호출하고 언어 정보를 전달
                    val translator = makeTranslator(detectedLanguage)

                    if (translator != null) {
                        translateStart(result.text, translator)
//                        val lowercase = "NEM".lowercase()
//                        Log.d(TAG, "lowercase: $lowercase")
//                        translateStart(lowercase, translator)
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
    
    suspend fun requestServer(resultText : String, lang : String) {

        val gson : Gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://3.36.255.141/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        val service = retrofit.create(RetrofitService::class.java)
        val translationRequest = TranslationRequest(
            text = resultText,
            language = lang)
        Log.d(TAG, "requestServer json: $translationRequest")

        try {
            val response = service.translateText(translationRequest)
            if (response.isSuccessful) {
                val translatedText = response.body()
                Log.d(TAG, "requestServer 성공: $translatedText")

            } else {
                // 서버에서 오류 응답을 받았을 때의 처리
                Log.d(TAG, "requestServer: ${response.body().toString()}")
            }
        } catch (e: Exception) {
            // 네트워크 오류 처리
            Log.d(TAG, "requestServer: ${e.message}")
        }







    }

    suspend fun recognizedText(image : Bitmap){

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val recognitionImage = InputImage.fromBitmap(image, 0)

        val result = recognizer.process(recognitionImage)
            .addOnSuccessListener { visionText ->

                val resultText = result.text
                Log.d(TAG, "recognizedText: $resultText")


                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    Log.d(TAG, "blockText: $blockText")

                    val blockFrame = block.boundingBox
                    Log.d(TAG, "blockFrame: $blockFrame")

                    for (line in block.lines) {

                        val lineText = line.text
                        Log.d(TAG, "lineText: $lineText")

                        val lineFrame = line.boundingBox
                        Log.d(TAG, "lineFrame: $lineFrame")

                        for (element in line.elements) {
                            val elementText = element.text
                            Log.d(TAG, "elementText: $elementText")
                            val elementX = element.boundingBox!!.left
                            Log.d(TAG, "elementX: $elementX")
                            val elementY = element.boundingBox!!.bottom
                            Log.d(TAG, "elementY: $elementY")
                            val elementFrame = element.boundingBox
                            Log.d(TAG, "elementFrame: $elementFrame")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                Log.d(TAG, "recognizedText e: ${e.message}")
            }




    }

    suspend fun checkLanguage(text: String): String? {

        val languageIdentifier = LanguageIdentification.getClient()

        try {
            val languageCode = languageIdentifier.identifyLanguage(text).await()

            if (languageCode != "und") {
                Log.d(TAG, "checkLanguage: $languageCode")
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
        Log.d(TAG, "translateStart text: $text")
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

