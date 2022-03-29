package edu.duke.ece568.em.server;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import edu.duke.ece568.em.client.Client;

public class ClientRequestTest {
  @Test
  public void test_processTransactionRequest() throws IOException, InterruptedException {
    Thread th = new Thread() {
        @Override
        public void run() {
          try {
            Server.main(null);
          }
          catch(Exception e) {
          }
        }
      };
    th.start();
    Thread.sleep(100);
    
    try {
      Client.main(null);
    } catch (Exception e) {
      System.out.println("Error in connecting to server: " + e.getMessage());
    }

    
  }

}
