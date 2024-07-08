/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package org.moire.ultrasonic.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.moire.ultrasonic.BuildConfig;
import org.moire.ultrasonic.api.subsonic.SubsonicAPIClient;
import org.moire.ultrasonic.api.subsonic.SubsonicAPIVersions;
import org.moire.ultrasonic.cache.Directories;
import org.moire.ultrasonic.cache.PermanentFileStorage;
import org.moire.ultrasonic.util.Constants;
import org.moire.ultrasonic.util.Util;

import java.io.File;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class MusicServiceFactory {
    private static final String LOG_TAG = MusicServiceFactory.class.getSimpleName();
    private static MusicService REST_MUSIC_SERVICE = null;
    private static MusicService OFFLINE_MUSIC_SERVICE = null;

    public static MusicService getMusicService(Context context) {
        if (Util.isOffline(context)) {
            Log.d(LOG_TAG, "App is offline, returning offline music service.");
            if (OFFLINE_MUSIC_SERVICE == null) {
                synchronized (MusicServiceFactory.class) {
                    if (OFFLINE_MUSIC_SERVICE == null) {
                        Log.d(LOG_TAG, "Creating new offline music service");
                        OFFLINE_MUSIC_SERVICE = new OfflineMusicService(
                                createSubsonicApiClient(context),
                                getPermanentFileStorage(context));
                    }
                }
            }

            return OFFLINE_MUSIC_SERVICE;
        } else {
            Log.d(LOG_TAG, "Returning rest music service");
            if (REST_MUSIC_SERVICE == null) {
                synchronized (MusicServiceFactory.class) {
                    if (REST_MUSIC_SERVICE == null) {
                        Log.d(LOG_TAG, "Creating new rest music service");
                        REST_MUSIC_SERVICE = new CachedMusicService(new RESTMusicService(
                                createSubsonicApiClient(context),
                                getPermanentFileStorage(context)));
                    }
                }
            }

            return REST_MUSIC_SERVICE;
        }
    }

    /**
     * Resets {@link MusicService} to initial state, so on next call to {@link #getMusicService(Context)}
     * it will return updated instance of it.
     */
    public static void resetMusicService() {
        Log.d(LOG_TAG, "Resetting music service");
        synchronized (MusicServiceFactory.class) {
            REST_MUSIC_SERVICE = null;
            OFFLINE_MUSIC_SERVICE = null;
        }
    }

    private static SubsonicAPIClient createSubsonicApiClient(final Context context) {
        final SharedPreferences preferences = Util.getPreferences(context);
        int instance = preferences.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
        String serverUrl = preferences.getString(Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
        String username = preferences.getString(Constants.PREFERENCES_KEY_USERNAME + instance, null);
        String password = preferences.getString(Constants.PREFERENCES_KEY_PASSWORD + instance, null);
        boolean allowSelfSignedCertificate = preferences
                .getBoolean(Constants.PREFERENCES_KEY_ALLOW_SELF_SIGNED_CERTIFICATE + instance, false);
        boolean enableLdapUserSupport = preferences
                .getBoolean(Constants.PREFERENCES_KEY_LDAP_SUPPORT + instance , false);

        if (serverUrl == null ||
                username == null ||
                password == null) {
            Log.i("MusicServiceFactory", "Server credentials is not available");
            return new SubsonicAPIClient("http://localhost", "", "",
                    SubsonicAPIVersions.fromApiVersion(Constants.REST_PROTOCOL_VERSION),
                    Constants.REST_CLIENT_ID, allowSelfSignedCertificate,
                    enableLdapUserSupport, BuildConfig.DEBUG);
        }

        return new SubsonicAPIClient(serverUrl, username, password,
                SubsonicAPIVersions.fromApiVersion(Constants.REST_PROTOCOL_VERSION),
                Constants.REST_CLIENT_ID, allowSelfSignedCertificate,
                enableLdapUserSupport, BuildConfig.DEBUG);
    }

    private static PermanentFileStorage getPermanentFileStorage(final Context context) {
        final SharedPreferences preferences = Util.getPreferences(context);
        int instance = preferences.getInt(Constants.PREFERENCES_KEY_SERVER_INSTANCE, 1);
        final String serverId = getServerId(preferences, instance);

        return new PermanentFileStorage(getDirectories(context), serverId, BuildConfig.DEBUG);
    }

    public static String getServerId(final SharedPreferences sp, final int instance) {
        String serverUrl = sp.getString(
                Constants.PREFERENCES_KEY_SERVER_URL + instance, null);
        return String.valueOf(Math.abs((serverUrl + instance).hashCode()));
    }

    public static Directories getDirectories(final Context context) {
        return new Directories() {
            @NotNull
            @Override
            public File getInternalCacheDir() {
                return context.getCacheDir();
            }

            @NotNull
            @Override
            public File getInternalDataDir() {
                return context.getFilesDir();
            }

            @Override
            public File getExternalCacheDir() {
                return context.getExternalCacheDir();
            }
        };
    }
}
