package com.fueru.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One entry in the user's Stage 3/4 charity list — project brief §7.3. `sentiment` is the whole
 * point of having two lists rather than one: "glad" is a charity the user would actually be glad
 * to support (the sting comes from "this miss cost me something real, even something good"), while
 * "resent" is the classic aversive pledge-device charity. First miss this week draws from "glad,"
 * repeat misses the same week escalate to "resent" — see ConsequenceExecutor.
 */
@Entity(tableName = "charity")
data class Charity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    /** "glad" or "resent". */
    val sentiment: String,
)
