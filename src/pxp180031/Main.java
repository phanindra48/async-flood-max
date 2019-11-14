/**
 * TEAM:
 * Phanindra Pydisetty PXP180031
 * Divya Gummadapu DXG170018
 */
package pxp180031;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.DelayQueue;

class Node implements Runnable {
  Thread process;
  String name;
  int uid;
  List<Integer> neighbors;
  volatile int maxUID;
  DelayQueue<DelayedItem> queue;
  String status;
  int messageCount;

  int rejectCount;
  int completeCount;
  boolean init;
  int neighborCount;
  int parentId;

  MasterNode master;

  public Node(int id, MasterNode master, List<Integer> neighbors, DelayQueue<DelayedItem> queue) {
    this.name = id + "";
    this.uid = id;
    this.maxUID = id;

    this.rejectCount = 0;
    this.completeCount = 0;
    this.init = true;


    this.master = master;
    this.neighbors = neighbors;
    this.neighborCount = neighbors.size();
    this.queue = queue;
    this.parentId = uid;
    this.status = "UNKNOWN";
  }

  @Override
  public void run() {
    List<DelayedItem> items = new ArrayList<>();
    try {
      this.master.sendMessage(uid, uid, Status.EXPLORE, null, null);
      this.messageCount += neighborCount;

      while (true) {
        // Take available message from the queue 
        DelayedItem item = queue.take();
        int senderUID = item.getSenderUID();
        int incomingMaxId = item.getMaxId();
        Status incomingStatus = item.getStatus();

        if (incomingStatus.equals(Status.EXPLORE)) {
          // if new max node is found, send reject to previous parent
          if (incomingMaxId > maxUID) {
            if (maxUID != uid) {
              this.messageCount++;
              this.master.sendMessage(uid, incomingMaxId, Status.REJECT, null, parentId);
            }
            this.parentId = senderUID;
            this.maxUID = incomingMaxId;

            // send explore to neighbors except to parent node
            this.messageCount += this.neighborCount - 1;
            this.master.sendMessage(uid, maxUID, Status.EXPLORE, parentId, null);

          } else {
            this.messageCount++;
            this.master.sendMessage(uid, maxUID, Status.REJECT, null, senderUID);
          }
        } else if (incomingStatus.equals(Status.COMPLETE) || incomingStatus.equals(Status.REJECT)) {
          if (incomingStatus.equals(Status.COMPLETE)) completeCount++;
          else rejectCount++;

          if (
            (rejectCount == neighborCount ||
              (rejectCount + completeCount == neighborCount)) &&
              parentId != uid
          ) {
            messageCount++;

            // update parent that its complete
            this.master.sendMessage(uid, maxUID, Status.COMPLETE, null, parentId);
          } else if (completeCount == neighborCount) {
            this.messageCount += neighborCount;

            // let master know that leader got elected
            this.master.leaderElected();

            // update all neighbors about leader election
            System.out.printf("%s is leader\n", uid);
            this.master.updateMessageCounts(uid, messageCount);
            this.master.sendMessage(uid, maxUID, Status.LEADER, null, null);
          }
        } else if (incomingStatus.equals(Status.LEADER)) {
          System.out.printf("%s is not a leader\n", uid);
          // pass leader broadcast message along all tree edges
          this.master.sendMessage(uid, maxUID, Status.LEADER, senderUID, null);
          this.messageCount += neighborCount - 1;
          // System.out.printf("UID %s Message count %s\n", uid, messageCount);
          this.master.updateMessageCounts(uid, messageCount);
          break;
        }
      }

    } catch (Exception ex) {
      System.err.println(ex);
    }
  }

  public void start() {
    System.out.println("Node " + name + " started");
    if (process == null) {
      process = new Thread(this, name);
      process.start();
    }
  }
}

class MasterNode implements Runnable {
  Thread process;
  String name;
  // Number of threads to create
  int n;
  volatile boolean isLeaderElected = false;
  Node[] nodes;
  int[] uIds;
  HashMap<Integer, List<Integer>> adj;
  HashMap<Integer, Integer> messageCounts;
  Random rand;
  final int FACTOR = 10_000;
  final int MAX_TIME_UNITS = 10;
  final int MIN_TIME_UNITS = 1;

  // Create FIFO queues
  HashMap<Integer, DelayQueue<DelayedItem>> queueMap;

  public MasterNode(String name, int n, int[] uIds, HashMap<Integer, List<Integer>> adj) {
    this.name = name;
    this.uIds = uIds;
    this.n = n;
    this.adj = adj;
    this.messageCounts = new HashMap<>();
    queueMap = new HashMap<>();
    rand = new Random();
    
    for (int i = 0; i < n; i++) {
      queueMap.put(uIds[i], new DelayQueue<>());
    }
  }

  public boolean sendMessage (int senderId, int maxId, Status status, Integer ignoreUID, Integer parentId) {
    if (parentId != null) {
      int randNum = rand.nextInt(MAX_TIME_UNITS) + MIN_TIME_UNITS;
      LocalDateTime delay = LocalDateTime.now().plusNanos(randNum * FACTOR);
      DelayedItem item = new DelayedItem(senderId, maxId, status, delay);
      // Add item to the neighbor's queue
      queueMap.get(parentId).offer(item);
      return true;
    }
    // Send messages to neighbors
    for (int neighbor: adj.get(senderId)) {
      if (ignoreUID != null && ignoreUID == neighbor) continue;
      // Generate random numbers
      int randNum = rand.nextInt(MAX_TIME_UNITS) + MIN_TIME_UNITS;
      LocalDateTime delay = LocalDateTime.now().plusNanos(randNum * FACTOR);
      DelayedItem item = new DelayedItem(senderId, maxId, status, delay);
      // Add item to the neighbor's queue
      queueMap.get(neighbor).offer(item);
    }
    return true;
  }

  /**
   * Elected child node notifies master to terminate
   */
  public void leaderElected() {
    this.isLeaderElected = true;
  }
  // Add message counts of each node to a hashmap
  public void updateMessageCounts(int uid, int count) {
    messageCounts.put(uid, count);
  }

  @Override
  public void run() {
    // Create all nodes
    nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      nodes[i] = new Node(uIds[i], this, adj.get(uIds[i]), queueMap.get(uIds[i]));
    }

    // Start all nodes
    for (Node node: nodes) {
      node.start();
    }

    try {
      while (true) {
        // Hold master if round is in progress
        if (!isLeaderElected || messageCounts.size() < n) continue;

        int totalMessageCount = 0;
        int totalEdges = 0;

        // Print statistics
        for (int key: messageCounts.keySet()) {
          int count = messageCounts.get(key);
          totalEdges += adj.get(key).size();
          totalMessageCount += count;
          System.out.printf("UID: %s, Message Count: %s, Neighbors: %s \n", key, count, adj.get(key).size());
        }

        System.out.println("Total edges: " + totalEdges);
        System.out.println("Total messages sent: " + totalMessageCount);
        break;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void start() {
    System.out.println("Starting " + name + " thread ");
    if (process == null) {
      process = new Thread(this, name);
      process.start();
    }
  }
}

public class Main {
  public static void main(String[] args) {
    // Read the input file
    File file = new File("connectivity3.txt");
    Scanner in;
    try {
      in = new Scanner(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    int n = 0;
    if(in.hasNext())
      n = in.nextInt();
    //Copy the ids in the input file into an array
    int[] uIds = new int[n];
    for( int i = 0; i < n; i++) {
      uIds[i] = in.nextInt();
    }

    System.out.println("Process UIDs:");
    for (int num: uIds) System.out.print(num + " ");
    System.out.println();

    int counter = 0;
    //Adjacency Lists of all nodes
    HashMap<Integer, List<Integer>> neighbors = new HashMap();

    while (counter < n) {
      boolean[] edges = new boolean[n];
      for( int i = 0; i < n; i++) {
        if (in.hasNext() && in.nextInt() == 1) {
          neighbors.computeIfAbsent(uIds[counter], k -> new ArrayList<>()).add(uIds[i]);
          edges[i] = true;
        }
      }
      counter++;
    }
    System.out.println("adj: " + neighbors);
    //Create Master Node
    MasterNode master = new MasterNode("master", n, uIds, neighbors);
    master.start();
  }
}
