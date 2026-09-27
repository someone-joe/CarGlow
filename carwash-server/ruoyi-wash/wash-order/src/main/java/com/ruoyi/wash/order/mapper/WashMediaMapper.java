package com.ruoyi.wash.order.mapper;

import com.ruoyi.wash.order.domain.WashMedia;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 影像读写。对外只认 fileId，磁盘路径不出现在任何接口里。 */
public interface WashMediaMapper {

    @Insert("insert into wash_media (file_id, biz_type, order_no, member_id, file_name, store_path, file_size, upload_by, del_flag, create_time) " +
            "values (#{fileId}, #{bizType}, #{orderNo}, #{memberId}, #{fileName}, #{storePath}, #{fileSize}, #{uploadBy}, '0', sysdate())")
    int insertMedia(WashMedia media);

    @Select("select media_id, file_id, biz_type, order_no, member_id, file_name, store_path, file_size, upload_by, create_time " +
            "from wash_media where file_id = #{fileId} and del_flag = '0' limit 1")
    WashMedia selectByFileId(@Param("fileId") String fileId);

    @Select("select media_id, file_id, biz_type, order_no, member_id, file_name, store_path, file_size, upload_by, create_time " +
            "from wash_media where order_no = #{orderNo} and del_flag = '0' order by create_time asc")
    List<WashMedia> selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 下单成功后把停车照挂到订单上：下单时订单号还没生成，这些影像是「无主」的。
     * 只更新尚未归属的（order_no is null），已归其他单的影像不能被抢走。
     */
    @Update("<script>" +
            "update wash_media set order_no = #{orderNo} " +
            "where del_flag = '0' and order_no is null " +
            "and file_id in " +
            "<foreach collection='fileIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int bindOrderNo(@Param("fileIds") List<String> fileIds, @Param("orderNo") String orderNo);
}
