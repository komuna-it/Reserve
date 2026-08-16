package site.komuna.reserve.sse

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class SseService {

    private val logger = LoggerFactory.getLogger(SseService::class.java)

    private val userEmitters = ConcurrentHashMap<Long, MutableSet<SseEmitter>>()
    private val anonymousEmitters = ConcurrentHashMap.newKeySet<SseEmitter>()

    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun subscribe(userId: Long?): SseEmitter {
        val emitter = SseEmitter(0L)

        if (userId == null) {
            anonymousEmitters.add(emitter)
        } else {
            userEmitters
                .computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }
                .add(emitter)
        }

        emitter.onCompletion {
            logger.debug("Emitter onCompletion for userId=$userId")
            removeEmitter(userId, emitter)
        }

        emitter.onTimeout {
            logger.debug("Emitter onTimeout for userId=$userId")
            removeEmitter(userId, emitter)
        }

        emitter.onError { ex ->
            logger.debug("Emitter onError for userId=$userId: ${ex?.message}")
            removeEmitter(userId, emitter)
        }

        return emitter
    }

    private fun removeEmitter(userId: Long?, emitter: SseEmitter) {
        try {
            emitter.complete()
        } catch (ignored: Exception) {
            // ignore
        }

        if (userId == null) {
            anonymousEmitters.remove(emitter)
            return
        }

        userEmitters[userId]?.let { set ->
            set.remove(emitter)
            if (set.isEmpty()) {
                userEmitters.remove(userId)
            }
        }
    }

    private fun sendSafely(userId: Long?, emitter: SseEmitter, eventName: String, data: Any) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data))
        } catch (ex: IOException) {
            logger.info("Removing emitter for userId=$userId due to IOException: ${ex.message}")
            removeEmitter(userId, emitter)
        } catch (ex: Exception) {
            logger.warn("Unexpected error sending SSE to userId=$userId: ${ex.message}", ex)
            removeEmitter(userId, emitter)
        }
    }

    fun sendToUser(userId: Long, reserveEvent: ReserveEvents, data: Any) {
        userEmitters[userId]?.toList()?.forEach { emitter ->
            executor.submit { sendSafely(userId, emitter, reserveEvent.name, data) }
        }
    }

    fun broadcast(reserveEvent: ReserveEvents, data: Any) {
        anonymousEmitters.toList().forEach { emitter ->
            executor.submit { sendSafely(null, emitter, reserveEvent.name, data) }
        }

        userEmitters.forEach { (userId, emitters) ->
            emitters.toList().forEach { emitter ->
                executor.submit { sendSafely(userId, emitter, reserveEvent.name, data) }
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    fun heartbeat() {
        broadcastHeartbeat()
    }

    fun broadcastHeartbeat() {
        anonymousEmitters.toList().forEach { emitter ->
            executor.submit { sendSafely(null, emitter, "heartbeat", "ping") }
        }

        userEmitters.forEach { (userId, emitters) ->
            emitters.toList().forEach { emitter ->
                executor.submit { sendSafely(userId, emitter, "heartbeat", "ping") }
            }
        }
    }

    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
