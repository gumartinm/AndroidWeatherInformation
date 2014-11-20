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
package name.gumartinm.weather.information.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

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

import org.apache.http.client.ClientProtocolException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class WidgetIntentService extends IntentService {
	private static final String TAG = "WidgetIntentService";
    private static final String WIDGET_PREFERENCES_NAME = "WIDGET_PREFERENCES";

	public WidgetIntentService() {
		super("WIS-Thread");
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		final boolean isRefreshAppWidget = intent.getBooleanExtra("refreshAppWidget", false);

		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			// Nothing to do. Something went wrong.
			return;
		}


		final DatabaseQueries query = new DatabaseQueries(this.getApplicationContext());
		final WeatherLocation weatherLocation = query.queryDataBase();
		
		if (weatherLocation == null) {
			// Nothing to do. Show error.
			final RemoteViews view = this.makeViewOnError(appWidgetId);
			this.updateWidget(view, appWidgetId);
			return;
		}

		if (!isRefreshAppWidget) {
            final RemoteViews view = this.makeViewOnNotRefresh(weatherLocation, appWidgetId);
            this.updateWidget(view, appWidgetId);
		} else {
            final RemoteViews view = this.makeViewOnRefresh(weatherLocation,appWidgetId);
            this.updateWidget(view, appWidgetId);
		}
	}

    public static void deleteWidgetCurrentData(final Context context, final int appWidgetId) {
        final PermanentStorage store = new PermanentStorage(context.getApplicationContext());

        store.removeWidgetCurrentData(appWidgetId);
    }

    private RemoteViews makeViewOnError(final int appWidgetId) {
        final RemoteViews view = this.makeErrorView(appWidgetId);

        final PermanentStorage store = new PermanentStorage(this.getApplicationContext());
        store.removeWidgetCurrentData(appWidgetId);

        return view;
    }

    private RemoteViews makeViewOnNotRefresh(final WeatherLocation weatherLocation, final int appWidgetId) {
        RemoteViews view;

        final PermanentStorage store = new PermanentStorage(this.getApplicationContext());
        final Current current = store.getWidgetCurrentData(appWidgetId);
        if (current != null) {
            // Update UI.
            view = this.makeView(current, weatherLocation, appWidgetId);
        } else {
            // Show error.
            view = this.makeErrorView(appWidgetId);

            store.removeWidgetCurrentData(appWidgetId);
        }

        return view;
    }

    private RemoteViews makeViewOnRefresh(final WeatherLocation weatherLocation, final int appWidgetId) {
        RemoteViews view;

        final PermanentStorage store = new PermanentStorage(this.getApplicationContext());
        final Current current = this.getRemoteCurrent(weatherLocation);
        if (current != null) {
            // Update UI.
            view = this.makeView(current, weatherLocation, appWidgetId);

            store.saveWidgetCurrentData(current, appWidgetId);
        } else {
            // Show error.
            view = this.makeErrorView(appWidgetId);

            store.removeWidgetCurrentData(appWidgetId);
        }

        return view;
    }

	private Current getRemoteCurrent(final WeatherLocation weatherLocation) {

		final ServiceCurrentParser weatherService = new ServiceCurrentParser(new JPOSCurrentParser());
		final CustomHTTPClient HTTPClient = new CustomHTTPClient(
				AndroidHttpClient.newInstance(this.getString(R.string.http_client_agent)));

		try {
			return this.getRemoteCurrentThrowable(weatherLocation, HTTPClient, weatherService);

		} catch (final JsonParseException e) {
			Log.e(TAG, "doInBackground exception: ", e);
		} catch (final ClientProtocolException e) {
			Log.e(TAG, "doInBackground exception: ", e);
		} catch (final MalformedURLException e) {
			Log.e(TAG, "doInBackground exception: ", e);
		} catch (final URISyntaxException e) {
			Log.e(TAG, "doInBackground exception: ", e);
		} catch (final IOException e) {
			// logger infrastructure swallows UnknownHostException :/
			Log.e(TAG, "doInBackground exception: " + e.getMessage(), e);
		} finally {
			HTTPClient.close();
		}

		return null;
	}

	private Current getRemoteCurrentThrowable(final WeatherLocation weatherLocation,
			final CustomHTTPClient HTTPClient, final ServiceCurrentParser weatherService)
					throws ClientProtocolException, MalformedURLException, URISyntaxException,
					JsonParseException, IOException {

		final String APIVersion = this.getResources().getString(R.string.api_version);

		final String urlAPI = this.getResources().getString(R.string.uri_api_weather_today);
		final String url = weatherService.createURIAPICurrent(urlAPI, APIVersion,
				weatherLocation.getLatitude(), weatherLocation.getLongitude());
		final String urlWithoutCache = url.concat("&time=" + System.currentTimeMillis());
		final String jsonData = HTTPClient.retrieveDataAsString(new URL(urlWithoutCache));

		return weatherService.retrieveCurrentFromJPOS(jsonData);
	}

    private interface UnitsConversor {
    	
    	public double doConversion(final double value);
    }
    
	private RemoteViews makeView(final Current current, final WeatherLocation weatherLocation, final int appWidgetId) {

		// 1. Update units of measurement.

        UnitsConversor tempUnitsConversor;
        String keyPreference = this.getApplicationContext().getString(R.string.widget_preferences_temperature_units_key);
        String realKeyPreference = keyPreference + "_" + appWidgetId;
        // What was saved to permanent storage (or default values if it is the first time)
        final int tempValue = this.getSharedPreferences(WIDGET_PREFERENCES_NAME, Context.MODE_PRIVATE).getInt(realKeyPreference, 0);
        final String tempSymbol = this.getResources().getStringArray(R.array.weather_preferences_temperature)[tempValue];
        if (tempValue == 0) {
        	tempUnitsConversor = new UnitsConversor(){

				@Override
				public double doConversion(final double value) {
					return value - 273.15;
				}

        	};
        } else if (tempValue == 1) {
        	tempUnitsConversor = new UnitsConversor(){

				@Override
				public double doConversion(final double value) {
					return (value * 1.8) - 459.67;
				}

        	};
        } else {
        	tempUnitsConversor = new UnitsConversor(){

				@Override
				public double doConversion(final double value) {
					return value;
				}

        	};
        }


        // 2. Update country.
        keyPreference = this.getApplicationContext().getString(R.string.widget_preferences_country_switch_key);
        realKeyPreference = keyPreference + "_" + appWidgetId;
        // What was saved to permanent storage (or default values if it is the first time)
        final boolean isCountry = this.getSharedPreferences(WIDGET_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(realKeyPreference, false);


		// 3. Formatters
		final DecimalFormat tempFormatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
		tempFormatter.applyPattern("#####.#####");


		// 4. Prepare data for RemoteViews.
		String tempMax = "";
		if (current.getMain().getTemp_max() != null) {
			double conversion = (Double) current.getMain().getTemp_max();
			conversion = tempUnitsConversor.doConversion(conversion);
			tempMax = tempFormatter.format(conversion) + tempSymbol;
		}
		String tempMin = "";
		if (current.getMain().getTemp_min() != null) {
			double conversion = (Double) current.getMain().getTemp_min();
			conversion = tempUnitsConversor.doConversion(conversion);
			tempMin = tempFormatter.format(conversion) + tempSymbol;
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
		final String city = weatherLocation.getCity();
		final String country = weatherLocation.getCountry();

		// 5. Insert data in RemoteViews.
		final RemoteViews remoteView = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.appwidget);
		remoteView.setImageViewBitmap(R.id.weather_appwidget_image, picture);
		remoteView.setTextViewText(R.id.weather_appwidget_temperature_max, tempMax);
		remoteView.setTextViewText(R.id.weather_appwidget_temperature_min, tempMin);
		remoteView.setTextViewText(R.id.weather_appwidget_city, city);
        if (!isCountry) {
            remoteView.setViewVisibility(R.id.weather_appwidget_country, View.GONE);
        } else {
            // TODO: It is as if Android had a view cache. If I did not set VISIBLE value,
            // the country field would be gone forever... :/
            remoteView.setViewVisibility(R.id.weather_appwidget_country, View.VISIBLE);
            remoteView.setTextViewText(R.id.weather_appwidget_country, country);
        }


		// 6. Activity launcher.
		final Intent resultIntent =  new Intent(this.getApplicationContext(), WidgetConfigure.class);
		resultIntent.putExtra("actionFromUser", true);
		resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// From: http://stackoverflow.com/questions/4011178/multiple-instances-of-widget-only-updating-last-widget
		final Uri data = Uri.withAppendedPath(Uri.parse("PAIN" + "://widget/id/") ,String.valueOf(appWidgetId));
		resultIntent.setData(data);

		final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getApplicationContext());
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(WidgetConfigure.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		final PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		remoteView.setOnClickPendingIntent(R.id.weather_appwidget, resultPendingIntent);
		
		return remoteView;
	}
	
	private RemoteViews makeErrorView(final int appWidgetId) {
		final RemoteViews remoteView = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.appwidget_error);

		// 6. Activity launcher.
		final Intent resultIntent =  new Intent(this.getApplicationContext(), WidgetConfigure.class);
		resultIntent.putExtra("actionFromUser", true);
		resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// From: http://stackoverflow.com/questions/4011178/multiple-instances-of-widget-only-updating-last-widget
		final Uri data = Uri.withAppendedPath(Uri.parse("PAIN" + "://widget/id/") ,String.valueOf(appWidgetId));
		resultIntent.setData(data);

		final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getApplicationContext());
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(WidgetConfigure.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		final PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
        remoteView.setOnClickPendingIntent(R.id.weather_appwidget_error, resultPendingIntent);

		return remoteView;
	}

	private void updateWidget(final RemoteViews remoteView, final int appWidgetId) {
		
		final AppWidgetManager manager = AppWidgetManager.getInstance(this.getApplicationContext());
		manager.updateAppWidget(appWidgetId, remoteView);
	}
}
