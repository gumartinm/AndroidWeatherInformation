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
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_main_tabs);

        this.mToolbar = (Toolbar) findViewById(R.id.weather_main_toolbar);
        this.mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        this.mToolbar.setNavigationContentDescription(R.string.drawer_open);
        this.setSupportActionBar(mToolbar);

        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.weather_main_drawer_layout);
        this.mDrawerToggle = this.doDrawerToggle(this.mDrawerLayout, this.mToolbar);
        // Set the drawer toggle as the DrawerListener
        this.mDrawerLayout.addDrawerListener(mDrawerToggle);

        this.mNavigationView = (NavigationView) findViewById(R.id.weather_main_left_drawer);
        // Set the list's click listener
        this.mNavigationView.setNavigationItemSelectedListener(new NavigationViewClickListener());

        final ViewPager pager = (ViewPager)this.findViewById(R.id.pager);
        pager.setAdapter(new TabsAdapter(this.getSupportFragmentManager()));

        // Give the TabLayout the ViewPager
        this.mTabLayout = (TabLayout) findViewById(R.id.weather_main_sliding_tabs);
        this.mTabLayout.setupWithViewPager(pager);

        PreferenceManager.setDefaultValues(this, R.xml.weather_preferences, false);
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
        final DatabaseQueries query = new DatabaseQueries(this.getApplicationContext());
        final WeatherLocation weatherLocation = query.queryDataBase();
        if (weatherLocation != null) {
            final String[] array = new String[2];
            array[0] = weatherLocation.getCity();
            array[1] = weatherLocation.getCountry();
            final MessageFormat message = new MessageFormat("{0},{1}", Locale.US);
            final String cityCountry = message.format(array);
            mToolbar.setTitle(cityCountry);
        } else {
            mToolbar.setTitle(this.getString(R.string.text_field_no_chosen_location));
        }

        // 2. Set currently tab text.
        final String currently = this.getString(R.string.text_tab_currently);
        this.mTabLayout.getTabAt(0).setText(currently);

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
        this.mTabLayout.getTabAt(1).setText(forecast);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        this.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final boolean drawerOpen = this.mDrawerLayout.isDrawerOpen(this.mNavigationView);
        if (!drawerOpen) {
            this.mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private ActionBarDrawerToggle doDrawerToggle(final DrawerLayout drawerLayout, final Toolbar toolbar) {
        return new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerOpened(drawerView);
                MainTabsActivity.this.invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                MainTabsActivity.this.invalidateOptionsMenu();
            }
        };
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

    private class NavigationViewClickListener implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
            item.setChecked(false);

            final int itemId = item.getItemId();
            if (itemId == R.id.weather_menu_settings) {
                MainTabsActivity.this.mDrawerLayout.closeDrawers();
                final Intent intent = new Intent(MainTabsActivity.this.getApplicationContext(), PreferencesActivity.class);
                MainTabsActivity.this.startActivity(intent);
                return true;
            } else if (itemId == R.id.weather_menu_map) {
                MainTabsActivity.this.mDrawerLayout.closeDrawers();
                final Intent intent = new Intent(MainTabsActivity.this.getApplicationContext(), MapActivity.class);
                MainTabsActivity.this.startActivity(intent);
                return true;
            } else if (itemId == R.id.weather_menu_about) {
                MainTabsActivity.this.mDrawerLayout.closeDrawers();
                final Intent intent = new Intent(MainTabsActivity.this.getApplicationContext(), AboutActivity.class);
                MainTabsActivity.this.startActivity(intent);
                return true;
            } else {
                return false;
            }

        }
    }
}
