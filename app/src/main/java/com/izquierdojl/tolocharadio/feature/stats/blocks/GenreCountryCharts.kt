package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreListDto

/** Repartos por género y país con la primitiva compartida. */
@Composable
fun GenreCountryCharts(
    genres: StatsGenreListDto,
    countries: StatsCountryListDto,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ShareBars(
            title = "Géneros favoritos",
            entries = genres.items.map { ShareItem(it.genre, it.totalMs) },
        )
        Spacer(Modifier.height(16.dp))
        ShareBars(
            title = "Países",
            entries = countries.items.map { ShareItem(it.countryLabel(), it.totalMs) },
        )
    }
}

private fun StatsCountryEntryDto.countryLabel(): String {
    val code = countryCode?.takeIf { it.isNotBlank() } ?: return country
    return "$country ($code)"
}
