package agenda.items;

import agenda.Config;

import com.google.api.client.util.DateTime;

import html.HtmlFactory;

import java.util.InputMismatchException;

public abstract class AgendaItem {
  public String title;
  public String list;
  public String colorHex;
  public DateTime start;
  public DateTime end;
  public Boolean isAllDay;
  public String description;

  public abstract String generateTableRow();
  public abstract String getRawLocation();
  public abstract String getSignificantLocation();

  /**
   * @param time Time string.
   * @param title Name of the event, task, etc...
   * @param list Display name of the list that the event belongs to.
   * @param colorHex Hex value of list colour.
   * @param description Event description
   * @return {@code String} containing html for the {@code AgendaItem} header. 
   */
  protected static String getAgendaItemHeader(String time, String title, String list, String colorHex, String description) {
    String html = HtmlFactory.importHtml("src/main/resources/html_blocks/agenda_item_main.txt");
    
    html = html.replace("$time$", time);
    html = html.replace("$title$", title);
    html = html.replace("$list$", list);
    html = html.replace("$colorHex$", colorHex);

    if (description == null) {
      description = "";
    }

    html = html.replace("$description$", description);

    return html;
  }

  /**
   * Wrap {@code AdendaItem} html blocks in appropriate html tags to format into table.
   * @param contents Complete html blocks to wrap.
   * @return contents {@code Sting} wrapped in appropriate html.
   */
  protected static String wrap(String contents) {
    String html = "<table class='content'>";
    
    html += contents;
    
    html += "<tr><td height='24'></td></tr></table>";

    return html;
  }

  protected static String wrapLocation(String location) {
    String html = "<tr><td></td><td></td><td><p>";
    html += location;
    html += "</p></td></tr>";

    return html;
  }

  /**
   * @param location {@code String} containing the location to navigate to via Google Maps.
   * @return Link to Google Maps directions for the provided location string.
   */
  protected String generateGoogleMapsLink(String location) {
    String mapsLocation = location.replace(" ", "+").replace("/","-");
    
    String link = "https://www.google.com/maps/dir//" + mapsLocation;

    String output = "<a href='" + link + "'target='_blank' rel='noopener noreferrer'>" + location + "</a>";

    return output;
  }

  protected String generateLocationRow(String location, String formattedAddress) {
    if (location == null) {
      return "";
    }

    // Format RMIT room string into readable text.
    if (Config.doRmitLocation) {
      String rmit = parseRmitLocation(location);

      if (rmit != null) {
        return wrapLocation(rmit);
      }
    }
    
    // Return plain text location if location provided isn't valid.
    if (formattedAddress == null) {
      return wrapLocation(location);
    }

    // Return location with link to directions via Google Maps.
    if (Config.doGeoLocation) {
      return wrapLocation(generateGoogleMapsLink(formattedAddress));
    }

    return "";
  }

  private String parseRmitLocation(String location) {
    if (location.length() == 10 &&
        location.charAt(3) == '.' &&
        location.charAt(6) == '.') {
          String building;
          String level;
          String room;
          
          try {
            building = "Building " + Integer.parseInt(location.substring(0, 3)); 
          } catch (InputMismatchException e) {
            e.printStackTrace();
            return null;
          }

          try {
            level = "Level " + Integer.parseInt(location.substring(4, 6)); 
          } catch (InputMismatchException e) {
            e.printStackTrace();
            return null;
          }

          try {
            room = "Room " + Integer.parseInt(location.substring(7, 10)); 
          } catch (InputMismatchException e) {
            e.printStackTrace();
            return null;
          }

          return building + " | " + level + " | " + room;
        }
        
        return null;
  }
}
