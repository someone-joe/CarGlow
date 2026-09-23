package com.ruoyi.wash.goods.mapper;

import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.domain.WashServiceCategory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 服务项读写。下单时按 serviceId 取名称与价格做快照。 */
public interface WashServiceMapper {

    @Select("select service_id, category_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where service_id = #{serviceId} and enabled = 'Y' and del_flag = '0' limit 1")
    WashService selectEnabledById(@Param("serviceId") Long serviceId);

    /** C 端服务项列表：只出上架且未删除的 */
    @Select("select service_id, category_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where enabled = 'Y' and del_flag = '0' order by service_id asc")
    List<WashService> selectEnabledList();

    @Select("select category_id, category_name, sort_num from wash_service_category " +
            "where del_flag = '0' order by sort_num asc, category_id asc")
    List<WashServiceCategory> selectCategories();

    // ---------------- 后台管理：服务项可配置（含下架项） ----------------

    /**
     * 后台列表：与 C 端的区别是不限 enabled —— 后台要能看到并重新上架已下架的服务。
     */
    @Select("<script>" +
            "select service_id, category_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where del_flag = '0' " +
            "<if test='serviceName != null and serviceName != \"\"'> " +
            "and service_name like concat('%', #{serviceName}, '%') </if> " +
            "<if test='categoryId != null'> and category_id = #{categoryId} </if> " +
            "order by service_id desc limit #{offset}, #{size}" +
            "</script>")
    List<WashService> selectAdminList(@Param("serviceName") String serviceName,
                                      @Param("categoryId") Long categoryId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    @Select("<script>" +
            "select count(*) from wash_service where del_flag = '0' " +
            "<if test='serviceName != null and serviceName != \"\"'> " +
            "and service_name like concat('%', #{serviceName}, '%') </if> " +
            "<if test='categoryId != null'> and category_id = #{categoryId} </if>" +
            "</script>")
    long countAdmin(@Param("serviceName") String serviceName, @Param("categoryId") Long categoryId);

    @Select("select service_id, category_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where service_id = #{serviceId} and del_flag = '0' limit 1")
    WashService selectById(@Param("serviceId") Long serviceId);

    @Insert("insert into wash_service (category_id, service_name, price_amount, work_minutes, enabled, " +
            "del_flag, create_time) values (#{categoryId}, #{serviceName}, #{priceAmount}, #{workMinutes}, " +
            "#{enabled}, '0', now())")
    @Options(useGeneratedKeys = true, keyProperty = "serviceId")
    int insertService(WashService service);

    @Update("update wash_service set category_id = #{categoryId}, service_name = #{serviceName}, " +
            "price_amount = #{priceAmount}, work_minutes = #{workMinutes}, enabled = #{enabled}, " +
            "update_time = now() where service_id = #{serviceId} and del_flag = '0'")
    int updateService(WashService service);

    /** 逻辑删除：与项目其它表一致，不做物理删除（历史订单还引用着服务名与价格快照） */
    @Update("update wash_service set del_flag = '2' where service_id = #{serviceId}")
    int logicDelete(@Param("serviceId") Long serviceId);
}
