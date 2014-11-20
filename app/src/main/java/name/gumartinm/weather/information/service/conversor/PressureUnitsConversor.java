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

public class PressureUnitsConversor implements UnitsConversor {
    private final Context context;

    public PressureUnitsConversor(final Context context) {
        this.context = context;
    }

    @Override
    public String getSymbol() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String keyPreference = context.getString(R.string.weather_preferences_pressure_key);
        final String[] values = context.getResources().getStringArray(R.array.weather_preferences_pressure);
        final String unitsPreferenceValue = sharedPreferences.getString(
                keyPreference, context.getString(R.string.weather_preferences_pressure_pascal));

        String pressureSymbol;
        if (unitsPreferenceValue.equals(values[0])) {
            pressureSymbol = values[0];

        } else {
            pressureSymbol = values[1];
        }

        return pressureSymbol;
    }

    @Override
    public double doConversion(final double value) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String keyPreference = context.getString(R.string.weather_preferences_pressure_key);
        final String[] values = context.getResources().getStringArray(R.array.weather_preferences_pressure);
        final String unitsPreferenceValue = sharedPreferences.getString(
                keyPreference, context.getString(R.string.weather_preferences_pressure_pascal));

        double convertedPressureUnits;
        if (unitsPreferenceValue.equals(values[0])) {
            convertedPressureUnits = value;
        } else {
            convertedPressureUnits = value / 113.25d;
        }

        return convertedPressureUnits;
    }
}
