package cn.kmbeast.mapper;

import cn.kmbeast.pojo.entity.UserDietRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserDietRecordMapper {
    void insert(UserDietRecord userDietRecord);

    // 查询当前用户当天的饮食记录
    @Select("SELECT * FROM user_diet_record WHERE user_id = #{userId} AND DATE(create_time) = CURDATE() ORDER BY create_time DESC")
    List<UserDietRecord> getTodayRecords(Integer userId);
}