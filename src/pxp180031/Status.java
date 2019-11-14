package pxp180031;

public enum Status {
  EXPLORE("EXPLORE"),
  REJECT("REJECT"),
  COMPLETE("COMPLETE"),
  LEADER("LEADER");

  private String type;

  private Status(String _type){
    this.type = _type;
  }

  public String getType(){
    return this.type;
  }
}
