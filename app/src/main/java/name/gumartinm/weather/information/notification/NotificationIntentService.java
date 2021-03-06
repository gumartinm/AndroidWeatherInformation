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
package name.gumartinm.weather.information.notification;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.fasterxml.jackson.core.JsonParseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.activity.MainTabsActivity;
import name.gumartinm.weather.information.httpclient.CustomHTTPClient;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;
import name.gumartinm.weather.information.model.currentweather.Current;
import name.gumartinm.weather.information.parser.JPOSCurrentParser;
import name.gumartinm.weather.information.service.IconsList;
import name.gumartinm.weather.information.service.ServiceCurrentParser;
import name.gumartinm.weather.information.service.conversor.TempUnitsConversor;
import name.gumartinm.weather.information.service.conversor.UnitsConversor;
import timber.log.Timber;


public class NotificationIntentService extends IntentService {

    public NotificationIntentService() {
        super("NIS-Thread");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final DatabaseQueries query = new DatabaseQueries(this.getApplicationContext());
        final WeatherLocation weatherLocation = query.queryDataBase();
        
        if (weatherLocation != null) {
            final ServiceCurrentParser weatherService = new ServiceCurrentParser(new JPOSCurrentParser());
            final CustomHTTPClient HTTPClient = CustomHTTPClient.newInstance(this.getString(R.string.http_client_agent));

            Current current = null;
            try {
            	current = this.doInBackgroundThrowable(weatherLocation, HTTPClient, weatherService);
                
            } catch (final JsonParseException e) {
                Timber.e(e, "doInBackground exception: ");
            } catch (final MalformedURLException e) {
                Timber.e(e, "doInBackground exception: ");
            } catch (final URISyntaxException e) {
                Timber.e(e, "doInBackground exception: ");
            } catch (final IOException e) {
                // logger infrastructure swallows UnknownHostException :/
                Timber.e(e, "doInBackground exception: " + e.getMessage());
            }
            
            if (current != null) {
            	this.showNotification(current, weatherLocation);
            }
        }
    }

    private Current doInBackgroundThrowable(final WeatherLocation weatherLocation,
            final CustomHTTPClient HTTPClient, final ServiceCurrentParser weatherService)
                    throws MalformedURLException, URISyntaxException,
                    JsonParseException, IOException {
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        final String APPID = sharedPreferences.getString(this.getString(R.string.weather_preferences_app_id_key), "");

        final String APIVersion = this.getResources().getString(R.string.api_version);

        final String urlAPI = this.getResources().getString(R.string.uri_api_weather_today);
        String url = weatherService.createURIAPICurrent(urlAPI, APIVersion,
                weatherLocation.getLatitude(), weatherLocation.getLongitude());
        if (!APPID.isEmpty()) {
            url = url.concat("&APPID=" + APPID);
        }
        final String jsonData = HTTPClient.retrieveDataAsString(new URL(url));

        return weatherService.retrieveCurrentFromJPOS(jsonData);
    }
    
    private void showNotification(final Current current, final WeatherLocation weatherLocation) {
		// 1. Update units of measurement.
        final UnitsConversor tempUnitsConversor = new TempUnitsConversor(this.getApplicationContext());


        // 2. Formatters
        final DecimalFormat tempFormatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        tempFormatter.applyPattern("###.##");


        // 3. Prepare data for RemoteViews.
        String tempMax = "";
        if (current.getMain().getTemp_max() != null) {
            double conversion = (Double) current.getMain().getTemp_max();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMax = tempFormatter.format(conversion) + tempUnitsConversor.getSymbol();
        }
        String tempMin = "";
        if (current.getMain().getTemp_min() != null) {
            double conversion = (Double) current.getMain().getTemp_min();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMin = tempFormatter.format(conversion) + tempUnitsConversor.getSymbol();
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
        
        // 4. Insert data in RemoteViews.
        final RemoteViews remoteView = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.weather_notification);
        remoteView.setImageViewBitmap(R.id.weather_notification_image, picture);
        remoteView.setTextViewText(R.id.weather_notification_temperature_max, tempMax);
        remoteView.setTextViewText(R.id.weather_notification_temperature_min, tempMin);
        remoteView.setTextViewText(R.id.weather_notification_city, city);
        remoteView.setTextViewText(R.id.weather_notification_country, country);

        // 5. Activity launcher.
        final Intent resultIntent =  new Intent(this.getApplicationContext(), MainTabsActivity.class);
        // The PendingIntent to launch our activity if the user selects this notification.
        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getApplicationContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainTabsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        final PendingIntent resultPendingIntent =
        		stackBuilder.getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT
                );
        
    	final NotificationManagerCompat notificationManager =
    			NotificationManagerCompat.from(this.getApplicationContext());
    	

    	// 6. Create notification.
        final NotificationCompat.Builder notificationBuilder =
        		new NotificationCompat.Builder(this.getApplicationContext())
        		.setContent(remoteView)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        final Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Send the notification.
        // Sets an ID for the notification, so it can be updated (just in case)
        int notifyID = 1;
        notificationManager.notify(notifyID, notification);
    }
}
