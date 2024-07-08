package org.moire.ultrasonic.domain

import java.io.Serializable

data class Playlist @JvmOverloads constructor(
        val id: String,
        var name: String,
        val owner: String = "",
        val comment: String = "",
        val songCount: String = "",
        val created: String = "",
        val public: Boolean? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = -4160515427075433798L
    }
}
