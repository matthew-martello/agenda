package agenda.items;

import agenda.*;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.Tasks;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class TaskEvent extends AgendaItem {
  
  private static final String APPLICATION_NAME = "Agenda - CountdownEvent";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  public static DateTime now = App.now;
  public static DateTime nowStartOfDay = App.nowStartOfDay;
  public static DateTime nowEndOfDay = App.nowEndOfDay;

  public TaskEvent( String title,
                    String list,
                    String colorHex, 
                    DateTime start, 
                    String description) {
                      this.title = title;
                      this.list = list;
                      this.colorHex = colorHex;
                      this.start = start;
                      this.end = this.start;
                      this.description = description;

                      //Tasks API doesn't return times, so treat it as an all day event.
                      this.isAllDay = true;
  }

  public static void getTasks(ArrayList<AgendaItem> agendaItems, EventList list) throws IOException, GeneralSecurityException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    
    Tasks taskService = new Tasks.Builder(HTTP_TRANSPORT, JSON_FACTORY, App.getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();

    List<Task> tasks = taskService.tasks()
        .list(list.id)
        .execute()
        .getItems();
    
    if (tasks.isEmpty()) {
      System.out.println("No tasks found in '" + list.name + "'\n");
    } else {
      String taskString = (tasks.size() == 1) ? " task" : " tasks";
      System.out.println("Found " + tasks.size() + taskString + " in '" + list.name + "'");

      for (Task task : tasks) {
        String title = task.getTitle();
        
        /* If the task is due on a date different to today, it is excluded from the list.
        * Otherwise if a due date isn't specified or the due date is today, it is added as normal.
        * I can't figure out how to get dueMin() and dueMax() to work, so this is my work around.
        */
        DateTime due;
        if (task.getDue() == null) {
          due = nowEndOfDay;
        } else {
          due = new DateTime(Util.forceTimezone(task.getDue(), Config.getTimezoneOffset(), true));

          //If the due date is after today, skip...
          if (Util.parseDateFromRFC3339(task.getDue().toString())
              .compareToIgnoreCase(Util.parseDateFromRFC3339(now.toString())) > 0) {
                continue;
          }
        }

        String description = task.getNotes();

        String colorHex = Colors.getHex(Integer.parseInt(list.colorId), true, Config.doModernColours);

        agendaItems.add(new TaskEvent(title, list.name, colorHex, due, description));
      }
    }
  }

  public String generateTableRow() {
    String html = AgendaItem.getAgendaItemHeader(this.getDueDate(), this.title, this.list, this.colorHex, this.description);

    html = html.replace("$location$", "");

    return AgendaItem.wrap(html);
  }
  
  public String getDueDate() {
    DateTime now = new DateTime(System.currentTimeMillis());

    String dueDate = Util.parseDateFromRFC3339(this.start.toString());
    String todayDate = Util.parseDateFromRFC3339(now.toString());
    
    // Overdue
    if (dueDate.compareToIgnoreCase(todayDate) < 0) {
      return "<p style='color: #D50000'><b>Overdue</b></p>";
    }
    
    // Due today
    if (this.start.toString().indexOf("23:59:59.999") == -1) {
      return "<p>Due today</p>"; 
    }

    return "";
  }

  public String getSignificantLocation() {
    return null;
  }

  public String getRawLocation() {
    return null;
  }
}
