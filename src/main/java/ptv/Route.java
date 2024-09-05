package ptv;

import java.util.ArrayList;

public class Route {
  public String routeName;
  public int routeId;

  public Route(String routeName, int routeId) {
    this.routeName = routeName;
    this.routeId = routeId;
  }

  public ArrayList<Direction> getDirections() {
    ArrayList<Direction> directions = new ArrayList<Direction>();

    String json = App.callPTVApi("/v3/directions/route/" + this.routeId);

    // System.out.println(json);

    while (json.indexOf("direction_id") != -1) {
      json = json.substring(json.indexOf("direction_id") + 14);
      int directionId = Integer.parseInt(json.substring(0, json.indexOf(",")));
      
      // System.out.println(directionId);

      json = json.substring(json.indexOf("direction_name") + 17);
      String directionName = json.substring(0, json.indexOf("\","));

      // System.out.println(directionName);

      directions.add(new Direction(directionId, directionName));
    }

    return directions;
  }
}
