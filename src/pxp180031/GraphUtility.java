package pxp180031;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

public class GraphUtility {

  public void findDiameter(int[] uIds, HashMap<Integer, List<Integer>> adj) {
    HashMap<Integer, Boolean> visited = new HashMap<>();
    for (int uid: uIds) visited.put(uid, false);
    dfs(uIds[0], visited, 0, adj);
  }

  public void dfs(int uid, HashMap<Integer, Boolean> visited, int pathLen, HashMap<Integer, List<Integer>> adj) {
    if (visited.get(uid)) return;
    visited.put(uid, true);
    for (int v: adj.get(uid)) {
      if (!visited.get(v)) dfs(v, visited, pathLen + 1, adj);
    }
    // diameter = Math.max(diameter, pathLen);
  }

  /**
   * Method to find maximum diameter in graph topology
   * @param graph representing network topology
   * @param n no. of vertices
   * @return maximum diameter
   */
  public static int findMaxDiameter(boolean[][] graph, int n){
    int maxDiameter = 0;
    for(int i=0; i<n; i++){
      maxDiameter = Math.max(maxDiameter, findDiameter(i, graph, n));
    }
    return maxDiameter;
  }

  /**
   * Method to find diameter with vertex as a source in graph topology
   * @param vertex source vertex
   * @param graph representing network topology
   * @param n no. of vertices
   * @return diameter with vertex as source
   */
  private static int findDiameter(int vertex, boolean[][] graph, int n){
    int diameter = -1;
    Deque<Integer> q = new ArrayDeque<>();
    boolean[] seen = new boolean[n];
    q.addFirst(vertex);
    seen[vertex] = true;
    while(!q.isEmpty()){
      int size = q.size();
      diameter++;
      for(int i=0; i<size; i++){
        int curVertex = q.removeLast();
        for(int j=0; j<n; j++){
          if(graph[curVertex][j] && !seen[j]){
            q.addFirst(j);
            seen[j] = true;
          }
        }
      }
    }
    return diameter;
  }
}

