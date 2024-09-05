package ptv;

import geolocation.Coordinates;

import java.util.ArrayList;

public class Stop {
  public String stopName;
  public int stopId;
  public ArrayList<Route> routes;

  public Stop(String stopName, int stopId, ArrayList<Route> routes) {
    this.stopName = stopName;
    this.stopId = stopId;
    this.routes = routes;
  }

  public static ArrayList<Stop> getStopsFromCoordinates(Coordinates coordinates, int routeType) {
    int maxDistance;
    
    switch (routeType) {
      case 0:
        maxDistance = 4000;
        break;
      case 1:
        maxDistance = 4000;
        break;
      case 3:
        maxDistance = 10000;
        break;
      default:
        maxDistance = 500;
    }

    String uri = "/v3/stops/location/" + coordinates.lat + "," + coordinates.lon +"?route_types=" + routeType + "&max_distance=" + maxDistance;
    // System.out.println(uri);

    String json = App.callPTVApi(uri);

    // System.out.println(json);

    ArrayList<Stop> stops = new ArrayList<Stop>();

    // Parse stops from json
    while (json.indexOf("stop_name") >= 0) {
      json = json.substring(json.indexOf("stop_name") + 12);

      String stopName = json.substring(0, json.indexOf("\","));

      //System.out.println(stopName);

      json = json.substring(json.indexOf("stop_id") + 9);
      int stopId = Integer.parseInt(json.substring(0, json.indexOf(",")));

      ArrayList<Route> routes = new ArrayList<Route>();
      
      int prevId = 0;

      // Parse routes from stops from json
      while (json.indexOf("route_id") < json.indexOf("stop_name")) {
        json = json.substring(json.indexOf("route_id") + 10);
        int routeId = Integer.parseInt(json.substring(0, json.indexOf(",")));

        if (routeId == prevId) {
          continue;
        }

        prevId = routeId;

        json = json.substring(json.indexOf("route_name") + 13);
        String routeName = json.substring(0, json.indexOf("\","));

        //System.out.println("\t" + routeId + " - " + routeName);
        
        routes.add(new Route(routeName, routeId));
      }
      stops.add(new Stop(stopName, stopId, routes));
    }

    return stops;
  }
}
