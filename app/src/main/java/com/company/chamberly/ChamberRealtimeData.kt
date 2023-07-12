package com.company.chamberly

data class ChamberRealtimeData(
    val host: String = "", // Chamber creator's UID
    var messages: Map<String, String> = emptyMap(), // Messages sent between users in the chamber
    val timestamp: Long = 0, // Time the chamber was created (in seconds since 1970)
    val title: String = "", // Chamber title
    var users: Map<String, String> = emptyMap() // Map of members with UID as key and displayName as value
)
