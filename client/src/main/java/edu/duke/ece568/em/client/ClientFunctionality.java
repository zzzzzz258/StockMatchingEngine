package edu.duke.ece568.em.client;

import java.time.Instant;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ClientFunctionality {
  final int numOfThreads = 10;
  final CyclicBarrier barrier = new CyclicBarrier(numOfThreads, new scalabilityCalculator());

  class scalabilityCalculator implements Runnable {
    private long startTime;

    public scalabilityCalculator() {
      this.startTime = Instant.now().getEpochSecond();
      System.out.println(
          "*******************************" + "Started sending requests..." + "*******************************\n");
    }

    @Override
    public void run() {
      long endTime = Instant.now().getEpochSecond();
      System.out.println(
          "*******************************" + "Received all responses..." + "*******************************\n");

      long diff = endTime - startTime;
      long thruPut = numOfThreads/diff;
      System.out.println("Possible throughput of the server: " + thruPut + " requests/second");
    }
  }

  class ClientWorker implements Runnable {

    @Override
    public void run() {
      String hostname = "127.0.0.1";
      int portNum = 12345;
      try {
        OldClient theClient = new OldClient(hostname, portNum);
        theClient.runSampleCreateTest();
      } catch (Exception e) {
        System.out.println("Error in connecting to server: " + e.getMessage());
      }

      try {
        System.out.println(Thread.currentThread().getName() + " waiting for others to reach barrier.");
        barrier.await();
      } catch (InterruptedException e) {
        // ...
      } catch (BrokenBarrierException e) {
        // ...
      }

    }

  }

  /**
   * Main method for Multi-Client Application This method will spawn multiple
   * threads and try to connect to the server and send request
   */
  public static void main(String[] args) {
    ClientFunctionality multiClient = new ClientFunctionality();
    // spawn ClientWorker threads
    for (int i = 0; i < multiClient.numOfThreads; i++) {
      Thread clientWorkerThread = new Thread(multiClient.new ClientWorker());
      clientWorkerThread.setName("Thread " + i);
      clientWorkerThread.start();
    }
    // multiClient.waitUntilAllClientDone();
  }

  /**
   * Method to let all clients complete their requests
   */
  private void waitUntilAllClientDone() {
    // wait for all threads
    try {
      System.out.println("Main waiting for others to reach barrier.");
      barrier.await();
    } catch (InterruptedException e) {
      // ...
    } catch (BrokenBarrierException e) {
      // ...
    }

  }

}
