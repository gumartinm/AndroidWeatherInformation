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
package name.gumartinm.weather.information.fragment.specific;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.model.forecastweather.Forecast;
import name.gumartinm.weather.information.service.IconsList;
import name.gumartinm.weather.information.service.PermanentStorage;
import name.gumartinm.weather.information.service.conversor.PressureUnitsConversor;
import name.gumartinm.weather.information.service.conversor.TempUnitsConversor;
import name.gumartinm.weather.information.service.conversor.UnitsConversor;
import name.gumartinm.weather.information.service.conversor.WindUnitsConversor;


public class SpecificFragment extends Fragment {
    private int mChosenDay;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AppCompatActivity appCompatActivity = (AppCompatActivity)this.getActivity();
        final ActionBar actionBar = appCompatActivity.getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_launcher);
        
        final Bundle extras = this.getActivity().getIntent().getExtras();

        if (extras != null) {
        	// handset layout
            this.mChosenDay = extras.getInt("CHOSEN_DAY", 0);
        } else {
        	// tablet layout
        	// Always 0 when tablet layout (by default shows the first day)
            this.mChosenDay = 0;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    
    	// Inflate the layout for this fragment
        return inflater.inflate(R.layout.weather_specific_fragment, container, false);
    }
    
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
        	// Restore UI state
            final Forecast forecast = (Forecast) savedInstanceState.getSerializable("Forecast");

            if (forecast != null) {
            	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
            	store.saveForecast(forecast);
            }

            this.mChosenDay = savedInstanceState.getInt("mChosenDay");
        }

        this.setHasOptionsMenu(false);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {

        // Save UI state
    	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Forecast forecast = store.getForecast();

        if (forecast != null) {
            savedInstanceState.putSerializable("Forecast", forecast);
        }

        savedInstanceState.putInt("mChosenDay", this.mChosenDay);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * This method is used by tablet layout.
     * 
     * @param chosenDay
     */
    public void updateUIByChosenDay(final int chosenDay) {
    	final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Forecast forecast = store.getForecast();

        if (forecast != null) {
            this.updateUI(forecast, chosenDay);
        }
    }

    private void updateUI(final Forecast forecastWeatherData, final int chosenDay) {

        // 1. Update units of measurement.
        final UnitsConversor tempUnitsConversor = new TempUnitsConversor(this.getActivity().getApplicationContext());
        final UnitsConversor windConversor = new WindUnitsConversor(this.getActivity().getApplicationContext());
        final UnitsConversor pressureConversor = new PressureUnitsConversor(this.getActivity().getApplicationContext());


        // 2. Formatters
        final DecimalFormat numberFormatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        numberFormatter.applyPattern("###.##");
        

        // 3. Prepare data for UI.
        final name.gumartinm.weather.information.model.forecastweather.List forecast = forecastWeatherData
                .getList().get((chosenDay));

        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE - MMM d", Locale.US);
        final Calendar calendar = Calendar.getInstance();
        final Long forecastUNIXDate = (Long) forecast.getDt();
        calendar.setTimeInMillis(forecastUNIXDate * 1000L);
        final Date date = calendar.getTime();     

        String tempMax = "";
        if (forecast.getTemp().getMax() != null) {
            double conversion = (Double) forecast.getTemp().getMax();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMax = numberFormatter.format(conversion) + tempUnitsConversor.getSymbol();
        }        
        String tempMin = "";
        if (forecast.getTemp().getMin() != null) {
            double conversion = (Double) forecast.getTemp().getMin();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMin = numberFormatter.format(conversion) + tempUnitsConversor.getSymbol();
        }
        Bitmap picture;
        if ((forecast.getWeather().size() > 0) && (forecast.getWeather().get(0).getIcon() != null)
                && (IconsList.getIcon(forecast.getWeather().get(0).getIcon()) != null)) {
            final String icon = forecast.getWeather().get(0).getIcon();
            picture = BitmapFactory.decodeResource(this.getResources(), IconsList.getIcon(icon)
                    .getResourceDrawable());
        } else {
            picture = BitmapFactory.decodeResource(this.getResources(),
                    R.drawable.weather_severe_alert);
        }

        String description = this.getString(R.string.text_field_description_when_error);
        if (forecast.getWeather().size() > 0) {
            description = forecast.getWeather().get(0).getDescription();
        }

        String humidityValue = "";
        if (forecast.getHumidity() != null) {
            final double conversion = (Double) forecast.getHumidity();
            humidityValue = numberFormatter.format(conversion);
        }        
        String pressureValue = "";
        if (forecast.getPressure() != null) {
            double conversion = (Double) forecast.getPressure();
            conversion = pressureConversor.doConversion(conversion);
            pressureValue = numberFormatter.format(conversion);
        }
        String windValue = "";
        if (forecast.getSpeed() != null) {
            double conversion = (Double) forecast.getSpeed();
            conversion = windConversor.doConversion(conversion);
            windValue = numberFormatter.format(conversion);
        }
        String rainValue = "";
        if (forecast.getRain() != null) {
            final double conversion = (Double) forecast.getRain();
            rainValue = numberFormatter.format(conversion);
        }
        String cloudsValue = "";
        if (forecast.getRain() != null) {
            final double conversion = (Double) forecast.getClouds();
            cloudsValue = numberFormatter.format(conversion);
        }

        final String tempSymbol = tempUnitsConversor.getSymbol();
        String tempDay = "";
        if (forecast.getTemp().getDay() != null) {
            double conversion = (Double) forecast.getTemp().getDay();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempDay = numberFormatter.format(conversion) + tempSymbol;
        }
        String tempMorn = "";
        if (forecast.getTemp().getMorn() != null) {
            double conversion = (Double) forecast.getTemp().getMorn();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempMorn = numberFormatter.format(conversion) + tempSymbol;
        }
        String tempEve = "";
        if (forecast.getTemp().getEve() != null) {
            double conversion = (Double) forecast.getTemp().getEve();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempEve = numberFormatter.format(conversion) + tempSymbol;
        }   
        String tempNight = "";
        if (forecast.getTemp().getNight() != null) {
            double conversion = (Double) forecast.getTemp().getNight();
            conversion = tempUnitsConversor.doConversion(conversion);
            tempNight = numberFormatter.format(conversion) + tempSymbol;
        }   


        // 4. Update UI.
        final AppCompatActivity appCompatActivity = (AppCompatActivity)this.getActivity();
        appCompatActivity.getSupportActionBar().setSubtitle(dayFormatter.format(date).toUpperCase());
        
        final TextView tempMaxView = (TextView) getActivity().findViewById(R.id.weather_specific_temp_max);
        tempMaxView.setText(tempMax);
        final TextView tempMinView = (TextView) getActivity().findViewById(R.id.weather_specific_temp_min);
        tempMinView.setText(tempMin);
        final ImageView pictureView = (ImageView) getActivity().findViewById(R.id.weather_specific_picture);
        pictureView.setImageBitmap(picture);    
        
        final TextView descriptionView = (TextView) getActivity().findViewById(R.id.weather_specific_description);
        descriptionView.setText(description);
        
        final TextView humidityValueView = (TextView) getActivity().findViewById(R.id.weather_specific_humidity_value);
        humidityValueView.setText(humidityValue);
        ((TextView) getActivity().findViewById(R.id.weather_specific_pressure_value)).setText(pressureValue);
        ((TextView) getActivity().findViewById(R.id.weather_specific_pressure_units)).setText(pressureConversor.getSymbol());
        ((TextView) getActivity().findViewById(R.id.weather_specific_wind_value)).setText(windValue);
        ((TextView) getActivity().findViewById(R.id.weather_specific_wind_units)).setText(windConversor.getSymbol());
        final TextView rainValueView = (TextView) getActivity().findViewById(R.id.weather_specific_rain_value);
        rainValueView.setText(rainValue);
        final TextView cloudsValueView = (TextView) getActivity().findViewById(R.id.weather_specific_clouds_value);
        cloudsValueView.setText(cloudsValue); 
        
        final TextView tempDayView = (TextView) getActivity().findViewById(R.id.weather_specific_day_temperature);
        tempDayView.setText(tempDay);
        final TextView tempMornView = (TextView) getActivity().findViewById(R.id.weather_specific_morn_temperature);
        tempMornView.setText(tempMorn);
        final TextView tempEveView = (TextView) getActivity().findViewById(R.id.weather_specific_eve_temperature);
        tempEveView.setText(tempEve);
        final TextView tempNightView = (TextView) getActivity().findViewById(R.id.weather_specific_night_temperature);
        tempNightView.setText(tempNight);
    }

    @Override
    public void onResume() {
        super.onResume();

        final PermanentStorage store = new PermanentStorage(this.getActivity().getApplicationContext());
        final Forecast forecast = store.getForecast();

        if (forecast != null) {
            this.updateUI(forecast, this.mChosenDay);
        }
    }
}
