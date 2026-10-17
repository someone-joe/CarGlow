package com.ruoyi.wash.goods.mapper;

import com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.domain.WashServiceCategory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 服务项读写。下单时按 serviceId 取名称与价格做快照。 */
public interface WashServiceMapper {

    @Select("select service_id, category_id, scene, service_name, price_amount, work_minutes, enabled, " +
            "detail_items as items, detail_not_included as notIncluded, detail_notices as notices, " +
            "detail_sample_images as sampleImages, applicable_models as applicableModels, cover_url as cover " +
            "from wash_service where service_id = #{serviceId} and enabled = 'Y' and del_flag = '0' limit 1")
    @Results(id = "washServiceDetail", value = {
            @Result(column = "items", property = "items", typeHandler = JsonStringListTypeHandler.class),
            @Result(column = "notIncluded", property = "notIncluded", typeHandler = JsonStringListTypeHandler.class),
            @Result(column = "notices", property = "notices", typeHandler = JsonStringListTypeHandler.class),
            @Result(column = "sampleImages", property = "sampleImages", typeHandler = JsonStringListTypeHandler.class)
    })
    WashService selectEnabledById(@Param("serviceId") Long serviceId);

    /** C 端服务项列表：只出上架且未删除的 */
    @Select("select service_id, category_id, scene, service_name, price_amount, work_minutes, enabled, " +
            "detail_items as items, detail_not_included as notIncluded, detail_notices as notices, " +
            "detail_sample_images as sampleImages, applicable_models as applicableModels, cover_url as cover " +
            "from wash_service where enabled = 'Y' and del_flag = '0' order by service_id asc")
    @ResultMap("washServiceDetail")
    List<WashService> selectEnabledList();

    /**
     * C 端服务项列表（按入口）：scene 为 null 时返回全部；
     * 否则返回该场景专属 + ALL 通用（两个入口都能看到通用服务）。
     */
    @Select("<script>" +
            "select service_id, category_id, scene, service_name, price_amount, work_minutes, enabled, " +
            "detail_items as items, detail_not_included as notIncluded, detail_notices as notices, " +
            "detail_sample_images as sampleImages, applicable_models as applicableModels, cover_url as cover " +
            "from wash_service where enabled = 'Y' and del_flag = '0' " +
            "<if test='scene != null and scene != \"\"'> and scene in (#{scene}, 'ALL') </if> " +
            "order by service_id asc" +
            "</script>")
    @ResultMap("washServiceDetail")
    List<WashService> selectEnabledListByScene(@Param("scene") String scene);

    @Select("select category_id, category_name, sort_num from wash_service_category " +
            "where del_flag = '0' order by sort_num asc, category_id asc")
    List<WashServiceCategory> selectCategories();

    // ---------------- 后台管理：服务项可配置（含下架项） ----------------

    /**
     * 后台列表：与 C 端的区别是不限 enabled —— 后台要能看到并重新上架已下架的服务。
     */
    @Select("<script>" +
            "select service_id, category_id, scene, service_name, price_amount, work_minutes, enabled, " +
            "detail_items as items, detail_not_included as notIncluded, detail_notices as notices, " +
            "detail_sample_images as sampleImages, applicable_models as applicableModels, cover_url as cover " +
            "from wash_service where del_flag = '0' " +
            "<if test='serviceName != null and serviceName != \"\"'> " +
            "and service_name like concat('%', #{serviceName}, '%') </if> " +
            "<if test='categoryId != null'> and category_id = #{categoryId} </if> " +
            "order by service_id desc limit #{offset}, #{size}" +
            "</script>")
    @ResultMap("washServiceDetail")
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

    @Select("select service_id, category_id, scene, service_name, price_amount, work_minutes, enabled, " +
            "detail_items as items, detail_not_included as notIncluded, detail_notices as notices, " +
            "detail_sample_images as sampleImages, applicable_models as applicableModels, cover_url as cover " +
            "from wash_service where service_id = #{serviceId} and del_flag = '0' limit 1")
    @ResultMap("washServiceDetail")
    WashService selectById(@Param("serviceId") Long serviceId);

    @Insert("insert into wash_service (category_id, scene, service_name, price_amount, work_minutes, enabled, " +
            "detail_items, detail_not_included, detail_notices, detail_sample_images, applicable_models, cover_url, " +
            "del_flag, create_time) values (#{categoryId}, #{scene}, #{serviceName}, #{priceAmount}, #{workMinutes}, " +
            "#{enabled}, #{items, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "#{notIncluded, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "#{notices, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "#{sampleImages, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "#{applicableModels}, #{cover}, '0', now())")
    @Options(useGeneratedKeys = true, keyProperty = "serviceId")
    int insertService(WashService service);

    @Update("update wash_service set category_id = #{categoryId}, scene = #{scene}, service_name = #{serviceName}, " +
            "price_amount = #{priceAmount}, work_minutes = #{workMinutes}, enabled = #{enabled}, " +
            "detail_items = #{items, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "detail_not_included = #{notIncluded, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "detail_notices = #{notices, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "detail_sample_images = #{sampleImages, typeHandler=com.ruoyi.wash.common.mybatis.JsonStringListTypeHandler}, " +
            "applicable_models = #{applicableModels}, cover_url = #{cover}, " +
            "update_time = now() where service_id = #{serviceId} and del_flag = '0'")
    int updateService(WashService service);

    /** 逻辑删除：与项目其它表一致，不做物理删除（历史订单还引用着服务名与价格快照） */
    @Update("update wash_service set del_flag = '2' where service_id = #{serviceId}")
    int logicDelete(@Param("serviceId") Long serviceId);
}
