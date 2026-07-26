package site.komuna.reserve.organization.organizationMember

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.organization.organizationMember.model.OrganizationMemberEntity
import site.komuna.reserve.user.Role
import java.util.Optional

@Repository
interface OrganizationMemberRepository: JpaRepository<OrganizationMemberEntity, Long> {
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): OrganizationMemberEntity?
    fun findByUserIdAndRole(userId: Long, role: OrganizationMemberRole): List<OrganizationMemberEntity>
    fun findByOrganizationIdAndRole(organizationId: Long, role: OrganizationMemberRole): List<OrganizationMemberEntity>
    fun existsByOrganizationIdAndUserIdAndRole(organizationId: Long, userId: Long, role: OrganizationMemberRole): Boolean


    fun deleteByOrganizationId(organizationId: Long)
}