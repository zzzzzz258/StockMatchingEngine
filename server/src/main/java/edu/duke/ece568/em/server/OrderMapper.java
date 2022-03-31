package edu.duke.ece568.em.server;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper {
  final String insert = "insert into stock_order (symbol, amount, limit_price, account_id, status, time) values (#{symbol}, #{amount}, #{limitPrice}, #{accountId}, #{status, typeHandler=org.apache.ibatis.type.EnumTypeHandler}, #{time})";
  final String selectAll = "select * from stock_order";

  final String selectSellOrderByHighestPrice = "select * from stock_order where symbol = #{symbol} and status = 'OPEN' and amount < 0 and limit_price <= #{limitPrice} and account_id != #{accountId} order by limit_price desc, time";
  final String selectBuyOrderByLowestPrice = "select * from stock_order where symbol = #{symbol} and status = 'OPEN' and amount > 0 and limit_price >= #{limitPrice} and account_id != #{accountId} order by limit_price asc, time";
  final String deleteAll = "delete from stock_order";
  final String deleteById = "delete from stock_order where order_id = #{orderId}";
  final String cancelById = "update stock_order set status = 'CANCELED' where order_id = #{orderId}";
  final String executeById = "update stock_order set status = 'COMPLETE' where order_id = #{orderId}";
  final String updateAmountStatusById = "update stock_order set status = #{status, typeHandler = org.apache.ibatis.type.EnumTypeHandler},  amount = #{amount} where order_id = #{orderId}";
  
  @Insert(insert)
  @Options(useGeneratedKeys = true, keyProperty = "orderId")
  public void insert(Order order);

  @Select(selectAll)
  public List<Order> selectAll();

  /**
   * Select all sell orders whose limit price is larger than or equal to given
   * price
   * 
   * @return results are return in ascending order orderby limit price
   * @param results are matched by given symbol and status
   */
  @Select(selectSellOrderByHighestPrice)
  public List<Order> selectSellOrderByHighestPrice(Order order);

  /**
   * Select all buy orders whose limit price is lower than or equal to given price
   * 
   * @return results are return in descending order orderby limit price
   * @param results are matched by given symbol and status
   */
  @Select(selectBuyOrderByLowestPrice)
  public List<Order> selectBuyOrderByLowestPrice(Order order);

  @Delete(deleteAll)
  public void deleteAll();

  @Delete(deleteById)
  public void deleteById(Order order);

  @Update(cancelById)
  public void cancelById(Order order);

  @Update(executeById)
  public void executeById(Order order);

  @Update(updateAmountStatusById)
  public void updateAmountStatusById(Order order);
  
}
