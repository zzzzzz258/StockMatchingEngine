package edu.duke.ece568.em.server;

public class Transaction {
  private int transactionId;
  private int orderId;
  private double amount;
  private double price;
  private long time;

  public Transaction(int transactionId, int orderId, double amount, double price, long time) {
    this.transactionId = transactionId;
    this.orderId = orderId;
    this.amount = amount;
    this.price = price;
    this.time = time;
  }

  public Transaction(int orderId, double amount, double price, long time) {
    this(0, orderId, amount, price, time);
  }
  
  public Transaction(int orderId, double amount, double price) {
    this(0, orderId, amount, price, System.currentTimeMillis());
  }
  
  public int getTransactionId() {
    return this.transactionId;
  }

  public void setTransactionid(int id) {
    this.transactionId = id;
  }

  public int getOrderId() {
    return this.orderId;
  }

  public void setOrderId(int id) {
    this.orderId = id;
  }

  public double getAmount() {
    return this.amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public double getPrice() {
    return this.price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  @Override
  public String toString() {
    return transactionId + ": order " + orderId + " " + amount + "*" + price + " at " + time;
  }
  
}
