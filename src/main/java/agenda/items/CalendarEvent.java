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
import java.util.ArrayList;
import java.util.List;

public class CalendarEvent extends AgendaItem {
  public String location;
  public String formattedAddress;

  private static final String APPLICATION_NAME = "Agenda - CalendarEvent";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  public static DateTime now = App.now;
  public static DateTime nowStartOfDay = App.nowStartOfDay;
  public static DateTime nowEndOfDay = App.nowEndOfDay;
  
  public CalendarEvent( String title, 
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
  public static void getCalendarEvents(ArrayList<AgendaItem> agendaItems, EventList list) throws IOException, GeneralSecurityException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Calendar service =
        new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, App.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

    Events events = service.events().list(list.id)
        .setMaxResults(30)
        .setTimeMin(nowStartOfDay)
        .setTimeMax(nowEndOfDay)
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute();

    List<Event> items = events.getItems();
    if (items.isEmpty()) {
      System.out.println("No upcoming events found in " + list.name + ".");
    } else {
      String calendarString = (items.size() == 1) ? " event" : " events";
      System.out.println("Found " + items.size() + calendarString + " in '" + list.name + "'");
      for (Event event : items) {
        
        String title = event.getSummary();
        
        String color = event.getColorId();
        String colorHex = null;

        if (color == null) {
          color = list.colorId;
          colorHex = Colors.getHex(Integer.parseInt(color), true, Config.doModernColours);
        } else {
          colorHex = Colors.getHex(Integer.parseInt(color), false, Config.doModernColours);
        }

        DateTime start = event.getStart().getDateTime();
        if (start == null) {
          start = event.getStart().getDate();
        }

        DateTime end = event.getEnd().getDateTime();
        if (end == null) {
          end = event.getEnd().getDate();
        }

        Boolean isAllDay;
        
        // If event is set as an all day event in Google Calendar, length will be 10
        if (start.toStringRfc3339().length() == 10 && end.toStringRfc3339().length() == 10) {
          isAllDay = true;
        } else {
          isAllDay = false;
        }

        if (Util.parseDateFromRFC3339(start.toString()).compareToIgnoreCase(Util.parseDateFromRFC3339(now.toString())) < 0 &&
        Util.parseDateFromRFC3339(end.toString()).compareToIgnoreCase(Util.parseDateFromRFC3339(now.toString())) > 0) {
          isAllDay = true;
        }

        String description = event.getDescription();

        String location = event.getLocation();

        agendaItems.add(new CalendarEvent(title, list.name, colorHex, start, end, isAllDay, description, location));
      }
    }
  }


  public String generateTableRow() {
    String html = AgendaItem.getAgendaItemHeader(this.getEventRange(), this.title, this.list, this.colorHex, this.description);
    
    html = html.replace("$location$", generateLocationRow(this.location, this.formattedAddress));

    return wrap(html);
  }

  private String getEventRange() {
    if (isAllDay) {
      return "All Day";
    }

    String startTime = Util.parseTimeFromRFC3339(start.toString());
    String endTime = Util.parseTimeFromRFC3339(end.toString());
    
    String startDate = Util.parseDateFromRFC3339(start.toString());
    String endDate = Util.parseDateFromRFC3339(end.toString());

    DateTime now = new DateTime(System.currentTimeMillis());
    String todayDate = Util.parseDateFromRFC3339(now.toString());

    if (startDate.compareTo(todayDate) < 0) {
      startTime = "00:00"; 
    }    

    if (endDate.compareTo(todayDate) > 0) {
      endTime = "23:59";
    }

    return startTime + " - " + endTime;
  }

  public String getSignificantLocation() {
    return this.formattedAddress;
  }
}
