package com.xuecheng.order.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    @Autowired
    private XcTaskHisRepository xcTaskHisRepository;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private XcTaskRepository xcTaskRepository;
    public List<XcTask> findTaskList(Date date,int n){
        PageHelper.startPage(0,n);
        List<XcTask> byUpdateTimeBefore = xcTaskRepository.findByUpdateTimeBefore(date);
        PageInfo<XcTask> pageInfo=new PageInfo<>(byUpdateTimeBefore);
        return pageInfo.getList();
    }
    @Transactional
    public void publish(XcTask xcTask,String ex,String routingKey){
        Optional<XcTask> byId = xcTaskRepository.findById(xcTask.getId());
        if (byId.isPresent()){
            XcTask xcTask1 = byId.get();
            amqpTemplate.convertAndSend(ex,routingKey,xcTask);
            xcTask1.setUpdateTime(new Date());
            xcTaskRepository.save(xcTask1);
        }
    }
    @Transactional
    public int getTask(String id,int version){
        int i = xcTaskRepository.updateTaskVersion(id, version);
        return i;
    }
    @Transactional
    public void finishTask(String taskId){
        Optional<XcTask> byId = xcTaskRepository.findById(taskId);
        if (byId.isPresent()){
            XcTask xcTask = byId.get();
            xcTask.setDeleteTime(new Date());
            XcTaskHis xcTaskHis=new XcTaskHis();
            BeanUtils.copyProperties(xcTask,xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.deleteById(taskId);
        }
    }
}
