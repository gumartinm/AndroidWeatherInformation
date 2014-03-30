package de.example.exampletdd.parser;

import org.json.JSONException;
import org.json.JSONObject;

import de.example.exampletdd.model.WeatherData;

public class JPOSWeatherParser implements IJPOSWeatherParser {

    @Override
    public WeatherData retrieveWeatherFromJPOS(final String jsonData) throws JSONException {
        final JSONObject jsonWeatherData = new JSONObject(jsonData);

        JSONObject jsonObject = jsonWeatherData.getJSONObject("coord");
        final double longitude = jsonObject.getDouble("lon");
        final double latitude = jsonObject.getDouble("lat");
        final WeatherData.Coord coord = new WeatherData.Coord(longitude,
                latitude);

        jsonObject = jsonWeatherData.getJSONObject("sys");
        final long sunRiseTime = jsonObject.getLong("sunrise");
        final long sunSetTime = jsonObject.getLong("sunset");
        final double message = jsonObject.getDouble("message");
        final String country = jsonObject.getString("country");
        final WeatherData.System system = new WeatherData.System(country,
                sunRiseTime, sunSetTime, message);

        jsonObject = jsonWeatherData.getJSONObject("weather");
        final int id = jsonObject.getInt("id");
        final String mainWeather = jsonObject.getString("main");
        final String description = jsonObject.getString("description");
        final String icon = jsonObject.getString("icon");
        final WeatherData.Weather weather = new WeatherData.Weather(id,
                mainWeather, description, icon);

        jsonObject = jsonWeatherData.getJSONObject("main");
        final double temp = jsonObject.getDouble("temp");
        final double minTemp = jsonObject.getDouble("minTemp");
        final double maxTemp = jsonObject.getDouble("maxTemp");
        final double humidity = jsonObject.getDouble("humidity");
        final double pressure = jsonObject.getDouble("pressure");
        final WeatherData.Main main = new WeatherData.Main(temp, minTemp,
                maxTemp, humidity, pressure);

        jsonObject = jsonWeatherData.getJSONObject("wind");
        final double speed = jsonObject.getDouble("speed");
        final double deg = jsonObject.getDouble("deg");
        final double gust = jsonObject.getDouble("gust");
        final double var_beg = jsonObject.getDouble("var_beg");
        final double var_end = jsonObject.getDouble("var_end");
        final WeatherData.Wind wind = new WeatherData.Wind(speed, deg, gust,
                var_beg, var_end);

        jsonObject = jsonWeatherData.getJSONObject("clouds");
        final double cloudiness = jsonObject.getDouble("all");
        final WeatherData.Clouds clouds = new WeatherData.Clouds(cloudiness);

        final double time = jsonObject.getDouble("time");
        final WeatherData.DataReceivingTime dataReceivingTime =
                new WeatherData.DataReceivingTime(time);


        return new WeatherData.Builder().setCoord(coord).setSystem(system)
                .setWeather(weather).setMain(main).setWind(wind)
                .setClouds(clouds).setDataReceivingTime(dataReceivingTime)
                .build();
    }
}
