package agenda;

import java.util.ArrayList;

public class Colors {
  public static ArrayList<Colors> calendarColors = new ArrayList<Colors>();
  public static ArrayList<Colors> eventColors = new ArrayList<Colors>();
  
  public int colorId;
  public String colorName;
  public String classicHex;
  public String modernHex;

  public Colors() {
    calendarColors.add(new Colors(1, "Cocoa", "#AC725E", "#795548"));
    calendarColors.add(new Colors(2, "Flamingo", "#D06B64", "#E67C73"));
    calendarColors.add(new Colors(3, "Tomato", "#F83A22", "#D50000"));
    calendarColors.add(new Colors(4, "Tangerine", "#FA573C", "#F4511E"));
    calendarColors.add(new Colors(5, "Pumpkin", "#FF7537", "#EF6C00"));
    calendarColors.add(new Colors(6, "Mango", "#FFAD46", "#F09300"));
    calendarColors.add(new Colors(7, "Eucalyptus", "#42D692", "#009688"));
    calendarColors.add(new Colors(8, "Basil", "#16A765", "#0B8043"));
    calendarColors.add(new Colors(9, "Pistachio", "#7BD148", "#7CB342"));
    calendarColors.add(new Colors(10, "Avocado", "#B3DC6C", "#C0CA33"));
    calendarColors.add(new Colors(11, "Citron", "#FBE983", "#E4C441"));
    calendarColors.add(new Colors(12, "Banana", "#FAD165", "#F6BF26"));
    calendarColors.add(new Colors(13, "Sage", "#92E1C0", "#33B679"));
    calendarColors.add(new Colors(14, "Peacock", "#9FE1E7", "#039BE5"));
    calendarColors.add(new Colors(15, "Cobalt", "#9FC6E7", "#4285F4"));
    calendarColors.add(new Colors(16, "Blueberry", "#4986E7", "#3F51B5"));
    calendarColors.add(new Colors(17, "Lavender", "#9A9CFF", "#7986CB"));
    calendarColors.add(new Colors(18, "Wisteria", "#B99AFF", "#B39DDB"));
    calendarColors.add(new Colors(19, "Graphite", "#C2C2C2", "#616161"));
    calendarColors.add(new Colors(20, "Birch", "#CABDBF", "#A79B8E"));
    calendarColors.add(new Colors(21, "Radicchio", "#CCA6AC", "#AD1457"));
    calendarColors.add(new Colors(22, "Cherry Blossom", "#F691B2", "#D81B60"));
    calendarColors.add(new Colors(23, "Grape", "#CD74E6", "#8E24AA"));
    calendarColors.add(new Colors(24, "Amethyst", "#A47AE2", "#9E69AF"));

    eventColors.add(new Colors(1, "Lavender", "#A4BDFC", "#7986CB"));
    eventColors.add(new Colors(2, "Sage", "#7AE7BF", "#33B679"));
    eventColors.add(new Colors(3, "Grape", "#DBADFF", "#8E24AA"));
    eventColors.add(new Colors(4, "Flamingo", "#FF887C", "#E67C73"));
    eventColors.add(new Colors(5, "Banana", "#FBD75B", "#F6BF26"));
    eventColors.add(new Colors(6, "Tangerine", "#FFB878", "#F4511E"));
    eventColors.add(new Colors(7, "Peacock", "#46D6DB", "#039BE5"));
    eventColors.add(new Colors(8, "Graphite", "#E1E1E1", "#616161"));
    eventColors.add(new Colors(9, "Blueberry", "#5484ED", "#3F51B5"));
    eventColors.add(new Colors(10, "Basil", "#51B749", "#0B8043"));
    eventColors.add(new Colors(11, "Tomato", "#DC2127", "#D50000"));
  }

  public Colors(int colorId, String colorName, String classicHex, String modernHex) {
    this.colorId = colorId;
    this.colorName = colorName;
    this.classicHex = classicHex;
    this.modernHex = modernHex;
  }

  /**
   * Convert the Google colorId to a hex value
   * @param id Google colorId.
   * @param isList If true, looks up id agaisnt calendar colour list. Event colour list if false.
   * @param modern If true, uses the newer 'modern' colour hexes. Classic colour hexes if false.
   * @return The hex value of the colour id matching the list colour.
   */
  public static String getHex(int id, Boolean isList, Boolean modern) {
    if (isList) {
      for (Colors c : calendarColors) {
        if (id == c.colorId) {
          return (!modern) ? c.classicHex : c.modernHex;
        }
      }
    }

    for (Colors c : eventColors) {
      if (id == c.colorId) {
        return (!modern) ? c.classicHex : c.modernHex;
      }
    }
    
    return "";
  }
}
