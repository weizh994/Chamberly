package com.company.chamberly

data class Message @JvmOverloads constructor(
    var UID: String ="",
    var message_content: String = "",
    var message_type: String = "",
    var sender_name: String ="",
)