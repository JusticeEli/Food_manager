package com.justice.foodmanager.data

import java.util.*

data class CurrentInfo(
    var currentDateString: String = "",
    var currentClassGrade: String = "all",
    var query: String = "",
    var dateChoosen: Date? = null
)