package com.ruoyi.wash.order.mapper;

import com.ruoyi.wash.order.domain.WashReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 评价读写。一单一评由 uk_wash_review_order 保证，插入前先在 Service 查一次以便给出明确报错。 */
public interface WashReviewMapper {

    @Insert("insert into wash_review (order_no, member_id, rating, content, tags, del_flag, create_time) " +
            "values (#{orderNo}, #{memberId}, #{rating}, #{content}, #{tags}, '0', sysdate())")
    int insertReview(WashReview review);

    @Select("select review_id from wash_review where order_no = #{orderNo} and del_flag = '0' limit 1")
    Long selectIdByOrderNo(@Param("orderNo") String orderNo);
}
