package edu.duke.ece568.em.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
  Socket theClientSocket;

  /**
   * Constructor for Client to setup Socket
   * 
   * @param hostname and port number of server
   */
  public Client(String hostname, int portNum) throws IOException {
    theClientSocket = new Socket(hostname, portNum);
  }

  /**
   * Main method for Client Application
   */
  public static void main(String[] args) {
    String hostname = "127.0.0.1";
    int portNum = 12345;
    try {
      Client theClient = new Client(hostname, portNum);
      theClient.runSampleCreateTest();
    } catch (Exception e) {
      System.out.println("Error in connecting to server: " + e.getMessage());
    }
  }

  /**
   * Method for sending sample create request to Client
   */
  public void runSampleCreateTest() throws IOException {
    /*
     * String msg = "Client is ready!"; theClient.sendToServer(msg); msg =
     * (String)theClient.receiveFromServer(); System.out.println("Server sent: " +
     * msg);
     */
    sendSampleCreateRequest();
    String msg = (String) receiveResponseFromServer();
    System.out.println("Server sent: " + msg);
  }

  /**
   * Method for sending sample create request to Client
   */
  public void runSampleCreateTest(String accountID, String sym) throws IOException {
    /*
     * String msg = "Client is ready!"; theClient.sendToServer(msg); msg =
     * (String)theClient.receiveFromServer(); System.out.println("Server sent: " +
     * msg);
     */
    sendSampleCreateRequest(accountID, sym);
    String msg = (String) receiveResponseFromServer();
    System.out.println("Server sent: " + msg);
  }

  /**
   * method to receive anything from server
   * 
   * @return received object
   */
  public Object receiveFromServer() {
    try {
      InputStream o = theClientSocket.getInputStream();
      ObjectInputStream s = new ObjectInputStream(o);
      Object obj = s.readObject();
      return obj;

    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println("Error during serialization");
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Method to send a request
   */
  private void sendSampleCreateRequest() throws IOException {
    String accountID = "123456";
    String sym = "SPY";
    sendSampleCreateRequest(accountID, sym);
  }

  /**
   * Method to send a request
   */
  private void sendSampleCreateRequest(String accountID, String sym) throws IOException {
    String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<create>\n" + "<account id=\"" + accountID
        + "\" balance=\"1000\"/>\n" + "<symbol sym=\"" + sym + "\">\n" + "<account id=\"" + accountID
        + "\">100000</account>\n" + "</symbol>\n" + "</create>";

    req = req.length() + "\n" + req;
    PrintWriter out = new PrintWriter(theClientSocket.getOutputStream(), true);
    out.println(req);
  }

  /**
   * Method to receive xml response from server
   */
  private String receiveResponseFromServer() throws IOException {
    InputStreamReader socketInput = new InputStreamReader(theClientSocket.getInputStream());
    BufferedReader in = new BufferedReader(socketInput);
    StringBuilder response = new StringBuilder("");
    String input;
    while ((input = in.readLine()) != null) {
      response.append(input);
    }
    return response.toString();
  }

  /**
   * Method to send object to server
   * 
   * @param object to send
   */
  public void sendToServer(Object obj) {
    try {
      OutputStream o = theClientSocket.getOutputStream();
      ObjectOutputStream s = new ObjectOutputStream(o);

      s.writeObject(obj);
      s.flush();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println("Error during serialization");
      e.printStackTrace();
    }
  }

}
