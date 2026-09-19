package com.ruoyi.wash.worker.mapper;

import com.ruoyi.wash.worker.domain.WashWorker;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 师傅表读写。禁止其他模块直接注入本 Mapper，跨域一律走 WashWorkerService / Manager。
 */
public interface WashWorkerMapper {

    /**
     * 按工号查师傅（登录用）。只返回未删除且未禁用之外的记录，
     * 禁用与否由 Service 判断，便于区分「工号不存在 D4001」与「已禁用 D4003」。
     */
    @Select("select worker_id, worker_no, password, name, phone, site_id, status, del_flag " +
            "from wash_worker where worker_no = #{workerNo} and del_flag = '0'")
    WashWorker selectByWorkerNo(@Param("workerNo") String workerNo);
}
