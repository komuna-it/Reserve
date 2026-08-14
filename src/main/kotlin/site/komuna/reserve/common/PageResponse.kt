package site.komuna.reserve.common

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val pageable: PageableResponse,
    val last: Boolean,
    val totalElements: Long,
    val totalPages: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val sort: SortResponse,
    val size: Int,
    val number: Int,
    val empty: Boolean,
)

data class PageableResponse(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: SortResponse,
    val offset: Long,
    val paged: Boolean,
    val unpaged: Boolean,
)

data class SortResponse(
    val sorted: Boolean,
    val unsorted: Boolean,
    val empty: Boolean,
)

fun <T> Page<T>.toPageResponse() = PageResponse(
    content = content,
    pageable = PageableResponse(
        pageNumber = pageable.pageNumber,
        pageSize = pageable.pageSize,
        sort = SortResponse(
            sorted = sort.isSorted,
            unsorted = sort.isUnsorted,
            empty = sort.isEmpty,
        ),
        offset = pageable.offset,
        paged = pageable.isPaged,
        unpaged = pageable.isUnpaged,
    ),
    last = isLast,
    totalElements = totalElements,
    totalPages = totalPages,
    numberOfElements = numberOfElements,
    first = isFirst,
    sort = SortResponse(
        sorted = sort.isSorted,
        unsorted = sort.isUnsorted,
        empty = sort.isEmpty,
    ),
    size = size,
    number = number,
    empty = isEmpty,
)