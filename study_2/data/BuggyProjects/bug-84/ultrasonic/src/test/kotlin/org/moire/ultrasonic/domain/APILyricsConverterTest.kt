@file:Suppress("IllegalIdentifier")

package org.moire.ultrasonic.domain

import org.amshove.kluent.`should equal`
import org.junit.Test
import org.moire.ultrasonic.api.subsonic.models.Lyrics

/**
 * Unit test for extension functions in [APILyricsConverter.kt] file.
 */
class APILyricsConverterTest {
    @Test
    fun `Should convert Lyrics entity to domain`() {
        val entity = Lyrics(artist = "some-artist", title = "some-title", text = "song-text")

        val convertedEntity = entity.toDomainEntity()

        with(convertedEntity) {
            artist `should equal` entity.artist
            title `should equal` entity.title
            text `should equal` entity.text
        }
    }
}
