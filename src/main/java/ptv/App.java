package ptv;

import agenda.Config;
import agenda.Util;

import com.google.api.client.util.DateTime;

import geolocation.Coordinates;
import geolocation.GeoLocation;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.security.Key;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class App {
  public static final String BASE_URL = "https://timetableapi.ptv.vic.gov.au";
  
  public static String getPtvUri(Scanner scnr) {

      // Location ---------------------------------------------------------------------
      Coordinates departureCoordinates = null;

      while (departureCoordinates == null) {
        System.out.println("Enter address or suburb to search for stops: (Enter '-1' to cancel.)");
        String departureLocation = scnr.nextLine();

        if (departureLocation.equals("-1")) {
          System.out.println("Cancelling...");

          return null;
        }

        if (!departureLocation.toLowerCase().endsWith("vic") || 
            !departureLocation.toLowerCase().endsWith("victoria")) {
              departureLocation += ", victoria";
            }

        // System.out.println(departureLocation);

        departureCoordinates = GeoLocation.getCoordinates(departureLocation);
      }

      // Mode of transport ------------------------------------------------------------
      ArrayList<RouteType> routeTypes = RouteType.getAllRouteTypes();

      RouteType selectedTransport = routeTypes.get(0);

      int input = -1;
      while (input < 0) {
        System.out.println("What type of transportation do you want to catch?");

        for (RouteType r : routeTypes) {
          System.out.println(r.id + ". " + r.name);
        }

        try {
          input = scnr.nextInt();
          scnr.nextLine();

          selectedTransport = routeTypes.get(input);
        } catch(InputMismatchException e) {
          System.out.println("Error: Please enter a valid number!");
        }
      }

      // Departure stop/station -------------------------------------------------------
      ArrayList<Stop> stops = Stop.getStopsFromCoordinates(departureCoordinates, selectedTransport.id);

      input = -1;

      while (input < 0 || input >= stops.size()) {
        System.out.println("Select stop to depart from:");

        for (int i = 0; i < stops.size(); ++i) {
          System.out.println(i + ". " + stops.get(i).stopName + " (" + stops.get(i).stopId + ")");
        }

        try {
          input = scnr.nextInt();
          scnr.nextLine();
          
        } catch(InputMismatchException e) {
          System.out.println("Error: Please enter a valid number!");
        }
      }

      Stop selectedStop = stops.get(input);

      // Route ------------------------------------------------------------------------
      ArrayList<Route> routes = selectedStop.routes;

      input = -1;

      Route selectedRoute;

      if (routes.size() == 1) {
        selectedRoute = routes.get(0);
      } else {

        while (input < 0 || input >= routes.size()) {
          System.out.println("Select route:");

          for (int i = 0; i < routes.size(); ++i) {
            System.out.println(i + ". " + routes.get(i).routeName + " (" + routes.get(i).routeId + ")");
          }

          try {
            input = scnr.nextInt();
            scnr.nextLine();
            
          } catch(InputMismatchException e) {
            System.out.println("Error: Please enter a valid number!");
          }
        }

        selectedRoute = routes.get(input);
      }

      // Direction --------------------------------------------------------------------
      ArrayList<Direction> directions = selectedRoute.getDirections();

      input = -1;

      while (input < 0 || input >= directions.size()) {
        System.out.println("Select direction:");

        for (int i = 0; i < directions.size(); ++i) {
          System.out.println(i + ". " + directions.get(i).directionName);
        }

        try {
          input = scnr.nextInt();
          scnr.nextLine();
          
        } catch(InputMismatchException e) {
          System.out.println("Error: Please enter a valid number!");
        }
      }

      Direction selectedDirection = directions.get(input);

      String uri = "/v3" + //
      "/departures" + //
      "/route_type/" + selectedTransport.id + //
      "/stop/" + selectedStop.stopId + //
      "/route/" + selectedRoute.routeId + //
      "?direction_id=" + selectedDirection.directionId;

      // ArrayList<String> departures = parseDepartureTimes(callPTVApi(uri));

      // for (String d : departures) {
      //   System.out.println(d.substring(11, 16));
      // }

      return uri;
  }

  public static String buildTTAPIURL(final String baseURL, final String privateKey, final String uri,
          final int developerId) throws Exception
  {
      
      String HMAC_SHA1_ALGORITHM = "HmacSHA1";
      StringBuffer uriWithDeveloperID = new StringBuffer().append(uri).append(uri.contains("?") ? "&" : "?")
              .append("devid=" + developerId);
      byte[] keyBytes = privateKey.getBytes();
      byte[] uriBytes = uriWithDeveloperID.toString().getBytes();
      Key signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
      mac.init(signingKey);
      byte[] signatureBytes = mac.doFinal(uriBytes);
      StringBuffer signature = new StringBuffer(signatureBytes.length * 2);
      for (byte signatureByte : signatureBytes)
      {
          int intVal = signatureByte & 0xff;
          if (intVal < 0x10)
          {
              signature.append("0");
          }
          signature.append(Integer.toHexString(intVal));
      }
      StringBuffer url = new StringBuffer(baseURL).append(uri).append(uri.contains("?") ? "&" : "?")
              .append("devid=" + developerId).append("&signature=" + signature.toString().toUpperCase());

      return url.toString();
  }

  public static String callPTVApi(String uri) {
    if (!Config.doPTV) {
      System.err.println("[WARNING]: PTV API disabled, check 'ptv_dev_id' or 'ptv_key' in config file.");
      return null;
    }

    int developerId = Integer.parseInt(Config.PTV_DEV_ID);
    String privateKey = Config.PTV_KEY;

    HttpRequest request = null;
    HttpResponse<String> response = null;

    try {
      request = HttpRequest.newBuilder()
        .uri(URI.create(buildTTAPIURL(BASE_URL, privateKey, uri, developerId)))
        .build();
    } catch(Exception e) {
      e.printStackTrace();
      
      return null;
    }

    try {
      response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (response == null) {
      System.err.println("[WARNING]: Reponse returned null.");
      return null;
    } else {
      return response.body();
    }
  }

  /**
   * Method to retrieve {@code scheduled_departure_utc} fields from json file.
   * This is definitely not the right way to do it, but it works. 
   * @return {@code ArrayList<String>} object containing all the scheduled departure times in RFC3339 format.
   */
  public static ArrayList<String> parseDepartureTimes(String json) {
    ArrayList<String> departures = new ArrayList<String>();
    
    //Completely backwards way of parsing this json file...
    while (json.indexOf("scheduled_departure_utc") >= 0) {
      int start = json.indexOf("scheduled_departure_utc") + 26;
      int end = start + 20;

      String utcTime = json.substring(start, end);
      
      Instant timestamp = Instant.parse(utcTime);
      ZonedDateTime localTime = timestamp.atZone(ZoneId.of("Australia/Melbourne"));
      utcTime = localTime.toString().substring(0, 16) + ":00.000+" + Config.getTimezoneOffsetString() + ":00";

      departures.add(utcTime);

      json = json.substring(end);
    }

    return departures;
  }

  /**
   * Gets the train times for the latest possible departure based on specified PTV uri and config prefs.
   * @param uri API uri as defined by user in lists.csv
   * @param latestDepartureTime {@code DateTime} object with the start time of the first event in the EventList with PTV selected.
   * @param event Title of event to search for departures.
   * @return <p> [0] Name of station/stop.<p>[1] Earlier departure time.<p>[2] Optimal departure time.<p>[3] Later departure time.<p>
   * [4] Title of event scheduled around.<p>[5] Start time of event.<p>[6]Type of transport. eg: train, bus...
   */
  public static String[] getPtvInfo(String uri, DateTime latestDepartureTime, String eventTitle) {
    if (latestDepartureTime == null) {
      return null;
    }

    String eventTime = Util.parseTimeFromRFC3339(latestDepartureTime.toString());
    
    latestDepartureTime = addBuffer(latestDepartureTime);

    // Retrieve departures.
    System.out.println("Searching for departures...");
    String response = callPTVApi(uri);
    ArrayList<String> departures = parseDepartureTimes(response);

    if (departures.size() == 0) {
      return null;
    }

    // Check if there are train services running before the latest possible departure,
    // returns null if no services.
    if (departures.get(0).compareToIgnoreCase(latestDepartureTime.toString()) > 0) {
      System.err.println("[WARNING]: No departures found before latest possible departure!");
      return null;
    }

    int optimalIndex = 0;
    
    for (int i = 0; i < departures.size(); ++i) {
      if (departures.get(i).compareToIgnoreCase(latestDepartureTime.toString()) <= 0) {
        optimalIndex = i;
      } else {
        break;
      }
    }

    String[] ptvInfo = new String[7];

    ptvInfo[0] = getStopName(uri);
    
    if (optimalIndex > 0) {
      ptvInfo[1] = departures.get(optimalIndex - 1).toString();
      ptvInfo[1] = Util.parseTimeFromRFC3339(ptvInfo[1]);
    } else {
      ptvInfo[1] = "--";
    }

    ptvInfo[2] = departures.get(optimalIndex).toString();
    ptvInfo[2] = Util.parseTimeFromRFC3339(ptvInfo[2]);
    
    if (optimalIndex < (departures.size() - 1)) {  
      ptvInfo[3] = departures.get(optimalIndex + 1).toString();
      ptvInfo[3] = Util.parseTimeFromRFC3339(ptvInfo[3]);
    } else {
      ptvInfo[3] = "--";
    }

    ptvInfo[4] = eventTitle;
    ptvInfo[5] = eventTime;
    
    String mode = uri.substring(uri.indexOf("route_type/") + 11);
    mode = mode.substring(0, 1);
    
    ArrayList<RouteType> routeTypes = RouteType.getAllRouteTypes();

    for (RouteType r : routeTypes) {
      if (Integer.parseInt(mode) == r.id) {
        ptvInfo[6] = r.name.toLowerCase();
      }
    }

    System.out.println("PTV:");
    System.out.println("  Departing from: " + ptvInfo[0]);
    System.out.println("  Earlier departure: " + ptvInfo[1]);
    System.out.println("  Optimal departure: " + ptvInfo[2]);
    System.out.println("  Later departure: " + ptvInfo[3]);
    System.out.println("  Mode: " + ptvInfo[6]);

    return ptvInfo;
  }

  public static String getStopName(String listUri) {
    String stopId = listUri.substring(listUri.indexOf("stop/") + 5);
    stopId = stopId.substring(0, stopId.indexOf("/"));

    String routeType = listUri.substring(listUri.indexOf("route_type/") + 11);
    routeType = routeType.substring(0, routeType.indexOf("/"));
    
    String uri = "/v3/stops/" + stopId + "/route_type/" + routeType;

    String response = callPTVApi(uri);

    response = response.substring(response.indexOf("stop_name") + 12);
    response = response.substring(0, response.indexOf("\""));
    
    return response.trim();
  }

  public static DateTime addBuffer(DateTime eventTime) {
    int bufferMinutes = Config.getDepartureBuffer();

    long bufferMilli = bufferMinutes * 60; // Minutes to seconds
    bufferMilli *= 1000; // Seconds to milliseconds
    
    Instant event = Instant.parse(eventTime.toString());
    long eventMilli = event.toEpochMilli();

    long updatedMilli = eventMilli - bufferMilli;

    DateTime buffered = new DateTime(updatedMilli);

    return buffered;
  }
}
