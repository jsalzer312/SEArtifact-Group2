package com.poupa.vinylmusicplayer.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import androidx.annotation.NonNull;

import com.poupa.vinylmusicplayer.model.PlaylistSong;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.provider.Discography;

import java.util.ArrayList;

public class PlaylistSongLoader {

    @NonNull
    public static ArrayList<Song> getPlaylistSongList(@NonNull final Context context, final long playlistId) {
        ArrayList<Song> songs = new ArrayList<>();
        Cursor cursor = makePlaylistSongCursor(context, playlistId);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getPlaylistSongFromCursorImpl(cursor, playlistId));
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return songs;
    }

    @NonNull
    private static PlaylistSong getPlaylistSongFromCursorImpl(@NonNull Cursor cursor, long playlistId) {
        final long id = cursor.getLong(0);
        final String data = cursor.getString(5);
        final int dateAdded = cursor.getInt(6);
        final int dateModified = cursor.getInt(7);
        final int idInPlaylist = cursor.getInt(12);

        // search in the discog cache first
        Song song = Discography.getInstance().getSong(id);
        if (song != null) {
            if (song.data.equals(data) && song.dateAdded == dateAdded && song.dateModified == dateModified) {
                PlaylistSong playlistSong = new PlaylistSong(
                        id,
                        song.title,
                        song.trackNumber,
                        song.year,
                        song.duration,
                        data,
                        dateAdded,
                        dateModified,
                        song.albumId,
                        song.albumName,
                        song.artistId,
                        song.artistName,
                        playlistId,
                        idInPlaylist);
                return playlistSong;
            }
        }

        // either none in cache, or obsolete
        final String title = cursor.getString(1);
        final int trackNumber = cursor.getInt(2);
        final int year = cursor.getInt(3);
        final long duration = cursor.getLong(4);
        final long albumId = cursor.getLong(8);
        final String albumName = cursor.getString(9);
        final long artistId = cursor.getLong(10);
        final String artistName = cursor.getString(11);

        PlaylistSong playlistSong = new PlaylistSong(
                id,
                title,
                trackNumber,
                year,
                duration,
                data,
                dateAdded,
                dateModified,
                albumId,
                albumName,
                artistId,
                artistName,
                playlistId,
                idInPlaylist);
        Discography.getInstance().addSong(playlistSong);

        return playlistSong;
    }

    public static Cursor makePlaylistSongCursor(@NonNull final Context context, final long playlistId) {
        try {
            return context.getContentResolver().query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                    new String[]{
                            MediaStore.Audio.Playlists.Members.AUDIO_ID,// 0
                            AudioColumns.TITLE,// 1
                            AudioColumns.TRACK,// 2
                            AudioColumns.YEAR,// 3
                            AudioColumns.DURATION,// 4
                            AudioColumns.DATA,// 5
                            AudioColumns.DATE_ADDED,// 6
                            AudioColumns.DATE_MODIFIED,// 7
                            AudioColumns.ALBUM_ID,// 8
                            AudioColumns.ALBUM,// 9
                            AudioColumns.ARTIST_ID,// 10
                            AudioColumns.ARTIST,// 11
                            MediaStore.Audio.Playlists.Members._ID // 12
                    }, SongLoader.BASE_SELECTION, null,
                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
        } catch (SecurityException e) {
            return null;
        }
    }
}
