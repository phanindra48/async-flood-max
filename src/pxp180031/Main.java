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
import java.util.stream.Collectors;

class Node implements Runnable {
  Thread process;
  String name;
  int diameter;
  int uid;
  volatile int round;
  List<Integer> neighbors;
  volatile int maxUID;
  DelayQueue<DelayedItem> queue;
  String status;
  int messageCount;

  MasterNode master;

  public Node(int id, int diameter, MasterNode master, List<Integer> neighbors, DelayQueue<DelayedItem> queue) {
    this.name = id + "";
    this.uid = id;
    this.maxUID = id;
    this.round = 0;

    this.master = master;
    this.neighbors = neighbors;
    this.queue = queue;
    this.diameter = diameter;
    this.status = "UNKNOWN";
  }

  @Override
  public void run() {
    List<DelayedItem> items = new ArrayList<>();
    try {
      while (true) {
        if (round < diameter) {
          this.master.sendMessage(uid, round, maxUID);
          this.messageCount += this.neighbors.size();
        }

        while (items.size() < neighbors.size()) {
          // System.out.println("Waiting for neighbors...");
          queue.drainTo(items);
        }

        // add items back if they are not of same round
        queue.addAll(items.stream().filter(i -> i.getRound() != round).collect(Collectors.toList()));

        round++;

        // Get max of all incoming messages
        maxUID = Math.max(maxUID, items.stream().max(Comparator.comparing(DelayedItem::getId)).get().getId());

        items = new ArrayList<>();

        if (round == diameter) {
          if (maxUID == uid) {
            status = "LEADER";
            System.out.printf("%s is elected as leader\n", uid);
            this.master.leaderElected();
          } else {
            status = "NON_LEADER";
            System.out.printf("%s is not a leader\n", uid);
          }

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
  int diameter;
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

  public MasterNode(String name, int n, int[] uIds, HashMap<Integer, List<Integer>> adj, boolean[][] graph) {
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

    this.diameter = GraphUtility.findMaxDiameter(graph, n);
  }

  public boolean sendMessage (int uId, int round, int message) {
    // send to neighbors
    for (int neighbor: adj.get(uId)) {
      // Generate random numbers
      int randNum = rand.nextInt(MAX_TIME_UNITS) + MIN_TIME_UNITS;
      LocalDateTime delay = LocalDateTime.now().plusNanos(randNum * FACTOR);
      DelayedItem item = new DelayedItem(message, round, "Sender-" + uId + "-Round-" + round, delay);
      // System.out.println(neighbor + " " + item);
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

  public void updateMessageCounts(int uid, int count) {
    messageCounts.put(uid, count);
  }

  @Override
  public void run() {
    // Create all nodes
    nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      nodes[i] = new Node(uIds[i], diameter, this, adj.get(uIds[i]), queueMap.get(uIds[i]));
    }

    // Start all nodes
    for (Node node: nodes) {
      node.start();
    }

    try {
      while (true) {
        // Hold master if round is in progress
        if (!isLeaderElected || messageCounts.size() < n) continue;
        System.out.println("Leader elected: " + isLeaderElected);


        int totalMessageCount = 0;
        int totalEdges = 0;
        for (int key: messageCounts.keySet()) {
          int count = messageCounts.get(key);
          totalEdges += adj.get(key).size();
          int neighborCount = adj.get(key).size();
          totalMessageCount += count;
          System.out.printf("UID: %s, Message Count: %s, Neighbors: %s, Diam * Neighbors: %s \n", key, count, adj.get(key).size(), (diameter * neighborCount));
        }
        System.out.println("\nDiameter of the graph: " + diameter);
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

    int[] uIds = new int[n];
    for( int i = 0; i < n; i++) {
      uIds[i] = in.nextInt();
    }

    System.out.println("Process UIDs:");
    for (int num: uIds) System.out.print(num + " ");
    System.out.println();

    int counter = 0;
    HashMap<Integer, List<Integer>> neighbors = new HashMap();

    // TODO: make use of adjacency list instead
    boolean[][] graph = new boolean[n][n];
    while (counter < n) {
      boolean[] edges = new boolean[n];
      for( int i = 0; i < n; i++) {
        if (in.hasNext() && in.nextInt() == 1) {
          neighbors.computeIfAbsent(uIds[counter], k -> new ArrayList<>()).add(uIds[i]);
          edges[i] = true;
        }
      }
      graph[counter] = edges;
      counter++;
    }
    System.out.println("adj: " + neighbors);

    MasterNode master = new MasterNode("master", n, uIds, neighbors, graph);
    master.start();
  }
}
