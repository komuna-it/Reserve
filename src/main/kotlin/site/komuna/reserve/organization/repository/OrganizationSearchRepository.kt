package site.komuna.reserve.organization.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import site.komuna.reserve.organization.model.OrganizationEntity
import site.komuna.reserve.organization.model.SearchOrganizationFilter

@Repository
class OrganizationSearchRepository(
    @PersistenceContext
    private val entityManager: EntityManager
) {
    fun search(
        filter: SearchOrganizationFilter,
        pageable: Pageable
    ): Page<OrganizationEntity> {

        val cb = entityManager.criteriaBuilder

        // Main query
        val query = cb.createQuery(OrganizationEntity::class.java)
        val root = query.from(OrganizationEntity::class.java)

        val predicates = mutableListOf<Predicate>()

        filter.organizationId?.let {
            predicates += cb.equal(root.get<Long>("id"), it)
        }

        if (filter.ownerId != null || filter.userId != null) {
            predicates += root.get<Long>("id").`in`(filter.organizationsIds)
        }

        filter.name?.let {
            predicates += cb.equal(root.get<String>("name"), it)
        }

        query.where(*predicates.toTypedArray())

        val typedQuery = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.offset.toInt()
        typedQuery.maxResults = pageable.pageSize

        val content = typedQuery.resultList

        // Count query
        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(OrganizationEntity::class.java)

        val countPredicates = mutableListOf<Predicate>()

        filter.organizationId?.let {
            countPredicates += cb.equal(countRoot.get<Long>("id"), it)
        }

        if (filter.ownerId != null || filter.userId != null) {
            countPredicates += countRoot.get<Long>("id").`in`(filter.organizationsIds)
        }

        filter.name?.let {
            countPredicates += cb.equal(countRoot.get<String>("name"), it)
        }

        countQuery.select(cb.count(countRoot))
        countQuery.where(*countPredicates.toTypedArray())

        val total = entityManager.createQuery(countQuery).singleResult

        return PageImpl(content, pageable, total)
    }
}