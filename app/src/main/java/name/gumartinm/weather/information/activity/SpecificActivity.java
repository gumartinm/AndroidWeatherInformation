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

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import java.text.MessageFormat;
import java.util.Locale;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;

public class SpecificActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_specific);

        final ActionBar actionBar = this.getSupportActionBar();

        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public void onResume() {
        super.onResume();

        // 1. Update title.
        final DatabaseQueries query = new DatabaseQueries(this);
        final WeatherLocation weatherLocation = query.queryDataBase();
        if (weatherLocation != null) {
        	final ActionBar actionBar = this.getSupportActionBar();
            final String[] array = new String[2];
            array[0] = weatherLocation.getCity();
            array[1] = weatherLocation.getCountry();
            final MessageFormat message = new MessageFormat("{0},{1}", Locale.US);
            final String cityCountry = message.format(array);
            actionBar.setTitle(cityCountry);
        }
    }
}
