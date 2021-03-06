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
  final String selectById = "select * from stock_order where order_id = #{orderId}";
  final String selectByIdL = "select * from stock_order where order_id = #{orderId} for update";
  final String selectSellOrderByHighestPrice = "select * from stock_order where symbol = #{symbol} and status = 'OPEN' and amount < 0 and limit_price <= #{limitPrice} and account_id != #{accountId} order by limit_price desc, time";
  final String selectBuyOrderByLowestPrice = "select * from stock_order where symbol = #{symbol} and status = 'OPEN' and amount > 0 and limit_price >= #{limitPrice} and account_id != #{accountId} order by limit_price asc, time";
  final String deleteAll = "delete from stock_order";
  final String deleteById = "delete from stock_order where order_id = #{orderId}";
  final String cancelById = "update stock_order set status = 'CANCELED', time = #{time} where order_id = #{orderId}";
  final String executeById = "update stock_order set status = 'COMPLETE' where order_id = #{orderId}";
  final String updateAmountStatusById = "update stock_order set status = #{status, typeHandler = org.apache.ibatis.type.EnumTypeHandler},  amount = #{amount} where order_id = #{orderId}";
  final String selectSellOrder = "select * from stock_order where symbol = #{symbol} and status = 'OPEN' and amount < 0 and limit_price <= #{limitPrice} and account_id != #{accountId} order by limit_price desc, time limit 1 for update";
  final String selectBuyOrder = "select * from stock_order where symbol = #{symbol} and status = 'OPEN' and amount > 0 and limit_price >= #{limitPrice} and account_id != #{accountId} order by limit_price asc, time limit 1 for update";
  final String lockOrder = "update stock_order set status = 'COMPLETE' where order_id = #{orderId} and status='OPEN'";
  final String freeLockedOrder = "update stock_order set status =#{status, typeHandler = org.apache.ibatis.type.EnumTypeHandler}, amount = #{amount} where order_id = #{orderId}";
  
  @Insert(insert)
  @Options(useGeneratedKeys = true, keyProperty = "orderId")
  public void insert(Order order);

  @Select(selectAll)
  public List<Order> selectAll();

  @Select(selectById)
  public Order selectById(int orderId);
  
  @Select(selectByIdL)
  public Order selectByIdL(int orderId);

  
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

  @Select(selectBuyOrder)
  public Order selectBuyOrder(Order order);
  
  @Select(selectSellOrder)
  public Order selectSellOrder(Order order);

  @Update(lockOrder)
  public int lockOrder(Order order);

  @Update(freeLockedOrder)
  public void freeLockedOrder(Order order);

  @Select("select * from stock_order where status = 'OPEN' and amount > 0 order by limit_price asc, time limit 1")
  public Order selectBestBuyer();

  @Select("select * from stock_order where status = 'OPEN' and amount < 0 order by limit_price desc, time limit 1")
  public Order selectBestSeller();
}
