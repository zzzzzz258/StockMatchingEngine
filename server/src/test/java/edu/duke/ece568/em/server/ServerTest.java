package edu.duke.ece651.mp.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ServerTest {
  @Test
  public void test_factorial() {
    Server s = new Server();
    assertEquals(6, s.factorial(3));
  }

}
