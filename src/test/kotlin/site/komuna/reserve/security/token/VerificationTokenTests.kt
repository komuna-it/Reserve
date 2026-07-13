package site.komuna.reserve.security.token

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import site.komuna.reserve.security.token.verification.VerificationTokenEntity
import site.komuna.reserve.security.token.verification.VerificationTokenRepository
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.model.UserEntity

class VerificationTokenTests {

    private val repository = mockk<VerificationTokenRepository>()
    private val tokenProperties = mockk<TokenProperties>()

    @Test
    fun generateTokenTest() {
        // Arrange

        every { tokenProperties.validationExpirationMinutes } returns 30
        val user = mockk<UserEntity>()
        val slot = slot<VerificationTokenEntity>()

        every { repository.save(capture(slot)) } answers { slot.captured }

        val service = VerificationTokenService(
            repository,
            tokenProperties
        )

        // Act

        val result = service.generateVerificationTokenEntity(user)

        // Assert

        verify(exactly = 1) {
            repository.save(any())
        }

        val savedToken = slot.captured

        assertEquals(user, savedToken.user)
        assertFalse(savedToken.token.isBlank())
        assertTrue(savedToken.expires.isAfter(savedToken.createdAt))
        assertEquals(savedToken.token, result.token)
    }
}