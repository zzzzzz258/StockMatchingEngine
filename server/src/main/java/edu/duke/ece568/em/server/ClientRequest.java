package edu.duke.ece568.em.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientRequest implements Runnable {
  private Socket theClientSocket;
  private int orderID;

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

      theClientSocket.close();
    } catch (Exception e) {
      System.out.println("Error in client thread: " + e.getMessage());
    }

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
