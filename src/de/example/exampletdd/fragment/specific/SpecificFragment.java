package de.example.exampletdd.fragment.specific;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.widget.ListView;
import de.example.exampletdd.R;
import de.example.exampletdd.WeatherInformationApplication;
import de.example.exampletdd.model.forecastweather.Forecast;
import de.example.exampletdd.service.IconsList;

public class SpecificFragment extends ListFragment {
    private int mChosenDay;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
    	// I could do the same in onCreate because I allow rotations.
    	// see: http://stackoverflow.com/a/11286961
    	// Anyhow I wanted to try this way just for fun.
    	
        final Bundle extras = this.getActivity().getIntent().getExtras();

        if (extras != null) {
        	// handset layout
            this.mChosenDay = extras.getInt("CHOSEN_DAY", 0);
        } else {
        	// tablet layout
        	// Always 0 when tablet layout.
            this.mChosenDay = 0;
        }
    }
    
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView listWeatherView = this.getListView();
        listWeatherView.setChoiceMode(ListView.CHOICE_MODE_NONE);     

        if (savedInstanceState != null) {
        	// Restore UI state
            final Forecast forecast = (Forecast) savedInstanceState.getSerializable("Forecast");

            // TODO: Could it be better to store in global data forecast even if it is null value?
            //       So, perhaps do not check for null value and always store in global variable.
            if (forecast != null) {
                final WeatherInformationApplication application =
                		(WeatherInformationApplication) getActivity().getApplication();
                application.setForecast(forecast);
            }

            this.mChosenDay = savedInstanceState.getInt("Chosen day");
            // TODO: Why don't I need mListState?
        }
        
        // TODO: Why don't I need Adapter?
        
        // TODO: string static resource
        this.setEmptyText("No data available");
        // TODO: Why is it different to Current and Overview fragments?
        this.setListShownNoAnimation(false);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {

        // Save UI state
    	final WeatherInformationApplication application =
        		(WeatherInformationApplication) getActivity().getApplication();
        final Forecast forecast = application.getForecast();

        // TODO: Could it be better to save forecast data even if it is null value?
        //       So, perhaps do not check for null value.
        if (forecast != null) {
            savedInstanceState.putSerializable("Forecast", forecast);
        }

        savedInstanceState.putInt("Chosend day", this.mChosenDay);
        
        // TODO: Why don't I need mListState?

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * This method is used by tablet layout.
     * 
     * @param chosenDay
     */
    public void updateUIByChosenDay(final int chosenDay) {
        final WeatherInformationApplication application =
        		(WeatherInformationApplication) getActivity().getApplication();
        final Forecast forecast = application.getForecast();

        if (forecast != null) {
            this.updateUI(forecast, chosenDay);
        }
    }


    private void updateUI(final Forecast forecastWeatherData, final int chosenDay) {

        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getActivity());

        // TODO: repeating the same code in Overview, Specific and Current!!!
        // 1. Update units of measurement.
        boolean isFahrenheit = false;
        final String keyPreference = this.getResources().getString(
                R.string.weather_preferences_units_key);
        final String unitsPreferenceValue = sharedPreferences.getString(keyPreference, "");
        final String celsius = this.getResources().getString(
                R.string.weather_preferences_units_celsius);
        if (unitsPreferenceValue.equals(celsius)) {
            isFahrenheit = false;
        } else {
            isFahrenheit = true;
        }
        final double tempUnits = isFahrenheit ? 0 : 273.15;
        final String symbol = isFahrenheit ? "ºF" : "ºC";
    	
        
        // 2. Formatters
        final DecimalFormat tempFormatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        tempFormatter.applyPattern("#####.#####");
        

        // 3. Prepare data for UI.
        final int[] layouts = new int[4];
        layouts[0] = R.layout.weather_current_data_entry_first;
        layouts[1] = R.layout.weather_current_data_entry_second;
        layouts[2] = R.layout.weather_current_data_entry_third;
        layouts[3] = R.layout.weather_current_data_entry_fourth;
        final SpecificAdapter adapter = new SpecificAdapter(this.getActivity(), layouts);


        final de.example.exampletdd.model.forecastweather.List forecast = forecastWeatherData
                .getList().get((chosenDay));

        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE - MMM d", Locale.US);
        final Calendar calendar = Calendar.getInstance();
        final Long forecastUNIXDate = (Long) forecast.getDt();
        calendar.setTimeInMillis(forecastUNIXDate * 1000L);
        final Date date = calendar.getTime();
        this.getActivity().getActionBar().setSubtitle(dayFormatter.format(date).toUpperCase());


        String tempMax = "";
        if (forecast.getTemp().getMax() != null) {
            double conversion = (Double) forecast.getTemp().getMax();
            conversion = conversion - tempUnits;
            tempMax = tempFormatter.format(conversion) + symbol;
        }
        String tempMin = "";
        if (forecast.getTemp().getMin() != null) {
            double conversion = (Double) forecast.getTemp().getMin();
            conversion = conversion - tempUnits;
            tempMin = tempFormatter.format(conversion) + symbol;
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
        final SpecificDataEntryFirst entryFirst = new SpecificDataEntryFirst(tempMax,
                tempMin, picture);
        adapter.add(entryFirst);

        String description = "no description available";
        if (forecast.getWeather().size() > 0) {
            description = forecast.getWeather().get(0).getDescription();
        }
        final SpecificDataEntrySecond entrySecond = new SpecificDataEntrySecond(
                description);
        adapter.add(entrySecond);


        String humidityValue = "";
        if (forecast.getHumidity() != null) {
            final double conversion = (Double) forecast.getHumidity();
            humidityValue = tempFormatter.format(conversion);
        }
        String pressureValue = "";
        if (forecast.getPressure() != null) {
            final double conversion = (Double) forecast.getPressure();
            pressureValue = tempFormatter.format(conversion);
        }
        String windValue = "";
        if (forecast.getSpeed() != null) {
            final double conversion = (Double) forecast.getSpeed();
            windValue = tempFormatter.format(conversion);
        }
        String rainValue = "";
        if (forecast.getRain() != null) {
            final double conversion = (Double) forecast.getRain();
            rainValue = tempFormatter.format(conversion);
        }
        String cloudsValue = "";
        if (forecast.getRain() != null) {
            final double conversion = (Double) forecast.getClouds();
            cloudsValue = tempFormatter.format(conversion);
        }
        final SpecificDataEntryThird entryThird = new SpecificDataEntryThird(
                humidityValue, pressureValue, windValue, rainValue, cloudsValue);
        adapter.add(entryThird);

        String tempDay = "";
        if (forecast.getTemp().getDay() != null) {
            double conversion = (Double) forecast.getTemp().getDay();
            conversion = conversion - tempUnits;
            tempDay = tempFormatter.format(conversion) + symbol;
        }
        String tempMorn = "";
        if (forecast.getTemp().getMorn() != null) {
            double conversion = (Double) forecast.getTemp().getMorn();
            conversion = conversion - tempUnits;
            tempMorn = tempFormatter.format(conversion) + symbol;
        }
        String tempEve = "";
        if (forecast.getTemp().getEve() != null) {
            double conversion = (Double) forecast.getTemp().getEve();
            conversion = conversion - tempUnits;
            tempEve = tempFormatter.format(conversion) + symbol;
        }
        String tempNight = "";
        if (forecast.getTemp().getNight() != null) {
            double conversion = (Double) forecast.getTemp().getNight();
            conversion = conversion - tempUnits;
            tempNight = tempFormatter.format(conversion) + symbol;
        }
        final SpecificDataEntryFourth entryFourth = new SpecificDataEntryFourth(
                tempMorn, tempDay, tempEve, tempNight);
        adapter.add(entryFourth);


        // 4. Update UI.
        // TODO: Why am I not doing the same as in OverviewFragment?
        this.setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        final WeatherInformationApplication application =
        		(WeatherInformationApplication) getActivity().getApplication();
        final Forecast forecast = application.getForecast();

        if (forecast != null) {
            this.updateUI(forecast, this.mChosenDay);
        }
        
        // TODO: Overview is doing things with mListState... Why not here?
    }
}
