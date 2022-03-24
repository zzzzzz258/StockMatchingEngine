package edu.duke.ece568.em.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ServerTest {
  @Test
  public void test_() {
    try {
      Server s = new Server(12345);
      assertEquals(12345, s.getListenerPort());
    } catch (Exception e) {
      // print exception message about Throwable object
      e.printStackTrace();
    }
  }

}
