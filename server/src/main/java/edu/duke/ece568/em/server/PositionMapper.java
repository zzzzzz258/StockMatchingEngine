package edu.duke.ece568.em.server;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PositionMapper {

  final String insert = "insert into position values (#{symbol}, #{amount}, #{accountId})";
  final String select = "select * from position where symbol = #{symbol} and account_id = #{accountId}";
  final String update = "update position set amount = #{amount} where symbol = #{symbol} and account_id = #{accountId}";
  
  @Insert(insert)
  public void insert(Position position);

  @Select(select)
  public Position select(Position position);

  @Update(update)
  public void update(Position position);
}
