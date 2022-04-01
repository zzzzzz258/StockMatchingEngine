package edu.duke.ece568.em.server;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


public interface AccountMapper {
  
  final String selectAll = "select * from account";
  final String selectOneByID = "select * from account where account_id = #{accountId}";
  final String selectOneByIDL = "select * from account where account_id = #{accountId} for update";
  final String insert = "insert into account (account_id, balance) values (#{accountId}, #{balance});";
  final String deleteAll = "delete from account";
  final String updateBalance = "update account set balance = #{balance} where account_id = #{accountId}";
  
  /**
   * select all data, mainly for testing
   */
  @Select(selectAll)
  @Results(value = {
      @Result(property = "accountId", column = "account_id"),
      @Result(property = "balance", column = "balance"),
  })
  List<Account> selectAll();

  /**
   * Select single row by its account id
   */
  @Select(selectOneByID)
  Account selectOneById(String accountId);

  /**
   * Select single row by its account id
   */
  @Select(selectOneByIDL)
  Account selectOneByIdL(String accountId);
  
  
  /**
   * Insert an account into db
   */
  @Insert(insert)
  void insert(Account account);

  /**
   * Delete all data, used in initialization of testing
   */
  @Delete(deleteAll)
  void deleteAll();

  @Update(updateBalance)
  void updateBalance(Account account);
}
