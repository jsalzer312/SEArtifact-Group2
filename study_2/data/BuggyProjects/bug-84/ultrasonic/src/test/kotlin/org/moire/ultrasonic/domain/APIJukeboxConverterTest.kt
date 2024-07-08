@file:Suppress("IllegalIdentifier")

package org.moire.ultrasonic.domain

import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test
import org.moire.ultrasonic.api.subsonic.models.JukeboxStatus

/**
 * Unit test for functions in [APIJukeboxConverter.kt] file.
 */
class APIJukeboxConverterTest {
    @Test
    fun `Should convert JukeboxStatus to domain entity`() {
        val entity = JukeboxStatus(45, true, 0.11f, 442)

        val convertedEntity = entity.toDomainEntity()

        with(convertedEntity) {
            currentPlayingIndex `should equal` entity.currentIndex
            gain `should equal` entity.gain
            isPlaying `should equal to` entity.playing
            positionSeconds `should equal` entity.position
        }
    }
}
