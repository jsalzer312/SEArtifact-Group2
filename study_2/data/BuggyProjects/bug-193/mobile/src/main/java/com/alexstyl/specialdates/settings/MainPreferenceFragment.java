package com.alexstyl.specialdates.settings;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import com.alexstyl.specialdates.ErrorTracker;
import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.events.namedays.NamedayLocale;
import com.alexstyl.specialdates.events.namedays.NamedayPreferences;
import com.alexstyl.specialdates.theming.MementoTheme;
import com.alexstyl.specialdates.theming.ThemingPreferences;
import com.alexstyl.specialdates.ui.base.MementoPreferenceFragment;
import com.novoda.notils.caster.Classes;
import com.novoda.notils.exception.DeveloperError;

final public class MainPreferenceFragment extends MementoPreferenceFragment {

    private final String FM_THEME_TAG = "fm_theme";

    private ListPreference namedayLanguageListPreferences;
    private NamedayPreferences namedaysPreferences;
    private ThemingPreferences themingPreferences = new ThemingPreferences();
    private Preference appThemePreference;

    private MainPreferenceActivity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = Classes.from(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_main);

        Preference bankholidaysLanguage = findPreference(R.string.key_bankholidays_language);
        bankholidaysLanguage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OnlyGreekSupportedDialog().show(getFragmentManager(), "OnlyGreek");
                return true;
            }
        });

        appThemePreference = findPreference(R.string.key_app_theme);
        appThemePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ThemeSelectDialog dialog = new ThemeSelectDialog();
                dialog.setOnThemeSelectedListener(themeSelectedListener);
                dialog.show(getFragmentManager(), FM_THEME_TAG);
                return true;
            }
        });
        appThemePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.reapplyTheme();
                return true;
            }
        });
        findPreference(R.string.key_enable_namedays).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (boolean) newValue;
                ErrorTracker.onNamedayLocaleChanged(enabled ? getLocale() : null);
                return true;
            }
        });
        namedaysPreferences = NamedayPreferences.newInstance(getActivity());
        findPreference(R.string.key_namedays_contacts_only).setOnPreferenceChangeListener(onPreferenceChangeListener);
        namedayLanguageListPreferences = findPreference(R.string.key_namedays_language);

        namedayLanguageListPreferences.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String selectedLanguage = (String) newValue;
                        String summary = getNamedaySummary(selectedLanguage);
                        preference.setSummary(summary);
                        namedaysPreferences.setSelectedLanguage(selectedLanguage);
                        return true;
                    }

                    private String getNamedaySummary(String newValue) {
                        int index = 0;
                        for (CharSequence entry : namedayLanguageListPreferences.getEntryValues()) {
                            if (entry.equals(newValue)) {
                                return namedayLanguageListPreferences.getEntries()[index].toString();
                            }
                            index++;
                        }
                        throw new DeveloperError(newValue + " is not a supported Locale");
                    }
                }
        );
        reattachThemeDialogIfNeeded();
    }

    private void reattachThemeDialogIfNeeded() {
        ThemeSelectDialog themeSelectDialog = (ThemeSelectDialog) getFragmentManager().findFragmentByTag(FM_THEME_TAG);
        if (themeSelectDialog != null) {
            themeSelectDialog.setOnThemeSelectedListener(themeSelectedListener);
        }
    }

    private NamedayLocale getLocale() {
        return namedaysPreferences.getSelectedLanguage();
    }

    @Override
    public void onStart() {
        super.onStart();
        namedayLanguageListPreferences.setValue(namedaysPreferences.getSelectedLanguage().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        namedayLanguageListPreferences.setSummary(namedayLanguageListPreferences.getEntry());
        appThemePreference.setSummary(themingPreferences.getSelectedTheme().getThemeName());

    }

    private final ThemeSelectDialog.OnThemeSelectedListener themeSelectedListener = new ThemeSelectDialog.OnThemeSelectedListener() {

        @Override
        public void onThemeSelected(MementoTheme theme) {
            themingPreferences.setSelectedTheme(theme);
            activity.reapplyTheme();
        }
    };

    private final Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            namedaysPreferences.setEnabledForContactsOnly((boolean) newValue);
            return true;
        }
    };
}
