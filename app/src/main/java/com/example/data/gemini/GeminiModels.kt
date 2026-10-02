package com.example.data.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Modelos para la respuesta estructurada de Gemini (Structured Output).
 * Representa los tramos agrupados y la recomendación de ruta segura.
 */
@JsonClass(generateAdapter = true)
data class TramoRiesgo(
    @field:Json(name = "nombreTramo") val nombreTramo: String = "",
    @field:Json(name = "nivelRiesgo") val nivelRiesgo: String = "MEDIO", // "ALTO", "MEDIO", "BAJO"
    @field:Json(name = "reportesAsociados") val reportesAsociados: List<String> = emptyList(),
    @field:Json(name = "advertencia") val advertencia: String = ""
)

@JsonClass(generateAdapter = true)
data class RecomendacionRuta(
    @field:Json(name = "rutaSugerida") val rutaSugerida: String = "",
    @field:Json(name = "calificacionSeguridad") val calificacionSeguridad: String = "PRECAUCIÓN", // "SEGURA", "PRECAUCIÓN", "PELIGROSA"
    @field:Json(name = "justificacion") val justificacion: String = "",
    @field:Json(name = "horarioRecomendado") val horarioRecomendado: String = ""
)

@JsonClass(generateAdapter = true)
data class AnalisisRutaResponse(
    @field:Json(name = "tramos") val tramos: List<TramoRiesgo> = emptyList(),
    @field:Json(name = "recomendacion") val recomendacion: RecomendacionRuta = RecomendacionRuta()
)

// --- Modelos de llamada a la REST API de Gemini ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @field:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @field:Json(name = "parts") val parts: List<GeminiPart>,
    @field:Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @field:Json(name = "contents") val contents: List<GeminiContent>,
    @field:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @field:Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @field:Json(name = "temperature") val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @field:Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @field:Json(name = "candidates") val candidates: List<GeminiCandidate>?
)
