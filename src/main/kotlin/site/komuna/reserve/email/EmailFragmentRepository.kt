package site.komuna.reserve.email

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.email.model.EmailFragmentEntity

@Repository
interface EmailFragmentRepository: JpaRepository<EmailFragmentEntity, Long> {
}