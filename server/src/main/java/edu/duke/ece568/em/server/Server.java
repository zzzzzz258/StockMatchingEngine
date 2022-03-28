package edu.duke.ece568.em.server;

import java.io.InputStream;
import java.io.IOException;
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
    int orderID = 0;
    // loop to keep accepting request
    while (true) {
      try {
        System.out.println("Waiting to accept client requests...");
        // accept client request
        Socket clientSocket = theServerSocket.accept();
        // Create a thread to process that request
        ClientRequest theClientRequest = new ClientRequest(clientSocket, orderID++);
        Thread theClientThread = new Thread(theClientRequest);
        theClientThread.start();
        theClientThread.join();

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

  public static void main(String[] args) throws IOException {
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
  }
}
