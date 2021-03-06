/**
 * Copyright 2014 Gustavo Martin Morcuende
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.gumartinm.weather.information.activity;

import android.content.Context;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.fragment.map.MapButtonsFragment;
import name.gumartinm.weather.information.fragment.map.MapProgressFragment;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;


public class MapActivity extends AppCompatActivity implements
									LocationListener,
									MapProgressFragment.TaskCallbacks {
    private static final String PROGRESS_FRAGMENT_TAG = "PROGRESS_FRAGMENT";
    private static final String BUTTONS_FRAGMENT_TAG = "BUTTONS_FRAGMENT";
    private WeatherLocation mRestoreUI;
       
    // Google Play Services Map
    private GoogleMap mMap;
    private Marker mMarker;
    // Async map loading.
    private ButtonsUpdate mButtonsUpdate;
    private MapUpdate mMapUpdate;

    // The Android rotate screen mess and callback coming from external API (nobody knows how
    // it was implemented, otherwise I could avoid this code)
    private MapActivityOnMapReadyCallback mMapActivityOnMapReadyCallback;
    
    private LocationManager mLocationManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_map);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.weather_main_toolbar);
        this.setSupportActionBar(toolbar);

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // Acquire a reference to the system Location Manager
        this.mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        // Google Play Services Map
        final MapFragment mapFragment = (MapFragment) this.getFragmentManager()
                .findFragmentById(R.id.weather_map_fragment_map);
        this.mMapActivityOnMapReadyCallback = new MapActivityOnMapReadyCallback(this);
        mapFragment.getMapAsync(this.mMapActivityOnMapReadyCallback);
    }
    
    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
    	// Instead of restoring the state during onCreate() you may choose to
    	// implement onRestoreInstanceState(), which the system calls after the
    	// onStart() method. The system calls onRestoreInstanceState() only if
    	// there is a saved state to restore, so you do not need to check whether
    	// the Bundle is null:
        super.onRestoreInstanceState(savedInstanceState);
        
        // Restore UI state
        this.mRestoreUI = (WeatherLocation) savedInstanceState.getSerializable("WeatherLocation");
    }

    @Override
    public void onResume() {
        super.onResume();

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setTitle(this.getString(R.string.weather_map_mark_location));
        
        WeatherLocation weatherLocation;
        if (this.mRestoreUI != null) {
        	// Restore UI state
        	weatherLocation = this.mRestoreUI;
        	// just once
        	this.mRestoreUI = null;
        } else if (this.mMarker != null ) {
        	final TextView city = (TextView) this.findViewById(R.id.weather_map_city);
            final TextView country = (TextView) this.findViewById(R.id.weather_map_country);
            final String cityString = city.getText().toString();
            final String countryString = country.getText().toString();
            
            final LatLng point = this.mMarker.getPosition();
            double latitude = point.latitude;
            double longitude = point.longitude;

            weatherLocation = new WeatherLocation()
            		.setCity(cityString)
            		.setCountry(countryString)
            		.setLatitude(latitude)
            		.setLongitude(longitude);
    	} else {
        	final DatabaseQueries query = new DatabaseQueries(this.getApplicationContext());
        	weatherLocation = query.queryDataBase();
        }
        
        if (weatherLocation != null) {
        	this.updateUI(weatherLocation);
        }
    }
    
    /**
     * I am not using fragment transactions in the right way. But I do not know other way for doing what I am doing.
     * 
     * {@link http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult}
     */
    @Override
    public void onPostResume() {
    	super.onPostResume();
    	
    	final FragmentManager fm = getSupportFragmentManager();
    	final Fragment progressFragment = fm.findFragmentByTag(PROGRESS_FRAGMENT_TAG);
    	if (progressFragment == null) {
    		 this.addButtonsFragment();
     	} else {
     		this.removeProgressFragment();
     		final Bundle bundle = progressFragment.getArguments();
         	double latitude = bundle.getDouble("latitude");
         	double longitude = bundle.getDouble("longitude");
     		this.addProgressFragment(latitude, longitude);
     	}
    }
    
    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
    	// Save UI state
    	// Save Google Maps Marker
    	if (this.mMarker != null) {
    		final TextView city = (TextView) this.findViewById(R.id.weather_map_city);
            final TextView country = (TextView) this.findViewById(R.id.weather_map_country);
            final String cityString = city.getText().toString();
            final String countryString = country.getText().toString();
            
            final LatLng point = this.mMarker.getPosition();
            double latitude = point.latitude;
            double longitude = point.longitude;

            final WeatherLocation location = new WeatherLocation()
            		.setCity(cityString)
            		.setCountry(countryString)
            		.setLatitude(latitude)
            		.setLongitude(longitude);
            savedInstanceState.putSerializable("WeatherLocation", location);
        }
    	        
    	super.onSaveInstanceState(savedInstanceState);
    }
    
	@Override
	public void onPause() {
		super.onPause();
		
		this.mLocationManager.removeUpdates(this);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        // The Android rotate screen mess.
        if (this.mMapActivityOnMapReadyCallback != null) {
            this.mMapActivityOnMapReadyCallback.onDestroy();
        }
        if (this.mButtonsUpdate != null) {
            this.mButtonsUpdate.onDestroy();
        }
        if (this.mMapUpdate != null) {
            this.mMapUpdate.onDestroy();
        }
    }
	
    public void onClickSaveLocation(final View v) {
    	if (this.mMarker != null) {
    		final LatLng position = this.mMarker.getPosition();
    		
    		final TextView city = (TextView) this.findViewById(R.id.weather_map_city);
            final TextView country = (TextView) this.findViewById(R.id.weather_map_country);
            final String cityString = city.getText().toString();
            final String countryString = country.getText().toString();
            
    		final DatabaseQueries query = new DatabaseQueries(this.getApplicationContext());
    		final WeatherLocation weatherLocation = query.queryDataBase();
            if (weatherLocation != null) {
            	weatherLocation
            	.setCity(cityString)
            	.setCountry(countryString)
            	.setLatitude(position.latitude)
            	.setLongitude(position.longitude)
            	.setLastCurrentUIUpdate(null)
            	.setLastForecastUIUpdate(null)
                .setIsNew(true);
            	query.updateDataBase(weatherLocation);
            } else {
            	final WeatherLocation location = new WeatherLocation()
            		.setCity(cityString)
            		.setCountry(countryString)
            		.setIsSelected(true)
            		.setLatitude(position.latitude)
            		.setLongitude(position.longitude)
                    .setIsNew(true);
            	query.insertIntoDataBase(location);
            }
    	}
    }
    
    public void onClickGetLocation(final View v) {
    	// TODO: Somehow I should show a progress dialog.
        // If Google Play Services is available
        if (this.mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        	// TODO: Hopefully there will be results even if location did not change...   
            final Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingAccuracy(Criteria.NO_REQUIREMENT);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(false);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
            criteria.setSpeedAccuracy(Criteria.NO_REQUIREMENT);
            criteria.setSpeedRequired(false);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
            
            this.mLocationManager.requestSingleUpdate(criteria, this, null);
        } else {
        	Toast.makeText(this, this.getString(R.string.weather_map_not_enabled_location), Toast.LENGTH_LONG).show();
        }
        // Trying to use the synchronous calls. Problems: mGoogleApiClient read/store from different threads.
        // new GetLocationTask(this).execute();
    }
    
    private void updateUI(final WeatherLocation weatherLocation) {

        final TextView city = (TextView) this.findViewById(R.id.weather_map_city);
        final TextView country = (TextView) this.findViewById(R.id.weather_map_country);
        city.setText(weatherLocation.getCity());
        country.setText(weatherLocation.getCountry());

        this.mMapUpdate = new MapUpdate(this, weatherLocation);
        this.mMapUpdate.doUpdate();
    }
    
    private class MapActivityOnMapLongClickListener implements OnMapLongClickListener {
        private final Context localContext;
        
    	private MapActivityOnMapLongClickListener(final Context context) {
    		this.localContext = context;
    	}
    	
		@Override
		public void onMapLongClick(final LatLng point) {
			final MapActivity activity = (MapActivity) this.localContext;
			activity.getAddressAndUpdateUI(point.latitude, point.longitude);
		}
    	
    }

    /**
     * Getting the address of the current location, using reverse geocoding only works if
     * a geocoding service is available.
     *
     */
    private void getAddressAndUpdateUI(final double latitude, final double longitude) {
        // In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent()) {
        	this.removeButtonsFragment();
        	this.removeProgressFragment();
        	this.addProgressFragment(latitude, longitude);
        } else {
        	this.removeProgressFragment();
        	this.addButtonsFragment();
        	// No geocoder is present. Issue an error message.
            Toast.makeText(this, this.getString(R.string.weather_map_no_geocoder_available), Toast.LENGTH_LONG).show();
            
            // Default values
            final String city = this.getString(R.string.city_not_found);
            final String country = this.getString(R.string.country_not_found); 
            final WeatherLocation weatherLocation = new WeatherLocation()
            		.setLatitude(latitude)
            		.setLongitude(longitude)
            		.setCity(city)
            		.setCountry(country);
            
            updateUI(weatherLocation);
        }
    }

	/*****************************************************************************************************
	 *
	 * 							MapProgressFragment.TaskCallbacks
	 *
	 *****************************************************************************************************/
	@Override
	public void onPostExecute(WeatherLocation weatherLocation) {

        this.updateUI(weatherLocation);
        this.removeProgressFragment();

        this.addButtonsFragment();
	}

	/*****************************************************************************************************
	 *
	 * 							MapProgressFragment
	 * I am not using fragment transactions in the right way. But I do not know other way for doing what I am doing.
     * Android sucks.
     *
     * "Avoid performing transactions inside asynchronous callback methods." :(
     * see: http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult
     * see: http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
     * How do you do what I am doing in a different way without using fragments?
	 *****************************************************************************************************/
	
	private void addProgressFragment(final double latitude, final double longitude) {
    	final Fragment progressFragment = new MapProgressFragment();
    	progressFragment.setRetainInstance(true);
    	final Bundle args = new Bundle();
    	args.putDouble("latitude", latitude);
    	args.putDouble("longitude", longitude);
    	progressFragment.setArguments(args);
    	
    	final FragmentManager fm = this.getSupportFragmentManager();
    	final FragmentTransaction fragmentTransaction = fm.beginTransaction();
    	fragmentTransaction.setCustomAnimations(R.anim.weather_map_enter_progress, R.anim.weather_map_exit_progress);
    	fragmentTransaction.add(R.id.weather_map_buttons_container, progressFragment, PROGRESS_FRAGMENT_TAG).commit();
    	fm.executePendingTransactions();
	}
	
	private void removeProgressFragment() {
    	final FragmentManager fm = this.getSupportFragmentManager();
    	final Fragment progressFragment = fm.findFragmentByTag(PROGRESS_FRAGMENT_TAG);
    	if (progressFragment != null) {
    		final FragmentTransaction fragmentTransaction = fm.beginTransaction();
        	fragmentTransaction.remove(progressFragment).commit();
        	fm.executePendingTransactions();
    	}
	}
	
	private void addButtonsFragment() {
		final FragmentManager fm = this.getSupportFragmentManager();
    	Fragment buttonsFragment = fm.findFragmentByTag(BUTTONS_FRAGMENT_TAG);
    	if (buttonsFragment == null) {
    		buttonsFragment = new MapButtonsFragment();
    		buttonsFragment.setRetainInstance(true);
    		final FragmentTransaction fragmentTransaction = fm.beginTransaction();
        	fragmentTransaction.setCustomAnimations(R.anim.weather_map_enter_progress, R.anim.weather_map_exit_progress);
        	fragmentTransaction.add(R.id.weather_map_buttons_container, buttonsFragment, BUTTONS_FRAGMENT_TAG).commit();
        	fm.executePendingTransactions();
    	}

        if (this.mButtonsUpdate == null) {
            this.mButtonsUpdate = new ButtonsUpdate(this);
        }
        this.mButtonsUpdate.doUpdate();
	}
	
	private void removeButtonsFragment() {
    	final FragmentManager fm = this.getSupportFragmentManager();
    	final Fragment buttonsFragment = fm.findFragmentByTag(BUTTONS_FRAGMENT_TAG);
    	if (buttonsFragment != null) {
    		final FragmentTransaction fragmentTransaction = fm.beginTransaction();
        	fragmentTransaction.remove(buttonsFragment).commit();
        	fm.executePendingTransactions();
    	}
	}

   /*****************************************************************************************************
    *
    * 							android.location.LocationListener
    *
    *****************************************************************************************************/

	@Override
	public void onLocationChanged(final Location location) {
		// It was called from onClickGetLocation (UI thread) This method will run in the same thread (the UI thread)

		// Display the current location in the UI
		// TODO: May location not be null?
		this.getAddressAndUpdateUI(location.getLatitude(), location.getLongitude());
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onProviderDisabled(String provider) {}

    private class MapActivityOnMapReadyCallback implements OnMapReadyCallback {
        private MapActivity mapActivity;

        private MapActivityOnMapReadyCallback(final MapActivity mapActivity) {
            this.mapActivity = mapActivity;
        }

        @Override
        public void onMapReady(final GoogleMap googleMap) {
            if (this.mapActivity != null) {
                this.mapActivity.mMap = googleMap;
                this.mapActivity.mMap.setMyLocationEnabled(false);
                this.mapActivity.mMap.getUiSettings().setMyLocationButtonEnabled(false);
                this.mapActivity.mMap.getUiSettings().setZoomControlsEnabled(true);
                this.mapActivity.mMap.getUiSettings().setCompassEnabled(true);
                this.mapActivity.mMap.setOnMapLongClickListener(new MapActivityOnMapLongClickListener(this.mapActivity));

                if (this.mapActivity.mMapUpdate != null)  {
                    this.mapActivity.mMapUpdate.doUpdate();
                }
                if (this.mapActivity.mButtonsUpdate != null) {
                    this.mapActivity.mButtonsUpdate.doUpdate();
                }
            }
        }

        /**
         * The Android rotate screen mess. Required because the Activity.isDestroyed method exists
         * just from API level 17.
         */
        public void onDestroy() {
            this.mapActivity = null;
        }
    }

    private class MapUpdate {
        private MapActivity mapActivity;
        private final WeatherLocation weatherLocation;

        private MapUpdate(MapActivity mapActivity, final WeatherLocation weatherLocation) {
            this.mapActivity = mapActivity;
            this.weatherLocation = weatherLocation;
        }

        private void doUpdate() {
            if (this.mapActivity.mMap == null) {
                // Do nothing.
                return;
            }

            final LatLng point = new LatLng(this.weatherLocation.getLatitude(), this.weatherLocation.getLongitude());
            if (this.mapActivity.mMarker != null) {
                // Just one marker on map
                this.mapActivity.mMarker.remove();
            }
            this.mapActivity.mMarker = this.mapActivity.mMap.addMarker(new MarkerOptions().position(point).draggable(true));
            this.mapActivity.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 5));
            this.mapActivity.mMap.animateCamera(CameraUpdateFactory.zoomIn());
            this.mapActivity.mMap.animateCamera(CameraUpdateFactory.zoomTo(8), 2000, null);
        }

        /**
         * The Android rotate screen mess. Required because the Activity.isDestroyed method exists
         * just from API level 17.
         */
        private void onDestroy() {
            this.mapActivity = null;
        }
    }

    private class ButtonsUpdate {
        private MapActivity mapActivity;

        private ButtonsUpdate(final MapActivity mapActivity) {
            this.mapActivity = mapActivity;
        }

        private void doUpdate() {
            if (this.mapActivity == null) {
                // Do nothing
                return;
            }

            if (this.mapActivity.mMap == null) {
                final Button getLocationButton = (Button) this.mapActivity.findViewById(R.id.weather_map_button_getlocation);
                final Button saveLocationButton = (Button) this.mapActivity.findViewById(R.id.weather_map_button_savelocation);
                if (getLocationButton != null) {
                    getLocationButton.setEnabled(false);
                }
                if (saveLocationButton != null) {
                    saveLocationButton.setEnabled(false);
                }
            } else {
                if (this.mapActivity.mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    final Button getLocationButton = (Button) this.mapActivity.findViewById(R.id.weather_map_button_getlocation);
                    if (getLocationButton != null) {
                        getLocationButton.setEnabled(true);
                    }
                }
                final Button saveLocationButton = (Button) this.mapActivity.findViewById(R.id.weather_map_button_savelocation);
                if (saveLocationButton != null) {
                    saveLocationButton.setEnabled(true);
                }
            }
        }

        /**
         * The Android rotate screen mess. Required because the Activity.isDestroyed method exists
         * just from API level 17.
         */
        private void onDestroy() {
            this.mapActivity = null;
        }
    }
}
