package geolocation;

import agenda.Config;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class GeoLocation {
  public static final String apiKey = Config.MAPS_KEY;

  public static String callMapsAPI(String location) {
    if (!Config.doGeoLocation) {
      System.err.println("[WARNING]: GeoLocation disabled, check 'maps_key' in config file.");
      return null;
    }

    String locationString = location;
    locationString = locationString.replace(" ", "+").replace("/","+");
    String call = "https://maps.googleapis.com/maps/api/geocode/json?address=" + locationString + "&key=" + apiKey;

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(call))
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
      System.err.println("[WARNING]: Response returned null.");
      return null;
    } else {
      return response.body();
    }
  }

  /**
   * Method to get the latitude and longitude for a given address or location.
   * @param location Location{@code String}. Can either be a full address, a suburb, city, etc...
   * @return A {@code Coordinates} object containing the latitude, longitude and suburb name of the location provided.
   */
  public static Coordinates getCoordinates(String location) {
    String response = callMapsAPI(location);
    String data = response;
    
    if (data.indexOf("NO_RESULTS") >= 0 ||
        data.indexOf("ZERO_RESULTS") >= 0) {
      System.out.println("Error: Could not find that location!");
      return null;
    }

    data = data.substring(data.indexOf("lat") + 7);

    double lat = Double.parseDouble(data.substring(0, data.indexOf(",")));
    
    data = data.substring(data.indexOf("lng") + 7);
    double lon = Double.parseDouble(data.substring(0, data.indexOf(",") - 1).trim());
    
    data = response;
    
    String suburb = null;
    
    /* Some formatted locations return more than one locality, this 
     * can result in a broad formatted address returning a neighnouring
     * suburb which may not be what the user intended.
     * 
     * This checks that the returned suburb is actually part of the
     * formatted location provided.
     */
    String[] parse = data.split("locality");
    
    for (int i = 0; i < parse.length; ++i) {
      String current = parse[i];

      while (current.indexOf("long_name") >= 0) {
        current = current.substring(current.indexOf("long_name") + 14, current.length());
        suburb = current.substring(0, current.indexOf("\""));
      }
      
      if (location.indexOf(suburb) >= 0) {
        break;
      }
    }

    Coordinates coordinates = new Coordinates(suburb, lat, lon);

    return coordinates;
  }

  public static String getFormattedAddress(String location) {
    String response = callMapsAPI(location);

    if (response == null) {
      return null;
    }
    
    if (response.indexOf("NO_RESULTS") >= 0 ||
        response.indexOf("ZERO_RESULTS") >= 0) {
      System.out.println("Error: Could not find that location!");
      return null;
    }
    
    String key = "formatted_address";
    response = response.substring(response.indexOf(key) + key.length() + 5);
    response = response.substring(0, response.indexOf("\""));

    return response;
  }
}
