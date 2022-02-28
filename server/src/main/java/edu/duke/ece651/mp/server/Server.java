package edu.duke.ece651.mp.server;

import edu.duke.ece651.mp.common.Thing;

public class Server {
  public int factorial(int x) {
    int ans = 1;
    while (x > 0) {
      ans = ans * x;
      x --;
    }
    return ans;
  }
  public static void main(String[] args) {
    Thing t = new Thing("server");
    System.out.println(t);
  }
}













