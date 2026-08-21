package site.komuna.reserve.policy

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/policy")
class PolicyController(
    private val service: PolicyService
) {

    @GetMapping("/privacy")
    fun getPrivacyPolicy(): ResponseEntity<String> {
        val response = service.getPrivacyPolicy()
        return ResponseEntity.ok(response)
    }
}