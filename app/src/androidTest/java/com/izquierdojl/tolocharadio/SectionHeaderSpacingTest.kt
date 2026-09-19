package com.izquierdojl.tolocharadio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
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

    @Test
    fun scaffoldInternoSinInsets_igualaReferenciaEnLista() {
        val reference = tituloY("Referencia lista", grid = false, fixed = false)
        val fixed = tituloY("Referencia lista", grid = false, fixed = true)
        assertEquals(reference, fixed, tolerance)
    }

    @Test
    fun scaffoldInternoSinInsets_igualaReferenciaEnCuadricula() {
        val reference = tituloY("Referencia parrilla", grid = true, fixed = false)
        val fixed = tituloY("Referencia parrilla", grid = true, fixed = true)
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
     * Compone la referencia (sin `Scaffold` interno, como Configuración) o el
     * patrón corregido (con `Scaffold` interno sin insets) bajo un `Scaffold`
     * externo con `TopAppBar`, y devuelve la Y del título.
     */
    private fun tituloY(
        title: String,
        grid: Boolean,
        fixed: Boolean,
    ): Float {
        compose.setContent {
            TolochaTheme {
                Scaffold(topBar = { TopAppBar(title = { Text("Barra") }) }) { outer ->
                    Column(Modifier.fillMaxSize().padding(outer)) {
                        if (!fixed) {
                            SectionHeader(title = title)
                            Cuerpo(grid = grid)
                        } else {
                            PatronCorregido(title = title, grid = grid)
                        }
                    }
                }
            }
        }
        return compose.onNodeWithText(title).fetchSemanticsNode().positionInRoot.y
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
