package edu.duke.ece568.em.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class NeoClient {
  Socket theClientSocket;

  /**
   * Constructor for Client to setup Socket
   * 
   * @param hostname and port number of server
   */
  public NeoClient(String hostname, int portNum) throws IOException {
    theClientSocket = new Socket(hostname, portNum);
  }

  /**
   * Main method for Client Application
   */
  public static void main(String[] args) {
    String hostname = "127.0.0.1";
    int portNum = 12345;
    try {
      OldClient theClient = new OldClient(hostname, portNum);
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
   * Method for sending sample create request to Client
   */
  public void runSampleCreateTest(String fileName) throws IOException {
    /*
     * String msg = "Client is ready!"; theClient.sendToServer(msg); msg =
     * (String)theClient.receiveFromServer(); System.out.println("Server sent: " +
     * msg);
     */
    sendSampleCreateRequest(fileName);
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
    /*String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<create>\n" + "<account id=\"" + accountID
        + "\" balance=\"1000\"/>\n" + "<symbol sym=\"" + sym + "\">\n" + "<account id=\"" + accountID
        + "\">100000</account>\n" + "</symbol>\n" + "</create>";
    */
    String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<create>\n" + "<symbol sym=\"" + sym + "\">\n" + "<account id=\"" + accountID
        + "\">100000</account>\n" + "</symbol>\n" + "</create>";

    req = req.length() + "\n" + req;
    PrintWriter out = new PrintWriter(theClientSocket.getOutputStream(), true);
    out.println(req);
  }

  
  /**
   * Method to send a request
   */
  private void sendSampleCreateRequest(String fileName) throws IOException {
    String req = readFile("orders/" + fileName);
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
   * Read a file
   * @param the location of file
   * @return a string of contents in file
   */
  private String readFile(String filePath) {
    StringBuilder sb = new StringBuilder();
    try {
      File myObj = new File(filePath);
      Scanner myReader = new Scanner(myObj);
      while (myReader.hasNextLine()) {
        String data = myReader.nextLine();
        //        System.out.println(data);
        sb.append(data);
      }
      myReader.close();
    } catch (FileNotFoundException e) {
      System.out.println("XML file not found.");
      e.printStackTrace();
    }
    return sb.toString();
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
