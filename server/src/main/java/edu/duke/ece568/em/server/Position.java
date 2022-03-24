package edu.duke.ece568.em.server;

public class Position {
  private String symbol;
  private double amount;
  private String accountId;

  public Position(String s, String ai) {
    symbol = s;
    accountId = ai;
  }
  
  public Position(String s, double sm, String ai) {
    symbol = s;
    amount = sm;
    accountId = ai;
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

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String id) {
    accountId = id;
  }
}
