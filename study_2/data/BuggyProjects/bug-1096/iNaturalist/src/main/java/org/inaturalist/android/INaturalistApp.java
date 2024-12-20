package org.inaturalist.android;

import com.crashlytics.android.Crashlytics;
import com.evernote.android.state.StateSaver;
import com.facebook.FacebookSdk;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.livefront.bridge.Bridge;
import com.livefront.bridge.SavedStateHandler;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import android.Manifest;

import io.fabric.sdk.android.Fabric;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.inaturalist.android.INaturalistService.LoginType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.content.res.ResourcesCompat;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class INaturalistApp extends MultiDexApplication {
    private final static String TAG = "INAT: Application";

    private static final int PERMISSIONS_REQUEST = 0x1234;

    private static final int DEFAULT_DEBUG_LOG_DAY_COUNT = 3;

    private SharedPreferences mPrefs;
    private NotificationManager mNotificationManager;
	private boolean mIsSyncing = false;
    public static Integer VERSION = 1;
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a z", Locale.ENGLISH);
    public static SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");
    public static SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("hh:mm a z");
    private static Integer SYNC_NOTIFICATION = 3;
    private static Context context;
    private Locale locale = null;
    private Locale deviceLocale = null;
    private OnDownloadFileProgress mDownloadCallback;
    private boolean mOnboardingShownBefore = false;

    public static final int NO_OBSERVATION = -1;
    private int mObservationIdBeingSynced = NO_OBSERVATION;
    private boolean mCancelSync = false;
    private GoogleApiClient mGoogleApiClient;
    private GlobalExceptionHandler mFileLoggingTree;

    // The ID of the observation being currently synced

    public int getObservationIdBeingSynced() {
        return mObservationIdBeingSynced;
    }

    public void setObservationIdBeingSynced(int value) {
        mObservationIdBeingSynced = value;
    }


    public boolean shownOnboarding() {
        return mOnboardingShownBefore;
    }

    public void setShownOnboarding(boolean value) {
        mOnboardingShownBefore = true;
    }

    public interface INotificationCallback {
    	public void onNotification(String title, String content);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    private void setDefaultFont(String staticTypefaceFieldName, int fontAsset) {
        Typeface regular = ResourcesCompat.getFont(this, fontAsset);
        replaceFont(staticTypefaceFieldName, regular);
    }

    private void replaceFont(String staticTypefaceFieldName, final Typeface newTypeface) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, Typeface> newMap = new HashMap<String, Typeface>();
            newMap.put("sans-serif", newTypeface);
            try {
                final Field staticField = Typeface.class.getDeclaredField("sSystemFontMap");
                staticField.setAccessible(true);
                staticField.set(null, newMap);
            } catch (NoSuchFieldException e) {
                Logger.tag(TAG).error(e);
            } catch (IllegalAccessException e) {
                Logger.tag(TAG).error(e);
            }
        } else {
            try {
                final Field staticField = Typeface.class.getDeclaredField(staticTypefaceFieldName);
                staticField.setAccessible(true);
                staticField.set(null, newTypeface);
            } catch (NoSuchFieldException e) {
                Logger.tag(TAG).error(e);
            } catch (IllegalAccessException e) {
                Logger.tag(TAG).error(e);
            }
        }
    }

    // How many days to save log files for
    public int getDebugLogDayCount() {
        return getPrefs().getInt("debug_log_day_count", DEFAULT_DEBUG_LOG_DAY_COUNT);
    }

    // How many days to save log files for
    public void setDebugLogDayCount(int count) {
        getPrefs().edit().putInt("debug_log_day_count", count).commit();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());
        FacebookSdk.sdkInitialize(getApplicationContext());
        AnalyticsClient.initAnalyticsClient(this);

        AnalyticsClient.getInstance().logEvent(AnalyticsClient.EVENT_NAME_APP_LAUNCH);

        Bridge.initialize(getApplicationContext(), new SavedStateHandler() {
            @Override
            public void saveInstanceState(@NonNull Object target, @NonNull Bundle state) {
                StateSaver.saveInstanceState(target, state);
            }

            @Override
            public void restoreInstanceState(@NonNull Object target, @Nullable Bundle state) {
                StateSaver.restoreInstanceState(target, state);
            }
        });

        // Initialize the logger
        LoggingUtils.initializeLogger(this);
        // Clear out old log files
        LoggingUtils.clearOldLogs(this, getDebugLogDayCount());
        // Compress any uncompressed log files
        LoggingUtils.compressDebugLogs(this);

        Logger.tag(TAG).debug("onCreate");

        SHORT_TIME_FORMAT = new SimpleDateFormat(DateFormat.is24HourFormat(getApplicationContext()) ? "HH:mm z" : "hh:mm a z");

        // Build a custom Picasso instance that uses more memory for image cache (50% of free memory
        // instead of the default 15%)
        Picasso picasso = new Picasso.Builder(getApplicationContext())
                .memoryCache(new LruCache((int)(Runtime.getRuntime().maxMemory() * 0.5)))
                .build();
        Picasso.setSingletonInstance(picasso);
        //Picasso.with(getApplicationContext())
        //        .setIndicatorsEnabled(true);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        INaturalistApp.context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deviceLocale = getResources().getConfiguration().getLocales().get(0);
        } else {
            deviceLocale = getResources().getConfiguration().locale;
        }
        applyLocaleSettings();

        // Create the root offline guides directory, if needed
        GuideXML.createOfflineGuidesDirectory(this);

        SharedPreferences pref = getSharedPreferences("iNaturalistPreferences", MODE_PRIVATE);
        String username = pref.getString("username", null);
        if (username != null) {
            setShownOnboarding(true);
        }
    }
    
    
    /* Used for accessing iNat service results - since passing large amounts of intent data
     * is impossible (for example, returning a huge list of projects/guides won't work via intents)
     */
    private Map<String, Serializable> mServiceResults = new HashMap<String, Serializable>();
	private INotificationCallback mNotificationCallback;

    public void setServiceResult(String key, Serializable value) {
    	mServiceResults.put(key,  value);
    }
    
    public Serializable getServiceResult(String key) {
    	return mServiceResults.get(key);
    }
   

 	/**
	 * Get ISO 3166-1 alpha-2 country code for this device (or null if not available)
	 * @param context Context reference to get the TelephonyManager instance from
	 * @return country code or null
	 */
	public static String getUserCountry(Context context) {
 		ActivityHelper helper;
        helper = new ActivityHelper(context);
		try {
			final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			final String simCountry = tm.getSimCountryIso();
			if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
				return simCountry.toLowerCase(Locale.US);
			} else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
				String networkCountry = tm.getNetworkCountryIso();
				if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
					return networkCountry.toLowerCase(Locale.US);
				}
			}
		}
		catch (Exception e) { }

		return null;
	}


    public boolean getSuggestSpecies() {
        SharedPreferences settings = getPrefs();
        return settings.getBoolean("pref_suggest_species", true);
    }

    public void setSuggestSpecies(boolean value) {
        SharedPreferences settings = getPrefs();
        Editor settingsEditor = settings.edit();

        settingsEditor.putBoolean("pref_suggest_species", value);
        settingsEditor.apply();
    }

    public boolean getShowScientificNameFirst() {
    	SharedPreferences settings = getPrefs();
        return settings.getBoolean("prefers_scientific_name_first", false);
	}

	public void setShowScientificNameFirst(boolean value) {
    	SharedPreferences settings = getPrefs();
    	Editor settingsEditor = settings.edit();

    	settingsEditor.putBoolean("prefers_scientific_name_first", value);
    	settingsEditor.apply();
	}

    public void setUserRoles(Set<String> roles) {
    	SharedPreferences settings = getPrefs();
    	Editor settingsEditor = settings.edit();

    	settingsEditor.putStringSet("user_roles", roles);
    	settingsEditor.apply();
	}

    public Set<String> getUserRoles() {
    	SharedPreferences settings = getPrefs();
        return settings.getStringSet("user_roles", new HashSet<String>());
	}

	public boolean getAutoSync() {
    	SharedPreferences settings = getPrefs();
        return settings.getBoolean("pref_auto_sync", true);
	}

    /** Whether or not auto sync settings has been set */
    public boolean hasAutoSync() {
    	SharedPreferences settings = getPrefs();
        return settings.contains("pref_auto_sync");
	}

	public void setAutoSync(boolean value) {
    	SharedPreferences settings = getPrefs();
    	Editor settingsEditor = settings.edit();

    	settingsEditor.putBoolean("pref_auto_sync", value);
    	settingsEditor.apply();
	}

	public void setOnBoardingTextsShown() {
    	SharedPreferences settings = getPrefs();
    	Editor settingsEditor = settings.edit();

    	settingsEditor.putBoolean("onboarded_syncing", true);
        settingsEditor.putBoolean("onboarded_species_guess", true);
    	settingsEditor.apply();
	}

    public void setErrorsForObservation(int obsId, int projectId, JSONArray errors) {
        SharedPreferences settings = getPrefs();
        String errorsJson = settings.getString("pref_observation_errors", "{}");
        Editor settingsEditor = settings.edit();

        try {
            JSONObject errorsByObservationId = new JSONObject(errorsJson);
            if (!errorsByObservationId.has(String.valueOf(obsId))) {
                errorsByObservationId.put(String.valueOf(obsId), new JSONObject());
            }
            JSONObject projectErrors = errorsByObservationId.getJSONObject(String.valueOf(obsId));

            projectErrors.put(String.valueOf(projectId), errors);
            settingsEditor.putString("pref_observation_errors", errorsByObservationId.toString());
        } catch (JSONException e) {
            Logger.tag(TAG).error(e);
        }

        settingsEditor.apply();
    }

    public JSONArray getErrorsForObservation(int obsId) {
        SharedPreferences settings = getPrefs();
        String errorsJson = settings.getString("pref_observation_errors", "{}");
        try {
            JSONObject errorsByObservationId = new JSONObject(errorsJson);
            if (!errorsByObservationId.has(String.valueOf(obsId))) {
                // No errors for that observation ID
                return new JSONArray();
            }

            JSONObject errorsByProject = errorsByObservationId.getJSONObject(String.valueOf(obsId));
            Iterator<String> keys = errorsByProject.keys();
            JSONArray errors = new JSONArray();

            while (keys.hasNext()) {
                String projectId = keys.next();
                JSONArray errorsForObservation = errorsByProject.getJSONArray(projectId);

                for (int i = 0; i < errorsForObservation.length(); i++) {
                    errors.put(errorsForObservation.getString(i));
                }
            }

            return errors;

        } catch (JSONException e) {
            Logger.tag(TAG).error(e);
            return new JSONArray();
        }
    }


	/** Returns the set inat network member */
	public String getInaturalistNetworkMember() {
    	SharedPreferences settings = getPrefs();
        return settings.getString("pref_network_member", null);
	}

    /** Set the inat network member */
    public void setInaturalistNetworkMember(String memberNetwork) {
        setInaturalistNetworkMember(memberNetwork, true);
    }

	/** Set the inat network member */
	public void setInaturalistNetworkMember(String memberNetwork, boolean updateServer) {
    	SharedPreferences settings = getPrefs();
    	Editor settingsEditor = settings.edit();

    	settingsEditor.putString("pref_network_member", memberNetwork);
    	settingsEditor.apply();

        if (updateServer) {
            // Update the server of the network change
            Intent serviceIntent = new Intent(INaturalistService.ACTION_UPDATE_USER_NETWORK, null, this, INaturalistService.class);
            serviceIntent.putExtra(INaturalistService.NETWORK_SITE_ID, Integer.valueOf(getStringResourceByName("inat_site_id_" + memberNetwork)));
            ContextCompat.startForegroundService(this, serviceIntent);
        }


        // Update app icon label

        final String[] inatNetworks = getINatNetworks();
        String networkForLabel = memberNetwork;

        if (!getStringResourceByName("change_app_title_" + memberNetwork).equalsIgnoreCase("1")) {
            // Don't change app icon label for this network
            networkForLabel = inatNetworks[0];
        }

        enableComponent(networkForLabel);
	}

	private void enableComponent(final String networkForLabel) {
        String packageName = getPackageName();
        final String[] inatNetworks = getINatNetworks();

        if (ObservationListActivity.sActivityCreated) {
            for (int i = 0; i < inatNetworks.length; i++) {
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(packageName, String.format("%s.%s.%s", packageName, ObservationListActivity.class.getSimpleName(), inatNetworks[i])),
                        inatNetworks[i].equalsIgnoreCase(networkForLabel) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        } else {
            // Edge case - observation list activity wasn't created yet, we can't disable the component yet (will cause a run time exception)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableComponent(networkForLabel);
                }
            }, 1000);
        }
    }

    // Called by isLocationEnabled to notify the rest of the app if place is enabled/disabled
    public interface OnLocationStatus {
        void onLocationStatus(boolean isEnabled);
    }

    /** Checks if place services are enabled */
    public boolean isLocationEnabled(final OnLocationStatus locationCallback) {
        // First, check if GPS is disabled
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = (
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            );

        if (!gpsEnabled) return false;


        // Next, see if specifically the user has revoked place access to our app
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }

        if (locationCallback != null) {
            final LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult locationSettingsResult) {
                    final Status status = locationSettingsResult.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All place settings are satisfied. The client can initialize place
                            // requests here.
                            locationCallback.onLocationStatus(true);
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied
                            locationCallback.onLocationStatus(false);
                            break;
                    }
                }
            });
        }

        return gpsEnabled;

    }

    public void detectUserCountryAndUpdateNetwork(Context context) {
 		// Don't ask the user again to switch to another network (if he's been asked before)
 		if (getInaturalistNetworkMember() != null) return;

 		ActivityHelper helper;
        helper = new ActivityHelper(context);

		Resources res = getBaseContext().getResources();
		
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View titleBarView = inflater.inflate(R.layout.change_network_title_bar, null);	
		ImageView titleBarLogo = (ImageView) titleBarView.findViewById(R.id.title_bar_logo);
		
		String country = getUserCountry(context);
		Logger.tag(TAG).debug("Detected country: " + country);
		
        final String[] inatNetworks = getINatNetworks();

		if (country == null) {
			// Couldn't detect country - set default iNat network
			setInaturalistNetworkMember(inatNetworks[0]);
			return;
		}
		

        String detectedNetwork = inatNetworks[0]; // Select default iNaturalist network
		for (int i = 0; i < inatNetworks.length; i++) {
			if (country.equalsIgnoreCase(getStringResourceByName("inat_country_" + inatNetworks[i]))) {
				detectedNetwork = inatNetworks[i];
				break;
			}
		}
		
        // Don't ask the user again to switch if it's the default iNat network
		if (!detectedNetwork.equals(inatNetworks[0])) {
			// Set the logo in the title bar according to network type
			String logoName = getStringResourceByName("inat_logo_" + detectedNetwork);
			String packageName = getPackageName();
			int resId = getResources().getIdentifier(logoName, "drawable", packageName);
			titleBarLogo.setImageResource(resId);
			
			final String selectedNetwork = detectedNetwork;
			helper.confirm(
					titleBarView,
					getStringResourceByName("alert_message_use_" + detectedNetwork),
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							setInaturalistNetworkMember(selectedNetwork);
                            restart();
						}
					},
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Set default iNaturalist network
							setInaturalistNetworkMember(inatNetworks[0]);
						}
					});
		} else {
			// Set default iNaturalist network
			setInaturalistNetworkMember(inatNetworks[0]);
		}

	}
   
    
    public String[] getINatNetworks() {
        Resources res = getResources();
        return res.getStringArray(R.array.inat_networks);
    }
    
    public String[] getStringArrayResourceByName(String aString) {
    	String packageName = getPackageName();
    	int resId = getResources().getIdentifier(aString, "array", packageName);
    	if (resId == 0) {
    		return new String[] { aString };
    	} else {
    		return getResources().getStringArray(resId);
    	}
    } 

    public int getColorResourceByName(String aString) {
    	int resId = getResourceIdByName(aString, "color");
    	if (resId == 0) {
    		return 0;
    	} else {
    		return getResources().getColor(resId);
    	}
    }
    
    public String getStringResourceByName(String aString) {
    	int resId = getResourceIdByName(aString, "string");
    	if (resId == 0) {
    		return aString;
    	} else {
    		return getString(resId);
    	}
    }

    public String getStringResourceByNameOrNull(String aString) {
    	int resId = getResourceIdByName(aString, "string");
    	if (resId == 0) {
    		return null;
    	} else {
    		return getString(resId);
    	}
    }


    public String getStringResourceByName(String name, String fallbackName) {
        Configuration configuration = new Configuration(getResources().getConfiguration());
        int resId = getResourceIdByName(name, "string");
        String value = null;

        if ((resId != 0) && (!locale.getLanguage().equals("en"))) {
            String defaultLanguageValue = null;

            try {
                value = getString(resId);
            } catch (Resources.NotFoundException exc) {
                return getStringResourceByName(fallbackName);
            }

            Locale defaultLocale = new Locale("en");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList localeList = new LocaleList(defaultLocale);
                configuration.setLocales(localeList);
                defaultLanguageValue = createConfigurationContext(configuration).getResources().getString(resId);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLocale(defaultLocale);
                defaultLanguageValue = createConfigurationContext(configuration).getString(resId);
            }

            if ((defaultLanguageValue != null) && (value.equals(defaultLanguageValue))) {
                // That means the current locale doesn't have the translated value - resolve to the fallback name
                return getStringResourceByName(fallbackName);
            }

        } else if (resId == 0) {
            // Resource not found
            return getStringResourceByName(fallbackName);
        }

        try {
            return getString(resId);
        } catch (Resources.NotFoundException exc) {
            return getStringResourceByName(fallbackName);
    	}
    }

    public void setStringResourceForView(Object parentView, int viewId, String name, String fallbackName) {
        View view = null;
        if (parentView instanceof Activity) {
            view = ((Activity)parentView).findViewById(viewId);
        } else {
            view = ((View) parentView).findViewById(viewId);
        }

        if (view == null) return;

        if (view instanceof TextView) {
            ((TextView) view).setText(getStringResourceByName(name, fallbackName));
        } else if (view instanceof Button) {
            ((Button) view).setText(getStringResourceByName(name, fallbackName));
        }
    }


    private int getResourceIdByName(String aString, String type) {
    	String packageName = getPackageName();
    	int resId = getResources().getIdentifier(aString, type, packageName);
        return resId;
    }

    
    public void applyLocaleSettings(){
    	SharedPreferences settings = getPrefs();

        Configuration config = getBaseContext().getResources().getConfiguration();
        
        String lang = settings.getString("pref_locale", "");
        if (! "".equals(lang) && ! config.locale.getLanguage().equals(lang))
        {
        	String parts[] = lang.split("-r");
        	if (parts.length > 1) {
        		// Language + country code
        		locale = new Locale(parts[0], parts[1]);
        	} else {
        		// Just the language code
        		locale = new Locale(lang);
        	}
        }else{        	
        	locale = deviceLocale;
        }
        Locale.setDefault(locale);
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public boolean hasLocaleChanged() {
        String lastLanguage = getPrefs().getString("last_language", null);
        if (lastLanguage == null) {
            setLastLocale();
            lastLanguage = locale.getLanguage();
        }
        return !locale.getLanguage().equals(lastLanguage);
    }

    public void setLastLocale() {
        String newLanguage = locale.getLanguage();
        getPrefs().edit().putString("last_language", newLanguage).commit();
    }
    
    public void restart(){
    	Intent i = getBaseContext().getPackageManager()
	             .getLaunchIntentForPackage(getBaseContext().getPackageName());
	    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    startActivity(i);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	Configuration config = new Configuration(newConfig);
    	if (locale != null)
        {
    		config.locale = locale;
            Locale.setDefault(locale);
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
    }
    
    public static Context getAppContext() {
        return INaturalistApp.context;
    }
    
    public void setIsSyncing(boolean isSyncing) {
    	mIsSyncing  = isSyncing;
    }
    public boolean getIsSyncing() {
    	return mIsSyncing;
    }


    public void setCancelSync(boolean cancel) {
        mCancelSync  = cancel;
    }
    public boolean getCancelSync() {
        return mCancelSync;
    }

    public void checkSyncNeeded() {
        Cursor oCursor = getContentResolver().query(Observation.CONTENT_URI, Observation.PROJECTION, 
                "_synced_at IS NULL OR (_updated_at > _synced_at)", null, Observation.DEFAULT_SORT_ORDER);
        Cursor opCursor = getContentResolver().query(ObservationPhoto.CONTENT_URI, ObservationPhoto.PROJECTION, 
                "_synced_at IS NULL OR (_updated_at > _synced_at)", null, ObservationPhoto.DEFAULT_SORT_ORDER);
        if (!mIsSyncing) {
            mNotificationManager.cancel(SYNC_NOTIFICATION);
        } else {
            Resources res = getResources();
            serviceNotify(SYNC_NOTIFICATION, 
                    res.getString(R.string.sync_required),
                    String.format(res.getString(R.string.sync_required_message), oCursor.getCount(), opCursor.getCount()),
                    null,
                    new Intent(INaturalistService.ACTION_SYNC, null, this, INaturalistService.class));
        }
    }

    public boolean loggedIn() {
        return getPrefs().contains("credentials");
    }

    public String getJWTToken() {
        return getPrefs().getString("jwt_token", null);
    }

    
    public LoginType getLoginType() {
        return LoginType.valueOf(getPrefs().getString("login_type", LoginType.OAUTH_PASSWORD.toString()));
    }


    public String currentUserLogin() {
        return getPrefs().getString("username", null);
    }

    public SharedPreferences getPrefs() {
        if (mPrefs == null) {
            mPrefs = getSharedPreferences("iNaturalistPreferences", MODE_PRIVATE);   
        }
        return mPrefs;
    }

    public void notify(Integer id, String title, String content) {
        notify(id, title, content, null);
    }
    
    public void notify(Integer id, String title, String content, String ticker) {
        Intent intent = new Intent(this, ObservationListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notify(id, title, content, ticker, intent);
    }

    public void sweepingNotify(Integer id, String title, String content, String ticker, Intent intent) {
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }
        notify(id, title, content, ticker, intent);
    }
    
    public void sweepingNotify(Integer id, String title, String content, String ticker) {
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }
        notify(id, title, content, ticker);
    }
    
    public void notify(Integer id, String title, String content, String ticker, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notify(id, title, content, ticker, pendingIntent);
    }
    
    public void serviceNotify(Integer id, String title, String content, String ticker, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        notify(id, title, content, ticker, pendingIntent);
    }

    public void notify(Integer id, String title, String content, String ticker, PendingIntent pendingIntent) {
        notify(title, content);
    }

    public void notify(String title, String content) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        //mNotificationManager.notify(id, notification);
        if (mNotificationCallback != null) {
        	mNotificationCallback.onNotification(title, content);
        }
    }
    
    public void setNotificationCallback(INotificationCallback callback) {
    	mNotificationCallback = callback;
    }
    
    public String formatDate(Timestamp date) { return DATE_FORMAT.format(date); }
    public String formatDatetime(Timestamp date) { return DATETIME_FORMAT.format(date); }
    public String shortFormatDate(Timestamp date) {
        SimpleDateFormat f;
        if (Locale.getDefault().getCountry().equals("US")) {
            f = new SimpleDateFormat("MMM d, yyyy");
        } else {
            f = SHORT_DATE_FORMAT;
        }
        return f.format(date);
    }
    public String shortFormatTime(Timestamp date) { return SHORT_TIME_FORMAT.format(date); }


    public interface OnDownloadFileProgress {
        public boolean onDownloadProgress(long downloaded, long total, String downloadedFilename);
        public void onDownloadError();
    }

    public void setDownloadCallback(OnDownloadFileProgress callback) {
        mDownloadCallback = callback;
    }

    public void downloadFile(final String downloadUrl, final OnDownloadFileProgress callback) {
        mDownloadCallback = callback;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(downloadUrl);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    final int fileSize = connection.getContentLength();

                    // Download the file
                    InputStream input = new BufferedInputStream(url.openStream(), 8192);

                    // Output stream (temp file)
                    File outputFile = File.createTempFile(UUID.randomUUID().toString(), null, getCacheDir());
                    String outputFilename = outputFile.getAbsolutePath();
                    OutputStream output = new FileOutputStream(outputFile);

                    byte data[] = new byte[1024];

                    long total = 0;
                    int count = 0;

                    // Write output data, chunk by chunk
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);

                        mDownloadCallback.onDownloadProgress(total, fileSize, outputFilename);
                    }

                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();

                } catch (IOException exc) {
                    Logger.tag(TAG).error(exc);
                    mDownloadCallback.onDownloadError();
                }

            }
        }).start();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public interface OnRequestPermissionResult {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    private Map<String, OnRequestPermissionResult> mPermissionsCbByPermissionName = new HashMap<>();

    public void requestCameraPermission(Activity activity, OnRequestPermissionResult cb) {
        requestPermissions(activity, new String[] { Manifest.permission.CAMERA }, cb);
    }

    public void requestLocationPermission(Activity activity, OnRequestPermissionResult cb) {
        requestPermissions(activity, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }, cb);
    }

    public void requestExternalStoragePermission(Activity activity, OnRequestPermissionResult cb) {
        requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, cb);
    }

    public void requestAudioRecordingPermission(Activity activity, OnRequestPermissionResult cb) {
        requestPermissions(activity, new String[] { Manifest.permission.RECORD_AUDIO }, cb);
    }

    private void requestPermissions(final Activity activity, final String[] permissions, OnRequestPermissionResult cb) {
        for (String permission: permissions) {
            mPermissionsCbByPermissionName.put(permission, cb);
        }

        // Run on a background thread, not to block / mess up the UI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Logger.tag(TAG).error(e);
                }

                ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_REQUEST);
            }
        }).start();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST) {
            return;
        }

        boolean granted = permissions.length > 0 ? true : false;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }


        for (String permission: permissions) {
            OnRequestPermissionResult cb = mPermissionsCbByPermissionName.get(permission);
            if (cb != null) {
                if (granted) {
                    cb.onPermissionGranted();
                } else {
                    cb.onPermissionDenied();
                }
            }
        }
    }


    public boolean isCameraPermissionGranted() {
        return (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }


    public boolean isLocationPermissionGranted() {
        return (
                (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                        (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        );
    }

    public boolean isExternalStoragePermissionGranted() {
        return (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public boolean isAudioRecordingPermissionGranted() {
        return (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }

}
