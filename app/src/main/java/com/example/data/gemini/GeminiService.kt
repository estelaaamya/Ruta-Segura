package com.example.data.gemini

import com.example.data.RiskPointEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

sealed interface GeminiResult {
    data object Idle : GeminiResult
    data object Loading : GeminiResult
    data class Success(val data: AnalisisRutaResponse, val isMock: Boolean = false) : GeminiResult
    data class Error(val userFriendlyMessage: String, val canUseMock: Boolean = true) : GeminiResult
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Ejemplo de respuesta de prueba fija para desarrollar y evaluar sin gastar llamadas de API.
     */
    val SAMPLE_MOCK_ANALISIS = AnalisisRutaResponse(
        tramos = listOf(
            TramoRiesgo(
                nombreTramo = "Tramo A: Calle Comercio - Esquina de la Tienda",
                nivelRiesgo = "ALTO",
                reportesAsociados = listOf("Poste sin luz", "Perro suelto"),
                advertencia = "Zona con poca visibilidad a partir de las 19:30. Conviene transitar en grupo."
            ),
            TramoRiesgo(
                nombreTramo = "Tramo B: Avenida Universitaria - Parada de Autobús",
                nivelRiesgo = "MEDIO",
                reportesAsociados = listOf("Zanja en la vereda"),
                advertencia = "Caminar con precaución por obras en el cordón. Calle con buena iluminación."
            ),
            TramoRiesgo(
                nombreTramo = "Tramo C: Pasaje Los Ceibos a Entrada del Instituto",
                nivelRiesgo = "BAJO",
                reportesAsociados = emptyList(),
                advertencia = "Tramo seguro, despejado y con cámaras de seguridad activas."
            )
        ),
        recomendacion = RecomendacionRuta(
            rutaSugerida = "Corredor iluminado por Avenida Universitaria y Pasaje Los Ceibos",
            calificacionSeguridad = "SEGURA",
            justificacion = "Evita la esquina a oscuras de la tienda desviando 100 metros por la avenida con iluminación permanente.",
            horarioRecomendado = "Seguro en cualquier horario; de noche priorizar avenida principal"
        )
    )

    /**
     * Llama a Gemini para agrupar los puntos por tramo y redactar la recomendación estructurada.
     */
    suspend fun analyzeRoutes(
        apiKey: String,
        points: List<RiskPointEntity>
    ): GeminiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                userFriendlyMessage = "No se configuró la llave de API de Gemini en los Secrets de AI Studio.",
                canUseMock = true
            )
        }

        if (points.isEmpty()) {
            return@withContext GeminiResult.Error(
                userFriendlyMessage = "No hay puntos de peligro registrados para que la IA los analice.",
                canUseMock = true
            )
        }

        val pointsSummary = points.joinToString("\n") { p ->
            "- Peligro: ${p.category} | Ubicación: ${p.description} | Horario crítico: ${p.timeOfDay}"
        }

        val systemPrompt = """
            Eres un asistente de seguridad comunitaria para estudiantes universitarios y secundarios camino a su instituto.
            Tu tarea es:
            1. Analizar la lista de reportes de peligro.
            2. Agrupar los reportes por tramo o zona geográfica con su nivel de riesgo ("ALTO", "MEDIO" o "BAJO") y una advertencia concreta.
            3. Determinar la recomendación de ruta más segura con su justificación técnica y horario recomendado.
            
            IMPORTANTE:
            Debes responder OBLIGATORIAMENTE en formato JSON exacto cumpliendo con este esquema:
            {
              "tramos": [
                {
                  "nombreTramo": "string",
                  "nivelRiesgo": "ALTO" | "MEDIO" | "BAJO",
                  "reportesAsociados": ["string"],
                  "advertencia": "string"
                }
              ],
              "recomendacion": {
                "rutaSugerida": "string",
                "calificacionSeguridad": "SEGURA" | "PRECAUCIÓN" | "PELIGROSA",
                "justificacion": "string",
                "horarioRecomendado": "string"
              }
            }
            No agregues texto explicativo fuera del JSON. Devuelve únicamente el JSON válido.
        """.trimIndent()

        val userPrompt = """
            Lista de reportes registrados por los estudiantes:
            $pointsSummary
            
            Agrupa estos reportes en tramos y genera la recomendación de la ruta más segura.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)), role = "user")
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = apiService.generateContent(apiKey = apiKey, request = request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (rawJson.isNullOrBlank()) {
                return@withContext GeminiResult.Error(
                    userFriendlyMessage = "La IA no devolvió ninguna respuesta. Podés reintentar en unos segundos.",
                    canUseMock = true
                )
            }

            // Limpieza de posibles delimitadores markdown ```json ... ```
            val cleanedJson = rawJson
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(AnalisisRutaResponse::class.java)
            val parsedResponse = adapter.fromJson(cleanedJson)

            if (parsedResponse != null && parsedResponse.tramos.isNotEmpty()) {
                GeminiResult.Success(data = parsedResponse, isMock = false)
            } else {
                GeminiResult.Error(
                    userFriendlyMessage = "La respuesta de la IA no cumplió con el formato de datos esperado.",
                    canUseMock = true
                )
            }
        } catch (e: java.net.SocketTimeoutException) {
            GeminiResult.Error(
                userFriendlyMessage = "La IA demoró demasiado en responder (tiempo de espera agotado). Podés reintentar o usar los datos de prueba.",
                canUseMock = true
            )
        } catch (e: java.io.IOException) {
            GeminiResult.Error(
                userFriendlyMessage = "No pudimos conectar con los servidores de Gemini. Comprobá tu conexión a internet.",
                canUseMock = true
            )
        } catch (e: Exception) {
            GeminiResult.Error(
                userFriendlyMessage = "Ocurrió un inconveniente al analizar los tramos con la IA: ${e.localizedMessage ?: "Error inesperado"}",
                canUseMock = true
            )
        }
    }
}
