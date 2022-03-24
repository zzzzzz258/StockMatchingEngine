package edu.duke.ece568.em.server;

public class Account {
  private  String accountId;
  private double balance;

  public Account(String id, double balance) {
    accountId = id;
    this.balance = balance;
  }
  
  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String id) {
    this.accountId = id;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  @Override
  public String  toString() {
    return accountId + ": " + balance;
  }

  @Override
  public boolean equals(Object rhs) {
    if (rhs.getClass() != Account.class) {
      return false;
    }
    Account rhsA = (Account) rhs;
    boolean sameId = rhsA.getAccountId().equals(this.getAccountId());
    boolean sameBl = balance == rhsA.getBalance();
    return sameId && sameBl;
  }
  
}
