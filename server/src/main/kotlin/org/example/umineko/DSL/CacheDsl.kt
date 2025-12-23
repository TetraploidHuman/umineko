package org.example.umineko.DSL

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.application.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

val KtorCachePlugin = createApplicationPlugin("KtorCachePlugin") {
    application.environment.monitor.subscribe(ApplicationStopping) {
        CacheManager.clearAll()
    }
}

@ConsistentCopyVisibility
data class CacheKey private constructor(
    private val parts: List<Any?>
) {
    companion object {
        fun of(vararg parts: Any?): CacheKey = CacheKey(parts.toList())
    }
}

object CacheManager {
    private val caches = ConcurrentHashMap<String, AsyncCache<Any, Any>>()

    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 获取或创建 AsyncCache
     * ttl / maxSize 仅首次创建生效
     */
    @Suppress("UNCHECKED_CAST")
    fun <K : Any, V : Any> getCache(
        name: String,
        ttl: Duration,
        maxSize: Long
    ): AsyncCache<K, V> =
        caches.computeIfAbsent(name) {
            Caffeine.newBuilder()
                .expireAfterWrite(ttl.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .maximumSize(maxSize)
                .buildAsync()
        } as AsyncCache<K, V>

    /**
     * 将 suspend loader 包装成 CompletableFuture
     */
    fun <V : Any> asyncLoad(loader: suspend () -> V): CompletableFuture<V> = loaderScope.future { loader() }

    fun evict(name: String, key: Any) { caches[name]?.synchronous()?.invalidate(key) }

    fun evictAll(name: String) { caches[name]?.synchronous()?.invalidateAll() }

    fun clearAll() {
        caches.values.forEach { it.synchronous().invalidateAll() }
        caches.clear()
        loaderScope.cancel()
    }
}

/**
 * Cache-Aside
 * - 同 key 并发请求只会触发一次 loader
 */
suspend fun <V : Any> cacheable(
    name: String,
    key: Any,
    ttl: Duration = 10.minutes,
    maxSize: Long = 1_000,
    loader: suspend () -> V
): V {
    val cache = CacheManager.getCache<Any, V>(name, ttl, maxSize)

    return cache.get(key) { _, _ -> CacheManager.asyncLoad(loader) }.await()
}

/**
 * 强制刷新缓存
 */
suspend fun <V : Any> cachePut(
    name: String,
    key: Any,
    ttl: Duration = 10.minutes,
    maxSize: Long = 1_000,
    loader: suspend () -> V
): V {
    val cache = CacheManager.getCache<Any, V>(name, ttl, maxSize)
    val value = loader()
    cache.put(key, CompletableFuture.completedFuture(value))
    return value
}

/**
 * 缓存失效
 */
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