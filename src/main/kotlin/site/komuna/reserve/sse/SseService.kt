package site.komuna.reserve.sse

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Service
class SseService {

    private val userEmitters = ConcurrentHashMap<Long, MutableSet<SseEmitter>>()
    private val anonymousEmitters = ConcurrentHashMap.newKeySet<SseEmitter>()

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
            removeEmitter(userId, emitter)
        }

        emitter.onTimeout {
            removeEmitter(userId, emitter)
        }

        emitter.onError {
            removeEmitter(userId, emitter)
        }

        return emitter
    }

    private fun removeEmitter(userId: Long?, emitter: SseEmitter) {
        if (userId == null) {
            anonymousEmitters.remove(emitter)
            return
        }

        userEmitters[userId]?.let {
            it.remove(emitter)

            if (it.isEmpty()) {
                userEmitters.remove(userId)
            }
        }
    }

    fun sendToUser(userId: Long, reserveEvent: ReserveEvents, data: Any) {
        userEmitters[userId]?.forEach {
            it.send(
                SseEmitter.event()
                    .name(reserveEvent.name)
                    .data(data)
            )
        }
    }

    fun broadcast(reserveEvent: ReserveEvents, data: Any) {
        anonymousEmitters.forEach {
            it.send(
                SseEmitter.event()
                    .name(reserveEvent.name)
                    .data(data)
            )
        }

        userEmitters.forEach { (userId, emitters) ->
            emitters.toList().forEach { emitter ->
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name(reserveEvent.name)
                            .data(data)
                    )
                } catch (ex: Exception) {
                    removeEmitter(userId, emitter)
                }
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    fun heartbeat() {
        broadcastHeartbeat()
    }

    fun broadcastHeartbeat() {
        anonymousEmitters.toList().forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("heartbeat")
                        .data("")
                )
            } catch (ex: Exception) {
                anonymousEmitters.remove(emitter)
            }
        }

        userEmitters.forEach { (userId, emitters) ->
            emitters.toList().forEach { emitter ->
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name("heartbeat")
                            .data("")
                    )
                } catch (ex: Exception) {
                    removeEmitter(userId, emitter)
                }
            }
        }
    }
}