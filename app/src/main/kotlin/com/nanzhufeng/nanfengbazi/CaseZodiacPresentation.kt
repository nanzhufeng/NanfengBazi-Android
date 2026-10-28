package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeWesternZodiacResolver

/** Shared display projection for lightweight catalogs and adopted detail headers. */
internal fun resolveCaseWesternZodiac(
    input: BirthCalendarInput,
    canonicalSolarDateTime: CivilDateTime?,
    adoptedWesternZodiac: String?,
): String? = adoptedWesternZodiac?.trim()?.takeIf { it.isNotEmpty() }
    ?: TymeWesternZodiacResolver.resolve(input, canonicalSolarDateTime)
