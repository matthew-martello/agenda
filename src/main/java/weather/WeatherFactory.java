package weather;

import agenda.Config;

import geolocation.Coordinates;
import geolocation.GeoLocation;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class WeatherFactory {
  public String location;
  public String suburb = null;
  private double lat;
  private double lon;

  public WeatherFactory(String location) {
    this.location = location;

    if (Config.doGeoLocation && location != null) {
      Coordinates parseLocation = GeoLocation.getCoordinates(location);

      this.lat = parseLocation.lat;
      this.lon = parseLocation.lon;
      this.suburb = parseLocation.suburb;
    }
  }

  public static String callWeatherAPI(double lat, double lon) {
    if (!Config.doWeather) {
      System.err.println("[WARNING]: Weather disabled, check 'weather_key' in config file.");
      return null;
    }

    String key = Config.WEATHER_KEY;

    String uri = "http://api.weatherapi.com/v1/forecast.xml?key=" + key + "&q=" + lat + "," + lon;
    
    System.out.println("Calling weather API...");

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();

    HttpResponse<String> response = null;

    try {
			response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    if (response == null) {
      return null;
    } else {
      return response.body();
    }
  }

  public String[] getWeatherInfo() {
    if (!Config.doGeoLocation) {
      System.err.println("[WARNING]: Cannot get weather data because geoLocation is disabled! Check 'maps_key' in config file.");
      return null;
    }
    String response = callWeatherAPI(this.lat, this.lon);

		// Icon, Conditions, Min, Max
    String[] info = {null, null, null, null};
    if (response == null) {
      return info;
    }

    //Only get the day section of the weather data.
    String responseString = response.substring(response.indexOf("<day>"), response.indexOf("</day>") + 6);

    info[0] = responseString.substring(responseString.indexOf("<icon>") + 6, responseString.indexOf("</icon>"));
    info[1] = responseString.substring(responseString.indexOf("<text>") + 6, responseString.indexOf("</text>"));
    info[2] = responseString.substring(responseString.indexOf("<mintemp_c>") + 11, responseString.indexOf("</mintemp_c>"));
    info[3] = responseString.substring(responseString.indexOf("<maxtemp_c>") + 11, responseString.indexOf("</maxtemp_c>"));

    System.out.printf("  Conditions: %s\n  Min temp: %s\n  Max temp: %s\n", info[1], info[2], info[3]);
    return info;
  }
}
