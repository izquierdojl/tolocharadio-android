package com.izquierdojl.tolocharadio.cast

data class CastDeviceInfo(
    val deviceId: String,
    val name: String,
    val deviceType: CastDeviceType,
    val isConnected: Boolean,
)
