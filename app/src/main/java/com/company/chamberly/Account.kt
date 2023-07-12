package com.company.chamberly

import com.google.firebase.firestore.FieldValue

data class Account(
    val displayName: String = "",
    val email: String = "",
    val uid: String = "",
    val platform: String = "Android",
    val timestamp: Any = FieldValue.serverTimestamp()
)
