package com.izquierdojl.tolocharadio.core.util

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CastPermissionsTest {
    @Test
    fun `api 26 a 30 no requieren permiso runtime para descubrir Cast`() {
        assertTrue(CastPermissions.required(Build.VERSION_CODES.O).isEmpty())
        assertTrue(CastPermissions.required(Build.VERSION_CODES.R).isEmpty())
    }

    @Test
    fun `api 31 y 32 usan ubicacion precisa`() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            CastPermissions.required(Build.VERSION_CODES.S),
        )
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            CastPermissions.required(Build.VERSION_CODES.S_V2),
        )
    }

    @Test
    fun `api 33 a 36 usan nearby wifi devices`() {
        assertEquals(
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES),
            CastPermissions.required(Build.VERSION_CODES.TIRAMISU),
        )
        assertEquals(
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES),
            CastPermissions.required(Build.VERSION_CODES.BAKLAVA),
        )
    }

    @Test
    fun `api 37 anade acceso a red local`() {
        assertEquals(
            listOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ),
            CastPermissions.required(Build.VERSION_CODES.CINNAMON_BUN),
        )
    }
}
