package agenda;

public class EventList {
  public ListType type;

  public String id;
  public String name;
  public String colorId;

  public String ptvUri;

  public EventList(String id, String name, ListType type, String colorId) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.colorId = colorId;
    this.ptvUri = null;
  }

  public void setPtvUri(String uri) {
    this.ptvUri = uri;
  }

  public String toString() {
    return this.type + "," + this.id + "," + this.name + "," + this.colorId + "," + this.ptvUri;
  }
}
