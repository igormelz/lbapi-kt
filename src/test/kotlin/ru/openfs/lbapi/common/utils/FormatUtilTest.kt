package ru.openfs.lbapi.common.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FormatUtilTest {
    @Test
    fun nextPaymentDate() {
        val result = FormatUtil.nextPaymentDate(LocalDate.of(2024, 1, 28), 6, LocalDate.of(2025, 1, 1))
        assert(result.isEqual(LocalDate.of(2025,1,28)))
    }

}