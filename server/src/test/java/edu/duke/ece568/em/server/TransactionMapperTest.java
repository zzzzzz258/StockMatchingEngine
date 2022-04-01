package edu.duke.ece568.em.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

public class TransactionMapperTest {
  @Test
  public void test_all() {
    SqlSessionFactory ssf = SingletonSQLFactory.getSqlSessionFactory();

    try (SqlSession session = ssf.openSession()) {
      AccountMapper accountMapper = session.getMapper(AccountMapper.class);
      PositionMapper pm = session.getMapper(PositionMapper.class);
      TransactionMapper tm = session.getMapper(TransactionMapper.class);
      OrderMapper om = session.getMapper(OrderMapper.class);

      accountMapper.deleteAll();

      Account a1 = new Account("1", 900);
      Account a2 = new Account("2", 900);
      accountMapper.insert(a1);
      accountMapper.insert(a2);

      Order o1 = new Order("TLSA", -100, 100, "1");
      Order o2 = new Order("TLSA", -105, 105, "1");
      Order o3 = new Order("TLSA", -110, 105, "1");
      Order o4 = new Order("TLSA", 100, 95, "1");
      Order o5 = new Order("TLSA", 120, 90, "1");
      Order o6 = new Order("TLSA", 125, 95, "1");

      om.insert(o1);
      om.insert(o2);
      om.insert(o3);
      om.insert(o4);
      om.insert(o5);
      om.insert(o6);
      
      Transaction t1 = new Transaction(o1.getOrderId(), -50, 101);
      Transaction t2 = new Transaction(o1.getOrderId(), -25, 101);
      tm.insert(t1);
      tm.insert(t2);
      List<Transaction> ts = tm.selectByOrderId(o1.getOrderId());
      assertEquals(2, ts.size());

      //ts.stream().forEach(a -> System.out.println(a));

      session.commit();
    }
  }

}
