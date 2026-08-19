package site.komuna.reserve.policy

import org.springframework.stereotype.Service

@Service
class PolicyService(
    private val privacyPolicyRepository: PrivacyPolicyRepository,
) {

    fun getPrivacyPolicy(): String {
        return privacyPolicyRepository.findFirstByOrderByDateDesc()?.content ?: ""
    }
}