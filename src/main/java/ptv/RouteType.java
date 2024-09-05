package ptv;
 
import agenda.Config;

import java.util.ArrayList;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.io.IOException;

public class RouteType {
  public int id;
  public String name;

  public RouteType(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public static ArrayList<RouteType> getAllRouteTypes() {
    ArrayList<RouteType> routeTypes = new ArrayList<RouteType>();

    int developerId = Integer.parseInt(Config.PTV_DEV_ID);
    String privateKey = Config.PTV_KEY;

    String uri = "/v3/route_types";
    
    HttpRequest request = null;
    HttpResponse<String> response = null;

    try {
      request = HttpRequest.newBuilder()
        .uri(URI.create(App.buildTTAPIURL(App.BASE_URL, privateKey, uri, developerId)))
        .build();
    } catch(Exception e) {
      e.printStackTrace();
    }

    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    if (response != null) {
      String output = response.body();
      
      // System.out.println(output);

      while (output.indexOf("route_type_name") >= 0) {
        output = output.substring(output.indexOf("route_type_name") + 18);

        String name = output.substring(0, output.indexOf("\","));
        // System.out.println(name);

        output = output.substring(output.indexOf("route_type") + 12);

        int id = Integer.parseInt(output.substring(0, 1));
        // System.out.println(id);

        routeTypes.add(new RouteType(id, name));
      }
    }

    return routeTypes;
  }
}
