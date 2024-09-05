package geolocation;

public class Coordinates {
  public String suburb;
  public double lat;
  public double lon;

  public Coordinates(String suburb, double lat, double lon) {
    this.suburb = suburb;
    this.lat = lat;
    this.lon = lon;
  }

  public String toString() {
    return this.lat + ", " + this.lon;
  }
}
