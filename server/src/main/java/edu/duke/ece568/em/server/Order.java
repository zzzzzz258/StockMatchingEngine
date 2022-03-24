package edu.duke.ece568.em.server;

public class Order {
  private int orderId;
  private String symbol;
  private double amount;
  private double limitPrice;
  private String accountId;
  private Status status;
  private long time;   // the time is in ms

  public Order(int id, String symbol, double amount, double limitPrice, String accountId, Status status, long t) {
    this.orderId = id;
    this.symbol = symbol;
    this.amount = amount;
    this.limitPrice = limitPrice;
    this.accountId = accountId;
    this.status = status;
    this.time = t;
  }
  
  public Order(String symbol, double amount, double limitPrice, String accountId, Status status, long t) {
    this.symbol = symbol;
    this.amount = amount;
    this.limitPrice = limitPrice;
    this.accountId = accountId;
    this.status = status;
    this.time = t;
  }

  public Order(String symbol, double amount, double limitPrice, String accountId, Status status) {
    this(symbol, amount, limitPrice, accountId, status, System.currentTimeMillis());
  }

  public Order(String symbol, double amount, double limitPrice, String accountId, long t) {
    this(symbol, amount, limitPrice, accountId, Status.OPEN,  t);
  }

  public Order(String symbol, double amount, double limitPrice, String accountId) {
    this(symbol, amount, limitPrice, accountId, Status.OPEN, System.currentTimeMillis());
  }
  
  public int getOrderId() {
    return orderId;
  }

  public void setOrderId(int id) {
    this.orderId = id;
  }
  
  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String s) {
    this.symbol = s;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double a) {
    amount = a;
  }

  public double getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(double limitPrice) {
    this.limitPrice = limitPrice;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String id) {
    accountId = id;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long t) {
    this.time = t;
  }

  @Override
  public String toString() {
    String result = status + " " + orderId + ": " + symbol + ", amount:" + amount + ", price:" + limitPrice + " by " + accountId + " at " + time;
    return result;
  }
}
