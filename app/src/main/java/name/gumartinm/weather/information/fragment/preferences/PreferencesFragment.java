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
package name.gumartinm.weather.information.fragment.preferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import name.gumartinm.weather.information.R;
import name.gumartinm.weather.information.notification.NotificationIntentService;

public class PreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        this.addPreferencesFromResource(R.xml.weather_preferences);
        
        
        // Temperature units
        String[] values = this.getResources().getStringArray(R.array.weather_preferences_temperature);
        String[] humanValues = this.getResources().getStringArray(R.array.weather_preferences_temperature_human_value);
        String keyPreference = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_temperature_key);
        Preference connectionPref = this.findPreference(keyPreference);
        String value = this.getPreferenceManager().getSharedPreferences()
                .getString(keyPreference, this.getString(R.string.weather_preferences_temperature_celsius));
        String humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        } else if (value.equals(values[2])) {
            humanValue = humanValues[2];
        }
        connectionPref.setSummary(humanValue);
        
        // Wind
        values = this.getResources().getStringArray(R.array.weather_preferences_wind);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_wind_human_value);
        keyPreference = this.getString(R.string.weather_preferences_wind_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences()
                .getString(keyPreference, this.getString(R.string.weather_preferences_wind_meters));
        humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        }
        connectionPref.setSummary(humanValue);

        // Pressure
        values = this.getResources().getStringArray(R.array.weather_preferences_pressure);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_pressure_human_value);
        keyPreference = this.getString(R.string.weather_preferences_pressure_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences()
                .getString(keyPreference, this.getString(R.string.weather_preferences_pressure_pascal));
        humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        }
        connectionPref.setSummary(humanValue);

        // Forecast days number
        values = this.getResources().getStringArray(R.array.weather_preferences_day_forecast);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_day_forecast_human_value);
        keyPreference = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_day_forecast_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences().getString(keyPreference, values[0]);
        humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        } else if (value.equals(values[2])) {
            humanValue = humanValues[2];
        }
        connectionPref.setSummary(humanValue);

        // Refresh interval
        values = this.getResources().getStringArray(R.array.weather_preferences_refresh_interval);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_refresh_interval_human_value);
        keyPreference = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_refresh_interval_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences().getString(keyPreference, values[0]);
        humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        } else if (value.equals(values[2])) {
            humanValue = humanValues[2];
        } else if (value.equals(values[3])) {
            humanValue = humanValues[3];
        } else if (value.equals(values[4])) {
            humanValue = humanValues[4];
        } else if (value.equals(values[5])) {
            humanValue = humanValues[5];
        }
        connectionPref.setSummary(humanValue);

        // Update Time Rate
        values = this.getResources().getStringArray(R.array.weather_preferences_update_time_rate);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_update_time_rate_human_value);
        keyPreference = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_update_time_rate_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences()
                .getString(keyPreference, values[0]);
        humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        } else if (value.equals(values[2])) {
            humanValue = humanValues[2];
        } else if (value.equals(values[3])) {
            humanValue = humanValues[3];
        } else if (value.equals(values[4])) {
            humanValue = humanValues[4];
        }
        connectionPref.setSummary(humanValue);

        // Notifications temperature units
        values = this.getResources().getStringArray(R.array.weather_preferences_temperature);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_temperature_human_value);
        keyPreference = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_notifications_temperature_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences()
                .getString(keyPreference, this.getString(R.string.weather_preferences_temperature_celsius));
        humanValue = "";
        if (value.equals(values[0])) {
            humanValue = humanValues[0];
        } else if (value.equals(values[1])) {
            humanValue = humanValues[1];
        } else if (value.equals(values[2])) {
            humanValue = humanValues[2];
        }
        connectionPref.setSummary(humanValue);

        // APPID
        keyPreference = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_app_id_key);
        connectionPref = this.findPreference(keyPreference);
        value = this.getPreferenceManager().getSharedPreferences()
                .getString(keyPreference, this.getString(R.string.weather_preferences_app_id_text_empty_key));
        if (value.isEmpty()) {
            value = this.getString(R.string.weather_preferences_app_id_text_empty_key);
        }
        connectionPref.setSummary(value);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.getPreferenceManager().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        this.getPreferenceManager().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(
            final SharedPreferences sharedPreferences, final String key) {
    	
    	// Temperature units
    	String[] values = this.getResources().getStringArray(R.array.weather_preferences_temperature);
    	String[] humanValues = this.getResources().getStringArray(R.array.weather_preferences_temperature_human_value);
        String keyValue = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_temperature_key);
        if (key.equals(keyValue)) {
        	final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
        	String humanValue = "";
        	if (value.equals(values[0])) {
        		humanValue = humanValues[0];
        	} else if (value.equals(values[1])) {
        		humanValue = humanValues[1];
        	} else if (value.equals(values[2])) {
        		humanValue = humanValues[2];
        	}

        	connectionPref.setSummary(humanValue);
        	return;
        }

        // Wind
        values = this.getResources().getStringArray(R.array.weather_preferences_wind);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_wind_human_value);
        keyValue = this.getString(R.string.weather_preferences_wind_key);
        if (key.equals(keyValue)) {
            final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
            String humanValue = "";
            if (value.equals(values[0])) {
            	humanValue = humanValues[0];
            } else if (value.equals(values[1])) {
            	humanValue = humanValues[1];
            }
        
        	connectionPref.setSummary(humanValue);
        	return;
        }

        // Pressure
        values = this.getResources().getStringArray(R.array.weather_preferences_pressure);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_pressure_human_value);
        keyValue = this.getString(R.string.weather_preferences_pressure_key);
        if (key.equals(keyValue)) {
            final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
            String humanValue = "";
            if (value.equals(values[0])) {
            	humanValue = humanValues[0];
            } else if (value.equals(values[1])) {
            	humanValue = humanValues[1];
            }
        
        	connectionPref.setSummary(humanValue);
        	return;
        }

        // Forecast days number
        values = this.getResources().getStringArray(R.array.weather_preferences_day_forecast);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_day_forecast_human_value);
        keyValue = this.getActivity().getString(
                R.string.weather_preferences_day_forecast_key);
        if (key.equals(keyValue)) {
            final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
            String humanValue = "";
            if (value.equals(values[0])) {
                humanValue = humanValues[0];
            } else if (value.equals(values[1])) {
                humanValue = humanValues[1];
            } else if (value.equals(values[2])) {
                humanValue = humanValues[2];
            }
            connectionPref.setSummary(humanValue);
            return;
        }

        // Refresh interval
        values = this.getResources().getStringArray(R.array.weather_preferences_refresh_interval);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_refresh_interval_human_value);
        keyValue = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_refresh_interval_key);
        if (key.equals(keyValue)) {
        	final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
            String humanValue = "";
            if (value.equals(values[0])) {
                humanValue = humanValues[0];
            } else if (value.equals(values[1])) {
                humanValue = humanValues[1];
            } else if (value.equals(values[2])) {
                humanValue = humanValues[2];
            } else if (value.equals(values[3])) {
                humanValue = humanValues[3];
            } else if (value.equals(values[4])) {
                humanValue = humanValues[4];
            } else if (value.equals(values[5])) {
                humanValue = humanValues[5];
            }
            connectionPref.setSummary(humanValue);
            return;
        }

        // Notification switch
        values = this.getResources().getStringArray(R.array.weather_preferences_update_time_rate);
        keyValue = this.getActivity().getApplicationContext().getString(
        		R.string.weather_preferences_notifications_switch_key);
        if (key.equals(keyValue)) {
        	final SwitchPreference preference = (SwitchPreference)this.findPreference(key);
        	if (preference.isChecked())
        	{
        		keyValue = this.getActivity().getApplicationContext().getString(
                		R.string.weather_preferences_update_time_rate_key);
        		final String value = sharedPreferences.getString(keyValue, values[0]);
        		this.updateNotification(value);
        	} else {
        		this.cancelNotification();
        	}
        }
        // Update Time Rate
        values = this.getResources().getStringArray(R.array.weather_preferences_update_time_rate);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_update_time_rate_human_value);
        keyValue = this.getActivity().getApplicationContext().getString(
        		R.string.weather_preferences_update_time_rate_key);
        if (key.equals(keyValue)) {
            final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
            String humanValue = "";
            if (value.equals(values[0])) {
                humanValue = humanValues[0];
            } else if (value.equals(values[1])) {
                humanValue = humanValues[1];
            } else if (value.equals(values[2])) {
                humanValue = humanValues[2];
            } else if (value.equals(values[3])) {
                humanValue = humanValues[3];
            } else if (value.equals(values[4])) {
                humanValue = humanValues[4];
            }

            this.updateNotification(value);
            connectionPref.setSummary(humanValue);
            return;
        }

        // Temperature units
        values = this.getResources().getStringArray(R.array.weather_preferences_temperature);
        humanValues = this.getResources().getStringArray(R.array.weather_preferences_temperature_human_value);
        keyValue = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_notifications_temperature_key);
        if (key.equals(keyValue)) {
            final Preference connectionPref = this.findPreference(key);
            final String value = sharedPreferences.getString(key, values[0]);
            String humanValue = "";
            if (value.equals(values[0])) {
                humanValue = humanValues[0];
            } else if (value.equals(values[1])) {
                humanValue = humanValues[1];
            } else if (value.equals(values[2])) {
                humanValue = humanValues[2];
            }

            connectionPref.setSummary(humanValue);
            return;
        }

        // APPID
        keyValue = this.getActivity().getApplicationContext().getString(
                R.string.weather_preferences_app_id_key);
        if (key.equals(keyValue)) {
            final Preference connectionPref = this.findPreference(key);
            String value = sharedPreferences.getString(key, this.getString(R.string.weather_preferences_app_id_text_empty_key));
            if (value.isEmpty()) {
                value = this.getString(R.string.weather_preferences_app_id_text_empty_key);
            }

            connectionPref.setSummary(value);
            return;
        }
    }

    private void updateNotification(final String updateTimeRate) {
    	final String[] values = this.getResources().getStringArray(R.array.weather_preferences_update_time_rate);
        long chosenInterval = 0;
        if (updateTimeRate.equals(values[0])) {
        	chosenInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        } else if (updateTimeRate.equals(values[1])) {
        	chosenInterval = AlarmManager.INTERVAL_HALF_HOUR;
        } else if (updateTimeRate.equals(values[2])) {
        	chosenInterval = AlarmManager.INTERVAL_HOUR;
        } else if (updateTimeRate.equals(values[3])) {
        	chosenInterval = AlarmManager.INTERVAL_HALF_DAY;
        } else if (updateTimeRate.equals(values[4])) {
        	chosenInterval = AlarmManager.INTERVAL_DAY;
        }

        final AlarmManager alarmMgr =
        		(AlarmManager) this.getActivity().getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent serviceIntent =
        		new Intent(this.getActivity().getApplicationContext(), NotificationIntentService.class);
        final PendingIntent alarmIntent =
        		PendingIntent.getService(
        				this.getActivity().getApplicationContext(),
        				0,
        				serviceIntent,
        				PendingIntent.FLAG_UPDATE_CURRENT);
        if (chosenInterval != 0) {   
            alarmMgr.setInexactRepeating(
            		AlarmManager.ELAPSED_REALTIME,
            		SystemClock.elapsedRealtime(),
            		chosenInterval,
            		alarmIntent);
        }
    }

    private void cancelNotification() {
    	final AlarmManager alarmMgr =
        		(AlarmManager) this.getActivity().getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    	final Intent serviceIntent =
        		new Intent(this.getActivity().getApplicationContext(), NotificationIntentService.class);
    	final PendingIntent alarmIntent =
        		PendingIntent.getService(
        				this.getActivity().getApplicationContext(),
        				0,
        				serviceIntent,
        				PendingIntent.FLAG_UPDATE_CURRENT);
    	alarmMgr.cancel(alarmIntent);
    }
}
