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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.fragment.current.CurrentFragment;
import name.gumartinm.weather.information.fragment.overview.OverviewFragment;
import name.gumartinm.weather.information.model.DatabaseQueries;
import name.gumartinm.weather.information.model.WeatherLocation;

import java.text.MessageFormat;
import java.util.Locale;


public class MainTabsActivity extends FragmentActivity {
    private static final int NUM_ITEMS = 2;
    private ViewPager mPager;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.fragment_pager);

        this.mPager = (ViewPager)this.findViewById(R.id.pager);
        this.mPager.setAdapter(new TabsAdapter(this.getSupportFragmentManager()));


        this.mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(final int position) {
                        MainTabsActivity.this.getActionBar().setSelectedNavigationItem(position);
                    }
                });


        final ActionBar actionBar = this.getActionBar();

        PreferenceManager.setDefaultValues(this, R.xml.weather_preferences, false);

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Create a tab listener that is called when the user changes tabs.
        final ActionBar.TabListener tabListener = new ActionBar.TabListener() {

            @Override
            public void onTabSelected(final Tab tab, final FragmentTransaction ft) {
                MainTabsActivity.this.mPager.setCurrentItem(tab.getPosition());

            }

            @Override
            public void onTabUnselected(final Tab tab, final FragmentTransaction ft) {

            }

            @Override
            public void onTabReselected(final Tab tab, final FragmentTransaction ft) {

            }

        };

        actionBar.addTab(actionBar.newTab().setText("CURRENTLY").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("FORECAST").setTabListener(tabListener));
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

        final ActionBar actionBar = this.getActionBar();
        
        // 1. Update title.
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

        // 2. Update forecast tab text.
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        final String keyPreference = this.getString(R.string.weather_preferences_day_forecast_key);
        final String value = sharedPreferences.getString(keyPreference, "");
        String humanValue = "";
        if (value.equals("5")) {
            humanValue = this.getString(R.string.text_tab_five_days_forecast);
        } else if (value.equals("10")) {
            humanValue = this.getString(R.string.text_tab_ten_days_forecast);
        } else if (value.equals("14")) {
            humanValue = this.getString(R.string.text_tab_fourteen_days_forecast);
        }
        actionBar.getTabAt(1).setText(humanValue);
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
