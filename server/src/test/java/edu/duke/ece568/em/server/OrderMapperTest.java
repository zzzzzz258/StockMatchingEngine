package edu.duke.ece568.em.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

public class OrderMapperTest {
  public static void addRows(OrderMapper om) {
    Order o1 = new Order("TLSA", -100, 100, "1");
    Order o2 = new Order("TLSA", -105, 105, "1");
    Order o3 = new Order("TLSA", -110, 105, "1");
    Order o4 = new Order("TLSA", -115, 95, "1");
    Order o5 = new Order("TLSA", -120, 90, "1");
    Order o6 = new Order("TLSA", -125, 95, "1");

    om.insert(o1);
    om.insert(o2);
    om.insert(o3);
    om.insert(o4);
    om.insert(o5);
    om.insert(o6);

    Order o11 = new Order("TLSA", 100, 88, "2");
    Order o12 = new Order("TLSA", 105, 85, "2");

    om.insert(o11);
    om.insert(o12);    

    System.out.println(o11);
  }

  @Test
  public void test_All() {
    SqlSessionFactory ssf = SingletonSQLFactory.getSqlSessionFactory();

    try (SqlSession session = ssf.openSession()) {
      AccountMapper accountMapper = session.getMapper(AccountMapper.class);
      PositionMapper pm = session.getMapper(PositionMapper.class);
      OrderMapper om = session.getMapper(OrderMapper.class);

      accountMapper.deleteAll();

      Account a1 = new Account("1", 900);
      Account a2 = new Account("2", 900);
      accountMapper.insert(a1);
      accountMapper.insert(a2);

      assertEquals(0, om.selectAll().size());

      addRows(om);
      List<Order> oss = om.selectAll();      
      
      List<Order> os1 = om.selectAll();
      assertEquals(8, os1.size());

      List<Order> os2 = om.selectSellOrderByHighestPrice(new Order("TLSA", 0, 99, "2"));
      assertEquals(3, os2.size());

      List<Order> os3 = om.selectSellOrderByHighestPrice(new Order("TLSA", 0, 110, "2"));
      for (Order order : os3) {
        //        System.out.println(order);
      }

      Order or1 = oss.get(0);
      Order or2 = oss.get(1);
      Order or3 = oss.get(2);

      om.cancelById(or1);
      om.executeById(or2);
      or3.setAmount(9999);
      om.updateAmountById(or3);

      oss = om.selectAll();
      Map<Integer, Order> osmap = new HashMap<Integer, Order>();
      //      System.out.println();
      for (Order or: oss) {
        osmap.put(or.getOrderId(), or);
        //        System.out.println(or);
      }
      
      assertEquals(Status.CANCELED, osmap.get(or1.getOrderId()).getStatus());
      assertEquals(Status.COMPLETE, osmap.get(or2.getOrderId()).getStatus());
      assertEquals(9999, osmap.get(or3.getOrderId()).getAmount());
      

      // test deleteById and deleteAll     
      int id = oss.get(0).getOrderId();
      om.deleteById(new Order(id, null, 0, 0, null, Status.CANCELED, 0));
      assertEquals(7, om.selectAll().size());

      om.deleteAll();
      assertEquals(0, om.selectAll().size());

      session.commit();
    }
  }

}
