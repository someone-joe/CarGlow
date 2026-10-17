package com.ruoyi.wash.worker.mapper;

import com.ruoyi.wash.worker.domain.WashWorkerQualification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 师傅资质读写（注解式）。一个师傅一条记录，靠 uk_qual_worker 兜底并发。 */
public interface WashWorkerQualificationMapper {

    @Select("select id, worker_id, id_name, id_no, id_front_file, id_back_file, license_file, " +
            "face_status, face_provider, face_ref, face_time, review_status, review_remark, reviewer, " +
            "submit_time, review_time " +
            "from wash_worker_qualification where worker_id = #{workerId} and del_flag = '0' limit 1")
    WashWorkerQualification selectByWorkerId(@Param("workerId") Long workerId);

    @Select("select id, worker_id, id_name, id_no, id_front_file, id_back_file, license_file, " +
            "face_status, face_provider, face_ref, face_time, review_status, review_remark, reviewer, " +
            "submit_time, review_time " +
            "from wash_worker_qualification where id = #{id} and del_flag = '0' limit 1")
    WashWorkerQualification selectById(@Param("id") Long id);

    /** 后台待审核列表：按提交时间正序（先提交先审） */
    @Select("select id, worker_id, id_name, id_no, id_front_file, id_back_file, license_file, " +
            "face_status, face_provider, face_ref, face_time, review_status, review_remark, reviewer, " +
            "submit_time, review_time " +
            "from wash_worker_qualification where del_flag = '0' " +
            "and review_status = #{reviewStatus} order by submit_time asc limit #{offset}, #{size}")
    List<WashWorkerQualification> selectByReviewStatus(@Param("reviewStatus") String reviewStatus,
                                                       @Param("offset") int offset,
                                                       @Param("size") int size);

    @Select("select count(*) from wash_worker_qualification where del_flag = '0' and review_status = #{reviewStatus}")
    long countByReviewStatus(@Param("reviewStatus") String reviewStatus);

    @Insert("insert into wash_worker_qualification (worker_id, id_name, id_no, id_front_file, id_back_file, " +
            "license_file, face_status, review_status, submit_time, del_flag, create_time) " +
            "values (#{workerId}, #{idName}, #{idNo}, #{idFrontFile}, #{idBackFile}, #{licenseFile}, " +
            "'PENDING', #{reviewStatus}, #{submitTime}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WashWorkerQualification qualification);

    /** 师傅重新提交：证件信息覆盖，审核回到待审；人脸核身结果保留（本人脸不会变） */
    @Update("update wash_worker_qualification set id_name = #{idName}, id_no = #{idNo}, " +
            "id_front_file = #{idFrontFile}, id_back_file = #{idBackFile}, license_file = #{licenseFile}, " +
            "review_status = 'PENDING', review_remark = null, submit_time = #{submitTime}, update_time = sysdate() " +
            "where worker_id = #{workerId} and del_flag = '0'")
    int resubmit(WashWorkerQualification qualification);

    /** 后台审核：PASSED / REJECTED（驳回必须带原因） */
    @Update("update wash_worker_qualification set review_status = #{reviewStatus}, review_remark = #{reviewRemark}, " +
            "reviewer = #{reviewer}, review_time = #{reviewTime}, update_time = sysdate() " +
            "where worker_id = #{workerId} and del_flag = '0'")
    int review(@Param("workerId") Long workerId, @Param("reviewStatus") String reviewStatus,
               @Param("reviewRemark") String reviewRemark, @Param("reviewer") String reviewer,
               @Param("reviewTime") Long reviewTime);

    /** 人脸核身结果回填（核身渠道回调或后台人工登记） */
    @Update("update wash_worker_qualification set face_status = #{faceStatus}, face_provider = #{faceProvider}, " +
            "face_ref = #{faceRef}, face_time = #{faceTime}, update_time = sysdate() " +
            "where worker_id = #{workerId} and del_flag = '0'")
    int updateFace(@Param("workerId") Long workerId, @Param("faceStatus") String faceStatus,
                   @Param("faceProvider") String faceProvider, @Param("faceRef") String faceRef,
                   @Param("faceTime") Long faceTime);
}
