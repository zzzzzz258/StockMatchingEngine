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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ibatis.session.SqlSession;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
  private SqlSession dbSession;
  private DocumentBuilder docBuilder;
  private Document responseToClient;

  /**
   * constructor for client socket
   * 
   * @throws ParserConfigurationException
   */
  public ClientRequest(Socket theClientSocket, int orderID) throws ParserConfigurationException {
    this.theClientSocket = theClientSocket;
    this.orderID = orderID;
    dbSession = null; // only created when there is valid xml request
    startDocBuilder();
    createNewResponse();
  }

  /**
   * Method to initialize XML doc builder which will be used for both reading and
   * writing
   * 
   * @throws ParserConfigurationException
   */
  private void startDocBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    // unknown XML better turn on this
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    docBuilder = docFactory.newDocumentBuilder();
  }

  /**
   * Start a new response for clinet in XML format
   */
  private void createNewResponse() {
    responseToClient = docBuilder.newDocument();
    Element rootElement = responseToClient.createElement("results");
    responseToClient.appendChild(rootElement);
  }

  @Override
  public void run() {
    try {
      System.out.println("New client order ID: " + orderID);
      /*
       * String msg = (String) receiveObjectFromClient();
       * System.out.println("Client sent: " + msg); msg = "Hi client!";
       * sendToClient(msg);
       */
      String xmlReq = receiveRequest();
      if (xmlReq != null) {
        parseAndProcessRequest(xmlReq);
        sendResponseToClient();
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
    int requestSize;
    if (input != null) {
      try {
        requestSize = Integer.parseInt(input, 10);
        // System.out.println("expecting: " + requestSize);
      } catch (NumberFormatException e) {
        System.out.println("Error: Received invalid request from client!");
        return null;
      }

      while ((input = in.readLine()) != null) {
        requestSize--;
        request.append(input);

        // System.out.println("receieved: " + input + "\nsize so far: " +
        // request.length());
        if (request.length() >= requestSize) {
          // received the entire request already
          break;
        }
      }
      System.out.println("Received req:");
      System.out.println(request);
    }

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
      Document doc = docBuilder.parse(new InputSource(new StringReader(xmlReq)));

      // First check if it is a create or transaction request
      String rootRequest = doc.getDocumentElement().getNodeName();
      if (rootRequest == "create") {
        processCreateRequest(doc);
      }

    } catch (Exception e) {
      System.out.println("Error in parsing XML request: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Method to process a create request
   * 
   * @param Document doc
   */
  private void processCreateRequest(Document doc) {
    try (SqlSession session = SingletonSQLFactory.getInstance().openSession()) {
      dbSession = session;
      /*
       * accountMapper = session.getMapper(AccountMapper.class); positionMapper =
       * session.getMapper(PositionMapper.class); orderMapper =
       * session.getMapper(OrderMapper.class);
       */
      if (doc.hasChildNodes()) {
        processCreateRequest_helper(doc.getChildNodes());
      }

      // session.commit();
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

        if (tempNodeName.equals("account")) {
          Node nodeParent = tempNode.getParentNode();
          if (nodeParent != null && nodeParent.getNodeName().equals("symbol")) {
            addSymbol(nodeParent.getAttributes(), tempNode);
          } else {
            createNewAccount(tempNode);
          }
        } else if (tempNodeName.equals("symbol")) {
          // continue to see child nodes
        } else { // invalid
        }

        if (tempNode.hasChildNodes()) {
          // loop again if has child nodes
          processCreateRequest_helper(tempNode.getChildNodes());
        }
        // System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");
      } // end of element node condition
    } // end of for-loop
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
   * @param Node from XML request holding the specific create req
   */
  private void createNewAccount(Node tempNode) {
    HashMap<String, String> attributes = new HashMap<>();
    if (tempNode.hasAttributes()) {
      // get attributes names and values
      NamedNodeMap nodeMap = tempNode.getAttributes();
      for (int i = 0; i < nodeMap.getLength(); i++) {
        Node node = nodeMap.item(i);
        attributes.put(node.getNodeName(), node.getNodeValue());
      }
    }

    String id = attributes.get("id");
    String balance = attributes.get("balance");
    if (id == null || balance == null) {
      // throw exception????
      // How to handle invalid request
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, id, null, "Invalid request");

    } else {
      try {
        Account newAccount = new Account(id, Integer.parseInt(balance, 10));
        AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
        accountMapper.insert(newAccount);
        dbSession.commit();

        Element successElement = responseToClient.createElement("created");
        addSuccessToResponse(successElement, id, null);
      } catch (Exception e) {
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, id, null, e.getMessage());
      }
    }

  }

  /**
   * Method to create a new symbol in the database if required and create shares
   * 
   * @param
   */
  private void addSymbol(NamedNodeMap symAttribute, Node tempNode) {
    // get the symbol
    Node symNode = symAttribute.getNamedItem("sym");
    String symVal = symNode.getNodeValue();

    // get the attributes for account
    NamedNodeMap attributes = tempNode.getAttributes();
    Node idNode = attributes.getNamedItem("id");
    String id = idNode.getNodeValue();
    String share = tempNode.getTextContent();
    
    if (id == null || share == null) {
      // throw exception????
      // How to handle invalid request
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, id, null, "Invalid request");

    } else {
      try {
        Position newPosition = new Position(symVal, Double.parseDouble(share), id);
        //System.out.println("Sym: " + newPosition.getSymbol() + " Account: " + newPosition.getAccountId());
        PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);

        // first check if the symbol already exists
        Position existingSym = positionMapper.select(newPosition);
        if (existingSym == null) { // new symbol
          positionMapper.insert(newPosition);
        } else { // just update the balance
          newPosition.setAmount(newPosition.getAmount() + existingSym.getAmount());
          positionMapper.update(newPosition);
        }

        dbSession.commit();

        Element successElement = responseToClient.createElement("created");
        addSuccessToResponse(successElement, id, symVal);
      } catch (Exception e) {
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, id, null, e.getMessage());
      }
    }

  }

  /**
   * Method to add a child for success to XML response to client
   * 
   * @param Element to add, account id and symbol (null if it is for account
   *                creation)
   */
  private void addSuccessToResponse(Element successElement, String id, String sym) {
    addToResponse(successElement, id, sym);
  }

  /**
   * Method to add a child for error to XML response to client
   * 
   * @param Element to add, account id and symbol (null if it is for account
   *                creation), error message
   */
  private void addErrorToResponse(Element errorElement, String id, String sym, String errMsg) {
    errorElement.setTextContent(errMsg);
    addToResponse(errorElement, id, sym);
  }

  /**
   * Method to add a child to XML response to client
   * 
   * @param Element to add, account id and symbol (null if it is for account
   *                creation)
   */
  private void addToResponse(Element newElement, String id, String sym) {
    if (sym != null) {
      newElement.setAttribute("sym", sym);
    }
    newElement.setAttribute("id", id);
    responseToClient.getFirstChild().appendChild(newElement);

  }

  /**
   * Method to send XML response to client
   */
  private void sendResponseToClient() {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(responseToClient);
      OutputStream output = theClientSocket.getOutputStream();
      StreamResult result = new StreamResult(output);
      transformer.transform(source, result);
    } catch (Exception e) {
      System.out.println("Error in sending response to client: " + e.getMessage());
    }
  }

}
