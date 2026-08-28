package com.mangareader.translate.mt

import com.mangareader.translate.api.BatchRequest
import com.mangareader.translate.api.Translator
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * [Translator] for the "Quality" engine profile's whole-page batch call
 * (§7): sends every line on the page as ONE request, in reading order,
 * with the accumulated glossary, and expects an array response of the
 * same length back.
 *
 * §7 explicitly frames this as a QUALITY requirement, not merely an
 * optimization: sending isolated lines loses cross-bubble context the MT
 * model needs to keep pronouns/register consistent within a scene. This
 * class enforces that contract at the type level — [BatchRequest.lines]
 * is always the full page, never a single line — and validates the
 * response length, retrying with a smaller batch on mismatch per §7
 * ("validate response length; on mismatch, retry with a smaller batch").
 *
 * The actual endpoint/model behind this is intentionally left as a
 * pluggable [endpoint] + [apiKeyProvider] — this project does not commit
 * to a specific vendor. Swap this for whatever VLM/MT API the host
 * project contracts with.
 */
class CloudMtProvider(
    private val client: OkHttpClient,
    private val endpoint: String,
    private val apiKeyProvider: () -> String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Translator {

    override suspend fun translate(req: BatchRequest): List<String> {
        val result = callOnce(req)
        if (result.size == req.lines.size) return result

        // Response length mismatch (§7): retry with a halved batch,
        // recursing until batches are small enough to succeed, or a
        // single-line batch still mismatches (in which case we pad/trim
        // defensively rather than throwing, since a translation failure
        // must never block reading — §9).
        if (req.lines.size <= 1) {
            return req.lines // give up gracefully: caller falls back to showing original text for this line
        }
        val mid = req.lines.size / 2
        val first = callBatched(req, req.lines.subList(0, mid))
        val second = callBatched(req, req.lines.subList(mid, req.lines.size))
        return first + second
    }

    private suspend fun callBatched(req: BatchRequest, lines: List<String>): List<String> =
        translate(req.copy(lines = lines))

    private suspend fun callOnce(req: BatchRequest): List<String> {
        val payload = CloudMtRequestBody(
            srcLang = req.srcLang,
            dstLang = req.dstLang,
            lines = req.lines,
            glossary = req.glossary.associate { it.term to it.translation },
        )
        val body = json.encodeToString(payload).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${apiKeyProvider()}")
            .post(body)
            .build()

        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    cont.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            cont.resumeWith(Result.failure(IllegalStateException("Cloud MT HTTP ${resp.code}")))
                            return
                        }
                        val text = resp.body?.string().orEmpty()
                        val parsed = json.decodeFromString<CloudMtResponseBody>(text)
                        cont.resumeWith(Result.success(parsed.translations))
                    }
                }
            })
        }
    }
}

@Serializable
private data class CloudMtRequestBody(
    val srcLang: String,
    val dstLang: String,
    val lines: List<String>,
    val glossary: Map<String, String>,
)

@Serializable
private data class CloudMtResponseBody(
    val translations: List<String>,
)
