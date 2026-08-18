package site.komuna.reserve.common.httpError.exception

import org.springframework.http.HttpStatus
import site.komuna.reserve.common.httpError.HttpReserveException
import site.komuna.reserve.common.httpError.ReserveErrorType

class ValidationException(body: Body):
    HttpReserveException(
        HttpStatus.BAD_REQUEST,
        ReserveErrorType.VALIDATION_ERROR,
        body
){
    class Body( val errors: Map<String, List<String>> ) {
    }

    constructor(errors: Map<String, List<String>>): this(Body(errors))
}