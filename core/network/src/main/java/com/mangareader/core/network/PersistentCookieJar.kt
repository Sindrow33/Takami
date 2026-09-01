package com.mangareader.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal in-memory, thread-safe [CookieJar] shared by every HTTP client in
 * the app. A future auto-parser needs cookies to survive across requests
 * within a session (Cloudflare clearance, login sessions); the reader's
 * bulk page downloader benefits from the same jar so it doesn't have to
 * re-establish a session per page.
 *
 * Disk persistence (surviving process death) is deliberately NOT
 * implemented here — that policy belongs to whichever module knows about
 * login/session semantics (the parser), which can wrap this class or
 * provide its own [CookieJar] to [NetworkClients.base].
 */
class PersistentCookieJar : CookieJar {

    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        val existing = store.getOrPut(key) { mutableListOf() }
        synchronized(existing) {
            val now = System.currentTimeMillis()
            existing.removeAll { it.expiresAt <= now }
            for (new in cookies) {
                existing.removeAll { it.name == new.name && it.path == new.path }
                if (new.expiresAt > now) existing.add(new)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val key = url.host
        val existing = store[key] ?: return emptyList()
        synchronized(existing) {
            val now = System.currentTimeMillis()
            return existing.filter { it.expiresAt > now && it.matches(url) }
        }
    }
}
