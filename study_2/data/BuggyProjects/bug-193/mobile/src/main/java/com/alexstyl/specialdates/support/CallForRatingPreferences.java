package com.alexstyl.specialdates.support;

import android.content.Context;

import com.alexstyl.specialdates.EasyPreferences;
import com.alexstyl.specialdates.R;

public class CallForRatingPreferences {

    private EasyPreferences preferences;

    public CallForRatingPreferences(Context context) {
        preferences = EasyPreferences.createForPrivatePreferences(context, R.string.pref_call_to_rate);
    }

    public boolean hasUserRated() {
        return preferences.getBoolean(R.string.key_has_user_rated, false);
    }

    public void setHasUserRated(boolean value) {
        preferences.setBoolean(R.string.key_has_user_rated, value);
    }

    public long lastAskTimeAsked() {
        return preferences.getLong(R.string.key_rate_previous_time_asked, System.currentTimeMillis());
    }

    public void saveToDisplayRating() {
        preferences.setBoolean(R.string.key_rate_triggered, true);
    }

    public boolean triggered() {
        return preferences.getBoolean(R.string.key_rate_triggered, false);
    }

    public void setLastAskTimedNow() {
        preferences.setLong(R.string.key_rate_previous_time_asked, System.currentTimeMillis());
    }
}
