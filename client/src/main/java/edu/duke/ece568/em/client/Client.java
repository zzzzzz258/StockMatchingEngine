package edu.duke.ece568.em.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
      String msg = "Client is ready!";
      theClient.sendToServer(msg);
      msg = (String)theClient.receiveFromServer();
      System.out.println("Server sent: " + msg);

    } catch (Exception e) {
      System.out.println("Error in connecting to server: " + e.getMessage());
    }
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
      // s.close();
      return obj;

    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println("Error during serialization");
      e.printStackTrace();
      return null;
    }
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
      // s.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println("Error during serialization");
      e.printStackTrace();
    }
  }

}
