package site.komuna.reserve.policy

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.policy.model.PrivacyPolicyEntity

@Repository
interface PrivacyPolicyRepository: JpaRepository<PrivacyPolicyEntity, Long> {
    fun findFirstByOrderByDateDesc(): PrivacyPolicyEntity?
}