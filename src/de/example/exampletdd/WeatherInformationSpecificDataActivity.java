package de.example.exampletdd;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import de.example.exampletdd.model.GeocodingData;
import de.example.exampletdd.service.ServicePersistenceStorage;

public class WeatherInformationSpecificDataActivity extends FragmentActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_specific_data);

        final ActionBar actionBar = this.getActionBar();

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public void onResume() {
        super.onResume();

        final ActionBar actionBar = this.getActionBar();

        final ServicePersistenceStorage weatherServicePersistenceFile = new ServicePersistenceStorage(this);
        final GeocodingData geocodingData = weatherServicePersistenceFile.getGeocodingData();

        if (geocodingData != null) {
            final String city = (geocodingData.getCity() == null) ? this
                    .getString(R.string.city_not_found) : geocodingData.getCity();
                    final String country = (geocodingData.getCountry() == null) ? this
                            .getString(R.string.country_not_found) : geocodingData.getCountry();
                            actionBar.setTitle(city + "," + country);

        }
    }
}
