package com.justice.foodmanager

import java.util.*

data class CurrentInfo(
    var currentDateString: String = "",
    var currentClassGrade: String = "all",
    var dateChoosen: Date? = null
)