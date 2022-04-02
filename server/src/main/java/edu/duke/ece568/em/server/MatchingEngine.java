package edu.duke.ece568.em.server;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.TransactionIsolationLevel;

public class MatchingEngine implements Runnable {

  private SqlSession getDefaultSession() {
    return SingletonSQLFactory.getInstance().openSession(true);
  }

  private SqlSession getSerilizableSession() {
    return SingletonSQLFactory.getInstance().openSession(TransactionIsolationLevel.SERIALIZABLE);
  }

  @Override
  public void run() {
    while (true) {
      try (SqlSession session = this.getSerilizableSession()) {
        OrderMapper orderMapper = session.getMapper(OrderMapper.class);
        TransactionMapper transactionMapper = session.getMapper(TransactionMapper.class);
        AccountMapper accountMapper = session.getMapper(AccountMapper.class);
        PositionMapper positionMapper = session.getMapper(PositionMapper.class);

        Order buyer = orderMapper.selectBestBuyer();
        Order seller = orderMapper.selectBestSeller();
        //        System.out.println(buyer + "||" + seller);
        if (buyer != null && seller != null && buyer.getLimitPrice() >= seller.getLimitPrice()) {
          //System.out.println("DEALDEALDEALDAEL");
          double matchPrice = seller.getTime() < buyer.getTime() ? seller.getLimitPrice() : buyer.getLimitPrice();
          double matchAmount = Math.min(Math.abs(seller.getAmount()), Math.abs(buyer.getAmount()));

          Transaction sellTransaction = doSellBusiness(seller, matchAmount, matchPrice);
          Transaction buyTransaction = doBuyBusiness(buyer, matchAmount, matchPrice);

          orderMapper.updateAmountStatusById(seller);
          orderMapper.updateAmountStatusById(buyer);

          transactionMapper.insert(buyTransaction);
          transactionMapper.insert(sellTransaction);

          session.commit();

          // add balance to account, add shraes to position
          addBalance(seller.getAccountId(), matchAmount * matchPrice);
          //System.out.println("money got");
          addPosition(buyer.getAccountId(), buyer.getSymbol(), matchAmount);
          //System.out.println("deal made");
          if (buyer.getLimitPrice() > matchPrice) { // refund
            addBalance(buyer.getAccountId(), matchAmount * (buyer.getLimitPrice() - matchPrice));
          }
        }
      } // end try
      catch (Exception e) {
         System.out.println(e.getMessage());
      }
    }// end while
  }

  /**
   * Method to do logics in buy operation
   */
  private Transaction doBuyBusiness(Order order, double amount, double price) {
    order.setAmount(order.getAmount() - amount);
    if (order.getAmount() == 0) {
      order.setStatus(Status.COMPLETE);
    }
    return new Transaction(order.getOrderId(), amount, price);
  }

  /**
   * Method to de logics in sell operation
   */
  private Transaction doSellBusiness(Order order, double amount, double price) {
    // update amount in order, and account's balance
    order.setAmount(order.getAmount() + amount);
    if (order.getAmount() == 0) {
      order.setStatus(Status.COMPLETE);
    }
    return new Transaction(order.getOrderId(), -amount, price);
  }

  /**
   * Method to add shares to a position
   */
  private void addPosition(String accountId, String symbol, double amount) {
    while (true) {
      try (SqlSession dbSession = this.getSerilizableSession()) {
        PositionMapper positionMapper = dbSession.getMapper(PositionMapper.class);
        Position offset = new Position(symbol, amount, accountId);
        Position position = positionMapper.select(offset);
        if (position == null) {
          positionMapper.insert(offset);
        } else {
          positionMapper.updateAddPosition(offset);
        }
        dbSession.commit();
        return;
      }
      catch (Exception e) {
        //        System.out.println(e.getMessage());
      }
    }
  }

  /**
   * method to add balance to account
   */
  private void addBalance(String accountId, double balance) {
    while (true) {
      try (SqlSession dbSession = this.getDefaultSession()) {
        AccountMapper accountMapper = dbSession.getMapper(AccountMapper.class);
        Account offset = new Account(accountId, balance);
        accountMapper.updateAddBalance(offset);
        return;
      }
      catch(Exception e) {
        
      }
    }
  }

}
