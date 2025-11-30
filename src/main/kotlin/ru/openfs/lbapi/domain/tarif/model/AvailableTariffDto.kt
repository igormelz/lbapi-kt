package ru.openfs.lbapi.domain.tarif.model

data class AvailableTariffDto(
    val tarId: Long,
    val tarDescr: String,
    val tarRent: Double,
    val tarName: String,
    val rateLevel: Int, // 1, 6, 12
    val shape: Int,
    val discount: Double?,
    val serviceCat: Long?,
    val currentTarId: Long,
    val agentId: Long,
)

internal fun AvailableTariff.toDto(
    tarName: String,
    discount: Double?,
    currentTarId: Long,
    agentId: Long,
): AvailableTariffDto =
    AvailableTariffDto(
        tarId = this.tarId,
        tarDescr = this.tarDescr,
        tarRent = this.tarRent,
        tarName = tarName,
        rateLevel = this.rateLevel,
        shape = this.shape,
        discount = discount,
        serviceCat = this.serviceCat,
        currentTarId = currentTarId,
        agentId = agentId,
    )
