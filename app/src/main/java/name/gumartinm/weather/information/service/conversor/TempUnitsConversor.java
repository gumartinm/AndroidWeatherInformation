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
package name.gumartinm.weather.information.service.conversor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import name.gumartinm.weather.information.R;

public class TempUnitsConversor implements UnitsConversor {
    private final Context context;

    public TempUnitsConversor(final Context context) {
        this.context = context;
    }

    @Override
    public String getSymbol() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String keyPreference = context.getString(R.string.weather_preferences_temperature_key);
        final String[] values = context.getResources().getStringArray(R.array.weather_preferences_temperature);
        final String unitsPreferenceValue = sharedPreferences.getString(
                keyPreference, context.getString(R.string.weather_preferences_temperature_celsius));
        String tempSymbol;
        if (unitsPreferenceValue.equals(values[0])) {
            tempSymbol = values[0];
        } else if (unitsPreferenceValue.equals(values[1])) {
            tempSymbol = values[1];
        } else {
            tempSymbol = values[2];
        }

        return tempSymbol;
    }

    @Override
    public double doConversion(double value) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String keyPreference = context.getString(R.string.weather_preferences_temperature_key);
        final String[] values = context.getResources().getStringArray(R.array.weather_preferences_temperature);
        final String unitsPreferenceValue = sharedPreferences.getString(
                keyPreference, context.getString(R.string.weather_preferences_temperature_celsius));

        double convertedTempUnits;
        if (unitsPreferenceValue.equals(values[0])) {
            convertedTempUnits = value - 273.15;
        } else if (unitsPreferenceValue.equals(values[1])) {
            convertedTempUnits = (value * 1.8) - 459.67;
        } else {
            convertedTempUnits = value;
        }

        return convertedTempUnits;
    }
}
