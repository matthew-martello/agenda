package agenda;

import agenda.items.AgendaItem;
import agenda.items.CalendarEvent;
import agenda.items.CountdownEvent;
import agenda.items.TaskEvent;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.tasks.TasksScopes;

import html.HtmlFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import mail.Mail;

public class App {
  public static final String LISTS_PATH = "src/main/resources/lists.csv";
  public static Colors colors = new Colors();

  public static DateTime now = new DateTime(System.currentTimeMillis());
  public static DateTime nowStartOfDay = new DateTime(Util.toDayBoundaries(now, true));
  public static DateTime nowEndOfDay = new DateTime(Util.toDayBoundaries(now, false));

  private static final String APPLICATION_NAME = "Agenda";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  // If modifying scopes, delete previously saved tokens folder.
  private static final List<String> SCOPES =
      Arrays.asList(
      CalendarScopes.CALENDAR_READONLY,
      TasksScopes.TASKS_READONLY,
      GmailScopes.GMAIL_SEND
    );

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  public static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
      throws IOException {
    // Load client secrets.
    InputStream in = App.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    //returns an authorized Credential object.
    return credential;
  }

  public static void main(String[] args) throws Exception {
    new Config();
    
    ArrayList<EventList> lists = validateLists();    
    
    ArrayList<AgendaItem> items = new ArrayList<AgendaItem>();

    String ptvList = null;
    String ptvUri = null;

    //Populate AgendaItem list.
    for (EventList list : lists) {
      switch (list.type) {
        case CALENDAR:
          CalendarEvent.getCalendarEvents(items, list);
          break;
        case CALENDAR_COUNTDOWN:
          CountdownEvent.getCountdownEvents(items, list);
          break;
        case TASK:
          TaskEvent.getTasks(items, list);
      }

      if (list.ptvUri != null) {
        ptvList = list.name;
        ptvUri = list.ptvUri;
      }
    }

    //Order the agenda
    Util.sortByDateTime(items);

    // Initialise the significant location with config preference.
    String significantLocation = Config.DEFAULT_LOCATION;

    for (AgendaItem a : items) {
      String itemLocation = a.getSignificantLocation();

      if (itemLocation == null) {
        continue;
      }

      significantLocation = itemLocation;
    }
    
    // Get latest possible departure time for PTV Timetabling data.
    DateTime latestDepartureTime = null;
    String targetEvent = null; //Title of event to calculate PTV around.

    if (Config.doPTV) {
      for (AgendaItem a : items) {
        if (a.list.equals(ptvList) && a.start.toString().length() > 10) {
          latestDepartureTime = a.start;
          targetEvent = a.title;
          break;
        } 
      }
    }

    String[] ptvInfo = ptv.App.getPtvInfo(ptvUri, latestDepartureTime, targetEvent);

    // Generate html
    HtmlFactory htmlFactory = new HtmlFactory();
    String html = htmlFactory.generateHtml(items, significantLocation, ptvInfo);
    
    // Send the email
    String userEmail = getUserEmail();
    String subject = "Agenda for " + Util.parseTodaysDateToHeader();
    Mail.sendEmail(userEmail, userEmail, subject, html);

    //debug (Export template)
    FileOutputStream outStream = new FileOutputStream("src/main/resources/output.html");
    PrintWriter outFS = new PrintWriter(outStream);
    outFS.println(html);
    outFS.close();

    // Updating config
    Config.writeToFile();
  }

  /**
   * Checks to determine if lists file exists and has at least 1  usable list. <p>
   * If checks fail, it prompts the user to generate a new lists file.
   * @return {@code ArrayList} of {@code EventList} objects.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static ArrayList<EventList> validateLists() throws IOException, GeneralSecurityException {
    System.out.println("Importing lists.csv...");
    
    ArrayList<EventList> lists = new ArrayList<EventList>();

    while (lists.isEmpty()) {
      try {
        FileInputStream inStream = new FileInputStream(LISTS_PATH);
        Scanner inFS = new Scanner(inStream);
        inFS.useDelimiter(",");

        int i = 0;
        while (inFS.hasNextLine()) {
          ++i;

          String type = inFS.next();
          ListType listType;
          
          switch (type) {
            case "CALENDAR":
              listType = ListType.CALENDAR;
              break;
            case "CALENDAR_COUNTDOWN":
              listType = ListType.CALENDAR_COUNTDOWN;
              break;
            case "TASK":
              listType = ListType.TASK;
              break;
            default:
              listType = null;
          }

          if (listType == null) {
            System.err.println("[ERROR] Invalid listType on line " + i + "! Skipping...");
            inFS.nextLine();
            continue;
          }

          String id = inFS.next();
          String name = inFS.next();
          
          String colourId = inFS.next();
          
          if (Integer.parseInt(colourId) < 0 || Integer.parseInt(colourId) > 24) {
            colourId = ListBuilder.DEFAULT_CALENDAR_COLOUR;
            System.err.println("[WARNING] Invalid colourId on line " + i + ", using default...");
          }

          String uri = inFS.nextLine().substring(1);

          EventList el = new EventList(id, name, listType, colourId);

          if (!uri.equals("null") && Config.doPTV) {
            String testCall = ptv.App.callPTVApi(uri);
            
            if (testCall.indexOf("404 - File or directory not found.") >= 0) {
              System.err.println("[ERROR] Invalid PTV uri on line " + i + "! Discarding...");
              continue;
            }
            
            el.setPtvUri(uri);
          }

          lists.add(el);
        }

        inFS.close();

      } catch(FileNotFoundException e) {
        // If a list file isn't found, prompt the user to create a new one.
        System.out.println("File not found! Generating new file...");
        ListBuilder.generateListsFile();
      }
    }

    return lists;
  }

  /**
   * Fetch the users email address from the primary calendar.
   * 
   * (Not the right way to do this, but having trouble with Oauth and
   * retrieving userInfo.)
   * 
   * @return {@code String} containing the user's Google email address.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static String getUserEmail() throws IOException, GeneralSecurityException {
    String userEmail = null;  
    
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Calendar service =
        new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

        List<CalendarListEntry> items;

        String pageToken = null;
        do {
          CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
          items = calendarList.getItems();

          for (CalendarListEntry c : items) {
            if (c.isPrimary()) {
              userEmail = c.getId();
            }
          }

          pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return userEmail;
  }
}
