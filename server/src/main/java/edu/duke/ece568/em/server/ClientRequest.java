package edu.duke.ece568.em.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
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
        request.append("\n");

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
      } else if (rootRequest == "transactions") {
        processTransactionRequest(doc);
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
   * Method to process a transaction request
   * 
   * @param Document doc
   */
  private void processTransactionRequest(Document doc) {
    try (SqlSession session = SingletonSQLFactory.getInstance().openSession()) {
      dbSession = session;
      Node topLevelNode = doc.getFirstChild();
      if (topLevelNode.hasChildNodes()) {
        Node idAttribute = topLevelNode.getAttributes().getNamedItem("id");
        String accountId = idAttribute == null ? "" : idAttribute.getNodeValue();
        if (checkAccountIdInTransaction(accountId)) {
          processTransactionsRequest_helper(topLevelNode.getChildNodes(), accountId);
        }
      } else {
        // response error of not having any children
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, null, null, "No children in transactions request");
      }
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
   * Helper method to process transactions request by iteratively working on
   * nodelist
   * 
   * @param NodeList nodeList
   */
  private void processTransactionsRequest_helper(NodeList nodeList, String accountId) {
    for (int count = 0; count < nodeList.getLength(); count++) {

      Node tempNode = nodeList.item(count);

      // make sure it's element node.
      if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

        // get node name and value
        String tempNodeName = tempNode.getNodeName();

        if (tempNodeName.equals("order")) {
          // put order into db
          Order newOrder = addOrder(tempNode, accountId);
          if (newOrder != null) {
            System.out.println(newOrder);
            tryMatchOrder(newOrder);
          }
        } else if (tempNodeName.equals("cancel")) {
          // TODO
          cancelOrder(tempNode, accountId);
        } else if (tempNodeName.equals("query")) {
          // TODO
        } else { // invalid
          Element errorElement = responseToClient.createElement("error");
          addErrorToResponse(errorElement, accountId, null, "Invalid tag:" + tempNodeName);
        }
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
   * Method to check if the transactions request has an account id and it's valid
   * generate error response if invalid
   * 
   * @return true if there is a valid account id
   * @param the xml document
   */
  private boolean checkAccountIdInTransaction(String accountId) {
    if (accountId.isEmpty()) {
      // transactions order must have a account id attribute in root tag
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, accountId, null, "account id not found");
      return false;
    }
    AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
    Account account = accountMapper.selectOneById(accountId);
    if (account == null) {
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, accountId, null, "given account does not exist");
      return false;
    }
    return true;
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
        // System.out.println("Sym: " + newPosition.getSymbol() + " Account: " +
        // newPosition.getAccountId());
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
   * Method to add a new order in database
   * 
   * @param node      is the order tag
   * @param accountId
   * @return new order if added successfully, null if fails
   */
  private Order addOrder(Node node, String accountId) {
    Order newOrder = getOrderFromNode(node, accountId);
    if (newOrder == null) {
      return null;
    }
    OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
    AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
    PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);

    double orderAmount = newOrder.getAmount();
    Account account = accountMapper.selectOneById(accountId);
    if (orderAmount == 0) {
      // rejected
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, newOrder, "Invalid amount 0");
      return null;
    } else if (orderAmount > 0) {
      // a buy order
      double orderPrice = newOrder.getLimitPrice();
      double totalCost = orderAmount * orderPrice;
      double currentBalance = account.getBalance();
      if (totalCost <= currentBalance) {
        currentBalance -= totalCost;
        account.setBalance(currentBalance);
        // update account balance
        accountMapper.updateBalance(account);
        // insert into database
        orderMapper.insert(newOrder);
        dbSession.commit();
        // send reponse
        addOpenToResponse(responseToClient.createElement("opened"), newOrder);
      } else {
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, newOrder, "Account balance insufficient");
      } // end if
    } else { // orderAmount < 0
      // sell order
      orderAmount = -orderAmount;
      Position position = new Position(newOrder.getSymbol(), accountId);
      position = positionMapper.select(position);
      double positionAmount = position == null ? 0 : position.getAmount();
      if (orderAmount <= positionAmount) {
        // update position amount
        positionAmount -= orderAmount;
        position.setAmount(positionAmount);
        positionMapper.update(position);
        // insert into database
        orderMapper.insert(newOrder);
        dbSession.commit();
        // send response
        addOpenToResponse(responseToClient.createElement("opened"), newOrder);
      } else {
        addErrorToResponse(responseToClient.createElement("error"), newOrder, "You don't enough to sell");
      } // end if
    } // end orderAmount condition
    return newOrder;
  }

  /**
   * Method to get an Order Object from node
   */
  private Order getOrderFromNode(Node node, String accountId) {
    NamedNodeMap attributes = node.getAttributes();

    try {
      Node symNode = attributes.getNamedItem("sym");
      String symVal = symNode.getNodeValue();

      Node amountNode = attributes.getNamedItem("amount");
      double amountVal = Double.parseDouble(amountNode.getNodeValue());

      Node limitNode = attributes.getNamedItem("limit");
      double limitVal = Double.parseDouble(limitNode.getNodeValue());

      Order newOrder = new Order(symVal, amountVal, limitVal, accountId);
      return newOrder;
    } catch (NumberFormatException e) {
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, null, null, "invalid limit or amount");
    } catch (NullPointerException e) {
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, null, null, "missing required attribute");
    }
    return null;
  }

  /**
   * Method to try to find matching order of new order
   *
   * @param newOrder is new order
   */
  private void tryMatchOrder(Order newOrder) {
    // select possible matching orders
    OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
    if (newOrder.getAmount() < 0) { // sell order, look for buy order
      List<Order> qualifiedOrders = orderMapper.selectBuyOrderByLowestPrice(newOrder);
      if (qualifiedOrders.size() > 0) {
        matchOrders(newOrder, qualifiedOrders);
      } // end if
    } else { // buy order, look for sell order
      List<Order> qualifiedOrders = orderMapper.selectSellOrderByHighestPrice(newOrder);
      if (qualifiedOrders.size() > 0) {
        matchOrders(newOrder, qualifiedOrders);
      } // end if
    }
  }

  /**
   * Method do matching logics. Record transactions
   */
  private void matchOrders(Order newOrder, List<Order> qualifiedOrders) {
    for (Order candidate : qualifiedOrders) {
      if (newOrder.getStatus() == Status.COMPLETE) {
        return;
      }
      OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
      TransactionMapper transactionMapper = dbSession.getMapper(TransactionMapper.class);

      double matchPrice = newOrder.getTime() < candidate.getTime() ? newOrder.getLimitPrice()
          : candidate.getLimitPrice();
      double matchAmount = Math.min(Math.abs(newOrder.getAmount()), Math.abs(candidate.getAmount()));
      matchOrders_helper(newOrder, orderMapper, transactionMapper, matchPrice, matchAmount);
      matchOrders_helper(candidate, orderMapper, transactionMapper, matchPrice, matchAmount);
    } // end for
  }

  /**
   * Helper method in match orders
   */
  private void matchOrders_helper(Order order, OrderMapper orderMapper, TransactionMapper transactionMapper,
      double matchPrice, double matchAmount) {
    // do business, transfer shares, transfer money, refund if needed
    Transaction orderTransaction = doBusiness(order, matchPrice, matchAmount);
    // update two orders to db
    if (order.getAmount() == 0) {
      order.setStatus(Status.COMPLETE);
    }
    orderMapper.updateAmountStatusById(order);
    // create relative transactions and insert
    transactionMapper.insert(orderTransaction);
  }

  /**
   * Method to do logical business, transfer shares and money, refund if needed
   * 
   * @return Transaction record of this business
   */
  private Transaction doBusiness(Order order, double price, double amount) {
    AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
    PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);
    Account account = accountMapper.selectOneById(order.getAccountId());
    if (order.getAmount() > 0) { // buy
      return doBuyBusiness(order, account, accountMapper, positionMapper, amount, price);
    } else { // sell
      return doSellBusiness(order, account, accountMapper, amount, price);
    }
  }

  /**
   * Method to do logics in buy operation
   */
  private Transaction doBuyBusiness(Order order, Account account, AccountMapper accountMapper,
      PositionMapper positionMapper, double amount, double price) {
    // update amount in order, and account's position
    Position position = positionMapper.select(new Position(order.getSymbol(), account.getAccountId()));
    double primitivePrice = order.getLimitPrice();
    order.setAmount(order.getAmount() - amount);
    if (position == null) {
      position = new Position(order.getSymbol(), amount, account.getAccountId());
      positionMapper.insert(position);
    } else {
      position.setAmount(position.getAmount() + amount);
      positionMapper.update(position);
    }
    // refund if necessary
    if (primitivePrice > price) {
      double refund = (primitivePrice - price) * amount;
      account.setBalance(account.getBalance() + refund);
      accountMapper.updateBalance(account);
    } // endif
    return new Transaction(order.getOrderId(), amount, price);
  }

  /**
   * Method to de logics in sell operation
   */
  private Transaction doSellBusiness(Order order, Account account, AccountMapper accountMapper, double amount,
      double price) {
    // update amount in order, and account's balance
    order.setAmount(order.getAmount() + amount);
    account.setBalance(account.getBalance() + (price * amount));
    accountMapper.updateBalance(account);
    return new Transaction(order.getOrderId(), -amount, price);
  }

  /**
   * Method to cancel an order.
   */
  private void cancelOrder(Node node, String accountId) {
    NamedNodeMap attributes = node.getAttributes();

    try {
      Node idNode = attributes.getNamedItem("id");
      String orderId = idNode.getNodeValue();

      OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
      AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
      PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);
      TransactionMapper transactionMapper = dbSession.getMapper(TransactionMapper.class);

      Account account = accountMapper.selectOneById(accountId);
      Order order = orderMapper.selectById(Integer.parseInt(orderId));
      Position position = positionMapper.select(new Position(order.getSymbol(), accountId));

      if (!order.getAccountId().equals(accountId)) { // order and account not match
        addErrorToResponse(responseToClient.createElement("error"), orderId, null,
            "cancel fails, order does not belong to this account");
        return;
      } // end if

      Element canceled = responseToClient.createElement("canceled");
      canceled.setAttribute("id", orderId);

      if (order.getStatus() == Status.OPEN) { // cancel it, return shares or funds
        if (order.getAmount() > 0) { // buy order, refund
          double refund = order.getAmount() * order.getLimitPrice();
          account.setBalance(account.getBalance() + refund);
          accountMapper.updateBalance(account);
        } else { // sell order, return
          double returnShares = order.getAmount();
          position.setAmount(position.getAmount() + returnShares);
          positionMapper.update(position);
        }
        // cancel order, add to response
        order.setStatus(Status.CANCELED);
        order.setTime(System.currentTimeMillis());
        orderMapper.cancelById(order);
        addCanceledElement(canceled, Math.abs(order.getAmount()), order.getTime());
      } // endif

      // add executed rows
      List<Transaction> transactions = transactionMapper.selectByOrderId(Integer.parseInt(orderId));
      System.out.println(transactions.size());
      addExecutedElements(canceled, transactions);
      addToResponse(canceled);

      dbSession.commit();
    } catch (NullPointerException e) {
      addErrorToResponse(responseToClient.createElement("error"), accountId, null, "order not found");
    }
  }

  /**
   * Method to add tag canceled to element*
   */
  private void addCanceledElement(Node parentNode, double shares, long time) {
    Element canceled = responseToClient.createElement("canceled");
    canceled.setAttribute("shares", Double.toString(shares));
    canceled.setAttribute("time", Long.toString(time / 1000));
    parentNode.appendChild(canceled);
  }

  /**
   * Add transactions(executed records) to response
   */
  private void addExecutedElements(Node parentNode, List<Transaction> transactions) {
    for (Transaction t : transactions) {
      Element executed = responseToClient.createElement("executed");
      executed.setAttribute("shares", Double.toString(Math.abs(t.getAmount())));
      executed.setAttribute("price", Double.toString(t.getPrice()));
      executed.setAttribute("time", Double.toString(t.getTime() / 1000));
      parentNode.appendChild(executed);
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
   * Method to add result open to response
   */
  private void addOpenToResponse(Element element, Order newOrder) {
    element.setAttribute("id", Integer.toString(newOrder.getOrderId()));
    element.setAttribute("sym", newOrder.getSymbol());
    element.setAttribute("amount", Double.toString(newOrder.getAmount()));
    element.setAttribute("limit", Double.toString(newOrder.getLimitPrice()));
    addToResponse(element);
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
   * Method to add error of order to response
   */
  private void addErrorToResponse(Element errorElement, Order newOrder, String errMsg) {
    errorElement.setTextContent(errMsg);
    errorElement.setAttribute("sym", newOrder.getSymbol());
    errorElement.setAttribute("amount", Double.toString(newOrder.getAmount()));
    errorElement.setAttribute("limit", Double.toString(newOrder.getLimitPrice()));
    addToResponse(errorElement);
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
    addToResponse(newElement);
  }

  /**
   * Method to add a child to XML response to client
   */
  private void addToResponse(Element newElement) {
    responseToClient.getFirstChild().appendChild(newElement);
  }

  /**
   * Method to send XML response to client
   */
  private void sendResponseToClient() {
    try {
      // First convert the xml to string
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(responseToClient);
      StringWriter writer = new StringWriter();
      // pretty print
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(source, new StreamResult(writer));
      String xmlString = writer.getBuffer().toString();

      // Add the size of the xml at the beginning
      int lengthOfXML = xmlString.length();
      xmlString = lengthOfXML + "\n" + xmlString;
      System.out.println("Sending response: " + xmlString);

      // get the output stream from the socket.
      OutputStream outputStream = theClientSocket.getOutputStream();
      // write the message we want to send
      PrintWriter out = new PrintWriter(outputStream, true);
      out.println(xmlString);
      out.flush();
    } catch (Exception e) {
      System.out.println("Error in sending response to client: " + e.getMessage());
    }
  }

}
