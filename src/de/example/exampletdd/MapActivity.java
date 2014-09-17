package de.example.exampletdd;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.example.exampletdd.fragment.ErrorDialogFragment;
import de.example.exampletdd.fragment.map.MapButtonsFragment;
import de.example.exampletdd.fragment.map.MapProgressFragment;
import de.example.exampletdd.gms.GPlayServicesErrorDialogFragment;
import de.example.exampletdd.model.DatabaseQueries;
import de.example.exampletdd.model.WeatherLocation;

public class MapActivity extends FragmentActivity implements
									GoogleApiClient.ConnectionCallbacks,
									GoogleApiClient.OnConnectionFailedListener,
									LocationListener,
									MapProgressFragment.TaskCallbacks {
	private static final String TAG = "MapActivity";
	// Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String PROGRESS_FRAGMENT_TAG = "PROGRESS_FRAGMENT";
    private static final String BUTTONS_FRAGMENT_TAG = "BUTTONS_FRAGMENT";
    private WeatherLocation mRestoreUI;
       
    // Google Play Services Map
    private GoogleMap mMap;
    // TODO: read and store from different threads? Hopefully always from UI thread.
    private Marker mMarker;
    
    // Google Play Services Location
    private GoogleApiClient mGoogleApiClient;
    // Boolean to track whether the app is already resolving an error
    // TODO: Store/read from UI thread?
    private boolean mResolvingError;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_map);
        
        // Google Play Services
        // Restore resolving error (for example if user rotates the screen)
        // It must be done (I guess) before onRestoreInstanceState because its value
        // is used by onStart method.
        if (savedInstanceState != null) {
        	mResolvingError = savedInstanceState.getBoolean("mResolvingError", false);
        }
        
        // Google Play Services Location
        mGoogleApiClient = new GoogleApiClient.Builder(this)
								.addApi(LocationServices.API)
								.addConnectionCallbacks(this)
								.addOnConnectionFailedListener(this)
								.enableAutoManage(this, this)
								.useDefaultAccount()
								.build();  
        
        // Google Play Services Map
        final MapFragment mapFragment = (MapFragment) this.getFragmentManager()
                .findFragmentById(R.id.weather_map_fragment_map);
        this.mMap = mapFragment.getMap();
        this.mMap.setMyLocationEnabled(false);
        this.mMap.getUiSettings().setCompassEnabled(false);
        this.mMap.setOnMapLongClickListener(new MapActivityOnMapLongClickListener(this));
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

        final ActionBar actionBar = this.getActionBar();
        // TODO: string resource
        actionBar.setTitle("Mark your location");
        
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
    	
    	savedInstanceState.putBoolean("mResolvingError", this.mResolvingError);
        
    	super.onSaveInstanceState(savedInstanceState);
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
            	.setLastForecastUIUpdate(null);
            	query.updateDataBase(weatherLocation);
            } else {
            	final WeatherLocation location = new WeatherLocation()
            		.setCity(cityString)
            		.setCountry(countryString)
            		.setIsSelected(true)
            		.setLatitude(position.latitude)
            		.setLongitude(position.longitude);
            	query.insertIntoDataBase(location);
            }
    	}
    }
    
    public void onClickGetLocation(final View v) {
    	// TODO: Somehow I should show a progress dialog.
        // If Google Play Services is available
        if (servicesConnected()) {
        	if (this.mGoogleApiClient.isConnected()) {
        		// TODO: Hopefully there will be results even if location did not change...
        		final LocationRequest locationRequest = LocationRequest.create();
            	locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            	locationRequest.setInterval(1000);
        		final FusedLocationProviderApi fusedLocationApi = LocationServices.FusedLocationApi;
        		// TODO: What if between mGoogleApiClient.isConnected() and this point mGoogleApiClient is disconnected? Android sucks?
        		fusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        	} else {
        		// TODO: string resource
        		Toast.makeText(this, "You are not yet connected to Google Play Services.", Toast.LENGTH_LONG).show();
        	}
        }
        // Trying to use the synchronous calls. Problems: mGoogleApiClient read/store from different threads.
        // new GetLocationTask(this).execute();
    }
    
    private void updateUI(final WeatherLocation weatherLocation) {

        final TextView city = (TextView) this.findViewById(R.id.weather_map_city);
        final TextView country = (TextView) this.findViewById(R.id.weather_map_country);
        city.setText(weatherLocation.getCity());
        country.setText(weatherLocation.getCountry());

        final LatLng point = new LatLng(weatherLocation.getLatitude(), weatherLocation.getLongitude());
        if (this.mMarker != null) {
        	// Just one marker on map
        	this.mMarker.remove();
        }
        this.mMarker = this.mMap.addMarker(new MarkerOptions().position(point).draggable(true));
        this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 5));
        this.mMap.animateCamera(CameraUpdateFactory.zoomIn());
        this.mMap.animateCamera(CameraUpdateFactory.zoomTo(8), 2000, null);
    }
    
    private class MapActivityOnMapLongClickListener implements OnMapLongClickListener {
    	// Store the context passed to the AsyncTask when the system instantiates it.
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
    
    /*****************************************************************************************************
     * 
     * 									Google Play Services 
     * 
     * ***************************************************************************************************/
    
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
        	// TODO: string resource
            Toast.makeText(this, "Cannot get address. No geocoder available.", Toast.LENGTH_LONG).show();
            
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
    
    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(TAG, "Google Play Services is available.");

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            final Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                final GPlayServicesErrorDialogFragment errorFragment = new GPlayServicesErrorDialogFragment(this);
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), TAG);
            }
            return false;
        }
    }
    
    /*****************************************************************************************************
     * 
     * 										Google Play Services 
     * 
     * 								GoogleApiClient.ConnectionCallbacks
     * 								GoogleApiClient.OnConnectionFailedListener
     * 
     * ***************************************************************************************************/
    
    /**
     * Called when the Activity is started/restarted, even before it becomes visible.
     */
    @Override
    protected void onStart() {
    	super.onStart();
    	
        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }
    
    /**
     * Called when the Activity is no longer visible at all.
     * Disconnect.
     */
    @Override
    public void onStop() {

        // Disconnecting the client invalidates it.
        // After disconnect() is called, the client is considered "dead".
    	mGoogleApiClient.unregisterConnectionCallbacks(this);
    	mGoogleApiClient.unregisterConnectionFailedListener(this);
        mGoogleApiClient.disconnect();

        super.onStop();
    }
    
    @Override
    public void onConnectionSuspended(final int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    	
    	// TODO: check out if this really works...
    	
    	final Button getLocationButton = (Button) this.findViewById(R.id.weather_map_button_getlocation);
    	getLocationButton.setEnabled(false);
    	
    	String stringCause;
    	switch(cause) {
    		case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
    			// TODO: string resource
    			stringCause = "Lost network.";
    			break;
    		case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
    			// TODO: string resource
    			stringCause = "Disconnected service.";
    			break;
    		default:
    			stringCause = "";
    			break;		
    	}
    	// TODO: string resource
    	Toast.makeText(this, "No connection to Google Play Services. " + stringCause, Toast.LENGTH_LONG).show();
    }
    
	@Override
	public void onConnected(final Bundle bundle) {
		final FragmentManager fm = getSupportFragmentManager();
    	Fragment buttonsFragment = fm.findFragmentByTag(BUTTONS_FRAGMENT_TAG);
    	if (buttonsFragment != null) {
    		final Button getLocationButton = (Button) this.findViewById(R.id.weather_map_button_getlocation);
    		getLocationButton.setEnabled(true);
    	}

		// TODO: string resource
		Toast.makeText(this, "Connected to Google Play Services.", Toast.LENGTH_SHORT).show();
	}

    /**
     * Called by Location Services if the attempt to
     * Location Services fails.
     *
     * IT NEVER DOES ANYTHING. ANDROID SUCKS!!!
     */
	@Override
	public void onConnectionFailed(final ConnectionResult result) {
		// TODO: Hopefully being called from UI see onDialogDismissed for concerns...
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
		if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
        	mResolvingError = true;
            try {
                // Start an Activity that tries to resolve the error
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (final SendIntentException e) {
            	// There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
        	// Show dialog using GooglePlayServicesUtil.getErrorDialog()
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
	}

    /**
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     *
     * IT NEVER DOES ANYTHING. ANDROID SUCKS!!!
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case REQUEST_RESOLVE_ERROR :
            	mResolvingError = false;
            	switch (resultCode) {
            		// If Google Play services resolved the problem
            		case Activity.RESULT_OK:
            			// Log the result
            			Log.d(TAG, "Error resolved. Please re-try operation");
            		
            			// Make sure the app is not already connected or attempting to connect
                        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                			// TODO: What if between mGoogleApiClient.isConnected() and this point mGoogleApiClient is disconnected? Android sucks?
                            mGoogleApiClient.connect();
                        }
            			
            			// TODO: Display the result?
            			//mConnectionState.setText("Client connected");
            			//mConnectionStatus.setText("Error resolved. Please re-try operation.");
            			break;		
            			// If any other result was returned by Google Play services
            		default:
            			// Log the result
            			Log.d(TAG, "Google Play services: unable to resolve connection error.");

            			// TODO: Display the result?
            			//mConnectionState.setText("Client disconnected";
            			//mConnectionStatus.setText("Google Play services: unable to resolve connection error.");
            			break;
                }
            // If any other request code was received
            default:
            	// TODO: show some error?
            	// Report that this Activity received an unknown requestCode
            	Log.d(TAG, "Received an unknown activity request code onActivityResult. Request code: " + requestCode);
            	break;
        }
        
        // TODO: documentation says I must call its parent's onActivityResult
        // see: https://developer.android.com/reference/com/google/android/gms/common/api/GoogleApiClient.Builder.html#enableAutoManage(android.support.v4.app.FragmentActivity, com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener)
        super.onActivityResult(requestCode, resultCode, intent);
    }
	
    private void showErrorDialog(final int errorCode) {

        // Get the error dialog from Google Play services
        final Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode, this, REQUEST_RESOLVE_ERROR);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            final GPlayServicesErrorDialogFragment GPSErrorFragment = new GPlayServicesErrorDialogFragment(this);

            // Set the dialog in the DialogFragment
            GPSErrorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            GPSErrorFragment.show(this.getSupportFragmentManager(), "GPlayServicesErrorDialog");
        }
    }
    
    /* Called from GPlayServicesErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
    	// IT DOES NOT WORK WHEN SCREEN ROTATES. ANDROID EXAMPLES SUCK!!!
        mResolvingError = false;
    }
    
    /*****************************************************************************************************
     *
     * 							Google Play Services LocationListener
     *
     *****************************************************************************************************/
    private class GetLocationTask extends AsyncTask<Void, Void, Void> {
    	// Store the context passed to the AsyncTask when the system instantiates it.
        private final Context localContext;
        
        private GetLocationTask(final Context localContext) {
			this.localContext = localContext;
		}

		@Override
		protected Void doInBackground(final Void... params) {
			final MapActivity activity = (MapActivity) this.localContext;
        	
	    	// TODO: Somehow I should show a progress dialog.
	        // If Google Play Services is available
	        if (activity.servicesConnected()) {
	        	if (activity.mGoogleApiClient.isConnected()) {
	        		// TODO: Hopefully there will be results even if location did not change...
	        		final LocationRequest locationRequest = LocationRequest.create();
	            	locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	            	locationRequest.setInterval(1000);
	        		final FusedLocationProviderApi fusedLocationApi = LocationServices.FusedLocationApi;
	        		// TODO: What if between mGoogleApiClient.isConnected() and this point mGoogleApiClient is disconnected? Android sucks?
	        		// esto tiene mala pinta:
	        		// 1. mGoogleApiClient read/store desde distintos hilos
	        		// 2. el hilo de UI podría desconectar entre este punto y isConnected y
	        		// aquí no sé que va a pasar si está desconectado. Quizás no pase nada y simplemente status retorna error?
	        		final com.google.android.gms.common.api.Status status =
	        				fusedLocationApi.requestLocationUpdates(
	        						activity.mGoogleApiClient,
	        						locationRequest,
	        						activity,
	        						activity.getMainLooper()).await();
	        	} else {
	        		// TODO: string resource
	        		Toast.makeText(activity, "You are not yet connected to Google Play Services.", Toast.LENGTH_LONG).show();
	        	}
	        }
			return null;
		}
    	
    }
    
	@Override
	public void onLocationChanged(final Location location) {
		// It was called from onClickGetLocation (UI thread) This method will run in the same thread (the UI thread)
		// so, I do no think there should be any problem.

		if (mGoogleApiClient.isConnected()) {
			final FusedLocationProviderApi fusedLocationApi = LocationServices.FusedLocationApi;
			// TODO: if user clicks many times onClickGetLocation I may not assure how many times
			// onLocationChanged will be called. Is it a problem? AFAIK it isn't.
			// TODO: What if between mGoogleApiClient.isConnected() and this point mGoogleApiClient is disconnected? Android sucks?
			fusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		}

		// Display the current location in the UI
		// TODO: May location not be null?
		this.getAddressAndUpdateUI(location.getLatitude(), location.getLongitude());
	}

	/*****************************************************************************************************
	 *
	 * 							MapProgressFragment.TaskCallbacks
	 *
	 *****************************************************************************************************/
	@Override
	public void onPostExecute(WeatherLocation weatherLocation) {

        if (weatherLocation == null) {
        	this.removeProgressFragment();
            final DialogFragment newFragment = ErrorDialogFragment.newInstance(R.string.error_dialog_location_error);
            newFragment.setRetainInstance(true);
            newFragment.show(this.getSupportFragmentManager(), "errorDialog");
        } else {
        	this.updateUI(weatherLocation);
        	this.removeProgressFragment();
        }
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
    	if (mGoogleApiClient.isConnected()) {
    		final Button getLocationButton = (Button) this.findViewById(R.id.weather_map_button_getlocation);
    		getLocationButton.setEnabled(true);
    	}
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
}
