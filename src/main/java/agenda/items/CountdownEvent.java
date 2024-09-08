package agenda.items;

import agenda.*;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import geolocation.GeoLocation;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class CountdownEvent extends AgendaItem {
  public String location;
  public String formattedAddress;

  private static final String APPLICATION_NAME = "Agenda - CountdownEvent";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  public static DateTime now = App.now;
  public static DateTime nowStartOfDay = App.nowStartOfDay;
  public static DateTime nowEndOfDay = App.nowEndOfDay;

  public CountdownEvent( String title, 
                        String list,
                        String colorHex, 
                        DateTime start, 
                        DateTime end, 
                        Boolean isAllDay, 
                        String description, 
                        String location) {
                          this.title = title;
                          this.list = list;
                          this.colorHex = colorHex;
                          this.start = start;
                          this.end = end;
                          this.isAllDay = isAllDay;
                          this.description = description;
                          this.location = location;

                          if (location != null && Config.doGeoLocation) {
                            this.formattedAddress = GeoLocation.getFormattedAddress(location);
                          }
  }

  /**
   * Calls the Google Calendar API to create an {@code ArrayList} of {@code CalendarEvent} objects for the current day. 
   * @param agendaItems {@code Arraylist} to store the {@code CalendarEvent} objects.
   * @param list {@code EventList} object to provide the parameters for the API.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static void getCountdownEvents(ArrayList<AgendaItem> agendaItems, EventList list) throws IOException, GeneralSecurityException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    
    Calendar service =
        new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, App.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

    Events events = service.events().list(list.id)
        .setMaxResults(30)
        .setTimeMin(nowStartOfDay)
        // .setTimeMax(nowEndOfDay)
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute();

    List<Event> items = events.getItems();
    if (items.isEmpty()) {
      System.out.println("No upcoming events found in " + list.name + ".");
    } else {
      String countdownString = (items.size() == 1) ? " event" : " events";
      System.out.println("Found " + items.size() + countdownString + " in '" + list.name + "'");
      for (Event event : items) {
        
        String title = event.getSummary();
        
        String color = event.getColorId();
        if (color == null) {
          color = list.colorId;
        }
        String colorHex = Colors.getHex(Integer.parseInt(color), true, Config.doModernColours);

        DateTime start = event.getStart().getDate();
        DateTime end = event.getEnd().getDate();
        Boolean isAllDay;

        if (start.toStringRfc3339().length() == 10 && end.toStringRfc3339().length() == 10) {
          isAllDay = true;
        } else {
          isAllDay = false;
        }

        String description = event.getDescription();

        String location = event.getLocation();

        agendaItems.add(new CountdownEvent(title, list.name, colorHex, start, end, isAllDay, description, location));
      }
    }
  }

  public String generateTableRow() {
    String html = AgendaItem.getAgendaItemHeader(this.getTimeToEvent(), this.title, this.list, this.colorHex, this.description);

    html = html.replace("$location$", generateLocationRow(this.location, this.formattedAddress));

    return AgendaItem.wrap(html);
  }

  public String getTimeToEvent() {
    String eventStart = this.start.toString() + "T00:00:00.000+" + Config.getTimezoneOffsetString() + ":00";

    Instant event = Instant.parse(eventStart);

    long eventMilli = event.toEpochMilli();
    long nowMilli = System.currentTimeMillis();

    long countdown = eventMilli - nowMilli; // Time in milliseconds
    countdown = countdown / 60000; // Time in minutes
    countdown = countdown / 1440; // Time in days; 
    
    long weeks = countdown / 7;
    long days = countdown % 7;

    if (countdown < 1) {
      return "All day";
    }
    
    String output = "";

    if (weeks > 1) {
      output += weeks + " weeks"; 
    } else if (weeks == 1) {
      output += weeks + " week";
    }
    
    if (days != 0 && weeks >= 1) {
      output += ",<br>";
    }

    if (days > 1) {
      output += days + " days"; 
    } else if (days == 1) {
      output += days + " day";
    }

    return output;
  }

  public String getSignificantLocation() {
    return this.formattedAddress;
  }
}
