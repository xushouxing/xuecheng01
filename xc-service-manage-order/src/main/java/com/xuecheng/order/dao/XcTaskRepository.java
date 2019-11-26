package com.xuecheng.order.dao;

import com.xuecheng.framework.domain.task.XcTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface XcTaskRepository extends JpaRepository<XcTask,String> {
    List<XcTask> findByUpdateTimeBefore(Date date);
    @Modifying
    @Query("update XcTask t set t.version= :version+1 where t.id= :taskId and t.version= :version")
    public int updateTaskVersion(@Param(value = "taskId") String Taskid,@Param(value = "version") int version);
}
