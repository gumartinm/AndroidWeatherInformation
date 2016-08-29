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
package name.gumartinm.weather.information.fragment.current;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonParseException;
import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.httpclient.CustomHTTPClient;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;
import name.gumartinm.weather.information.model.currentweather.Current;
import name.gumartinm.weather.information.parser.JPOSCurrentParser;
import name.gumartinm.weather.information.service.IconsList;
import name.gumartinm.weather.information.service.PermanentStorage;
import name.gumartinm.weather.information.service.ServiceCurrentParser;
import name.gumartinm.weather.information.service.conversor.PressureUnitsConversor;
import name.gumartinm.weather.information.service.conversor.TempUnitsConversor;
import name.gumartinm.weather.information.service.conversor.UnitsConversor;
import name.gumartinm.weather.information.service.conversor.WindUnitsConversor;
import name.gumartinm.weather.information.widget.WidgetProvider;
import timber.log.Timber;

public class CurrentFragment extends Fragment {
    private static final String BROADCAST_INTENT_ACTION = "name.gumartinm.weather.information.UPDATECURRENT";
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    
    	// Inflate the layout for this fragment
        return inflater.inflate(R.layout.weather_current_fragment, container, false);
    }
    
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
        	// Restore UI state
            final Current current = (Current) savedInstanceState.getSerializable("Current");

            if (current != null) {
            	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
            	store.saveCurrent(current);
            }
        }     
        
        this.setHasOptionsMenu(false);

        this.getActivity().findViewById(R.id.weather_current_data_container).setVisibility(View.GONE);
        this.getActivity().findViewById(R.id.weather_current_progressbar).setVisibility(View.VISIBLE);
    	this.getActivity().findViewById(R.id.weather_current_error_message).setVisibility(View.GONE);  	
    }

    @Override
    public void onResume() {
        super.onResume();


        this.mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(final Context context, final Intent intent) {
				final String action = intent.getAction();
				if (action.equals(BROADCAST_INTENT_ACTION)) {
					final Current currentRemote = (Current) intent.getSerializableExtra("current");

					// 1. Check conditions. They must be the same as the ones that triggered the AsyncTask.
					final DatabaseQueries query = new DatabaseQueries(context.getApplicationContext());
			        final WeatherLocation weatherLocation = query.queryDataBase();
			        final PermanentStorage store = new PermanentStorage(context.getApplicationContext());
			        final Current current = store.getCurrent();

			        if (current == null || !CurrentFragment.this.isDataFresh(weatherLocation.getLastCurrentUIUpdate())) {

                        if (currentRemote != null) {
                            // 2. Update UI.
                            CurrentFragment.this.updateUI(currentRemote);

                            // 3. Update current data.
                            store.saveCurrent(currentRemote);

                            // 4. Update location data.
                            weatherLocation.setLastCurrentUIUpdate(new Date());
                            query.updateDataBase(weatherLocation);
                        } else {
                            // Empty UI and show error message
                            CurrentFragment.this.getActivity().findViewById(R.id.weather_current_data_container).setVisibility(View.GONE);
                            CurrentFragment.this.getActivity().findViewById(R.id.weather_current_progressbar).setVisibility(View.GONE);
                            CurrentFragment.this.getActivity().findViewById(R.id.weather_current_error_message).setVisibility(View.VISIBLE);
                        }
			        }
				}
			}
        };

        // Register receiver
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_INTENT_ACTION);
        LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext())
        						.registerReceiver(this.mReceiver, filter);

        // Empty UI
        this.getActivity().findViewById(R.id.weather_current_data_container).setVisibility(View.GONE);
        
        final DatabaseQueries query = new DatabaseQueries(this.getActivity().getApplicationContext());
        final WeatherLocation weatherLocation = query.queryDataBase();
        if (weatherLocation == null) {
            // Nothing to do.
        	// Show error message
        	final ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.weather_current_progressbar);
	        progress.setVisibility(View.GONE);
			final TextView errorMessage = (TextView) getActivity().findViewById(R.id.weather_current_error_message);
	        errorMessage.setVisibility(View.VISIBLE);
            return;
        }

        // If is new location update widgets.
        if (weatherLocation.getIsNew()) {
            WidgetProvider.refreshAllAppWidgets(this.getActivity().getApplicationContext());
            // Update location data.
            weatherLocation.setIsNew(false);
            query.updateDataBase(weatherLocation);
        }


        final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Current current = store.getCurrent();

        if (current != null && this.isDataFresh(weatherLocation.getLastCurrentUIUpdate())) {
            this.updateUI(current);
        } else {
            // Load remote data (asynchronous)
            // Gets the data from the web.
        	this.getActivity().findViewById(R.id.weather_current_progressbar).setVisibility(View.VISIBLE);
        	this.getActivity().findViewById(R.id.weather_current_error_message).setVisibility(View.GONE);
            final CurrentTask task = new CurrentTask(
            		this.getActivity().getApplicationContext(),
                    CustomHTTPClient.newInstance(this.getString(R.string.http_client_agent)),
                    new ServiceCurrentParser(new JPOSCurrentParser()));

            task.execute(weatherLocation.getLatitude(), weatherLocation.getLongitude());
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {

        // Save UI state
    	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Current current = store.getCurrent();

        if (current != null) {
            savedInstanceState.putSerializable("Current", current);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext()).unregisterReceiver(this.mReceiver);

        super.onPause();
    }

    private void updateUI(final Current current) {
        // 1. Update units of measurement.
        final UnitsConversor tempUnitsConversor = new TempUnitsConversor(this.getActivity().getApplicationContext());
        final UnitsConversor windConversor = new WindUnitsConversor(this.getActivity().getApplicationContext());
        final UnitsConversor pressureConversor = new PressureUnitsConversor(this.getActivity().getApplicationContext());


        // 2. Formatters
        final DecimalFormat numberFormatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        numberFormatter.applyPattern("###.##");
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);

        
        // 3. Prepare data for UI.
        String tempMax = "";
        if (current.getMain().getTemp_max() != null) {
            double conversion = (Double) current.getMain().getTemp_max();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMax = numberFormatter.format(conversion) + tempUnitsConversor.getSymbol();
        }
        String tempMin = "";
        if (current.getMain().getTemp_min() != null) {
            double conversion = (Double) current.getMain().getTemp_min();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMin = numberFormatter.format(conversion) + tempUnitsConversor.getSymbol();
        }
        Bitmap picture;
        if ((current.getWeather().size() > 0)
                && (current.getWeather().get(0).getIcon() != null)
                && (IconsList.getIcon(current.getWeather().get(0).getIcon()) != null)) {
            final String icon = current.getWeather().get(0).getIcon();
            picture = BitmapFactory.decodeResource(this.getResources(), IconsList.getIcon(icon)
                    .getResourceDrawable());
        } else {
            picture = BitmapFactory.decodeResource(this.getResources(),
                    R.drawable.weather_severe_alert);
        }

        String description = this.getString(R.string.text_field_description_when_error);
        if (current.getWeather().size() > 0) {
            description = current.getWeather().get(0).getDescription();
        }

        String humidityValue = "";
        if ((current.getMain() != null)
                && (current.getMain().getHumidity() != null)) {
            final double conversion = (Double) current.getMain().getHumidity();
            humidityValue = numberFormatter.format(conversion);
        }
        String pressureValue = "";
        if ((current.getMain() != null)
                && (current.getMain().getPressure() != null)) {
            double conversion = (Double) current.getMain().getPressure();
            conversion = pressureConversor.doConversion(conversion);
            pressureValue = numberFormatter.format(conversion);
        }
        String windValue = "";
        if ((current.getWind() != null)
                && (current.getWind().getSpeed() != null)) {
            double conversion = (Double) current.getWind().getSpeed();
            conversion = windConversor.doConversion(conversion);
            windValue = numberFormatter.format(conversion);
        }
        String rainValue = "";
        if ((current.getRain() != null)
                && (current.getRain().get3h() != null)) {
            final double conversion = (Double) current.getRain().get3h();
            rainValue = numberFormatter.format(conversion);
        }
        String cloudsValue = "";
        if ((current.getClouds() != null)
                && (current.getClouds().getAll() != null)) {
            final double conversion = (Double) current.getClouds().getAll();
            cloudsValue = numberFormatter.format(conversion);
        }
        String snowValue = "";
        if ((current.getSnow() != null)
                && (current.getSnow().get3h() != null)) {
            final double conversion = (Double) current.getSnow().get3h();
            snowValue = numberFormatter.format(conversion);
        }
        String feelsLike = "";
        if (current.getMain().getTemp() != null) {
            double conversion = (Double) current.getMain().getTemp();
            conversion = tempUnitsConversor.doConversion(conversion);
            feelsLike = numberFormatter.format(conversion);
        }
        String sunRiseTime = "";
        if (current.getSys().getSunrise() != null) {
            final long unixTime = (Long) current.getSys().getSunrise();
            final Date unixDate = new Date(unixTime * 1000L);
            sunRiseTime = dateFormat.format(unixDate);
        }
        String sunSetTime = "";
        if (current.getSys().getSunset() != null) {
            final long unixTime = (Long) current.getSys().getSunset();
            final Date unixDate = new Date(unixTime * 1000L);
            sunSetTime = dateFormat.format(unixDate);
        }


        // 4. Update UI.
        final TextView tempMaxView = (TextView) getActivity().findViewById(R.id.weather_current_temp_max);
        tempMaxView.setText(tempMax);
        final TextView tempMinView = (TextView) getActivity().findViewById(R.id.weather_current_temp_min);
        tempMinView.setText(tempMin);
        final ImageView pictureView = (ImageView) getActivity().findViewById(R.id.weather_current_picture);
        pictureView.setImageBitmap(picture);    
        
        final TextView descriptionView = (TextView) getActivity().findViewById(R.id.weather_current_description);
        descriptionView.setText(description);
        
        ((TextView) getActivity().findViewById(R.id.weather_current_humidity_value)).setText(humidityValue);
        ((TextView) getActivity().findViewById(R.id.weather_current_humidity_units)).setText(
        		this.getActivity().getApplicationContext().getString(R.string.text_units_percent));
        
        ((TextView) getActivity().findViewById(R.id.weather_current_pressure_value)).setText(pressureValue);
        ((TextView) getActivity().findViewById(R.id.weather_current_pressure_units)).setText(pressureConversor.getSymbol());
        
        ((TextView) getActivity().findViewById(R.id.weather_current_wind_value)).setText(windValue);
        ((TextView) getActivity().findViewById(R.id.weather_current_wind_units)).setText(windConversor.getSymbol());
        
        ((TextView) getActivity().findViewById(R.id.weather_current_rain_value)).setText(rainValue);
        ((TextView) getActivity().findViewById(R.id.weather_current_rain_units)).setText(
        		this.getActivity().getApplicationContext().getString(R.string.text_units_mm3h));
        
        ((TextView) getActivity().findViewById(R.id.weather_current_clouds_value)).setText(cloudsValue);
        ((TextView) getActivity().findViewById(R.id.weather_current_clouds_units)).setText(
        		this.getActivity().getApplicationContext().getString(R.string.text_units_percent));
        
        ((TextView) getActivity().findViewById(R.id.weather_current_snow_value)).setText(snowValue);
        ((TextView) getActivity().findViewById(R.id.weather_current_snow_units)).setText(
        		this.getActivity().getApplicationContext().getString(R.string.text_units_mm3h));
        
        ((TextView) getActivity().findViewById(R.id.weather_current_feelslike_value)).setText(feelsLike);
        ((TextView) getActivity().findViewById(R.id.weather_current_feelslike_units)).setText(tempUnitsConversor.getSymbol());
        
        ((TextView) getActivity().findViewById(R.id.weather_current_sunrise_value)).setText(sunRiseTime);

        ((TextView) getActivity().findViewById(R.id.weather_current_sunset_value)).setText(sunSetTime);
        
        this.getActivity().findViewById(R.id.weather_current_data_container).setVisibility(View.VISIBLE);
        this.getActivity().findViewById(R.id.weather_current_progressbar).setVisibility(View.GONE);
        this.getActivity().findViewById(R.id.weather_current_error_message).setVisibility(View.GONE);       
    }
    
    private boolean isDataFresh(final Date lastUpdate) {
    	if (lastUpdate == null) {
    		return false;
    	}
    	
    	final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
    			this.getActivity().getApplicationContext());
        final String keyPreference = this.getString(R.string.weather_preferences_refresh_interval_key);
        final String refresh = sharedPreferences.getString(
        		keyPreference,
        		this.getResources().getStringArray(R.array.weather_preferences_refresh_interval)[0]);
        final Date currentTime = new Date();
    	if (((currentTime.getTime() - lastUpdate.getTime())) < Long.valueOf(refresh)) {
    		return true;
    	}
    	
    	return false;
    }

    private static class CurrentTask extends AsyncTask<Object, Void, Current> {
    	// Store the context passed to the AsyncTask when the system instantiates it.
        private final Context localContext;
        final CustomHTTPClient HTTPClient;
        final ServiceCurrentParser weatherService;

        public CurrentTask(final Context context, final CustomHTTPClient HTTPClient,
        		final ServiceCurrentParser weatherService) {
        	this.localContext = context;
            this.HTTPClient = HTTPClient;
            this.weatherService = weatherService;
        }

        @Override
        protected Current doInBackground(final Object... params) {
        	final double latitude = (Double) params[0];
            final double longitude = (Double) params[1];
  
            Current current = null;
            try {
            	current = this.doInBackgroundThrowable(latitude, longitude);
            } catch (final JsonParseException e) {
                Timber.e(e, "CurrentTask doInBackground exception: ");
            } catch (final MalformedURLException e) {
                Timber.e(e, "CurrentTask doInBackground exception: ");
            } catch (final URISyntaxException e) {
                Timber.e(e, "CurrentTask doInBackground exception: ");
            } catch (final IOException e) {
                // logger infrastructure swallows UnknownHostException :/
                Timber.e(e, "CurrentTask doInBackground exception: " + e.getMessage());
            } catch (final Throwable e) {
                // I loathe doing this but we must show some error to our dear user by means
                // of returning Forecast null value.
                Timber.e(e, "CurrentTask doInBackground exception: ");
            }

            return current;
        }

        private Current doInBackgroundThrowable(final double latitude, final double longitude)
                        throws URISyntaxException, JsonParseException, IOException {
            final SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(localContext.getApplicationContext());
            final String APPID = sharedPreferences.getString(localContext.getString(R.string.weather_preferences_app_id_key), "");

        	final String APIVersion = localContext.getString(R.string.api_version);
            final String urlAPI = localContext.getString(R.string.uri_api_weather_today);
            String url = weatherService.createURIAPICurrent(urlAPI, APIVersion, latitude, longitude);
            if (!APPID.isEmpty()) {
                url = url.concat("&APPID=" + APPID);
            }
            final String jsonData = HTTPClient.retrieveDataAsString(new URL(url));

            return weatherService.retrieveCurrentFromJPOS(jsonData);
        }

        @Override
        protected void onPostExecute(final Current current) {

            // Call updateUI on the UI thread.
            final Intent currentData = new Intent(BROADCAST_INTENT_ACTION);
            currentData.putExtra("current", current);
            LocalBroadcastManager.getInstance(this.localContext).sendBroadcastSync(currentData);
        }
    }
}
