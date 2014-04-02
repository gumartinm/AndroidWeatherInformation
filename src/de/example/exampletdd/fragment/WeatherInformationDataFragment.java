package de.example.exampletdd.fragment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import de.example.exampletdd.R;
import de.example.exampletdd.activityinterface.ErrorMessage;
import de.example.exampletdd.activityinterface.OnClickButtons;
import de.example.exampletdd.httpclient.WeatherHTTPClient;
import de.example.exampletdd.model.WeatherData;
import de.example.exampletdd.parser.IJPOSWeatherParser;
import de.example.exampletdd.parser.JPOSWeatherParser;
import de.example.exampletdd.service.WeatherService;

public class WeatherInformationDataFragment extends Fragment implements OnClickButtons {
    private WeatherDataAdapter mAdapter;
    private boolean isFahrenheit;


    @Override
    public View onCreateView(final LayoutInflater inflater,
            final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.weather_data_list,
                container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView listWeatherView = (ListView) this.getActivity().findViewById(
                R.id.weather_data_list_view);

        this.mAdapter = new WeatherDataAdapter(this.getActivity(),
                R.layout.weather_data_entry_list);

        final Collection<WeatherDataEntry> entries = new ArrayList<WeatherDataEntry>();
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_description), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_tem), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_tem_max), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_tem_min), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_sun_rise), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_sun_set), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_cloudiness), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_rain_time), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_rain_amount), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_wind_speed), ""));
        entries.add(new WeatherDataEntry(this.getString(R.string.text_field_humidity), ""));

        this.mAdapter.addAll(entries);
        listWeatherView.setAdapter(this.mAdapter);
    }

    @Override
    public void onClickGetWeather(final View v) {

        final IJPOSWeatherParser JPOSWeatherParser = new JPOSWeatherParser();
        final WeatherService weatherService = new WeatherService(
                JPOSWeatherParser);
        final AndroidHttpClient httpClient = AndroidHttpClient
                .newInstance("Android Weather Information Agent");
        final WeatherHTTPClient HTTPweatherClient = new WeatherHTTPClient(
                httpClient);

        final WeatherTask weatherTask = new WeatherTask(HTTPweatherClient, weatherService);


        weatherTask.execute("London,uk");
    }

    public void updateWeatherData(final WeatherData weatherData) {
        final DecimalFormat tempFormatter = new DecimalFormat("#####.#####");
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss Z");
        final double tempUnits = this.isFahrenheit ? 0 : 273.15;

        if (weatherData.getWeather() != null) {

        }

        if (weatherData.getSystem() != null) {

        }

        if (weatherData.getIconData() != null) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getActivity());

        final String unitsKey = this.getResources().getString(
                R.string.weather_preferences_units_key);
        final String units = sharedPreferences.getString(unitsKey, "");
        final String celsius = this.getResources().getString(
                R.string.weather_preferences_units_celsius);
        if (units.equals(celsius)) {
            this.isFahrenheit = false;
        } else {
            this.isFahrenheit = true;
        }
    }

    public class WeatherTask extends AsyncTask<Object, Void, WeatherData> {
        private static final String TAG = "JSONWeatherTask";
        private final WeatherHTTPClient weatherHTTPClient;
        private final WeatherService weatherService;

        public WeatherTask(final WeatherHTTPClient weatherHTTPClient,
                final WeatherService weatherService) {
            this.weatherHTTPClient = weatherHTTPClient;
            this.weatherService = weatherService;
        }

        @Override
        protected WeatherData doInBackground(final Object... params) {
            WeatherData weatherData = null;

            try {
                weatherData = this.doInBackgroundThrowable(params);
            } catch (final ClientProtocolException e) {
                Log.e(TAG, "WeatherHTTPClient exception: ", e);
            } catch (final MalformedURLException e) {
                Log.e(TAG, "Syntax URL exception: ", e);
            } catch (final URISyntaxException e) {
                Log.e(TAG, "WeatherHTTPClient exception: ", e);
            } catch (final IOException e) {
                Log.e(TAG, "WeatherHTTPClient exception: ", e);
            } catch (final JSONException e) {
                Log.e(TAG, "WeatherService exception: ", e);
            } finally {
                this.weatherHTTPClient.close();
            }

            return weatherData;
        }

        @Override
        protected void onPostExecute(final WeatherData weatherData) {
            if (weatherData != null) {
                WeatherInformationDataFragment.this.updateWeatherData(weatherData);
            } else {
                ((ErrorMessage) WeatherInformationDataFragment.this.getActivity())
                .createErrorDialog(R.string.error_dialog_generic_error);
            }

            this.weatherHTTPClient.close();
        }

        @Override
        protected void onCancelled(final WeatherData weatherData) {
            this.onCancelled();
            ((ErrorMessage) WeatherInformationDataFragment.this.getActivity())
            .createErrorDialog(R.string.error_dialog_connection_tiemout);

            this.weatherHTTPClient.close();
        }

        private WeatherData doInBackgroundThrowable(final Object... params)
                throws ClientProtocolException, MalformedURLException,
                URISyntaxException, IOException, JSONException {
            final String cityCountry = (String) params[0];
            final String urlAPICity = WeatherInformationDataFragment.this.getResources()
                    .getString(R.string.uri_api_city);
            final String APIVersion = WeatherInformationDataFragment.this.getResources()
                    .getString(R.string.api_version);
            String url = this.weatherService.createURIAPICityCountry(
                    cityCountry, urlAPICity, APIVersion);


            final String jsonData = this.weatherHTTPClient.retrieveJSONDataFromAPI(new URL(url));


            final WeatherData weatherData = this.weatherService.retrieveWeather(jsonData);


            final String icon = weatherData.getWeather().getIcon();
            final String urlAPIicon = WeatherInformationDataFragment.this
                    .getResources().getString(R.string.uri_api_icon);
            url = this.weatherService.createURIAPIicon(icon, urlAPIicon);
            final byte[] iconData = this.weatherHTTPClient
                    .retrieveDataFromAPI(new URL(url)).toByteArray();
            weatherData.setIconData(iconData);


            return weatherData;
        }
    }
}
