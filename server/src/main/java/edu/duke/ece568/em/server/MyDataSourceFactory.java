package edu.duke.ece568.em.server;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;

public class MyDataSourceFactory {
  public static DataSource getDataSource(String driver, String database, String username, String password) {
    PooledDataSource dataSource = new PooledDataSource();
    dataSource.setDriver(driver);
    dataSource.setUrl(database);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    return dataSource;
  }
}
