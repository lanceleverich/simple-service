package org.kie.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class ServiceHandler implements WorkItemHandler {
	private KieSession ksession;
	
	public ServiceHandler() {
		
	}
	
	public ServiceHandler(KieSession ksession) {
		this.ksession = ksession;
	}
	
	private Optional<SLAInfo> createSLAInfo(Map<String,Object> params, Long processInstanceId, Long nodeInstanceId) {
		String slaMode = (String)params.getOrDefault("SLAMode", TimerMode.INDIVIDUAL.name());
		String violationAction = (String)params.getOrDefault("ViolationAction", ViolationAction.SKIP.name());
		String retrySignalName = (String)params.getOrDefault("RetrySignalName","SLA_NoSignal");
		Integer maxRetries = Integer.valueOf((String)params.getOrDefault("MaxAutoRetry", "0"));
		String slaGroupId = (String)params.getOrDefault("SLAGroupId", "");
		SLAInfo info = null;
		try {
			info = new SLAInfo(processInstanceId, nodeInstanceId, violationAction, slaMode, slaGroupId, maxRetries, retrySignalName);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return (info != null) ? Optional.of(info) : Optional.empty();
	}

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		System.out.println("Here!");
		System.out.println("Start of parameters!");
		workItem.getParameters().keySet().forEach(key -> {System.out.println(key);});
		System.out.println("End of parameters");
		String slaMode = (String)workItem.getParameter("SLAMode");
		String violationAction = (String)workItem.getParameter("ViolationAction");
		Integer maxRetries = Integer.valueOf((String)workItem.getParameter("MaxAutoRetry"));
		final long workItemId = workItem.getId();
		final String deploymentId = ((WorkItemImpl)workItem).getDeploymentId();
		final long processInstanceId = workItem.getProcessInstanceId();
		long niid = ((WorkItemImpl)workItem).getNodeInstanceId();
		Map<String,Object> slaMap = (Map<String, Object>)
				((WorkflowProcessInstance)ksession.getProcessInstance(processInstanceId)).getVariable("slaMap");
		SLAInfo info = createSLAInfo(workItem.getParameters(),processInstanceId,niid).orElse(null);
		if (info != null) {
			System.out.println("adding to map = "+info.getKey());
			slaMap.put(info.getKey(), info);
		} else {
			System.out.println("didn't add to map");
		}
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(25000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Map<String,Object> output = new HashMap();
				output.put("name", "Gavin");
				RuntimeManager rtm = RuntimeManagerRegistry.get().getManager(deploymentId == null ? "" : deploymentId);
				if (rtm != null) {
					RuntimeEngine engine = rtm.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId));
					engine.getKieSession().getWorkItemManager().completeWorkItem(workItemId, output);
					rtm.disposeRuntimeEngine(engine);
				} else {
					ksession.getWorkItemManager().completeWorkItem(workItemId, output);
				}
			}
		});
		thread.start();
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// TODO Auto-generated method stub

	}

	public String getName(Object obj) {
		if (obj != null) {
			System.out.println(obj.getClass().getName());
			System.out.println(obj.toString());
		}
		System.out.println("Getting a name!");
		try {
			Thread.sleep(25000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Gavin";
	}
}
