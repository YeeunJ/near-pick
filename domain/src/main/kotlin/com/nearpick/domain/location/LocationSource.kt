package com.nearpick.domain.location

enum class LocationSource {
    DIRECT,   // lat/lng 직접 전달 (기본값)
    CURRENT,  // ConsumerProfile.currentLat/currentLng
    SAVED,    // SavedLocation[savedLocationId]
}
