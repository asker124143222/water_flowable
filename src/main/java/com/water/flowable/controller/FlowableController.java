package com.water.flowable.controller;

import com.water.flowable.entity.HolidayRequest;
import com.water.flowable.response.Result;
import com.water.flowable.util.FlowableUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @Author: xu.dm
 * @Date: 2020/10/13 9:45
 * @Version: 1.0
 * @Description: TODO
 **/
@RequestMapping("/flow")
@RestController
public class FlowableController {

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

    // 启动流程
    @PostMapping("/bus")
    public Result<Object> startBus(@RequestBody HolidayRequest holidayRequest) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", holidayRequest.getUserName());
        variables.put("nrOfHolidays", holidayRequest.getNrOfHolidays());
        variables.put("description", holidayRequest.getDescription());
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("bus01", variables);
        return Result.ok("processInstance id:" + processInstance.getId() + " processInstance name:" + processInstance.getName() + " -> " + processInstance.toString());
    }

    @PostMapping("/holiday")
    public Result<Object> startHoliday(@RequestBody HolidayRequest holidayRequest) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", holidayRequest.getUserName());
        variables.put("nrOfHolidays", holidayRequest.getNrOfHolidays());
        variables.put("description", holidayRequest.getDescription());
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("holidayRequest", variables);
        return Result.ok("processInstance id:" + processInstance.getId() + " processInstance name:" + processInstance.getName() + " -> " + processInstance.toString());
    }

    // 启动流程 并直接完成第一个节点
    @PostMapping("/plane")
    public Result<Object> startPlane(@RequestBody HolidayRequest holidayRequest) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", holidayRequest.getUserName());
        variables.put("nrOfHolidays", holidayRequest.getNrOfHolidays());
        variables.put("description", holidayRequest.getDescription());
        variables.put("fee", holidayRequest.getFee());
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("plane01",variables);

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskAssignee(holidayRequest.getUserName())
                .singleResult();
        taskService.complete(task.getId());
        return Result.ok("processInstance id:" + processInstance.getId() + " processInstance name:" + processInstance.getName() + " -> " + processInstance.toString());
    }


    // 获取目前所有可执行的任务
    @GetMapping("/getTasks")
    public Result<Object> getTasks() {
        List<Task> tasks = taskService.createTaskQuery()
//                .taskCandidateGroup("managers")
                .list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + ") " + tasks.get(i).getName()
                    + " task id:" + tasks.get(i).getId()
                    + " assignee:" + tasks.get(i).getAssignee()
                    + " owner:" + tasks.get(i).getOwner());
        }
        return Result.ok(tasks.toString());
    }

    // 根据候选人或者候选组获取所有可执行任务
    @GetMapping("/getTasksByGroup/{candidateGroup}")
    public Result<Object> getTasksByCandidateGroup(@PathVariable String candidateGroup) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + ") " + tasks.get(i).getName() + " task id:" + tasks.get(i).getId() + " assignee:" + tasks.get(i).getAssignee());
        }
        return Result.ok(tasks.toString());
    }



    @GetMapping("/getTasks/{userId}")
    public Result<Object> getTasks(@PathVariable String userId) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            System.out.println("task id:" + task.getId() + "  task name:" + task.getName());
            Map<String, Object> processVariables = taskService.getVariables(task.getId());
            System.out.println(processVariables);

        }
        return Result.ok(tasks.toString());
    }

    // 申领任务
    @PostMapping("/claim")
    public Result<Object> claimTask(String taskId,String userId) {
        taskService.claim(taskId,userId);
        return Result.ok();
    }

    // 转交任务
    @PostMapping("/transfer")
    public Result<Object> transferTask(String taskId,String userId){
        taskService.setAssignee(taskId,userId);
        return Result.ok();
    }

    // 代理任务
    @PostMapping("/delegate")
    public Result<Object> delegateTask(String taskId,String userId,String delegateUserId){
        // 正常情况下owner应该是null值
        taskService.setOwner(taskId,userId);
        taskService.setAssignee(taskId,delegateUserId);
        return Result.ok();
    }

    @PostMapping("/complete/{taskId}")
    public Result<Object> complete(@PathVariable String taskId,String userId) {
        Map<String, Object> processVariables = taskService.getVariables(taskId);

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("approved", true);
        if(StringUtils.isBlank(userId)) userId = UUID.randomUUID().toString();
        taskService.setVariableLocal(taskId,"userId",userId);
        taskService.complete(taskId, variables);
        return Result.ok(processVariables);
    }

    @PostMapping("/reject/{taskId}")
    public Result<Object> reject(@PathVariable String taskId) {
        Map<String, Object> processVariables = taskService.getVariables(taskId);

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("approved", false);
        taskService.complete(taskId, variables);
        return Result.ok(processVariables);
    }

    // 删除启动的流程
    @DeleteMapping("/delete/{taskId}")
    public Result<Object> deleteProcess(@PathVariable String taskId){
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        runtimeService.deleteProcessInstance(task.getProcessInstanceId(),"删除流程实例");

        return Result.ok();
    }

    @GetMapping("/history/{assignee}")
    public Result<Object> getHistory(@PathVariable String assignee) {
        List<HistoricTaskInstance> taskInstanceList = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee).list();
        for (HistoricTaskInstance taskInstance : taskInstanceList) {
            System.out.println("task id:" + taskInstance.getId() + " task name:" + taskInstance.getName() + " " + taskInstance.getDescription());


            List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(taskInstance.getProcessInstanceId())
                   .list();
            for (HistoricVariableInstance instance : variableInstances) {
                // 获取流程变量
                System.out.println("ProcessInstanceId:"+instance.getProcessInstanceId()+ " VariableName: "+instance.getVariableName()+" value:"+instance.getValue());
            }

            // 获取本地变量
            List<HistoricDetail> details = historyService.createHistoricDetailQuery()
                    .variableUpdates()
                    .taskId(taskInstance.getId()).orderByVariableName().asc()
                    .list();
            System.out.println(details);

        }


        return Result.ok(taskInstanceList.toString());
    }

    @GetMapping("/instance")
    public Result<Object> queryHistory(String instanceId){
        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instanceId)
                .list();
        for (HistoricProcessInstance instance : processInstances) {
            System.out.println("name:"+instance.getName()+" startUserId:"+instance.getStartUserId());
            Map<String, Object> processVariables = instance.getProcessVariables();
            System.out.println(processVariables);
        }
        return Result.ok(processInstances.toString());
    }

    // 查看流程定义图
    @RequestMapping(value = "diagram")
    public void getDiagram(HttpServletResponse httpServletResponse, String processDefinedId) throws Exception {
        // processDefinedId = act_re_procdef表的id
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        for (ProcessDefinition processDefinition : processDefinitions) {
            System.out.println("processDefinition id:"+processDefinition.getId()+" key:"+processDefinition.getKey()
                    +" name:"+processDefinition.getName()+" resource name:"+processDefinition.getResourceName());
        }
        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinedId);
        ProcessEngineConfiguration config = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = config.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png",
                config.getActivityFontName(),config.getLabelFontName(),config.getAnnotationFontName(),
                config.getClassLoader(),true);
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

    // 查看当前流程运行图
    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String taskId) throws Exception {
        Task taskChecked = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processId = taskChecked.getProcessInstanceId();

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

    /**
     * 流程收回/驳回
     * @param taskId 当前任务ID
     * @param comment 审核意见
     * @return
     */
    @GetMapping(value = "/flowTackBack/{taskId}")
    public Result<Object> flowTackBack(@PathVariable("taskId") String taskId, @RequestParam(value = "comment", defaultValue = "") String comment) {
        if (taskService.createTaskQuery().taskId(taskId).singleResult().isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 类型为用户节点
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    // 获取节点信息
                    source = flowElement;
                }
            }
        }


        // 目的获取所有跳转到的节点 targetIds
        // 获取当前节点的所有父级用户任务节点
        // 深度优先算法思想：延边迭代深入
        List<UserTask> parentUserTaskList = FlowableUtils.iteratorFindParentUserTasks(source, null, null);
        if (parentUserTaskList == null || parentUserTaskList.size() == 0) {
            throw new RuntimeException ("当前节点为初始任务节点，不能驳回");
        }
        // 获取活动 ID 即节点 Key
        List<String> parentUserTaskKeyList = new ArrayList<>();
        parentUserTaskList.forEach(item -> parentUserTaskKeyList.add(item.getId()));
        // 获取全部历史节点活动实例，即已经走过的节点历史，数据采用开始时间升序
        List<HistoricTaskInstance> historicTaskInstanceList = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).orderByHistoricTaskInstanceStartTime().asc().list();
        // 数据清洗，将回滚导致的脏数据清洗掉
        List<String> lastHistoricTaskInstanceList = FlowableUtils.historicTaskInstanceClean(allElements, historicTaskInstanceList);
        // 此时历史任务实例为倒序，获取最后走的节点
        List<String> targetIds = new ArrayList<>();
        // 循环结束标识，遇到当前目标节点的次数
        int number = 0;
        StringBuilder parentHistoricTaskKey = new StringBuilder();
        for (String historicTaskInstanceKey : lastHistoricTaskInstanceList) {
            // 当会签时候会出现特殊的，连续都是同一个节点历史数据的情况，这种时候跳过
            if (parentHistoricTaskKey.toString().equals(historicTaskInstanceKey)) {
                continue;
            }
            parentHistoricTaskKey = new StringBuilder(historicTaskInstanceKey);
            if (historicTaskInstanceKey.equals(task.getTaskDefinitionKey())) {
                number ++;
            }
            // 在数据清洗后，历史节点就是唯一一条从起始到当前节点的历史记录，理论上每个点只会出现一次
            // 在流程中如果出现循环，那么每次循环中间的点也只会出现一次，再出现就是下次循环
            // number == 1，第一次遇到当前节点
            // number == 2，第二次遇到，代表最后一次的循环范围
            if (number == 2) {
                break;
            }
            // 如果当前历史节点，属于父级的节点，说明最后一次经过了这个点，需要退回这个点
            if (parentUserTaskKeyList.contains(historicTaskInstanceKey)) {
                targetIds.add(historicTaskInstanceKey);
            }
        }



        // 目的获取所有需要被跳转的节点 currentIds
        // 取其中一个父级任务，因为后续要么存在公共网关，要么就是串行公共线路
        UserTask oneUserTask = parentUserTaskList.get(0);
        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需驳回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(oneUserTask, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));



        // 规定：并行网关之前节点必须需存在唯一用户任务节点，如果出现多个任务节点，则并行网关节点默认为结束节点，原因为不考虑多对多情况
        if (targetIds.size() > 1 && currentIds.size() > 1) {
            throw new RuntimeException("任务出现多对多情况，无法撤回");
        }

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        // 设置驳回信息
        currentTaskIds.forEach(item -> {
            taskService.addComment(item, task.getProcessInstanceId(), "taskStatus", "reject");
            taskService.addComment(item, task.getProcessInstanceId(), "taskMessage", "已驳回");
            taskService.addComment(item, task.getProcessInstanceId(), "taskComment", comment);
        });

        try {
            // 如果父级任务多于 1 个，说明当前节点不是并行节点，原因为不考虑多对多情况
            if (targetIds.size() > 1) {
                // 1 对 多任务跳转，currentIds 当前节点(1)，targetIds 跳转到的节点(多)
                runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId()).moveSingleActivityIdToActivityIds(currentIds.get(0), targetIds).changeState();
            }
            // 如果父级任务只有一个，因此当前任务可能为网关中的任务
            if (targetIds.size() == 1) {
                // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetIds.get(0) 跳转到的节点(1)
                runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId()).moveActivityIdsToSingleActivityId(currentIds, targetIds.get(0)).changeState();
            }
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException ("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new RuntimeException("无法取消或开始活动");
        }
        return Result.ok();
    }

    /**
     * 流程回退
     * @param taskId 当前任务ID
     * @param targetKey 要回退的任务 Key
     * @return
     */
    @GetMapping(value = "/flowReturn/{taskId}/{targetKey}")
    public Result<Object> flowReturn(@PathVariable("taskId") String taskId, @PathVariable("targetKey") String targetKey) {
        if (taskService.createTaskQuery().taskId(taskId).singleResult().isSuspended()) {
            throw new RuntimeException("任务处于挂起状态");
        }
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        // 获取跳转的节点元素
        FlowElement target = null;
        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 当前任务节点元素
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = flowElement;
                }
                // 跳转的节点元素
                if (flowElement.getId().equals(targetKey)) {
                    target = flowElement;
                }
            }
        }


        // 从当前节点向前扫描
        // 如果存在路线上不存在目标节点，说明目标节点是在网关上或非同一路线上，不可跳转
        // 否则目标节点相对于当前节点，属于串行
        Boolean isSequential = FlowableUtils.iteratorCheckSequentialReferTarget(source, targetKey, null, null);
        if (!isSequential) {
            throw new RuntimeException("当前节点相对于目标节点，不属于串行关系，无法回退");
        }



        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需退回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        // 设置回退信息
        for (String currentTaskId: currentTaskIds) {
            taskService.addComment(currentTaskId, task.getProcessInstanceId(), "taskStatus", "return");
            taskService.addComment(currentTaskId, task.getProcessInstanceId(), "taskMessage", "已退回");
            taskService.addComment(currentTaskId, task.getProcessInstanceId(), "taskComment", "流程回退到" + target.getName() + "节点");
        }

        try {
            // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetKey 跳转到的节点(1)
            runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId()).moveActivityIdsToSingleActivityId(currentIds, targetKey).changeState();
        } catch (FlowableObjectNotFoundException e) {
            throw new RuntimeException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new RuntimeException("无法取消或开始活动");
        }
        return Result.ok();
    }

    /**
     * 获取所有可回退的节点
     * @param taskId
     * @return
     */
    @GetMapping(value = "/findReturnUserTask/{taskId}")
    public Result<Object> findReturnUserTask(@PathVariable(value = "taskId") String taskId) {
        // 当前任务 task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息，暂不考虑子流程情况
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();
        // 获取当前任务节点元素
        UserTask source = null;
        if (flowElements != null) {
            for (FlowElement flowElement : flowElements) {
                // 类型为用户节点
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = (UserTask) flowElement;
                }
            }
        }
        // 获取节点的所有路线
        List<List<UserTask>> roads = FlowableUtils.findRoad(source, null, null, null);
        // 可回退的节点列表
        List<UserTask> userTaskList = new ArrayList<>();
        for (List<UserTask> road : roads) {
            if (userTaskList.size() == 0) {
                // 还没有可回退节点直接添加
                userTaskList = road;
            } else {
                // 如果已有回退节点，则比对取交集部分
                userTaskList.retainAll(road);
            }
        }
        return Result.ok(userTaskList);
    }
}
