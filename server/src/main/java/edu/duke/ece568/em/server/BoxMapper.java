package edu.duke.ece568.em.server;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface BoxMapper {
  @Select("SELECT * FROM box")
  Box selectBox();

  @Insert("insert into box (size) values (#{size})")
  void addBox(int size);

  @Delete("delete from box where true")
  void deleteAll();
}
