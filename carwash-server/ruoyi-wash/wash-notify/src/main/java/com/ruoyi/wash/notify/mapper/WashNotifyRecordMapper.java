package com.ruoyi.wash.notify.mapper;

import com.ruoyi.wash.notify.domain.WashNotifyRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/** 推送记录读写。 */
public interface WashNotifyRecordMapper {

    @Insert("insert into wash_notify_record (member_id, openid, template_key, template_id, biz_no, content, status, fail_reason, del_flag, create_time) " +
            "values (#{memberId}, #{openid}, #{templateKey}, #{templateId}, #{bizNo}, #{content}, #{status}, #{failReason}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(WashNotifyRecord record);

    /** 幂等：同一业务号是否已发过 */
    @Select("select count(*) from wash_notify_record where biz_no = #{bizNo} and del_flag = '0'")
    long countByBizNo(@Param("bizNo") String bizNo);

    /** 月度限额统计：该会员该模板当月已成功发送条数 */
    @Select("select count(*) from wash_notify_record where member_id = #{memberId} and template_key = #{templateKey} " +
            "and status = 'SENT' and del_flag = '0' and create_time >= #{monthStart} and create_time < #{monthEnd}")
    long countSentInMonth(@Param("memberId") Long memberId, @Param("templateKey") String templateKey,
                          @Param("monthStart") Date monthStart, @Param("monthEnd") Date monthEnd);
}
