package edu.duke.ece568.em.server;

import javax.sql.DataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

public class SingletonSQLFactory {
  // The field must be declared volatile so that double check lock would work
  // correctly.
  private static volatile SingletonSQLFactory instance;
  private static SqlSessionFactory sqlSessionFactory;

  /**
   * Constructor for singleton class
   */
  private SingletonSQLFactory() {
    sqlSessionFactory = getSqlSessionFactory();
  }

  /**
   * Method to get instance og Singleton class
   */
  public static SingletonSQLFactory getInstance() {
    SingletonSQLFactory result = instance;
    if (result != null) {
      return result;
    }
    synchronized (SingletonSQLFactory.class) {
      if (instance == null) {
        instance = new SingletonSQLFactory();
      }
      return instance;
    }
  }

  /**
   * Method to open a new session with the database
   */
  public SqlSession openSession() {
    return getSqlSessionFactory().openSession();
  }

  /**
   * Method to open a new session with autoCommitted
   */
  public SqlSession openSession(boolean autoCommitted) {
    return getSqlSessionFactory().openSession(autoCommitted);
  }

  /**
   * Method to open a new session with IsolationLevel
   */
  public SqlSession openSession(TransactionIsolationLevel level) {
    return getSqlSessionFactory().openSession(level);
  }
  
  /**
   * Connect to the postgresql database.
   * 
   * @return {@link SqlSessionFactory}
   */
  public static SqlSessionFactory getSqlSessionFactory() {
    if (sqlSessionFactory == null) {
      DataSource dataSource = MyDataSourceFactory.getDataSource("org.postgresql.Driver", "jdbc:postgresql:stock_market",
          "postgres", "ece568hw4");

      TransactionFactory transactionFactory = new JdbcTransactionFactory(dataSource, TransactionIsolationLevel.SERIALIZABLE, true);
      Environment environment = new Environment("development", transactionFactory, dataSource);
      Configuration configuration = new Configuration(environment);
      configuration.addMapper(AccountMapper.class);
      configuration.addMapper(PositionMapper.class);
      configuration.addMapper(OrderMapper.class);
      configuration.addMapper(TransactionMapper.class);
      
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }
    return sqlSessionFactory;
  }

}
