package site.komuna.reserve.room

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import site.komuna.reserve.room.model.RoomDto
import java.net.URI

@RestController
@RequestMapping("/rooms")
class RoomController(
    private val service: RoomService
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @GetMapping("")
    fun getRooms(): ResponseEntity<List<RoomDto>> {
        val response = service.getRoomsDto()

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getRoom(@PathVariable id: Long): ResponseEntity<RoomDto> {
        val response = service.getRoomDto(id)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/create/{roomName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun createRoom(@PathVariable roomName: String): ResponseEntity<RoomDto> {
        val response = service.createRoom(roomName)

        val location = URI.create("/rooms/${response.id}")

        return ResponseEntity.created(location).body(response)
    }

    @PostMapping("/{roomId}/setNewPrice/{reservationType}/{price}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun setNewPrice(@PathVariable roomId: Long, @PathVariable reservationType: String, @PathVariable price: Int, authentication: Authentication): RoomDto {
        val changedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to set up a new price for room with id: $roomId" }

        service.setNewPrice(roomId, reservationType, price, changedBy)
        return service.getRoomDto(roomId)
    }

    @PatchMapping("/{roomId}/setRecordable/{value}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun setRecordable(@PathVariable roomId: Long, @PathVariable value: Boolean, authentication: Authentication): RoomDto {
        val changedBy = authentication.name.toLong()

        logger.info { "Received a request from user id ${authentication.name} to set recordable status for room with id: $roomId" }

        service.setRecordable(roomId, value)
        return service.getRoomDto(roomId)
    }
}