package edu.duke.ece568.em.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

public class Server {
  private final int listenerPort;
  private final ServerSocket theServerSocket;

  /**
   * Constructor to setup the Server
   * 
   * @param port number to run the server
   * @throws IOException
   */
  public Server(int listenerPort) throws IOException {
    this.listenerPort = listenerPort;
    theServerSocket = new ServerSocket(listenerPort);
  }

  /**
   * Getter for listener port
   */
  public int getListenerPort() {
    return listenerPort;
  }

  /**
   * method to start the listening on the listener port
   */

  private void acceptRequests() {
    System.out.println("Starting to accept client requests...");

    // loop to keep accepting request
    while (true) {
      try {
        // accept client request
        Socket clientSocket = theServerSocket.accept();
        String msg = (String)receiveObjectFromClient(clientSocket);
        System.out.println("Client sent: " + msg);
        msg = "Hi client!";
        sendToClient(msg, clientSocket);
        // TO-DO: create a thread to process that request
        
      } catch (Exception e) {
        System.out.println("Error in accepting client request: " + e.getMessage());
      }
    }

  }

  /**
   * method to close server socket
   */
  private void closeServer() throws IOException {
    theServerSocket.close();
  }

  /**
   * Method to receive Object over the socket
   * 
   * @return Object received from client
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public Object receiveObjectFromClient(Socket clientSocket) throws IOException, ClassNotFoundException {
    InputStream o = clientSocket.getInputStream();
    ObjectInputStream s = new ObjectInputStream(o);
    Object obj = s.readObject();
    return obj;
  }

  /**
   * Method to send object to a specific socket
   * 
   * @param object to send and the client's socket
   */
  public void sendToClient(Object obj, Socket soc) {
    try {
      OutputStream o = soc.getOutputStream();
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


  public static void main(String[] args) throws IOException {
    // simple application case for common package
    System.out.println("Starting Exchange Matching Server...");
    try {
      Server theExchangeServer = new Server(12345); // port# per the requirement
      theExchangeServer.acceptRequests();
      theExchangeServer.closeServer();
      ;
    } catch (Exception e) {
      // print exception message about Throwable object
      e.printStackTrace();
    }

    // simple test case for mybatis ORM
    SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoxMapper mapper = session.getMapper(BoxMapper.class);
      mapper.deleteAll();
      mapper.addBox(15);

      session.commit();
      System.out.println("Add a box");
    }

  }

  /**
   * Connect to the postgresql database.
   * 
   * @return {@link SqlSessionFactory}
   */
  public static SqlSessionFactory getSqlSessionFactory() {
    DataSource dataSource = MyDataSourceFactory.getDataSource("org.postgresql.Driver", "jdbc:postgresql:test_db",
        "postgres", "ece568hw4"); // TODO: update the arguments after creating tables

    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    Environment environment = new Environment("development", transactionFactory, dataSource);
    Configuration configuration = new Configuration(environment);
    configuration.addMapper(BoxMapper.class);
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    return sqlSessionFactory;
  }
}
