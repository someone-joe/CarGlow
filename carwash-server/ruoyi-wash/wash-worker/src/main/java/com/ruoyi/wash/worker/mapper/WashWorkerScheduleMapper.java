package com.ruoyi.wash.worker.mapper;

import com.ruoyi.wash.worker.domain.WashWorkerSchedule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 师傅周排班读写（注解式）。 */
public interface WashWorkerScheduleMapper {

    @Select("select id, worker_id, week_day, start_minute, end_minute, enabled " +
            "from wash_worker_schedule where worker_id = #{workerId} and del_flag = '0' " +
            "order by week_day asc, start_minute asc")
    List<WashWorkerSchedule> selectByWorkerId(@Param("workerId") Long workerId);

    @Insert("insert into wash_worker_schedule (worker_id, week_day, start_minute, end_minute, enabled, del_flag, create_time) " +
            "values (#{workerId}, #{weekDay}, #{startMinute}, #{endMinute}, #{enabled}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WashWorkerSchedule schedule);

    /** 整体覆盖前先清空旧排班 */
    @Delete("delete from wash_worker_schedule where worker_id = #{workerId}")
    int deleteByWorkerId(@Param("workerId") Long workerId);
}
