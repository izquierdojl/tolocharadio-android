package com.izquierdojl.tolocharadio.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permisos runtime necesarios para descubrir dispositivos Google Cast.
 *
 * El descubrimiento Cast usa mDNS (`_googlecast._tcp`), una operación de red
 * local, por lo que está sujeto a los permisos de descubrimiento de cada
 * versión de Android:
 * - API 31-32 (Android 12): `ACCESS_FINE_LOCATION`.
 * - API 33-36 (Android 13-16): `NEARBY_WIFI_DEVICES`.
 * - API 37+ (Android 17): `ACCESS_LOCAL_NETWORK`, que bloquea el acceso a la
 *   red local por defecto para apps que targetean API 37. Sin él el selector
 *   de Cast no encuentra ningún dispositivo.
 */
object CastPermissions {
    fun required(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        buildList {
            when {
                sdkInt >= Build.VERSION_CODES.TIRAMISU -> add(Manifest.permission.NEARBY_WIFI_DEVICES)
                sdkInt >= Build.VERSION_CODES.S -> add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (sdkInt >= Build.VERSION_CODES.CINNAMON_BUN) {
                add(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        }

    fun areGranted(context: Context): Boolean =
        required().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
}
