package com.ruoyi.wash.invoice.mapper;

import com.ruoyi.wash.invoice.domain.WashInvoice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 发票申请读写。 */
public interface WashInvoiceMapper {

    @Insert("insert into wash_invoice (member_id, order_no, amount, title_type, title, tax_no, email, status, remark, del_flag, create_time) " +
            "values (#{memberId}, #{orderNo}, #{amount}, #{titleType}, #{title}, #{taxNo}, #{email}, #{status}, #{remark}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "invoiceId")
    int insert(WashInvoice invoice);

    @Select("select invoice_id, member_id, order_no, amount, title_type, title, tax_no, email, status, invoice_no, reject_reason, remark, create_time, update_time " +
            "from wash_invoice where member_id = #{memberId} and del_flag = '0' order by invoice_id desc")
    List<WashInvoice> selectByMember(@Param("memberId") Long memberId);

    @Select("select invoice_id, member_id, order_no, amount, title_type, title, tax_no, email, status, invoice_no, reject_reason, remark, create_time, update_time " +
            "from wash_invoice where invoice_id = #{id} and member_id = #{memberId} and del_flag = '0' limit 1")
    WashInvoice selectByMemberAndId(@Param("memberId") Long memberId, @Param("id") Long id);

    /** 同一订单是否已存在有效申请（被驳回的可重新申请） */
    @Select("select invoice_id, member_id, order_no, amount, title_type, title, tax_no, email, status, invoice_no, reject_reason, remark, create_time, update_time " +
            "from wash_invoice where member_id = #{memberId} and order_no = #{orderNo} and status != 'REJECTED' and del_flag = '0' limit 1")
    WashInvoice selectApplyingByOrder(@Param("memberId") Long memberId, @Param("orderNo") String orderNo);

    @Select("select invoice_id, member_id, order_no, amount, title_type, title, tax_no, email, status, invoice_no, reject_reason, remark, create_time, update_time " +
            "from wash_invoice where invoice_id = #{id} and del_flag = '0' limit 1")
    WashInvoice selectById(@Param("id") Long id);

    @Update("<script>" +
            "update wash_invoice set update_time = sysdate() " +
            "<if test='status != null'> , status = #{status} </if> " +
            "<if test='invoiceNo != null'> , invoice_no = #{invoiceNo} </if> " +
            "<if test='rejectReason != null'> , reject_reason = #{rejectReason} </if> " +
            "where invoice_id = #{invoiceId} and del_flag = '0'" +
            "</script>")
    int update(WashInvoice invoice);

    @Select({"<script>",
            "select invoice_id, member_id, order_no, amount, title_type, title, tax_no, email, status, invoice_no, reject_reason, remark, create_time, update_time ",
            "from wash_invoice where del_flag = '0' ",
            "<if test='status != null and status != \"\"'> and status = #{status} </if>",
            "<if test='memberId != null'> and member_id = #{memberId} </if>",
            "<if test='orderNo != null and orderNo != \"\"'> and order_no like concat('%', #{orderNo}, '%') </if>",
            "order by invoice_id desc limit #{offset}, #{limit}",
            "</script>"})
    List<WashInvoice> selectAdminPage(Map<String, Object> params);

    @Select({"<script>",
            "select count(*) from wash_invoice where del_flag = '0' ",
            "<if test='status != null and status != \"\"'> and status = #{status} </if>",
            "<if test='memberId != null'> and member_id = #{memberId} </if>",
            "<if test='orderNo != null and orderNo != \"\"'> and order_no like concat('%', #{orderNo}, '%') </if>",
            "</script>"})
    long countAdmin(Map<String, Object> params);
}
