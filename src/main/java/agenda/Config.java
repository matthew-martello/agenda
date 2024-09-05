package agenda;

import geolocation.GeoLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.InputMismatchException;
import java.util.Properties;

public class Config {
  public static Properties properties;
  public static File config;

  public static String DEFAULT_CALENDAR_COLOUR;
  public static String DEFAULT_PRIMARY_ALIAS;
  public static String WEATHER_KEY;
  public static String MAPS_KEY;
  public static String PTV_DEV_ID;
  public static String PTV_KEY;
  public static String RMIT_LOCATION;
  public static String DEFAULT_LOCATION;
  public static String TIMEZONE_OFFSET;
  public static String LATEST_DEPARTURE_BUFFER;
  public static String MODERN_COLOURS;
  
  public static Boolean doWeather = true;
  public static Boolean doGeoLocation = true;
  public static Boolean doPTV = true;
  public static Boolean doRmitLocation = true;
  public static Boolean doModernColours = true;

  public Config() {
    System.out.println("Importing config...");
    config = new File("src/main/resources/config.xml");
    properties = new Properties();

    InputStream inStream = null;
    try {
      inStream = new FileInputStream(config);
    } catch(FileNotFoundException e) {
      e.printStackTrace();
    }

    try {
      properties.loadFromXML(inStream);      
    } catch (NullPointerException n) {
      // Skip if properties is null.
    } catch (IOException e) {    
      e.printStackTrace();
    }

    DEFAULT_CALENDAR_COLOUR = properties.getProperty("default_calendar_colour");
    validateDefaultCalendarColour();

    DEFAULT_PRIMARY_ALIAS = properties.getProperty("default_primary_alias");
    validateDefaultPrimaryAlias();

    WEATHER_KEY = properties.getProperty("weather_key");
    validateWeatherKey();

    MAPS_KEY = properties.getProperty("maps_key");
    validateMapsKey();

    PTV_DEV_ID = properties.getProperty("ptv_dev_id");
    PTV_KEY = properties.getProperty("ptv_key");
    validatePtv();

    RMIT_LOCATION = properties.getProperty("rmit_location");
    validateRmitLocation();

    DEFAULT_LOCATION = properties.getProperty("default_location");
    validateDefaultLocation();

    TIMEZONE_OFFSET = properties.getProperty("timezone_offset");
    validateTimezoneOffset();

    LATEST_DEPARTURE_BUFFER = properties.getProperty("latest_departure_buffer");
    validateLatestDepartureBuffer();

    MODERN_COLOURS = properties.getProperty("modern_colours");
    validateModernColours();

    System.out.println("Config imported.");
  }

  public static void validateDefaultCalendarColour() {
    String def = "15"; // Default

    if (DEFAULT_CALENDAR_COLOUR == null) {
      DEFAULT_CALENDAR_COLOUR = def;
      properties.setProperty("default_calendar_colour", DEFAULT_CALENDAR_COLOUR);
      return;
    }

    try {
      Integer.parseInt(DEFAULT_CALENDAR_COLOUR);
    } catch(InputMismatchException e) {
      System.err.println("[WARNING]: Invalid 'default_calendar_colour', must be a number! (Using value: 15)");
      DEFAULT_CALENDAR_COLOUR = def;
      properties.setProperty("default_calendar_colour", DEFAULT_CALENDAR_COLOUR);
    }

    if (Integer.parseInt(DEFAULT_CALENDAR_COLOUR) < 1 || 
        Integer.parseInt(DEFAULT_CALENDAR_COLOUR) > 24 ) {
          System.err.println("[WARNING]: 'default_calendar_color' must be a value between 1 and 24. (Using value: " + def + ")");
          DEFAULT_CALENDAR_COLOUR = def;
          properties.setProperty("default_calendar_colour", DEFAULT_CALENDAR_COLOUR);
    }
  }

  public static void validateDefaultPrimaryAlias() {
    if (DEFAULT_PRIMARY_ALIAS == null) { 
      DEFAULT_PRIMARY_ALIAS = "Personal";
      properties.setProperty("default_primary_alias", DEFAULT_PRIMARY_ALIAS);
    }
  }

  public static void validateWeatherKey() {
    if (WEATHER_KEY == null) {
      doWeather = false;
      System.err.println("[ERROR]: 'weather_key' not found!");
    }

    //TODO Verify weather key
  }

  public static void validateMapsKey() {
    if (MAPS_KEY == null) {
      System.err.println("[ERROR]: 'maps_key' not found!");
      doGeoLocation = false;
      return;
    }
    
    System.out.println("Verifying 'maps_key'...");
    String mapsTest = GeoLocation.callMapsAPI("test");

    if (mapsTest.indexOf("REQUEST_DENIED") >= 0) {
      System.err.println("[ERROR]: 'maps_key' is invalid! GeoLocation is unavaliable.");
      doGeoLocation = false;
    } else {
      System.out.println("'maps_key' verified.");
    }
  }

  public static void validatePtv() {
    if (PTV_DEV_ID == null) {
      System.err.println("[ERROR]: 'ptv_dev_id' not found!");
      doPTV = false;
      return;
    }

    try {
      Integer.parseInt(PTV_DEV_ID);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.err.println("[ERROR]: Could not parse 'ptv_dev_id'! Must be a number.");
      doPTV = false;
      return;
    }

    if (PTV_KEY == null) {
      System.err.println("[ERROR]: 'ptv_key' not found!");
      doPTV = false;
      return;
    }

    System.out.println("Verifying PTV credentials.");
    String ptvTest = ptv.App.callPTVApi("/v3/route_types");

    if (ptvTest.indexOf("Forbidden (403)") >= 0) {
      System.err.println("[ERROR]: PTV credentials are invalid! PTV data is unavaliable.");
      doPTV = false;
    } else {
      System.out.println("PTV credentials verified.");
    }
  }

  public static void validateRmitLocation() {
    if (RMIT_LOCATION == null) {
      doRmitLocation = false;
      properties.setProperty("rmit_location", doRmitLocation.toString());
      return;
    }

    if (RMIT_LOCATION.equalsIgnoreCase("true")) {
      doRmitLocation = true;
      properties.setProperty("rmit_location", doRmitLocation.toString());
      return;
    } else if (RMIT_LOCATION.equalsIgnoreCase("false")) {
      doRmitLocation = false;
      properties.setProperty("rmit_location", doRmitLocation.toString());
      return;
    } else {
      System.err.println("[ERROR]: Could not parse 'rmit_location'! Value must be 'true' or 'false'.");
      doRmitLocation = false;
      properties.setProperty("rmit_location", doRmitLocation.toString());
    }
  }

  public static void validateDefaultLocation() {
    if (DEFAULT_LOCATION == null || !doGeoLocation) {
      DEFAULT_LOCATION = "Melbourne VIC, Australia";
      properties.setProperty("default_location", DEFAULT_LOCATION);
      return;
    }

    if (doGeoLocation) {
      String location = GeoLocation.getFormattedAddress(DEFAULT_LOCATION);

      if (location == null) {
        DEFAULT_LOCATION = "Melbourne VIC, Australia";
      } else {
        DEFAULT_LOCATION = location;
      }

      properties.setProperty("default_location", DEFAULT_LOCATION);
    }
  }

  public static void validateTimezoneOffset() {
    String def = "10";
    if (TIMEZONE_OFFSET == null) {
      TIMEZONE_OFFSET = def;
      properties.setProperty("timezone_offset", TIMEZONE_OFFSET);
      return;
    }

    try {
      Integer tz = Integer.parseInt(TIMEZONE_OFFSET);
      
      if (tz >= 0 && tz <= 23) {
        TIMEZONE_OFFSET = tz.toString();
      } else {
        System.err.println("[WARNING]: 'timezone_offset' must be a value between 0 and 23. (Using value: " + def + ")");
        TIMEZONE_OFFSET = def;
        properties.setProperty("timezone_offset", TIMEZONE_OFFSET);
        return;
      }
      
    } catch(InputMismatchException e) {
      System.err.println("[ERROR]: Could not parse 'timezone_offset'! Must be a number.");
      TIMEZONE_OFFSET = def;
      properties.setProperty("timezone_offset", TIMEZONE_OFFSET);
      return;
    }
  }

  public static int getTimezoneOffset() {
    return Integer.parseInt(TIMEZONE_OFFSET);
  }

  public static String getTimezoneOffsetString() {
    int tz = getTimezoneOffset();

    if (tz < 10) {
      return "0" + tz;
    } else {
      return "" + tz;
    }
  }

  public static void validateLatestDepartureBuffer() {
    String def = "60";
    if (LATEST_DEPARTURE_BUFFER == null) {
      LATEST_DEPARTURE_BUFFER = def;
      properties.setProperty("latest_departure_buffer", LATEST_DEPARTURE_BUFFER);
      return;
    }

    try {
      Integer tz = Integer.parseInt(LATEST_DEPARTURE_BUFFER);
      
      if (tz >= 0 && tz <= 120) {
        LATEST_DEPARTURE_BUFFER = tz.toString();
      } else {
        System.err.println("[WARNING]: 'latest_departure_buffer' must be a value between 0 and 120. (Using value: " + def + ")");
        LATEST_DEPARTURE_BUFFER = def;
        properties.setProperty("latest_departure_buffer", LATEST_DEPARTURE_BUFFER);
        return;
      }
      
    } catch(InputMismatchException e) {
      System.err.println("[ERROR]: Could not parse 'latest_departure_buffer'! Must be a number.");
      LATEST_DEPARTURE_BUFFER = def;
      properties.setProperty("latest_departure_buffer", LATEST_DEPARTURE_BUFFER);
      return;
    }
  }

  public static int getDepartureBuffer() {
    return Integer.parseInt(LATEST_DEPARTURE_BUFFER);
  }

  public static void validateModernColours() {
    if (MODERN_COLOURS == null) {
      doModernColours = true;
      properties.setProperty("modern_colours", doModernColours.toString());
      return;
    }

    if (MODERN_COLOURS.equalsIgnoreCase("true")) {
      doModernColours = true;
      properties.setProperty("modern_colours", doModernColours.toString());
      return;
    } else if (MODERN_COLOURS.equalsIgnoreCase("false")) {
      doModernColours = false;
      properties.setProperty("modern_colours", doModernColours.toString());
      return;
    } else {
      System.err.println("[ERROR]: Could not parse 'modern_colours'! Value must be 'true' or 'false'.");
      doModernColours = true;
      properties.setProperty("modern_colours", doModernColours.toString());
    }
  }
  
  public static void writeToFile() throws FileNotFoundException, IOException {
    System.out.println("Saving config...");

    OutputStream outStream = new FileOutputStream(config);
    properties.storeToXML(outStream, null);
  }
}