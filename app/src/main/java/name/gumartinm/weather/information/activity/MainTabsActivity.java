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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.text.MessageFormat;
import java.util.Locale;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.fragment.APIKeyNoticeDialogFragment;
import name.gumartinm.weather.information.fragment.current.CurrentFragment;
import name.gumartinm.weather.information.fragment.overview.OverviewFragment;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;


public class MainTabsActivity extends AppCompatActivity {
    private static final int NUM_ITEMS = 2;
    private ViewPager mPager;
    private TabLayout tabLayout;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_main_tabs);

        this.mPager = (ViewPager)this.findViewById(R.id.pager);
        this.mPager.setAdapter(new TabsAdapter(this.getSupportFragmentManager()));

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mPager);

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_launcher);

        PreferenceManager.setDefaultValues(this, R.xml.weather_preferences, false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        this.getMenuInflater().inflate(R.menu.weather_main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        super.onOptionsItemSelected(item);

        Intent intent;
        final int itemId = item.getItemId();
        if (itemId == R.id.weather_menu_settings) {
            intent = new Intent(this.getApplicationContext(), PreferencesActivity.class);
            this.startActivity(intent);
            return true;
        } else if (itemId == R.id.weather_menu_map) {
            intent = new Intent(this.getApplicationContext(), MapActivity.class);
            this.startActivity(intent);
            return true;
        } else if (itemId == R.id.weather_menu_about) {
            intent = new Intent(this.getApplicationContext(), AboutActivity.class);
            this.startActivity(intent);
            return true;
        } else {
        }

        // TODO: calling again super method?
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        final String APPID = sharedPreferences.getString(this.getString(R.string.weather_preferences_app_id_key), "");

        if (APPID.isEmpty()) {
            final FragmentManager fm = this.getSupportFragmentManager();
            final Fragment buttonsFragment = fm.findFragmentByTag("noticeDialog");
            if (buttonsFragment == null) {
                final DialogFragment newFragment = APIKeyNoticeDialogFragment.newInstance(R.string.api_id_key_notice_title);
                newFragment.setRetainInstance(true);
                newFragment.setCancelable(false);
                newFragment.show(fm, "noticeDialog");
            }
        }


        // 1. Update title.
        final ActionBar actionBar = this.getSupportActionBar();
        final DatabaseQueries query = new DatabaseQueries(this.getApplicationContext());
        final WeatherLocation weatherLocation = query.queryDataBase();
        if (weatherLocation != null) {
            final String[] array = new String[2];
            array[0] = weatherLocation.getCity();
            array[1] = weatherLocation.getCountry();
            final MessageFormat message = new MessageFormat("{0},{1}", Locale.US);
            final String cityCountry = message.format(array);
            actionBar.setTitle(cityCountry);
        } else {
        	actionBar.setTitle(this.getString(R.string.text_field_no_chosen_location));
        }

        // 2. Set currently tab text.
        final String currently = this.getString(R.string.text_tab_currently);
        tabLayout.getTabAt(0).setText(currently);

        // 3. Update forecast tab text.
        final String keyPreference = this.getString(R.string.weather_preferences_day_forecast_key);
        final String value = sharedPreferences.getString(keyPreference, "");
        String forecast = "";
        if (value.equals(this.getString(R.string.weather_preferences_day_forecast_five_day))) {
            forecast = this.getString(R.string.text_tab_five_days_forecast);
        } else if (value.equals(this.getString(R.string.weather_preferences_day_forecast_ten_day))) {
            forecast = this.getString(R.string.text_tab_ten_days_forecast);
        } else if (value.equals(this.getString(R.string.weather_preferences_day_forecast_fourteen_day))) {
            forecast = this.getString(R.string.text_tab_fourteen_days_forecast);
        }
        tabLayout.getTabAt(1).setText(forecast);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    private class TabsAdapter extends FragmentPagerAdapter {

        public TabsAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(final int position) {
            if (position == 0) {
                return new CurrentFragment();
            } else {
                return new OverviewFragment();
            }

        }
    }
}
