package site.komuna.reserve.security.token

import io.mockk.verify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import site.komuna.reserve.security.token.refresh.RefreshTokenEntity
import site.komuna.reserve.security.token.refresh.RefreshTokenRepository
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test


class RefreshTokenTests {

    private val repository = mockk<RefreshTokenRepository>()
    private val service = RefreshTokenService(repository, TokenProperties("forUnitTest"))

    @Test
    fun returnFalseWhenTokenDoesNotExist() {

        // Arrange
        every { repository.findByToken("token") } returns null
        // Act
        val result = service.isTokenValid("token")

        // Assert
        assertFalse(result)
        verify(exactly = 1) { repository.findByToken("token") }
    }

    @Test
    fun returnTrueWhenTokenIsValid() {

        // Arrange
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tokenEntity = RefreshTokenEntity(
            token = "token",
            user = mockk(relaxed = true),
            expires = now.plusMinutes(30),
            createdAt = now
        )

        every { repository.findByToken("token") } returns tokenEntity

        // Act
        val result = service.isTokenValid("token")

        // Assert
        assertTrue(result)
        verify(exactly = 1) { repository.findByToken("token") }
    }

    @Test
    fun returnFalseWhenTokenIsExpired() {
        // Arrange
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tokenEntity = RefreshTokenEntity(
            token = "token",
            user = mockk(relaxed = true),
            expires = now.minusMinutes(1),
            createdAt = now.minusHours(1)
        )

        every { repository.findByToken("token") } returns tokenEntity

        // Act
        val result = service.isTokenValid("token")

        // Assert
        assertFalse(result)
        verify(exactly = 1) { repository.findByToken("token") }
    }
}