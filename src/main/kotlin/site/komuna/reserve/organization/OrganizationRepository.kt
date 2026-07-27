package site.komuna.reserve.organization

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import site.komuna.reserve.organization.model.OrganizationEntity

@Repository
interface OrganizationRepository:
    JpaRepository<OrganizationEntity, Long>,
    JpaSpecificationExecutor<OrganizationEntity> {
}