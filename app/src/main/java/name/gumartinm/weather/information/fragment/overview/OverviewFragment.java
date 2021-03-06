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
package name.gumartinm.weather.information.fragment.overview;

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
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;

import com.fasterxml.jackson.core.JsonParseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.activity.SpecificActivity;
import name.gumartinm.weather.information.fragment.specific.SpecificFragment;
import name.gumartinm.weather.information.httpclient.CustomHTTPClient;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;
import name.gumartinm.weather.information.model.forecastweather.Forecast;
import name.gumartinm.weather.information.parser.JPOSForecastParser;
import name.gumartinm.weather.information.service.IconsList;
import name.gumartinm.weather.information.service.PermanentStorage;
import name.gumartinm.weather.information.service.ServiceForecastParser;
import name.gumartinm.weather.information.service.conversor.TempUnitsConversor;
import name.gumartinm.weather.information.service.conversor.UnitsConversor;
import timber.log.Timber;

public class OverviewFragment extends ListFragment {
    private static final String TAG = "OverviewFragment";
    private static final String BROADCAST_INTENT_ACTION = "name.gumartinm.weather.information.UPDATEFORECAST";
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView listWeatherView = this.getListView();
        listWeatherView.setChoiceMode(ListView.CHOICE_MODE_NONE);

        if (savedInstanceState != null) {
            // Restore UI state
            final Forecast forecast = (Forecast) savedInstanceState.getSerializable("Forecast");

            if (forecast != null) {
            	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
            	store.saveForecast(forecast);
            }
        }

        this.setHasOptionsMenu(false);

        this.setEmptyText(this.getString(R.string.text_field_remote_error));
        this.setListShownNoAnimation(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        this.mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(final Context context, final Intent intent) {
				final String action = intent.getAction();
				if (action.equals(BROADCAST_INTENT_ACTION)) {
					final Forecast forecastRemote = (Forecast) intent.getSerializableExtra("forecast");

				    // 1. Check conditions. They must be the same as the ones that triggered the AsyncTask.
					final DatabaseQueries query = new DatabaseQueries(context.getApplicationContext());
			        final WeatherLocation weatherLocation = query.queryDataBase();
			        final PermanentStorage store = new PermanentStorage(context.getApplicationContext());
			        final Forecast forecast = store.getForecast();

				    if (forecast == null || !OverviewFragment.this.isDataFresh(weatherLocation.getLastForecastUIUpdate())) {

                        if (forecastRemote != null) {
				            // 2. Update UI.
				            OverviewFragment.this.updateUI(forecastRemote);

				        	// 3. Update Data.
				        	store.saveForecast(forecastRemote);
				            weatherLocation.setLastForecastUIUpdate(new Date());
				            query.updateDataBase(weatherLocation);

				            // 4. Show list.
				            OverviewFragment.this.setListShownNoAnimation(true);
                        } else {
                            // Empty list and show error message (see setEmptyText in onCreate)
                            OverviewFragment.this.setListAdapter(null);
                            OverviewFragment.this.setListShownNoAnimation(true);
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

        final DatabaseQueries query = new DatabaseQueries(this.getActivity().getApplicationContext());
        final WeatherLocation weatherLocation = query.queryDataBase();
        if (weatherLocation == null) {
            // Nothing to do.
        	// Empty list and show error message (see setEmptyText in onCreate)
			this.setListAdapter(null);
			this.setListShownNoAnimation(true);
            return;
        }

        final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Forecast forecast = store.getForecast();

        if (forecast != null && this.isDataFresh(weatherLocation.getLastForecastUIUpdate())) {
            this.updateUI(forecast);
        } else {
            // Load remote data (aynchronous)
            // Gets the data from the web.
            this.setListShownNoAnimation(false);
            final OverviewTask task = new OverviewTask(
            		this.getActivity().getApplicationContext(),
                    CustomHTTPClient.newInstance(this.getString(R.string.http_client_agent)),
                    new ServiceForecastParser(new JPOSForecastParser()));

            task.execute(weatherLocation.getLatitude(), weatherLocation.getLongitude());
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {

        // Save UI state
    	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Forecast forecast = store.getForecast();

        if (forecast != null) {
            savedInstanceState.putSerializable("Forecast", forecast);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext()).unregisterReceiver(this.mReceiver);

        super.onPause();
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        final SpecificFragment fragment = (SpecificFragment) this
                .getFragmentManager().findFragmentById(R.id.weather_specific_fragment);
        if (fragment == null) {
            // handset layout
            final Intent intent = new Intent(this.getActivity().getApplicationContext(), SpecificActivity.class);
            intent.putExtra("CHOSEN_DAY", (int) id);
            OverviewFragment.this.getActivity().startActivity(intent);
        } else {
            // tablet layout
            fragment.updateUIByChosenDay((int) id);
        }
    }
    
    private void updateUI(final Forecast forecastWeatherData) {

        // 1. Update units of measurement.
        final UnitsConversor tempUnitsConversor = new TempUnitsConversor(this.getActivity().getApplicationContext());

        // 2. Update number day forecast.
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getActivity().getApplicationContext());
        final String keyPreference = this.getResources().getString(R.string.weather_preferences_day_forecast_key);
        final String dayForecast = sharedPreferences.getString(keyPreference, "5");
        final int mDayForecast = Integer.valueOf(dayForecast);


        // 3. Formatters
        final DecimalFormat tempFormatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        tempFormatter.applyPattern("###.##");
        final SimpleDateFormat dayNameFormatter = new SimpleDateFormat("EEE", Locale.US);
        final SimpleDateFormat monthAndDayNumberormatter = new SimpleDateFormat("MMM d", Locale.US);


        // 4. Prepare data for UI.
        final List<OverviewEntry> entries = new ArrayList<OverviewEntry>();
        final OverviewAdapter adapter = new OverviewAdapter(this.getActivity(),
                R.layout.weather_main_entry_list);
        final Calendar calendar = Calendar.getInstance();
        int count = mDayForecast;
        for (final name.gumartinm.weather.information.model.forecastweather.List forecast : forecastWeatherData
                .getList()) {

            Bitmap picture;

            if ((forecast.getWeather().size() > 0) &&
                    (forecast.getWeather().get(0).getIcon() != null) &&
                    (IconsList.getIcon(forecast.getWeather().get(0).getIcon()) != null)) {
                final String icon = forecast.getWeather().get(0).getIcon();
                picture = BitmapFactory.decodeResource(this.getResources(), IconsList.getIcon(icon)
                        .getResourceDrawable());
            } else {
                picture = BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.weather_severe_alert);
            }

            final Long forecastUNIXDate = (Long) forecast.getDt();
            calendar.setTimeInMillis(forecastUNIXDate * 1000L);
            final Date dayTime = calendar.getTime();
            final String dayTextName = dayNameFormatter.format(dayTime);
            final String monthAndDayNumberText = monthAndDayNumberormatter.format(dayTime);

            Double maxTemp = null;
            if (forecast.getTemp().getMax() != null) {
                maxTemp = (Double) forecast.getTemp().getMax();
                maxTemp = tempUnitsConversor.doConversion(maxTemp);
            }

            Double minTemp = null;
            if (forecast.getTemp().getMin() != null) {
                minTemp = (Double) forecast.getTemp().getMin();
                minTemp = tempUnitsConversor.doConversion(minTemp);
            }

            if ((maxTemp != null) && (minTemp != null)) {
                // TODO i18n?
                final String tempSymbol = tempUnitsConversor.getSymbol();
                entries.add(new OverviewEntry(dayTextName, monthAndDayNumberText,
                        tempFormatter.format(maxTemp) + tempSymbol, tempFormatter.format(minTemp) + tempSymbol,
                        picture));
            }

            count = count - 1;
            if (count == 0) {
                break;
            }
        }


        // 5. Update UI.
        adapter.addAll(entries);
        this.setListAdapter(adapter);
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

    private static class OverviewTask extends AsyncTask<Object, Void, Forecast> {
    	// Store the context passed to the AsyncTask when the system instantiates it.
        private final Context localContext;
        private final CustomHTTPClient HTTPClient;
        private final ServiceForecastParser weatherService;

        public OverviewTask(final Context context, final CustomHTTPClient HTTPClient,
        		final ServiceForecastParser weatherService) {
        	this.localContext = context;
            this.HTTPClient = HTTPClient;
            this.weatherService = weatherService;
        }
        
        @Override
        protected Forecast doInBackground(final Object... params) {
            final double latitude = (Double) params[0];
            final double longitude = (Double) params[1];

            Forecast forecast = null;

            try {
                forecast = this.doInBackgroundThrowable(latitude, longitude);
            } catch (final JsonParseException e) {
                Timber.e(e, "OverviewTask doInBackground exception: ");
            } catch (final MalformedURLException e) {
                Timber.e(e, "OverviewTask doInBackground exception: ");
            } catch (final URISyntaxException e) {
                Timber.e(e, "OverviewTask doInBackground exception: ");
            } catch (final IOException e) {
                // logger infrastructure swallows UnknownHostException :/
                Timber.e(e, "OverviewTask doInBackground exception: " + e.getMessage());
            } catch (final Throwable e) {
                // I loathe doing this but we must show some error to our dear user by means
                // of returning Forecast null value.
                Timber.e(e, "OverviewTask doInBackground exception: ");
            }

            return forecast;
        }

        private Forecast doInBackgroundThrowable(final double latitude, final double longitude)
                        throws URISyntaxException, JsonParseException, IOException {
            final SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(localContext.getApplicationContext());
            final String APPID = sharedPreferences.getString(localContext.getString(R.string.weather_preferences_app_id_key), "");

            final String APIVersion = localContext.getString(R.string.api_version);
            final String urlAPI = localContext.getString(R.string.uri_api_weather_forecast);
            String url = weatherService.createURIAPIForecast(
                    urlAPI, APIVersion, latitude, longitude,
                    localContext.getString(R.string.weather_preferences_day_forecast_fourteen_day));
            if (!APPID.isEmpty()) {
                url = url.concat("&APPID=" + APPID);
            }
            final String jsonData = HTTPClient.retrieveDataAsString(new URL(url));

            return weatherService.retrieveForecastFromJPOS(jsonData);
        }

        @Override
        protected void onPostExecute(final Forecast forecast) {
        	
            // Call updateUI on the UI thread.
        	final Intent forecastData = new Intent(BROADCAST_INTENT_ACTION);
        	forecastData.putExtra("forecast", forecast);
            LocalBroadcastManager.getInstance(this.localContext).sendBroadcastSync(forecastData);
        }
    }
}
