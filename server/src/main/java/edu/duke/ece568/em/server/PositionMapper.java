package edu.duke.ece568.em.server;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PositionMapper {

  final String insert = "insert into position values (#{symbol}, #{amount}, #{accountId})";
  final String select = "select * from position where symbol = #{symbol} and account_id = #{accountId}";
  final String selectL = "select * from position where symbol = #{symbol} and account_id = #{accountId} for update of position";
  final String update = "update position set amount = #{amount} where symbol = #{symbol} and account_id = #{accountId}";
  final String updateAddPosition = "update position set amount = amount + #{amount} where symbol = #{symbol} and account_id = #{accountId}";
  final String updateRemovePosition = "update position set amount = amount - #{amount} where symbol = #{symbol} and account_id = #{accountId}";

  @Insert(insert)
  public void insert(Position position);

  @Select(select)
  public Position select(Position position);

  @Select(selectL)
  public Position selectL(Position position);

  @Update(update)
  public void update(Position position);

  @Update(updateAddPosition)
  public void updateAddPosition(Position position);

  @Update(updateRemovePosition)
  public void updateRemovePosition(Position position);
}
