package edu.duke.ece568.em.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

enum RequestType_t {
  CREATE,
  TRANSACTION
};

public class ClientRequest implements Runnable {
  private Socket theClientSocket;
  private int orderID;

  private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

  /**
   * constructor for client socket
   */
  public ClientRequest(Socket theClientSocket, int orderID) {
    this.theClientSocket = theClientSocket;
    this.orderID = orderID;
  }

  @Override
  public void run() {
    try {
      System.out.println("New client order ID: " + orderID);
      String msg = (String) receiveObjectFromClient();
      System.out.println("Client sent: " + msg);
      msg = "Hi client!";
      sendToClient(msg);
      String xmlReq = receiveRequest();
      if(xmlReq != null) {
        parseRequest(xmlReq);
      }

      theClientSocket.close();
    } catch (Exception e) {
      System.out.println("Error in client thread: " + e.getMessage());
    }

  }

  /**
   * Method to receive request from client
   * @returns request as a string
   * @throws IOException
   */
  private String receiveRequest() throws IOException {
    InputStreamReader socketInput = new InputStreamReader(theClientSocket.getInputStream());
    BufferedReader in = new BufferedReader(socketInput);

    StringBuilder request = new StringBuilder("");
    String input = in.readLine();
    System.out.println("first line: " + input);
    int requestSize;
    if (input != null) {
      try {
        requestSize = Integer.parseInt(input, 10);
        System.out.println(requestSize);
      } catch (NumberFormatException e) {
        System.out.println("Error: Received invalid request from client!");
        return null;
      }

      while ((input = in.readLine()) != null) {
        request.append(input);
         System.out.println(request.length());
        if (request.length() == requestSize) {
          // received the entire request already
          break;
        }
      }
      System.out.println("Received req:");
      System.out.println(request);
    }

    socketInput.close();
    return request.toString();
  }

  
  /**
   * Method to parse received XML request
   * @param XML request
   * @throws IOException
   */
  private void parseRequest(String xmlReq)  {
    
  }

  /**
   * Method to receive Object over the socket
   * 
   * @return Object received from client
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public Object receiveObjectFromClient() throws IOException, ClassNotFoundException {
    InputStream o = theClientSocket.getInputStream();
    ObjectInputStream s = new ObjectInputStream(o);
    Object obj = s.readObject();
    return obj;
  }

  /**
   * Method to send object to a specific socket
   * 
   * @param object to send to the client's socket
   */
  public void sendToClient(Object obj) {
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
