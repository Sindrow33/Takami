package com.mangareader.core.network

import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttp plumbing for the whole future app (parser + reader).
 *
 * This module intentionally contains no manga-domain logic: no page
 * models, no source interfaces. It only provides the HTTP building blocks
 * that a future auto-parser and the reader's own bulk-download path (used
 * to build local files consumed via [com.mangareader.core.model.PageLoad])
 * both need, so both use one connection pool, one cache, one cookie jar.
 */
object NetworkClients {

    /**
     * Builds a base client with a shared disk HTTP cache and the given
     * [cookieJar]. Callers add their own interceptors (auth, referer
     * spoofing, rate limiting, mirror fallback — all parser concerns) on
     * top via [OkHttpClient.newBuilder].
     */
    fun base(
        cacheDir: File,
        cacheSizeBytes: Long = 50L * 1024 * 1024,
        cookieJar: PersistentCookieJar = PersistentCookieJar(),
        connectTimeoutSec: Long = 15,
        readTimeoutSec: Long = 20,
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(cacheDir, "http_cache"), cacheSizeBytes))
        .cookieJar(cookieJar)
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
