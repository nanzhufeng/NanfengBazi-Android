package com.nanzhufeng.nanfengbazi.engine.tyme

import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.tyme.lunar.LunarHour
import com.tyme.solar.SolarDay

/** Calendar-only projection, using the same civil-date constellation as TymeBaziEngine. */
object TymeWesternZodiacResolver {
    fun resolve(input: BirthCalendarInput, canonicalSolarDateTime: CivilDateTime? = null): String? =
        try {
            val solar = canonicalSolarDateTime ?: (input as? BirthCalendarInput.Solar)?.dateTime
            if (solar != null) {
                SolarDay.fromYmd(solar.year, solar.month, solar.day).constellation.name
            } else {
                val lunar = (input as BirthCalendarInput.Lunar).dateTime
                LunarHour.fromYmdHms(
                    lunar.year, if (lunar.isLeapMonth) -lunar.month else lunar.month,
                    lunar.day, lunar.hour, lunar.minute, lunar.second,
                ).solarTime.solarDay.constellation.name
            }
        } catch (_: IllegalArgumentException) {
            null
        }
}
