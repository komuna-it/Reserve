package site.komuna.reserve.organization.organizationMember

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity

@Repository
interface OrganizationMemberRepository: JpaRepository<OrganizationMemberEntity, Long> {
    fun findAllByOrganizationIdAndUserId(organizationId: Long, userId: Long): List<OrganizationMemberEntity>
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): OrganizationMemberEntity?
    fun existsByOrganizationIdAndUserId(organizationId: Long, userId: Long): Boolean
    fun findByOrganizationIdAndRole(organizationId: Long, role: OrganizationMemberRole): List<OrganizationMemberEntity>
    fun findByUserIdAndRole(userId: Long, role: OrganizationMemberRole): List<OrganizationMemberEntity>

    fun deleteByOrganizationId(organizationId: Long)

    fun countByOrganizationIdAndRole(organizationId: Long, role: OrganizationMemberRole): Long

    fun findByUserId(userId: Long): List<OrganizationMemberEntity>

}