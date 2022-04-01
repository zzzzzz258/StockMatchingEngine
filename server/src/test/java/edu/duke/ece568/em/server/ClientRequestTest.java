package edu.duke.ece568.em.server;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import edu.duke.ece568.em.client.Client;

public class ClientRequestTest {
  private void callClient(String fileName) {
    System.out.println("\n" + fileName);
    try {
      Client.main(new String[] {fileName});
    } catch (Exception e) {
      System.out.println("Error in connecting to server: " + e.getMessage());
    }
  }
  
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

    // create an account 123456
    callClient("create1.xml");

    // create account 12345
    callClient("create2.xml");
    
    // standard trasactions file, find 123456
    callClient("transaction.xml");

    // missign account id
    callClient("transaction_no_account_id.xml");
    
    // invalid account id
    callClient("transaction_invalid_account_id.xml");

    // missing children
    callClient("transaction_no_children.xml");

    // containing invalid children tag
    callClient("transaction_invalid_child_tag.xml");
//TODO: match and deal
    // normal and abnormal orders
    callClient("transaction_orders.xml");

    // do businesses
    callClient("transaction_orders2.xml");
    callClient("transaction_orders3.xml");
    
  }

}
