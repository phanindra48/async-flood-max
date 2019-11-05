package pxp180031;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedItem implements Delayed {
  private int id;
  private LocalDateTime activationDateTime;
  private String name;
  private int round;

  public DelayedItem(int id, int round, String name, LocalDateTime activationDateTime) {
    super();
    this.id = id;
    this.name = name;
    this.round = round;
    this.activationDateTime = activationDateTime;
  }

  public int getId() {
    return id;
  }

  public int getRound() {
    return round;
  }

  public LocalDateTime getActivationDateTime() {
    return activationDateTime;
  }

  @Override
  public int compareTo(Delayed that) {
    long result = this.getDelay(TimeUnit.NANOSECONDS) - that.getDelay(TimeUnit.NANOSECONDS);
    if (result < 0) {
      return -1;
    } else if (result > 0) {
      return 1;
    }
    return 0;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    LocalDateTime now = LocalDateTime.now();
    long diff = now.until(activationDateTime, ChronoUnit.MILLIS);
    return unit.convert(diff, TimeUnit.MILLISECONDS);
  }

  @Override
  public String toString() {
    return "DelayedEvent [id=" + id + ", name= " + name + ", activationDateTime=" + activationDateTime + " ," + getDelay(TimeUnit.MILLISECONDS) + "]";
  }
}
