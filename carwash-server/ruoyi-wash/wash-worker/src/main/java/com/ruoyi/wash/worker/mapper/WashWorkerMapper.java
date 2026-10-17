package com.ruoyi.wash.worker.mapper;

import com.ruoyi.wash.worker.domain.WashWorker;
import com.ruoyi.wash.worker.dto.AdminWorkerVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 师傅表读写。禁止其他模块直接注入本 Mapper，跨域一律走 WashWorkerService / Manager。
 */
public interface WashWorkerMapper {

    /**
     * 按工号查师傅（登录用）。只返回未删除且未禁用之外的记录，
     * 禁用与否由 Service 判断，便于区分「工号不存在 D4001」与「已禁用 D4003」。
     */
    @Select("select worker_id, worker_no, password, name, phone, site_id, community_id, status, work_status, del_flag " +
            "from wash_worker where worker_no = #{workerNo} and del_flag = '0'")
    WashWorker selectByWorkerNo(@Param("workerNo") String workerNo);

    /** 按 workerId 查师傅：已登录师傅取所属站点（任务池按站点过滤）用。 */
    @Select("select worker_id, worker_no, password, name, phone, site_id, community_id, status, work_status, del_flag " +
            "from wash_worker where worker_id = #{workerId} and del_flag = '0'")
    WashWorker selectByWorkerId(@Param("workerId") Long workerId);

    // ---------------- 后台管理 ----------------

    /** 后台列表：含已禁用项（运营要能找回），不含已删除项 */
    @Select("<script>" +
            "select worker_id, worker_no, name, phone, site_id, community_id, status, work_status, remark, " +
            "       create_time as createTime " +
            "from wash_worker where del_flag = '0' " +
            "<if test='keyword != null and keyword != &quot;&quot;'> " +
            "  and (worker_no like concat('%', #{keyword}, '%') " +
            "    or name like concat('%', #{keyword}, '%') " +
            "    or phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='siteId != null'> and site_id = #{siteId} </if>" +
            "<if test='status != null and status != &quot;&quot;'> and status = #{status} </if>" +
            "order by create_time desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<AdminWorkerVO> selectAdminList(@Param("keyword") String keyword,
                                        @Param("siteId") Long siteId,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    /** 后台详情：与列表同一套 VO（不含 password 列，密码不进任何查询） */
    @Select("select worker_id, worker_no, name, phone, site_id, community_id, status, work_status, remark, " +
            "       create_time as createTime " +
            "from wash_worker where worker_id = #{workerId} and del_flag = '0'")
    AdminWorkerVO selectAdminById(@Param("workerId") Long workerId);

    @Select("<script>" +
            "select count(*) from wash_worker where del_flag = '0' " +
            "<if test='keyword != null and keyword != &quot;&quot;'> " +
            "  and (worker_no like concat('%', #{keyword}, '%') " +
            "    or name like concat('%', #{keyword}, '%') " +
            "    or phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='siteId != null'> and site_id = #{siteId} </if>" +
            "<if test='status != null and status != &quot;&quot;'> and status = #{status} </if>" +
            "</script>")
    long countAdmin(@Param("keyword") String keyword,
                    @Param("siteId") Long siteId,
                    @Param("status") String status);

    /** 工号唯一性：同一个工号被已删除的记录占用也不允许，避免删了再加的边界纠纷 */
    @Select("select count(*) from wash_worker where worker_no = #{workerNo}")
    long countByWorkerNo(@Param("workerNo") String workerNo);

    @Insert("insert into wash_worker (worker_no, password, name, phone, site_id, community_id, status, work_status, remark, del_flag, create_time) " +
            "values (#{workerNo}, #{password}, #{name}, #{phone}, #{siteId}, #{communityId}, #{status}, " +
            "#{workStatus}, #{remark}, '0', sysdate())")
    int insertWorker(AdminWorkerVO worker);

    @Update("<script>" +
            "update wash_worker " +
            "<set>" +
            "  <if test='name != null'> name = #{name}, </if>" +
            "  <if test='phone != null'> phone = #{phone}, </if>" +
            "  <if test='siteId != null'> site_id = #{siteId}, </if>" +
            "  <if test='communityId != null'> community_id = #{communityId}, </if>" +
            "  <if test='status != null'> status = #{status}, </if>" +
            "  <if test='workStatus != null'> work_status = #{workStatus}, </if>" +
            "  <if test='remark != null'> remark = #{remark}, </if>" +
            "</set>" +
            "where worker_id = #{workerId} and del_flag = '0'" +
            "</script>")
    int updateWorker(AdminWorkerVO worker);

    /**
     * 单独改密码。为什么不给 updateWorker 顺手改：修改资料时误触密码会把师傅锁在门外，
     * 两个动作必须分成两个接口、两个权限。
     */
    @Update("update wash_worker set password = #{password} where worker_id = #{workerId} and del_flag = '0'")
    int updatePassword(@Param("workerId") Long workerId, @Param("password") String password);

    /** 逻辑删除：历史任务的作业人仍指向本记录，禁止物理删行 */
    @Update("update wash_worker set del_flag = '2' where worker_id = #{workerId} and del_flag = '0'")
    int logicDelete(@Param("workerId") Long workerId);

    /** 师傅打卡上下班（本人操作，不涉及资料修改） */
    @Update("update wash_worker set work_status = #{workStatus}, update_time = sysdate() " +
            "where worker_id = #{workerId} and del_flag = '0' and status = 'NORMAL'")
    int updateWorkStatus(@Param("workerId") Long workerId, @Param("workStatus") String workStatus);

    /** 派单候选：本站点、账号正常、已上班的师傅（资质是否通过由 Service 再过滤） */
    @Select("select worker_id, worker_no, name, phone, site_id, community_id, status, work_status, del_flag " +
            "from wash_worker where del_flag = '0' and status = 'NORMAL' " +
            "and work_status in ('ONLINE', 'BUSY') and site_id = #{siteId}")
    List<WashWorker> selectDispatchCandidates(@Param("siteId") Long siteId);

    /** 该师傅进行中的订单数（派单负载均衡用） */
    @Select("select count(*) from wash_order where del_flag = '0' and worker_id = #{workerId} " +
            "and status in ('KEY_IN', 'PICKING', 'TO_STATION', 'WASHING', 'QC', 'WAIT_RETURN', 'RETURNING', 'RETURNED')")
    long countActiveOrders(@Param("workerId") Long workerId);
}
