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
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ibatis.session.SqlSession;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

enum RequestType_t {
  CREATE, TRANSACTION
};

public class ClientRequest implements Runnable {
  private Socket theClientSocket;
  private int orderID;
  private AccountMapper accountMapper;
  private PositionMapper positionMapper;
  private OrderMapper orderMapper;

  private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

  /**
   * constructor for client socket
   */
  public ClientRequest(Socket theClientSocket, int orderID) {
    this.theClientSocket = theClientSocket;
    this.orderID = orderID;
    accountMapper = null;
    positionMapper = null;
    orderMapper = null;
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
        parseAndProcessRequest(xmlReq);
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
    String input = in.readLine(); // reading the first line with req size out of the request to process
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
  private void parseAndProcessRequest(String xmlReq) {
    try {
      // unknown XML better turn on this
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder dBuilder = dbf.newDocumentBuilder();
      Document doc = dBuilder.parse(new InputSource(new StringReader(xmlReq)));

      // First check if it is a create or transaction request
      String rootRequest = doc.getDocumentElement().getNodeName();
      if (rootRequest == "create") {
        processCreateRequest(doc);
      }

    } catch (Exception e) {
      System.out.println("Error in parsing XML request: " + e.getMessage());
    }
  }

  /**
   * Method to process a create request
   * 
   * @param Document doc
   */
  private void processCreateRequest(Document doc) {
    try (SqlSession session = SingletonSQLFactory.getInstance().openSession()) {
      accountMapper = session.getMapper(AccountMapper.class);
      positionMapper = session.getMapper(PositionMapper.class);
      orderMapper = session.getMapper(OrderMapper.class);

      if (doc.hasChildNodes()) {
        processCreateRequest_helper(doc.getChildNodes());
      }

      session.commit();
    } // end of try
  }

  /**
   * Helper method to process create request by recursively working on nodelist
   * 
   * @param NodeList nodeList
   */
  private void processCreateRequest_helper(NodeList nodeList) {
    for (int count = 0; count < nodeList.getLength(); count++) {

      Node tempNode = nodeList.item(count);

      // make sure it's element node.
      if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

        // get node name and value
        String tempNodeName = tempNode.getNodeName();

        HashMap<String, String> attributes = new HashMap<>();
        if (tempNode.hasAttributes()) {
          // get attributes names and values
          NamedNodeMap nodeMap = tempNode.getAttributes();
          for (int i = 0; i < nodeMap.getLength(); i++) {
            Node node = nodeMap.item(i);
            attributes.put(node.getNodeName(), node.getNodeValue());
          }
        }

        if (tempNodeName.equals("account")) {
          createNewAccount(attributes);
        } else if (tempNodeName.equals("symbol")) {
        } else { // invalid
        }

        if (tempNode.hasChildNodes()) {
          // loop again if has child nodes
          processCreateRequest_helper(tempNode.getChildNodes());
        }

        System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

      }

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

  /**
   * Method to create a new account in the database
   * 
   * @param HashMap of attribute of name and values
   */
  private void createNewAccount(HashMap<String, String> attributes) {
    String id = attributes.get("id");
    String balance = attributes.get("balance");
    if (id == null || balance == null) {
      // throw exception
    } else {
      Account newAccount = new Account(id, Integer.parseInt(balance, 10));
      accountMapper.insert(newAccount);
    }

  }

}
