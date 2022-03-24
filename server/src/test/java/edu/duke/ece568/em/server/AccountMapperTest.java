package edu.duke.ece568.em.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

public class AccountMapperTest {
  @Test
  public void test_Everything() {
    SqlSessionFactory ssf = Server.getSqlSessionFactory();

    try (SqlSession session = ssf.openSession()) {
      AccountMapper accountMapper = session.getMapper(AccountMapper.class);

      accountMapper.deleteAll();

      Account a = new Account("1234", 900);
      accountMapper.insert(a);
      Account a1 = accountMapper.selectOneById("1234");
      assertEquals(a, a1);
      Account a2 = accountMapper.selectOneById("1233");
      assertNull(a2);

      accountMapper.deleteAll();
      List<Account> as = accountMapper.selectAll();
      assertEquals(0, as.size());

      session.commit();
    }
  }

}
