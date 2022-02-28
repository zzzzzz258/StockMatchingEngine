package edu.duke.ece651.mp.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ClientTest {
  @Test
  public void test_sum() {
    Client c = new Client();
    assertEquals(10, c.sum(4));
  }

}
