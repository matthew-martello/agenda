package agenda;

import agenda.items.AgendaItem;

import com.google.api.client.util.DateTime;

import geolocation.Coordinates;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class Util {
  
  /**
   * Prompts the user to provide a boolean value for a prompt.
   * @param scnr {@code Scanner} object to get user input.
   * @param prompt {@code String} to prompt the user what they are choosing.
   * @return {@code Boolean} based on user input.
   */
  public static boolean getBoolFromUser(Scanner scnr, String prompt) {
    String input = "";
    Boolean output = false;

    while (!input.equalsIgnoreCase("y") || !input.equalsIgnoreCase("n")) {
      System.out.println(prompt + " (y/n)");

      input = scnr.nextLine();

      if (input.equalsIgnoreCase("y")) {
        output = true;
        break;
      }

      if (input.equalsIgnoreCase("n")) {
        output = false;
        break;
      }

      System.out.println("Error: Input must be either 'y' or 'n'");
    }
    
    return output;
  }

  /**
   * Method to get the latitude and longitude for a given address or location.
   * @param location Location{@code String}. Can either be a full address, a suburb, city, etc...
   * @param apiKey API Key for the Google Maps Geocode API.
   * @return A {@code Coordinates} object containing the latitude, longitude and suburb name of the location provided.
   */
  public static Coordinates getCoordinates(String location, String apiKey) {
    // System.out.println("Calling geocoding API...");
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
      return null;
    } else {
      String data = response.body();

      // System.out.println(data);
  
      if (data.indexOf("NO_RESULTS") >= 0 || data.indexOf("locality") == -1) {
        System.out.println("Error: Could not find that location!");
        return null;
      }

      data = data.substring(data.indexOf("lat") + 7);

      double lat = Double.parseDouble(data.substring(0, data.indexOf(",")));
      
      data = data.substring(data.indexOf("lng") + 7);
      double lon = Double.parseDouble(data.substring(0, data.indexOf(",") - 1).trim());
      
      data = response.body();

      data = data.substring(0, data.indexOf("locality"));
      
      String suburb = null;
      while (data.indexOf("long_name") >= 0) {
          data = data.substring(data.indexOf("long_name") + 14, data.length());
          suburb = data.substring(0, data.indexOf("\""));
      }

      // System.out.println(lat + " " + lon);

      Coordinates coordinates = new Coordinates(suburb, lat, lon);

      return coordinates;
    }
  }

   /**
   * @param date The date in full RFC3339 format.
   * @return A substring of the RFC3339 String containing only the date information. ({@code YYYY-MM-DD})
   */
  public static String parseDateFromRFC3339(String date) {
    return date.substring(0, 10);
  }

  /**
   * @param time the time in full RFC3339 format.
   * @return A substring of the RFC3339 String containing only the hour/minute information. ({@code hh:mm})
   */
  public static String parseTimeFromRFC3339(String time) {
    return time.substring(11, 16);
  }

  /**
   * Method for rounding the the provided {@code DateTime} object either up or down to the nearest day.   
   * @param dt The {@code DateTime} object to affect.
   * @param startOfDay <p>true - {@code 00:00:00.000} <p>false - {@code 23:59:59.999}
   * @return An rfc3339 formatted date/time {@code String} rounded to the specified boundary (floor/ceiling).
   */
  public static String toDayBoundaries(DateTime dt, boolean startOfDay) {
    String currentTime = valueOf(dt);
    String output = "";
    
    output += Util.parseDateFromRFC3339(currentTime);

    if (startOfDay) {
      output += "T00:00:00.000";
    } else {
      output += "T23:59:59.999";
    }
    
    return output += currentTime.substring(currentTime.indexOf("+"), currentTime.length());
  }

  /**
   * @param obj An {@code Object} to get the {@code String} value of.
   * @return The {@code String} of an object using .toString() method.
   * <p>If object is {@code null}, the {@code String} "null"
   */
  public static String valueOf(Object obj) {
    return (obj ==  null) ? "null" : obj.toString();
  }

  /**
   * Method for manually correcting the timezone of events in GMT+00:00<p>
   * SHOULD ONLY BE USED IN THE CONTEXT OF CLASSES CALENDAR ADDING +10. <p>
   * (Will break if change crosses midnight)
   * @param dateTime An {@code RFC3339} formatted string to modify.
   * @param modifier An {@code int} specifying the manual offset to apply to {@code dateTime}
   * @param timezoneOnly <p>true - Only adds the {@code modifier} to the timezone portion of the string.<p>
   * false - Adds the {@code modifier} to both the timezone and time portions of the string.
   * @return An rfc3339 formatted date/time {@code String} with the updated timezone.
   */
  public static String forceTimezone(String dateTime, int modifier, boolean timezoneOnly) {
    String currentTime = dateTime;

    currentTime = currentTime.substring(0, currentTime.length() - 1);

    currentTime += "+" + modifier + ":00";

    String output = "";

    output += currentTime.substring(0, 11);

    int hr; 
    
    if (!timezoneOnly) {
      hr = Integer.parseInt(currentTime.substring(11, 13)) + modifier;
    } else {
      hr = Integer.parseInt(currentTime.substring(11, 13));
    }
    
    if (hr == 0) {
      output += "0";
    }

    output += Integer.toString(hr);

    output += currentTime.substring(13);

    return output;
  }

  /**
   * Sorts the provided array by the start date time, so objects are correctly printed in chronological order.
   * @param agenda {@code ArrayList} of AgendaItem objects.
   */
  public static void sortByDateTime(ArrayList<AgendaItem> agenda) {
    for (int i = 1; i < agenda.size(); ++i) {
      int j = i;
      
      while (j > 0 && agenda.get(j).start.toString().compareToIgnoreCase(agenda.get(j - 1).start.toString()) < 0) {
        
        AgendaItem temp = agenda.get(j);
        agenda.set(j, agenda.get(j - 1));
        agenda.set(j - 1, temp);
        
        --j;
      }
    }
  }

  /**
   * Method to get the day of the week from the current time.
   * @return Day of the week based on current time (ex: {@code Monday})
   */
  public static String getDayOfWeek() {
    LocalDate date = LocalDate.now();
    DayOfWeek day = date.getDayOfWeek();

    Locale locale = new Locale("en");
    return day.getDisplayName(TextStyle.FULL, locale);
  }

    /**
   * Method for parsing a {@code DateTime} object into a readble String for headers.
   * @param today A {@code DateTime} object to be parsed into a {@code String}.
   * @return A {@code String} with the formatted date. <p>
   * Example: {@code 30th of April 2000}
   */
  public static String parseTodaysDateToHeader() {
    DateTime today = new DateTime(System.currentTimeMillis());
    
    String currentDate = today.toString();
    
    String year = currentDate.substring(0, 4);
    
    String month = getMonthFromInt(Integer.parseInt(currentDate.substring(5, 7)));

    int day = Integer.parseInt(currentDate.substring(8, 10));

    return day + getDaySuffix(day) + " of " + month + " " + year;
  }

  /**
   * @param input An {@code int} represented the month of the year.
   * @return The month parsed from {@code input}.
   */
  public static String getMonthFromInt(int input) {
    switch (input) {
      case 1:
        return "January";
      case 2:
        return "Febuary";
      case 3:
        return "March";
      case 4:
        return "April";
      case 5:
        return "May";
      case 6:
        return "June";
      case 7:
        return "July";
      case 8:
        return "August";
      case 9:
        return "September";
      case 10:
        return "October";
      case 11:
        return "November";
      case 12:
        return "December";
      default:
        return "null";
    }
  }

  /**
   * Method for determining the suffix of a number as read aloud. <p>
   * Example: 1 returns "st", 43 returns "rd", 100 returns "th" 
   * @param day An {@code int} representing the day of the month.
   * @return A {@code String} containing the suffix for the provided day.
   */
  public static String getDaySuffix(int day) {
    int i = day % 10;
    int j = day % 100;

    if (i == 1 && j != 11) {
      return "st";
    }

    if (i == 2 && j != 12) {
      return "nd";
    }

    if (i == 3 && j != 13) {
      return "rd";
    }

    return "th";
  }
}