package edu.duke.ece568.em.server;

import java.io.IOException;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import edu.duke.ece568.em.common.Thing;

public class Server {
  public int factorial(int x) {
    int ans = 1;
    while (x > 0) {
      ans = ans * x;
      x--;
    }
    return ans;
  }

  

  public static void main(String[] args) throws IOException {
    // simple application case for common package
    Thing t = new Thing("server");
    System.out.println(t);    

    // simple test case for mybatis ORM
    SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
    try (SqlSession session = sqlSessionFactory.openSession()) {
      BoxMapper mapper = session.getMapper(BoxMapper.class);
      mapper.deleteAll();
      mapper.addBox(15);

      session.commit();
      System.out.println("Add a box");
    }
      
  }

  /**
   * Connect to the postgresql database.
   * @return {@link SqlSessionFactory}
   */
  public static SqlSessionFactory getSqlSessionFactory() {
    DataSource dataSource = MyDataSourceFactory.getDataSource("org.postgresql.Driver", "jdbc:postgresql:test_db", "postgres", "ece568hw4");  // TODO: update the arguments after creating tables
    
    TransactionFactory transactionFactory =
      new JdbcTransactionFactory();
    Environment environment =
      new Environment("development", transactionFactory, dataSource);
    Configuration configuration = new Configuration(environment);
    configuration.addMapper(BoxMapper.class);
    SqlSessionFactory sqlSessionFactory =
      new SqlSessionFactoryBuilder().build(configuration);
    return sqlSessionFactory;
  }
}
