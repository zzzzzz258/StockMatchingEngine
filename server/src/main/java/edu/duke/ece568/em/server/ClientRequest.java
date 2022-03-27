package edu.duke.ece568.em.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

enum RequestType_t {
  CREATE, TRANSACTION
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
      if (xmlReq != null) {
        parseRequest(xmlReq);
      }

      theClientSocket.close();
    } catch (Exception e) {
      System.out.println("Error in client thread: " + e.getMessage());
    }

  }

  /**
   * Method to receive request from client
   * 
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
   * 
   * @param XML request
   * @throws IOException
   */
  private void parseRequest(String xmlReq) {
    try {
      // unknown XML better turn on this
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder dBuilder = dbf.newDocumentBuilder();
      Document doc = dBuilder.parse(new InputSource(new StringReader(xmlReq)));

      // First check if it is a create or transaction request
      String rootRequest = doc.getDocumentElement().getNodeName();
      if(rootRequest == "create") {
        
      }
      
    } catch (Exception e) {
      System.out.println("Error in parsing XML request: " + e.getMessage());
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
