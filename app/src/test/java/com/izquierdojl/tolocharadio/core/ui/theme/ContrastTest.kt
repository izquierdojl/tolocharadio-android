package com.izquierdojl.tolocharadio.core.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-011: pares texto/fondo y marca/fondo MUST cumplir WCAG AA en dark/light. */
class ContrastTest {
    private fun luminance(hex: Long): Double {
        fun c(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        val r = ((hex shr 16) and 0xFF).toInt()
        val g = ((hex shr 8) and 0xFF).toInt()
        val b = (hex and 0xFF).toInt()
        return 0.2126 * c(r) + 0.7152 * c(g) + 0.0722 * c(b)
    }

    private fun ratio(
        a: Long,
        b: Long,
    ): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test fun `dark foreground sobre surface cumple AA`() {
        assertTrue(ratio(0xDEEAE2, 0x08100B) >= 4.5)
    }

    @Test fun `dark brand sobre pine-950 cumple AA large`() {
        assertTrue(ratio(0xD3A568, 0x08100B) >= 3.0)
    }

    @Test fun `light foreground sobre surface cumple AA`() {
        assertTrue(ratio(0x123424, 0xEEF3EF) >= 4.5)
    }

    @Test fun `light brand ochre-700 sobre blanco cumple AA`() {
        assertTrue(ratio(0x8A5F26, 0xFFFFFF) >= 4.5)
    }

    @Test fun `light muted sobre surface cumple AA large`() {
        assertTrue(ratio(0x4C6B58, 0xEEF3EF) >= 3.0)
    }

    @Test fun `play activo ochre-500 sobre pine-950 legible`() {
        assertTrue(ratio(0xC0883E, 0x08100B) >= 3.0)
    }

    // Spec 0061: pares usados por tarjetas, chips y barra inferior.

    @Test fun `dark secondary on card cumple AA`() {
        assertTrue(ratio(0x8CB29A, 0x24422E) >= 4.5)
    }

    @Test fun `dark chip text on chip cumple AA`() {
        assertTrue(ratio(0x8CB29A, 0x17291C) >= 4.5)
    }

    @Test fun `dark card surface sobre fondo`() {
        assertTrue(ratio(0x24422E, 0x08100B) >= 1.3)
    }

    @Test fun `dark card border sobre tarjeta`() {
        assertTrue(ratio(0x3C6A4D, 0x24422E) >= 1.5)
    }

    @Test fun `dark player border sobre panel`() {
        assertTrue(ratio(0x3C6A4D, 0x1B3121) >= 1.5)
    }

    @Test fun `dark nav indicator sobre barra`() {
        assertTrue(ratio(0xD3A568, 0x0F1A12) >= 3.0)
    }

    @Test fun `dark nav selected sobre indicador`() {
        assertTrue(ratio(0x08100B, 0xD3A568) >= 4.5)
    }

    @Test fun `light secondary on card cumple AA`() {
        assertTrue(ratio(0x4C6B58, 0xFFFFFF) >= 4.5)
    }

    @Test fun `light chip text on chip cumple AA`() {
        assertTrue(ratio(0x4C6B58, 0xD9E4DC) >= 4.5)
    }

    @Test fun `light card border sobre tarjeta`() {
        assertTrue(ratio(0xC4D4C9, 0xFFFFFF) >= 1.5)
    }

    @Test fun `light nav indicator sobre barra`() {
        assertTrue(ratio(0x8A5F26, 0xFFFFFF) >= 3.0)
    }

    @Test fun `light nav selected sobre indicador`() {
        assertTrue(ratio(0xFFFFFF, 0x8A5F26) >= 4.5)
    }
}
