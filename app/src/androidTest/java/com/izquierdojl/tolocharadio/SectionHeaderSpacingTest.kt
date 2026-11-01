package com.izquierdojl.tolocharadio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Regresión del bug 0038: el `Scaffold` interno de Mis emisoras, Favoritos e
 * Historial reañadía el inset superior (la app usa `enableEdgeToEdge`), así
 * que el `SectionHeader` quedaba más bajo que en Configuración/Explorar.
 *
 * Las pantallas completas requieren Hilt y no se pueden componer aquí; el
 * test fija el contrato estructural con el mismo patrón: `Scaffold` externo
 * con `TopAppBar` (inset determinista) + `Scaffold` interno con
 * `WindowInsets(0, 0, 0, 0)` frente a la referencia sin `Scaffold` interno.
 * En lista y en cuadrícula (el fallo se daba en ambas).
 */
@OptIn(ExperimentalMaterial3Api::class)
class SectionHeaderSpacingTest {
    @get:Rule
    val compose = createComposeRule()

    /** Tolerancia de comparación de posiciones (px). */
    private val tolerance = 0.5f

    private companion object {
        const val REFERENCE_TITLE = "Referencia"
        const val FIXED_TITLE = "Corregido"
    }

    @Test
    fun scaffoldInternoSinInsets_igualaReferenciaEnLista() {
        val (reference, fixed) = tituloY(grid = false)
        assertEquals(reference, fixed, tolerance)
    }

    @Test
    fun scaffoldInternoSinInsets_igualaReferenciaEnCuadricula() {
        val (reference, fixed) = tituloY(grid = true)
        assertEquals(reference, fixed, tolerance)
    }

    @Test
    fun sectionHeader_respetaPaddingVerticalCanonico() {
        compose.setContent {
            TolochaTheme {
                Box(Modifier.fillMaxSize()) {
                    SectionHeader(title = "Canónico")
                }
            }
        }
        val titleY = compose.onNodeWithText("Canónico").fetchSemanticsNode().positionInRoot.y
        val expected = with(compose.density) { 8.dp.toPx() }
        assertEquals(expected, titleY, tolerance)
    }

    @Test
    fun windowInsetsCero_noAportaPaddingSuperior() {
        var top: Dp? = null
        compose.setContent {
            TolochaTheme {
                Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
                    top = padding.calculateTopPadding()
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.runOnIdle { assertEquals(0.dp, top) }
    }

    /**
     * Compone la referencia y el patrón corregido en dos columnas hermanas de
     * idéntico ancho y devuelve la Y de cada título. Así se evita llamar a
     * `setContent` dos veces en el mismo test (la regla solo lo permite una).
     */
    private fun tituloY(grid: Boolean): Pair<Float, Float> {
        compose.setContent {
            TolochaTheme {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Scaffold(topBar = { TopAppBar(title = { Text("Barra referencia") }) }) { outer ->
                            Column(Modifier.fillMaxSize().padding(outer)) {
                                SectionHeader(title = REFERENCE_TITLE)
                                Cuerpo(grid = grid)
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Scaffold(topBar = { TopAppBar(title = { Text("Barra corregida") }) }) { outer ->
                            Column(Modifier.fillMaxSize().padding(outer)) {
                                PatronCorregido(title = FIXED_TITLE, grid = grid)
                            }
                        }
                    }
                }
            }
        }
        val reference = compose.onNodeWithText(REFERENCE_TITLE).fetchSemanticsNode().positionInRoot.y
        val fixed = compose.onNodeWithText(FIXED_TITLE).fetchSemanticsNode().positionInRoot.y
        return reference to fixed
    }

    /** Patrón de las pantallas corregidas: `Scaffold` interno solo para el `Snackbar`. */
    @Composable
    private fun PatronCorregido(
        title: String,
        grid: Boolean,
    ) {
        val snackbar = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { inner ->
            Column(Modifier.fillMaxSize().padding(inner)) {
                SectionHeader(title = title)
                Cuerpo(grid = grid)
            }
        }
    }

    /** Cuerpo mínimo en lista o cuadrícula bajo el encabezado. */
    @Composable
    private fun Cuerpo(grid: Boolean) {
        if (!grid) {
            Column {
                Text("Elemento")
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(2) { Text("Elemento $it") }
            }
        }
    }
}
