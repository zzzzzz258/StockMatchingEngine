package edu.duke.ece568.em.server;

import java.io.BufferedReader;
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
import java.sql.SQLException;
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
import org.apache.ibatis.session.TransactionIsolationLevel;
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
    if (doc.hasChildNodes()) {
      processCreateRequest_helper(doc.getChildNodes());
    }
  }

  private SqlSession getDefaultSession() {
    return SingletonSQLFactory.getInstance().openSession(true);
  }

  /**
   * Method to get read_committed level session
   */
  private SqlSession getReadCommittedSession() {
    // return SingletonSQLFactory.getInstance().openSession();
    return SingletonSQLFactory.getInstance().openSession(TransactionIsolationLevel.SERIALIZABLE);
    // return
    // SingletonSQLFactory.getInstance().openSession(TransactionIsolationLevel.REPEATABLE_READ);
    // return
    // SingletonSQLFactory.getInstance().openSession(TransactionIsolationLevel.READ_COMMITTED);
  }

  /**
   * Method to process a transaction request
   * 
   * @param Document doc
   */
  private void processTransactionRequest(Document doc) {
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
            tryMatchOrder(newOrder);
          }
        } else if (tempNodeName.equals("cancel")) {
          cancelOrder(tempNode, accountId);
        } else if (tempNodeName.equals("query")) {
          queryOrder(tempNode, accountId);
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
      try (SqlSession dbSession = this.getDefaultSession()) {                
        AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
        accountMapper.insert(new Account(id, Double.parseDouble(balance)));

        System.out.println(id + " insereted");
        Element successElement = responseToClient.createElement("created");
        addSuccessToResponse(successElement, id, null);
      } catch (Exception e) {
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, id, null, e.getMessage());
      }
    }
  }

  private Account getAccount(String accountId) {
    try (SqlSession session = this.getDefaultSession()) {
      AccountMapper accountMapper = session.getMapper(AccountMapper.class);
      return accountMapper.selectOneById(accountId);
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
    dbSession = this.getReadCommittedSession();
    if (accountId.isEmpty()) {
      // transactions order must have a account id attribute in root tag
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, accountId, null, "account id not found");
      return false;
    }
    AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
    Account account = accountMapper.selectOneById(accountId);
    dbSession.close();
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

        insertPosition(newPosition);

        Element successElement = responseToClient.createElement("created");
        addSuccessToResponse(successElement, id, symVal);
      } catch (Exception e) {
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, id, null, e.getMessage());
      }
    }
  }

  /**
   * Insert position into db
   */
  private void insertPosition(Position newPosition) {
    while (true) {
      try (SqlSession dbSession = this.getDefaultSession();) {        
        PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);
        // first check if the symbol already exists
        Position existingSym = positionMapper.select(newPosition);
        if (existingSym == null) { // new symbol
          positionMapper.insert(newPosition); // race condition in inserting
        } else { // just update the balance
          Position addedPosition = new Position(newPosition.getSymbol(), newPosition.getAmount(),
              newPosition.getAccountId());
          positionMapper.updateAddPosition(newPosition);
        }

        //        dbSession.commit();
        return;
      } catch (Exception e) {
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
    double orderAmount = newOrder.getAmount();
    if (orderAmount == 0) {
      // rejected
      Element errorElement = responseToClient.createElement("error");
      addErrorToResponse(errorElement, newOrder, "Invalid amount 0");
      return null;
    } else if (orderAmount > 0) {
      // a buy order
      if (insertBuyOrder(newOrder, accountId) == true) {
        // send reponse
        addOpenToResponse(responseToClient.createElement("opened"), newOrder);
      } else {
        Element errorElement = responseToClient.createElement("error");
        addErrorToResponse(errorElement, newOrder, "Account balance insufficient");
      } // end if
    } else { // orderAmount < 0
      // sell order
      if (insertSellOrder(newOrder, accountId)) {
        // send response
        addOpenToResponse(responseToClient.createElement("opened"), newOrder);
      } else {
        addErrorToResponse(responseToClient.createElement("error"), newOrder, "You don't enough to sell");
      } // end if
    } // end orderAmount condition
    return newOrder;
  }

  /**
   * Insert Buy order to db and update account balance
   *
   * @return true if order placed successfully
   */
  private boolean insertBuyOrder(Order newOrder, String accountId) {
    dbSession = this.getReadCommittedSession();
    OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
    AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);

    Account account = accountMapper.selectOneById(accountId);

    double orderAmount = newOrder.getAmount();
    double orderPrice = newOrder.getLimitPrice();
    double totalCost = orderAmount * orderPrice;
    double currentBalance = account.getBalance();
    try {
      if (totalCost <= currentBalance) {
        currentBalance -= totalCost;
        account.setBalance(currentBalance);
        // update account balance
        accountMapper.updateBalance(account);
        // insert into database
        orderMapper.insert(newOrder);
        dbSession.commit();
        return true;
      } else {
        return false;
      }
    } // end try
    catch (Exception e) {
      System.out.println("retry buy");
      dbSession.close();
      return insertBuyOrder(newOrder, accountId);
    }
  }

  /**
   * Insert sell order to db and update position shares
   *
   * @return true if order placed
   */
  private boolean insertSellOrder(Order newOrder, String accountId) {
    dbSession = this.getReadCommittedSession();
    OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
    PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);

    double orderAmount = -newOrder.getAmount();
    Position position = new Position(newOrder.getSymbol(), accountId);
    position = positionMapper.select(position);
    double positionAmount = position == null ? 0 : position.getAmount();
    try {
      if (orderAmount <= positionAmount) {
        // update position amount
        positionAmount -= orderAmount;
        position.setAmount(positionAmount);
        positionMapper.update(position);
        // insert into database
        orderMapper.insert(newOrder);
        dbSession.commit();
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      System.out.println("retry buy");
      dbSession.close();
      return insertSellOrder(newOrder, accountId);
    }
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
    if (newOrder.getAmount() < 0) { // sell order, look for buy order

      matchOrder(newOrder, true);
    } else { // buy order, look for sell order
      matchOrder(newOrder, false);
    }
  }

  /**
   * Method to match and execute sell order
   */
  private void matchOrder(Order newOrder, boolean ifSell) {
    while (true) {
      try {
        dbSession = this.getReadCommittedSession();
        // dbSession = this.getManualCommitSession();
        OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);

        PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);

        newOrder = orderMapper.selectById(newOrder.getOrderId());
        // temp and swap
        Order seller = ifSell ? new Order(newOrder) : orderMapper.selectSellOrder(newOrder);
        Order buyer = ifSell ? orderMapper.selectBuyOrder(newOrder) : new Order(newOrder);
        if (seller == null || buyer == null) {
          dbSession.close();
          return;
        }
        double matchPrice = seller.getTime() < buyer.getTime() ? seller.getLimitPrice() : buyer.getLimitPrice();
        double matchAmount = Math.min(Math.abs(seller.getAmount()), Math.abs(buyer.getAmount()));

        Transaction sellTransaction = doSellBusiness(seller, matchAmount, matchPrice);
        Transaction buyTransaction = doBuyBusiness(buyer, matchAmount, matchPrice);

        if (seller.getAmount() == 0)
          seller.setStatus(Status.COMPLETE);
        if (buyer.getAmount() == 0)
          buyer.setStatus(Status.COMPLETE);

        orderMapper.updateAmountStatusById(seller);
        orderMapper.updateAmountStatusById(buyer);

        dbSession.commit();

        // add balance to account, add shraes to position
        addBalance(newOrder.getAccountId(), matchAmount * matchPrice);
        addPosition(newOrder.getAccountId(), newOrder.getSymbol(), matchAmount);
        if (buyer.getLimitPrice() > matchPrice) { // refund
          addBalance(newOrder.getAccountId(), matchAmount * (buyer.getLimitPrice() - matchPrice));
        }

        // insert transaction
        insertTransaction(sellTransaction);
        insertTransaction(buyTransaction);

        // change new order status
        newOrder = ifSell ? seller : buyer;
        if (newOrder.getStatus() == Status.COMPLETE) {
          return;
        }
      } catch (Exception e) {
        System.out.println("Retry match order");
        dbSession.close();
      }
    }
  }

  /**
   * Method to do logics in buy operation
   */
  private Transaction doBuyBusiness(Order order, double amount, double price) {
    order.setAmount(order.getAmount() - amount);
    return new Transaction(order.getOrderId(), amount, price);
  }

  /**
   * Method to de logics in sell operation
   */
  private Transaction doSellBusiness(Order order, double amount, double price) {
    // update amount in order, and account's balance
    order.setAmount(order.getAmount() + amount);
    return new Transaction(order.getOrderId(), -amount, price);
  }

  /**
   * Method to add shares to a position
   */
  private void addPosition(String accountId, String symbol, double amount) {
    while (true) {
      try {
        dbSession = this.getReadCommittedSession();
        PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);
        Position position = positionMapper.selectL(new Position(symbol, accountId));
        if (position == null) {
          position = new Position(symbol, amount, accountId);
          positionMapper.insert(position);
        } else {
          position.setAmount(position.getAmount() + amount);
          positionMapper.update(position);
        }
        dbSession.commit();
        return;
      } catch (Exception e) {
        System.out.println("retry addPosition");
        dbSession.close();
      }
    }
  }

  /**
   * method to add balance to account
   */
  private void addBalance(String accountId, double balance) {
    while (true) {
      try {
        dbSession = this.getReadCommittedSession();
        AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
        Account account = accountMapper.selectOneByIdL(accountId);
        account.setBalance(account.getBalance() + (balance));
        accountMapper.updateBalance(account);
        dbSession.commit();
        return;
      } catch (Exception e) {
        System.out.println("retry addBalance");
        dbSession.close();
      }
    }
  }

  /**
   * Method to insert a transaction to db
   */
  private void insertTransaction(Transaction transaction) {
    try {
      dbSession = this.getReadCommittedSession();
      TransactionMapper transactionMapper = dbSession.getMapper(TransactionMapper.class);
      transactionMapper.insert(transaction);
      dbSession.commit();
    } catch (Exception e) {
      System.out.println("Error ocurred in inserting transaction");
      System.out.println(e.getStackTrace());
    }
  }

  /**
   * Method to cancel an order.
   */
  private void cancelOrder(Node node, String accountId) {
    NamedNodeMap attributes = node.getAttributes();

    Node idNode = attributes.getNamedItem("id");
    String orderId = idNode.getNodeValue();

    Element canceled = responseToClient.createElement("canceled");
    canceled.setAttribute("id", orderId);

    if (tryCancel(orderId, accountId, canceled)) {
      addTransactions(orderId, canceled);
    }
  }

  /**
   * Method to try to cancel an order
   *
   * @return true if canceled or complete, false if order and account no match
   */
  private boolean tryCancel(String orderId, String accountId, Element canceled) {
    while (true) {
      try {
        dbSession = this.getReadCommittedSession();

        OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
        Order order = orderMapper.selectByIdL(Integer.parseInt(orderId));

        if (order == null || !order.getAccountId().equals(accountId)) { // order and account not match
          addErrorToResponse(responseToClient.createElement("error"), orderId, null,
              "cancel fails, order does not exist or not belong to this account");
          dbSession.close();
          return false;
        } // end if

        if (order.getStatus() == Status.OPEN) { // cancel it, return shares or funds
          System.out.println("Found open order to cancel");

          // cancel order, add to response
          order.setStatus(Status.CANCELED);
          order.setTime(System.currentTimeMillis());
          orderMapper.cancelById(order);

          dbSession.commit();

          if (order.getAmount() > 0) { // buy order, refund
            double refund = order.getAmount() * order.getLimitPrice();
            addBalance(accountId, refund);
          } else { // sell order, return
            double returnShares = order.getAmount();
            addPosition(accountId, order.getSymbol(), returnShares);
          }

          addCanceledElement(canceled, Math.abs(order.getAmount()), order.getTime());
          return true;
        } // endif
        else if (order.getStatus() == Status.CANCELED) {
          dbSession.close();
          addCanceledElement(canceled, Math.abs(order.getAmount()), order.getTime());
          return true;
        } else { // complete
          dbSession.close();
          return true;
        }
      } catch (Exception e) {
        System.out.println("Retry cancel " + orderId);
        System.out.println(e.getMessage());
        dbSession.close();
      }
    }
  }

  /**
   * Method to query an order.
   */
  private void queryOrder(Node node, String accountId) {
    NamedNodeMap attributes = node.getAttributes();

    Node idNode = attributes.getNamedItem("id");
    String orderId = idNode.getNodeValue();

    Element status = responseToClient.createElement("status");
    status.setAttribute("id", orderId);

    if (addOrderStatus(orderId, status, accountId)) {
      addTransactions(orderId, status);
    }
  }

  /**
   * Method to add open/cancelled order into response to query
   */
  private boolean addOrderStatus(String orderId, Element element, String accountId) {
    dbSession = this.getReadCommittedSession();
    OrderMapper orderMapper = dbSession.getMapper(OrderMapper.class);
    Order order = orderMapper.selectById(Integer.parseInt(orderId));
    dbSession.close();
    if (order == null || !order.getAccountId().equals(accountId)) {
      addErrorToResponse(responseToClient.createElement("error"), orderId, null,
          "Order does not exist or not belong to this account");
      return false;
    }
    if (order.getStatus() == Status.CANCELED) {
      addCanceledElement(element, order.getAmount(), order.getTime());
    } else if (order.getStatus() == Status.OPEN) {
      addOpenElement(element, order.getAmount());
    }
    return true;
  }

  /**
   * Method to add transactions
   */
  private void addTransactions(String orderId, Element element) {
    // add executed rows
    dbSession = this.getReadCommittedSession();
    TransactionMapper transactionMapper = dbSession.getMapper(TransactionMapper.class);
    List<Transaction> transactions = transactionMapper.selectByOrderId(Integer.parseInt(orderId));
    addExecutedElements(element, transactions);
    addToResponse(element);

    dbSession.close();
  }

  /**
   * Method to add tag open to element*
   */
  private void addOpenElement(Node parentNode, double shares) {
    Element element = responseToClient.createElement("open");
    element.setAttribute("shares", Double.toString(shares));
    parentNode.appendChild(element);
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
      executed.setAttribute("time", Long.toString(t.getTime() / 1000));
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
