package ru.openfs.lbapi.common.exception

import io.smallrye.graphql.api.ErrorCode

@ErrorCode("E3000")
class ApiException(message: String) : RuntimeException(message)

@ErrorCode("E3001")
class NotAuthorizeException(message: String) : RuntimeException(message)

@ErrorCode("E3002")
class NotfoundAccountException(message: String) : RuntimeException(message)

@ErrorCode("E3003")
class PromisePaymentNotAllowedException(message: String) : RuntimeException(message)

@ErrorCode("E3004")
class PromisePaymentOverdueException(message: String) : RuntimeException(message)