package org.inaturalist.android;
import java.util.HashMap;
import org.inaturalist.android.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationChooserActivity extends AppCompatActivity implements LocationListener {
    public final static String TAG = "INaturalistMapActivity";
	protected static final String LATITUDE = "latitude";
	protected static final String LONGITUDE = "longitude";
	protected static final String ACCURACY = "accuracy";
    protected static final String ICONIC_TAXON_NAME = "iconic_taxon_name";
    private GoogleMap mMap;
    private HashMap<String, Observation> mMarkerObservations;
    private INaturalistApp mApp;
	private double mLatitude;
	private double mLongitude;
	private boolean mZoomToLocation = false;
	private LocationManager mLocationManager;
	private double mAccuracy;
    private ActivityHelper mHelper;
    private String mIconicTaxonName;

    @Override
	protected void onStart()
	{
		super.onStart();
		FlurryAgent.onStartSession(this, INaturalistApp.getAppContext().getString(R.string.flurry_api_key));
		FlurryAgent.logEvent(this.getClass().getSimpleName());
	}

	@Override
	protected void onStop()
	{
		super.onStop();		
		FlurryAgent.onEndSession(this);
	}	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHelper = new ActivityHelper(this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        mLongitude = getIntent().getDoubleExtra(LONGITUDE, 0);
        mLatitude = getIntent().getDoubleExtra(LATITUDE, 0);
        mAccuracy = getIntent().getDoubleExtra(ACCURACY, 0);
        mIconicTaxonName = getIntent().getStringExtra(ICONIC_TAXON_NAME);

        if ((mLongitude != 0) && (mLatitude != 0) && (savedInstanceState == null)) {
        	mZoomToLocation = true;
        }
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setLogo(R.drawable.ic_arrow_back);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(Html.fromHtml(getString(R.string.details)));

        if (savedInstanceState != null) {
        	mLongitude = savedInstanceState.getDouble("longitude");
        	mLatitude = savedInstanceState.getDouble("latitude");
            mAccuracy = savedInstanceState.getDouble("accuracy");
            mIconicTaxonName = savedInstanceState.getString("iconic_taxon_name");
        }



        setContentView(R.layout.location_chooser);
    }

    @Override 
    public void onResume() {
        super.onResume();
        if (mApp == null) {
            mApp = (INaturalistApp) getApplicationContext();
        }
        setUpMapIfNeeded();
        
        double longitude = mLongitude;
        double latitude = mLatitude;
        
        if ((longitude != 0) && (latitude != 0)) {
        	LatLng location = new LatLng(latitude, longitude);

        	int zoom = 15;

        	if (mAccuracy > 0) {
            	DisplayMetrics metrics = new DisplayMetrics();
            	getWindowManager().getDefaultDisplay().getMetrics(metrics);

            	int screenWidth = metrics.widthPixels;
            	
                double equatorLength = 40075004; // in meters
                double widthInPixels = screenWidth * 0.4 * 0.5;
                double metersPerPixel = equatorLength / 256;
                int zoomLevel = 1;
                while ((metersPerPixel * widthInPixels) > mAccuracy) {
                    metersPerPixel /= 2;
                    ++zoomLevel;
                    Log.e(TAG, "\t** Zoom = " + zoomLevel + "; CurrentAcc = " + (metersPerPixel * widthInPixels) +  "; Accuracy = " + mAccuracy);
                }
                Log.e(TAG, "Zoom = " + zoomLevel + "; Accuracy = " + mAccuracy);
                zoom = zoomLevel;
        	}
        	

        	if (mZoomToLocation) {
        		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoom));
        		mZoomToLocation = false;
        	} else {
        		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoom), 1, null);
        	}
        } else {

        }
        
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putDouble("longitude", mLongitude);
        outState.putDouble("latitude", mLatitude);
        outState.putDouble("accuracy", mAccuracy);
        outState.putString("iconic_taxon_name", mIconicTaxonName);
        super.onSaveInstanceState(outState);
    }
 
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem layersItem = menu.findItem(R.id.layers);
        if (mMap != null) {
        	if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
        		layersItem.setTitle(R.string.street);
        	} else {
        		layersItem.setTitle(R.string.satellite);
        	}
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.choose_location_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    private void onCancel() {
        mHelper.confirm(getString(R.string.edit_location), getString(R.string.discard_location_changes),
                new DialogInterface.OnClickListener () {
                    public void onClick (DialogInterface dialog, int buttonId) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                },
                new DialogInterface.OnClickListener () {
                    public void onClick (DialogInterface dialog, int buttonId) {
                        dialog.cancel();
                    }
                },
                R.string.yes, R.string.no);
    }
    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
    		onCancel();
    		return false;
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onCancel();

                return true;
            case R.id.save_location:
                Bundle bundle = new Bundle();

                float currentZoom = mMap.getCameraPosition().zoom;

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                int screenWidth = metrics.widthPixels;

                //////////////
                double equatorLength = 40075004; // in meters
                double metersPerPixel = equatorLength / 256;
                int zoomLevel = 1;
                while (zoomLevel < currentZoom) {
                    metersPerPixel /= 2;
                    ++zoomLevel;
                }
                double accuracy = (double) ((screenWidth * 0.4 * 0.5) * metersPerPixel);
                Log.e(TAG, "Meters per radius = " + accuracy + "; zoom = " + zoomLevel);

                ////////////

                bundle.putDouble(LATITUDE, mMap.getCameraPosition().target.latitude);
                bundle.putDouble(LONGITUDE, mMap.getCameraPosition().target.longitude);
                bundle.putDouble(ACCURACY, accuracy);

                Intent resultIntent = new Intent();
                resultIntent.putExtras(bundle);
                setResult(RESULT_OK, resultIntent);

                finish();

                return true;

            case R.id.layers:
                if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    item.setTitle(R.string.satellite);
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    item.setTitle(R.string.street);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void setUpMapIfNeeded() {
        if (mMarkerObservations == null) {
            mMarkerObservations = new HashMap<String, Observation>();
        }
        if (mMap == null) {
            mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.
                mMap.setMyLocationEnabled(true);
                if (!mMarkerObservations.isEmpty()) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (Observation o: mMarkerObservations.values()) {
                        if (o.private_latitude != null && o.private_longitude != null) {
                            builder.include(new LatLng(o.private_latitude, o.private_longitude));
                        } else {
                            builder.include(new LatLng(o.latitude, o.longitude));
                        }
                    }
                }

                mMap.clear();
                MarkerOptions opts = new MarkerOptions().position(new LatLng(mLatitude, mLongitude)).icon(INaturalistMapActivity.observationIcon(mIconicTaxonName));
                Marker m = mMap.addMarker(opts);

            }
        }
    }

   
  
	@Override
	public void onLocationChanged(Location location) {
        if (location != null) {
            Log.v("Location Changed", location.getLatitude() + " and " + location.getLongitude());
            mLocationManager.removeUpdates(this);

        	LatLng camLocation = new LatLng(location.getLatitude(), location.getLongitude());
        	mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(camLocation, 15));
        }
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
