package site.komuna.reserve.organization.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import site.komuna.reserve.organization.model.OrganizationEntity

@Repository
interface OrganizationRepository: JpaRepository<OrganizationEntity, Long> {
}