package pxp180031;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedItem implements Delayed {
  private int maxId;
  private int senderUID;
  private LocalDateTime activationDateTime;
  private Status status;
  private int round;

  public DelayedItem(int senderUID, int maxId, Status status, LocalDateTime activationDateTime) {
    super();
    this.maxId = maxId;
    this.senderUID = senderUID;
    this.status = status;
    this.activationDateTime = activationDateTime;
  }

  public int getSenderUID() {
    return senderUID;
  }

  public int getMaxId() {
    return maxId;
  }

  public Status getStatus() {
    return status;
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
    return "DelayedEvent [id=" + maxId + ", senderUID= " + senderUID + ", activationDateTime=" + activationDateTime + " ," + getDelay(TimeUnit.MILLISECONDS) + "]";
  }
}
