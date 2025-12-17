package org.example.umineko
// CachePlugin.kt
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


val KtorCachePlugin = createApplicationPlugin("KtorCachePlugin") {

    application.environment.monitor.subscribe(ApplicationStopping) {
        CacheManager.clearAll()
    }
}

// CacheKey.kt
data class CacheKey(
    private val parts: List<Any?>
) {
    companion object {
        fun of(vararg parts: Any?): CacheKey = CacheKey(parts.toList())
    }
}


object CacheManager {

    private val caches = ConcurrentHashMap<String, Cache<Any, Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <K : Any, V : Any> getCache(
        name: String,
        ttl: Duration,
        maxSize: Long
    ): Cache<K, V> =
        caches.computeIfAbsent(name) {
            Caffeine.newBuilder()
                .expireAfterWrite(ttl.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .maximumSize(maxSize)
                .build<Any, Any>()
        } as Cache<K, V>

    fun evict(name: String, key: Any) {
        caches[name]?.invalidate(key)
    }

    fun evictAll(name: String) {
        caches[name]?.invalidateAll()
    }

    fun clearAll() {
        caches.values.forEach { it.invalidateAll() }
    }
}


private val locks = ConcurrentHashMap<Any, Mutex>()

suspend fun <V : Any> cacheable(
    name: String,
    key: Any,
    ttl: Duration = 10.minutes,
    maxSize: Long = 1_000,
    loader: suspend () -> V
): V {
    val cache = CacheManager.getCache<Any, V>(name, ttl, maxSize)

    cache.getIfPresent(key)?.let { return it }

    val mutex = locks.computeIfAbsent("$name:$key") { Mutex() }
    return mutex.withLock {
        cache.getIfPresent(key)?.let { return it }

        loader().also {
            cache.put(key, it)
        }
    }
}

suspend fun <V : Any> cachePut(
    name: String,
    key: Any,
    ttl: Duration = 10.minutes,
    maxSize: Long = 1_000,
    loader: suspend () -> V
): V {
    val cache = CacheManager.getCache<Any, V>(name, ttl, maxSize)
    return loader().also {
        cache.put(key, it)
    }
}

fun cacheEvict(
    name: String,
    key: Any? = null
) {
    if (key == null) {
        CacheManager.evictAll(name)
    } else {
        CacheManager.evict(name, key)
    }
}