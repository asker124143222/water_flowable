package com.water.flowable.controller;

import com.water.flowable.entity.HolidayRequest;
import com.water.flowable.response.Result;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: xu.dm
 * @Date: 2020/9/27 15:35
 * @Version: 1.0
 * @Description: TODO
 **/
@RestController
@RequestMapping("/holiday")
public class HolidayController {
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private ProcessEngine processEngine;

    @PostMapping
    public Result<Object> startFlow(@RequestBody HolidayRequest holidayRequest){
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", holidayRequest.getUserName());
        variables.put("nrOfHolidays", holidayRequest.getNrOfHolidays());
        variables.put("description", holidayRequest.getDescription());
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("holidayRequest", variables);
        return Result.ok("processInstance id:"+processInstance.getId()+" processInstance name:"+processInstance.getName()+" -> "+processInstance.toString());
    }

    @GetMapping("/getTasks")
    public Result<Object> getTasks(){
        List<Task> tasks = taskService.createTaskQuery()
//                .taskCandidateGroup("managers")
                .list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName() +" task id:"+tasks.get(i).getId());
        }
        return Result.ok(tasks.toString());
    }

    @GetMapping("/getTasks/{userId}")
    public Result<Object> getTasks(@PathVariable String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            System.out.println(task.getId()+" "+task.getAssignee() +" "+task.getName()+" "+task.getDescription());
        }
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().list();
        for (HistoricTaskInstance taskInstance : list) {
            System.out.println(taskInstance);
            System.out.println(taskInstance.getDescription()+" "+taskInstance.getAssignee()+" "+taskInstance.getOwner());
        }
        return Result.ok(tasks.toString());
    }

    @PostMapping("/complete/{taskId}")
    public Result<Object> complete(@PathVariable String taskId){
        Map<String, Object> processVariables = taskService.getVariables(taskId);

        Map<String,Object> variables = new HashMap<String, Object>();
        variables.put("approved", true);
        taskService.complete(taskId, variables);
        return Result.ok(processVariables);
    }

    @PostMapping("/reject/{taskId}")
    public Result<Object> reject(@PathVariable String taskId){
        Map<String, Object> processVariables = taskService.getVariables(taskId);

        Map<String,Object> variables = new HashMap<String, Object>();
        variables.put("approved", false);
        taskService.complete(taskId, variables);
        return Result.ok(processVariables);
    }

    @GetMapping("/processInstance")
    public Result<Object> getProcessInstance(){
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().listPage(0, 10);
        for (ProcessInstance processInstance : processInstances) {
            System.out.println(processInstance.getId());
        }
        return Result.ok(processInstances.toString());
    }

    @GetMapping("/history/{processInstanceId}")
    public Result<Object> getHistory(@PathVariable String processInstanceId){
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }

        return Result.ok(activities.toString());
    }

    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
//        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
//        String processId =task.getProcessInstanceId();

        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

        //流程走完的不显示图
        if (pi == null) {
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration config = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = config.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds,
                flows, config.getActivityFontName(),
                config.getLabelFontName(), config.getAnnotationFontName(),
                config.getClassLoader(), 1.0, true);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int length = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

}
