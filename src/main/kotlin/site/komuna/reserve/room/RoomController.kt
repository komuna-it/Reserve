package site.komuna.reserve.room

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.room.model.RoomDto
import java.net.URI

@RestController
@RequestMapping("/rooms")
class RoomController(
    private val service: RoomService
) {

    @GetMapping("")
    fun getRooms(): ResponseEntity<List<RoomDto>> {
        val response = service.getRooms()
            .map { RoomDto(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getRoom(@PathVariable id: Long): ResponseEntity<RoomDto> {
        val response = RoomDto(service.getRoom(id))

        return ResponseEntity.ok(response)
    }

    @PostMapping("/create/{roomName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun createRoom(@PathVariable roomName: String): ResponseEntity<RoomDto> {
        val response = RoomDto(service.createRoom(roomName))

        val location = URI.create("/rooms/${response.id}")

        return ResponseEntity.created(location).body(response)
    }
}