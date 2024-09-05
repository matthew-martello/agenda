package html;

import agenda.items.AgendaItem;
import agenda.Util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import weather.WeatherFactory;

public class HtmlFactory {

  private static String importHtml() {
    String html = "";

    try {
      FileInputStream inStream = new FileInputStream("src/main/resources/html_blocks/template.html");
      Scanner inFS = new Scanner(inStream);

      while (inFS.hasNextLine()) {
        html += inFS.nextLine() + "\n";
      }
      
      inFS.close();
    } catch(FileNotFoundException e) {
      System.err.println("[ERROR]: Could not find 'template.html'!");
      System.exit(-1);
    }
    return html;
  }

  /**
   * Imports html blocks to build appropriate agenda item html.
   * @param blockPath Path to file containing partial/incomplete html.
   * @return Imported html as a string.
   */
  public static String importHtml(String blockPath) {
    String html = "";

    try {
      FileInputStream inStream = new FileInputStream(blockPath);
      Scanner inFS = new Scanner(inStream);

      while (inFS.hasNextLine()) {
        html += inFS.nextLine() + "\n";
      }
      
      inFS.close();
    } catch(FileNotFoundException e) {
      System.err.println("[ERROR]: Could not find 'template.html'!");
      System.exit(-1);
    }
    return html;
  }

  /**
   * Builds the html string to send as an email
   * @param items ArrayList of {@code AgendaItems} containing the events and tasks for the day.
   * @param significantLocation {@code String} containing the first significant location for the day. (Used for weather data.) 
   * @return
   */
  public String generateHtml(ArrayList<AgendaItem> items, String significantLocation, String[] ptvInfo) {
    String html = importHtml();

    // Day of week
    html = html.replace("$day$", Util.getDayOfWeek());

    // Date
    html = html.replace("$date$", Util.parseTodaysDateToHeader());

    // Weather
    WeatherFactory wf = new WeatherFactory(significantLocation);
    String[] weatherInfo = wf.getWeatherInfo();
    
    if (weatherInfo == null) {
      html = html.replace("$weather$", "");
    } else {
      String weatherHtml = HtmlFactory.importHtml("src/main/resources/html_blocks/weather.txt");

      weatherHtml = weatherHtml.replace("$icon$", weatherInfo[0]);
      weatherHtml = weatherHtml.replace("$conditions$", weatherInfo[1]);
      weatherHtml = weatherHtml.replace("$min$", weatherInfo[2]);
      weatherHtml = weatherHtml.replace("$max$", weatherInfo[3]);

      if (wf.suburb == null) {
        weatherHtml = weatherHtml.replace("$suburb$", "");
      } else {
        weatherHtml = weatherHtml.replace("$suburb$", " in " + wf.suburb);
      }

      // Insert weather module
      html = html.replace("$weather$", weatherHtml);

      // Weather disclaimer
      String disclaimer = HtmlFactory.importHtml("src/main/resources/html_blocks/weather_disclaimer.txt");
      html = html.replace("$wDisclaimer$", disclaimer);
    }

    if (ptvInfo == null) {
      html = html.replace("$ptv$", "");
      html = html.replace("$disclaimer$", "");
    } else {
      String ptvHtml = importHtml("src/main/resources/html_blocks/ptv.txt");

      ptvHtml = ptvHtml.replace("$optimal$", ptvInfo[2]);
      ptvHtml = ptvHtml.replace("$station$", ptvInfo[0]);
      ptvHtml = ptvHtml.replace("$title$", ptvInfo[4]);
      ptvHtml = ptvHtml.replace("$start$", ptvInfo[5]);
      ptvHtml = ptvHtml.replace("$earlier$", ptvInfo[1]);
      ptvHtml = ptvHtml.replace("$later$", ptvInfo[3]);
      
      html = html.replace("$ptv$", ptvHtml);

      // PTV Disclaimer
      String disclaimer = importHtml("src/main/resources/html_blocks/ptv_disclaimer.txt");
      html = html.replace("$disclaimer$", disclaimer);
    }

    // Populate list
    for (int i = 0; i < items.size(); ++i) {
      if (i < (items.size() - 1)) {
        html = html.replace("$$$", items.get(i).generateTableRow() + "\n$$$"); 
      } else {
        html = html.replace("$$$", items.get(i).generateTableRow() + "\n");
      }
    }

    return html;
  }
}