package ru.openfs.lbapi.exception

import io.smallrye.graphql.api.ErrorCode

@ErrorCode("E3000")
class ApiException: RuntimeException {
    constructor(message: String) : super(message)
}

@ErrorCode("E3001")
class NotAuthorizeException: RuntimeException {
    constructor(message: String) : super(message)
}