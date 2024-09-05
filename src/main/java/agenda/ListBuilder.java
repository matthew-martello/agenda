package agenda;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.Tasks;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class ListBuilder {
  private static final String APPLICATION_NAME = "Agenda - ListBuilder";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  public static String DEFAULT_CALENDAR_COLOUR = Config.DEFAULT_CALENDAR_COLOUR;

  public static void generateListsFile() throws IOException, GeneralSecurityException {
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Calendar calendarService =
        new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, agenda.App.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

    Scanner scnr = new Scanner(System.in);

    List<CalendarListEntry> items;
    ArrayList<CalendarListEntry> remainingItems = new ArrayList<CalendarListEntry>();
    ArrayList<EventList> lists = new ArrayList<EventList>();

    String pageToken = null;
    do {
      CalendarList calendarList = calendarService.calendarList().list().setPageToken(pageToken).execute();
      items = calendarList.getItems();

      System.out.println("Select calendars to include agenda items:");
      for (CalendarListEntry c : items) {
        // default calendar colour
        if (c.isPrimary()) {
          DEFAULT_CALENDAR_COLOUR = c.getColorId();
        }
                
        String summary = c.getSummary();

        if (c.getSummaryOverride() != null) {
          summary = c.getSummaryOverride();
        }

        if (Util.getBoolFromUser(scnr, "Include " + summary + "?")) {
          if (c.isPrimary()) {
            summary = Config.DEFAULT_PRIMARY_ALIAS;
          }

          lists.add(new EventList(c.getId(), summary, ListType.CALENDAR, c.getColorId()));
        } else {
          remainingItems.add(c);
        }
      }

      if (Config.doPTV && Util.getBoolFromUser(scnr, "Add ptv timetable to a calendar?")) {
        int input = -1;

        if (lists.size() == 1) {
          input = 0;
        }
        
        while (input < 0 || input >= lists.size()) {
          System.out.println("Select a calendar:");

          for (int i = 0; i < lists.size(); ++i) {
            System.out.println(i + ") " + lists.get(i).name);
          }

          try {
            input = scnr.nextInt();

            if (input < 0 || input >= lists.size()) {
              System.out.println("Error: Invalid number! Must be between 0 and " + (lists.size() - 1) + ".");
            }
          } catch(InputMismatchException e) {
            System.out.println("Error: Input must be a number!");
          }

          scnr.nextLine();
        }

        String uri = ptv.App.getPtvUri(scnr);

        if (uri != null) {
          lists.get(input).setPtvUri(uri);
        }
      }

      System.out.println("Select calendars to include countdown items:");
      for (CalendarListEntry c : remainingItems) {
        String summary = c.getSummary();

        if (c.getSummaryOverride() != null) {
          summary = c.getSummaryOverride();
        }

        if (Util.getBoolFromUser(scnr, "Include " + summary + "?")) {
          lists.add(new EventList(c.getId(), summary, ListType.CALENDAR_COUNTDOWN, c.getColorId()));
        }
      }

      pageToken = calendarList.getNextPageToken();
    } while (pageToken != null); 


    // Tasks
    Tasks service = new Tasks.Builder(HTTP_TRANSPORT, JSON_FACTORY, agenda.App.getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();

    TaskLists result = service.tasklists().list()
        .setMaxResults(10)
        .execute();
    List<TaskList> taskLists = result.getItems();
    if (taskLists == null || taskLists.isEmpty()) {
      System.out.println("No task lists found.");
    } else {
      
      System.out.println("Select tasks lists to include agenda items:");

      
      for (TaskList tasklist : taskLists) {
        if (Util.getBoolFromUser(scnr, "Include " + tasklist.getTitle() + "?")) {
          lists.add(new EventList(tasklist.getId(), tasklist.getTitle(), ListType.TASK, DEFAULT_CALENDAR_COLOUR));
        }
        System.out.printf("%s (%s)\n", tasklist.getTitle(), tasklist.getId());
      }
    }

    // Summary
    System.out.println("Lists:");
    for (EventList e : lists) {
      System.out.println(e.name + " - " + e.id);
    }

    scnr.close();

    exportListsToFile(lists);
  }

  /**
   * Method for saving user list preferences to file. - {@code lists.csv}
   * @param lists An {@code ArrayList<EventList>} object containing the lists and associated preferences.
   * @throws FileNotFoundException
   */
  public static void exportListsToFile(ArrayList<EventList> lists) throws FileNotFoundException {
    FileOutputStream outStream = new FileOutputStream("src/main/resources/lists.csv");
    PrintWriter outFS = new PrintWriter(outStream);

    for (EventList e : lists) {
      outFS.println(e.toString());
    }

    outFS.close();
  }
}