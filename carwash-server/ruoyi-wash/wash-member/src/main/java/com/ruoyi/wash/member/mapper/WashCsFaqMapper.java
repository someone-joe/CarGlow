package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashCsFaq;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 客服常见问题读写。后台「客服配置」页维护，C 端客服 tab 读取展示。 */
public interface WashCsFaqMapper {

    /** 全部有效问题，按 sortOrder 升序、faqId 升序（C 端与后台页共用） */
    @Select("select faq_id, question, answer, sort_order, create_time, update_time " +
            "from wash_cs_faq where del_flag = '0' order by sort_order asc, faq_id asc")
    List<WashCsFaq> selectAll();

    /** 新增问题并回填主键 */
    @Insert("insert into wash_cs_faq (question, answer, sort_order, del_flag, create_time) " +
            "values (#{question}, #{answer}, #{sortOrder}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "faqId")
    int insert(WashCsFaq faq);

    /** 更新问题（仅改内容，不改删除标记） */
    @Update("update wash_cs_faq set question = #{question}, answer = #{answer}, " +
            "sort_order = #{sortOrder}, update_time = sysdate() " +
            "where faq_id = #{faqId} and del_flag = '0'")
    int update(WashCsFaq faq);

    /** 逻辑删除全部（后台保存时整体替换，先清后插） */
    @Update("update wash_cs_faq set del_flag = '2', update_time = sysdate() where del_flag = '0'")
    int logicDeleteAll();
}
