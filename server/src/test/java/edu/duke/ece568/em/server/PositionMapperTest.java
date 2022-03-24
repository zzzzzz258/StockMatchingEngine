package edu.duke.ece568.em.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

public class PositionMapperTest {
  @Test
  public void test_Everything() {
    SqlSessionFactory ssf = Server.getSqlSessionFactory();

    try (SqlSession session = ssf.openSession()) {
      AccountMapper accountMapper = session.getMapper(AccountMapper.class);
      PositionMapper pm = session.getMapper(PositionMapper.class);
        
      accountMapper.deleteAll();
      
      Account a = new Account("1234", 900);
      accountMapper.insert(a);

      Position p = new Position("BTC", 232, "1234");
      pm.insert(p);

      Position p1 = pm.select(new Position("BTC", "1234"));
      assertEquals(232, p1.getAmount());
      Position p2 = pm.select(new Position("TSLA", "1234"));
      assertNull(p2);

      p.setAmount(666.8);
      pm.update(p);
      p1 = pm.select(p1);
      assertEquals(666.8, p1.getAmount());
      
      session.commit();
    }
  }

}
