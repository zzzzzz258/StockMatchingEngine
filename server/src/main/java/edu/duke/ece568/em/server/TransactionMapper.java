package edu.duke.ece568.em.server;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface TransactionMapper {
  final String insert = "insert into transaction (order_id, amount, price, time) values (#{orderId}, #{amount}, #{price}, #{time})";
  final String selectAll = "select * from transaction";
  final String deleteAll = "delete from transaction";
  final String selectByOrderId = "select * from transaction where order_id = #{orderId}";


  
  @Insert(insert)
  @Options(useGeneratedKeys = true, keyProperty = "transactionId")
  public void insert(Transaction transaction);

  @Select(selectAll)
  public List<Transaction> selectAll();

  @Delete(deleteAll)
  public void deleteAll();

  @Select(selectByOrderId)
  public List<Transaction> selectByOrderId(int orderId);
}
