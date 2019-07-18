package com.fred.learning;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Hello world!
 */
@Slf4j
public class App {

    public static void main(String[] args) throws ParseException {
        ProcessEngine processEngine = getProcessEngine();

        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);


        processTask(processEngine, processInstance);
    }


    /**
     * 创建流程引擎
     *
     * @return
     */
    private static ProcessEngine getProcessEngine() {
        //创建默认的基于内存数据库的流程引擎配置对象
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        //构造流程引擎
        ProcessEngine processEngine = cfg.buildProcessEngine();
        //获取流程引擎的name
        String engineName = processEngine.getName();
        //获取流程引擎的版本信息
        String version = ProcessEngine.VERSION;

        log.info("流程引擎名称 [{}], 版本 [{}]", engineName, version);
        return processEngine;
    }

    /**
     * 部署流程定义文件
     *
     * @param processEngine processEngine
     */
    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        //创建一个对流程编译库操作的Service
        RepositoryService repositoryService = processEngine.getRepositoryService();
        //获取一个builder
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        //这里写上流程编译路径
        deploymentBuilder.addClasspathResource("ask_for_leave.bpmn20.xml");
        //部署
        Deployment deployment = deploymentBuilder.deploy();
        //获取deployment的ID
        String deploymentId = deployment.getId();
        //根据deploymentId来获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        log.info("流程定义文件 [{}] , 流程ID [{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }


    /**
     * 启动运行流程
     *
     * @param processEngine
     * @param processDefinition
     */
    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        //启动流程要有一个运行时对象
        RuntimeService runtimeService = processEngine.getRuntimeService();

        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        log.info("启动流程 [{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    /**
     * 处理流程任务
     *
     * @param processEngine
     * @param processInstance
     */
    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        //判断流程不为空，且流程没有结束
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            //列出当前需要处理的任务
            List<Task> list = taskService.createTaskQuery().list();
            log.info("待处理任务数量 [{}]", list.size());
            for (Task task : list) {

                log.info("待处理任务 [{}]", task.getName());
                //获取用户的输入信息
                Map<String, Object> variables = getMap(processEngine, scanner, task);
                taskService.complete(task.getId(), variables);
                processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }
        }
        scanner.close();
    }

    /**
     * 获取用户的输入信息
     *
     * @param processEngine
     * @param scanner
     * @param task
     * @return
     * @throws ParseException
     */
    private static Map<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        //通过formService来获取form表单输入
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        //获取taskFormData的表单内容
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        //这个Map键值对来存对应表单用户输入的内容
        Map<String, Object> variables = new HashMap<>();
        //property为表单中的内容
        for (FormProperty property : formProperties) {
            //这里获取输入的信息
            String line = null;
            //如果是String类型的话
            if (StringFormType.class.isInstance(property.getType())) {
                //输入form表单的某一项内容
                log.info("请输入 [{}] ?", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);

            } else if (property.getType() instanceof DateFormType) {
                //如果是日期类型的话
                log.info("请输入 [{}] ? 格式为（yyyy-MM-dd）", property.getName());
                //输入form表单的某一项内容
                line = scanner.nextLine();
                //设置输入的日期格式
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(line);
                variables.put(property.getId(), date);
            } else {
                log.info("类型不支持 [{}]", property.getType());
            }
            log.info("您输入的内容是 [{}] ", line);
        }
        return variables;
    }
}
