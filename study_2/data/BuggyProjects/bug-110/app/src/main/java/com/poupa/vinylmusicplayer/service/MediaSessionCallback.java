package com.poupa.vinylmusicplayer.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;

import com.poupa.vinylmusicplayer.auto.AutoMediaIDHelper;
import com.poupa.vinylmusicplayer.helper.ShuffleHelper;
import com.poupa.vinylmusicplayer.loader.AlbumLoader;
import com.poupa.vinylmusicplayer.loader.ArtistLoader;
import com.poupa.vinylmusicplayer.loader.PlaylistLoader;
import com.poupa.vinylmusicplayer.loader.SongLoader;
import com.poupa.vinylmusicplayer.loader.TopAndRecentlyPlayedTracksLoader;
import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Playlist;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.provider.MusicPlaybackQueueStore;
import com.poupa.vinylmusicplayer.util.MusicUtil;

import java.util.ArrayList;
import java.util.List;

import static com.poupa.vinylmusicplayer.helper.MusicPlayerRemote.cycleRepeatMode;
import static com.poupa.vinylmusicplayer.helper.MusicPlayerRemote.getCurrentSong;
import static com.poupa.vinylmusicplayer.helper.MusicPlayerRemote.openQueue;
import static com.poupa.vinylmusicplayer.service.MusicService.CYCLE_REPEAT;
import static com.poupa.vinylmusicplayer.service.MusicService.TAG;
import static com.poupa.vinylmusicplayer.service.MusicService.TOGGLE_FAVORITE;
import static com.poupa.vinylmusicplayer.service.MusicService.TOGGLE_SHUFFLE;

public final class MediaSessionCallback extends MediaSessionCompat.Callback {

    private Context context;
    private MusicService musicService;

    MediaSessionCallback(MusicService musicService, Context context) {
        this.context = context;
        this.musicService = musicService;
    }

    @Override
    public void onPlay() {
        musicService.play();
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);

        final String musicId = AutoMediaIDHelper.extractMusicID(mediaId);
        final int itemId = musicId != null ? Integer.valueOf(musicId) : -1;
        final ArrayList<Song> songs = new ArrayList<>();

        final String category = AutoMediaIDHelper.extractCategory(mediaId);
        switch (category) {
            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM:
                Album album = AlbumLoader.getAlbum(context, itemId);
                songs.addAll(album.songs);
                openQueue(songs, 0, true);
                break;

            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST:
                Artist artist = ArtistLoader.getArtist(context, itemId);
                songs.addAll(artist.getSongs());
                openQueue(songs, 0, true);
                break;

            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST:
                Playlist playlist = PlaylistLoader.getPlaylist(context, itemId);
                songs.addAll(playlist.getSongs(context));
                openQueue(songs, 0, true);
                break;

            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY:
            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS:
            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE:
                List<Song> tracks;
                if (category.equals(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY)) {
                    tracks = TopAndRecentlyPlayedTracksLoader.getRecentlyPlayedTracks(context);
                } else if (category.equals(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS)) {
                    tracks = TopAndRecentlyPlayedTracksLoader.getTopTracks(context);
                } else {
                    tracks = MusicPlaybackQueueStore.getInstance(context).getSavedOriginalPlayingQueue();
                }
                songs.addAll(tracks);
                int songIndex = MusicUtil.indexOfSongInList(tracks, itemId);
                if (songIndex == -1) {
                    songIndex = 0;
                }
                openQueue(songs, songIndex, true);
                break;

            case AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SHUFFLE:
                ArrayList<Song> allSongs = SongLoader.getAllSongs(context);
                ShuffleHelper.makeShuffleList(allSongs, -1);
                openQueue(allSongs, 0, true);
                break;

            default:
                break;
        }

        musicService.play();
    }

    @Override
    public void onPause() {
        musicService.pause();
    }

    @Override
    public void onSkipToNext() {
        musicService.playNextSong(true);
    }

    @Override
    public void onSkipToPrevious() {
        musicService.back(true);
    }

    @Override
    public void onStop() {
        musicService.quit();
    }

    @Override
    public void onSeekTo(long pos) {
        musicService.seek((int) pos);
    }

    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        return MediaButtonIntentReceiver.handleIntent(context, mediaButtonEvent);
    }

    @Override
    public void onCustomAction(@NonNull String action, Bundle extras) {
        switch (action) {
            case CYCLE_REPEAT:
                cycleRepeatMode();
                musicService.updateMediaSessionPlaybackState();
                break;

            case TOGGLE_SHUFFLE:
                musicService.toggleShuffle();
                musicService.updateMediaSessionPlaybackState();
                break;

            case TOGGLE_FAVORITE:
                MusicUtil.toggleFavorite(context, getCurrentSong());
                musicService.updateMediaSessionPlaybackState();
                break;

            default:
                Log.d(TAG, "Unsupported action: " + action);
                break;
        }
    }
}
