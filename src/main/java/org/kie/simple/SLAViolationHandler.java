package org.kie.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.event.process.SLAViolatedEvent;

public class SLAViolationHandler implements ProcessEventListener {
	
	public SLAViolationHandler() {
		System.out.println("SLAViolationHandler event listener loaded");
	}

	@Override
	public void beforeProcessStarted(ProcessStartedEvent event) {
		// TODO Auto-generated method stub
		((WorkflowProcessInstance)event.getProcessInstance()).setVariable("slaMap", new HashMap<>());
	}

	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeProcessCompleted(ProcessCompletedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeVariableChanged(ProcessVariableChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void beforeSLAViolated(SLAViolatedEvent event) {
		System.out.println("About to trigger SLA violation event for "+event.getProcessInstance().getProcessName());
	}
	
	private Optional<SLAInfo> getSLAInfo(SLAViolatedEvent event) {
		if (event != null && event.getProcessInstance() != null && event.getNodeInstance() != null) {
			System.out.println("we have valid event");
			Object obj = ((WorkflowProcessInstance)event.getProcessInstance()).getVariable("slaMap");
			Long procInstId = event.getProcessInstance().getId();
			Long nodeInstId = event.getNodeInstance().getId();
			if (obj != null && obj instanceof Map) {
				System.out.println("we have the slaMap");
				Map<String,Object> slaMap = (Map<String,Object>)obj;
				String key = SLAInfo.generateKey(procInstId, nodeInstId);
				Object innerObj = slaMap.get(key);
				
				if (innerObj != null && innerObj instanceof SLAInfo) {
					System.out.println("we have the object");
					return Optional.of((SLAInfo)innerObj);
				} else {
					System.out.println("can't find node sla = "+key);
				}
			}
		}
		System.out.println("didn't get the SLAInfo");
		return Optional.empty();
	}
	
	@Override
	public void afterSLAViolated(SLAViolatedEvent event) {
		System.out.println("***** SLA Violated Event *****");
		System.out.println(event.getProcessInstance().getProcessName()+" has violated an SLA");
		if (event.getNodeInstance() != null) {
			System.out.println("Violating node is "+event.getNodeInstance().getNodeName());
			NodeInstanceImpl nii = (NodeInstanceImpl)event.getNodeInstance();
			System.out.println("Node instance id = "+nii.getId());
			getSLAInfo(event).ifPresent(info -> {
				System.out.println(info.toString());
			});
			
			nii.cancel();
			event.getKieRuntime().signalEvent("sla_violation", event, event.getProcessInstance().getId());
		} else {
			System.out.println("Unable to determine the violating node");
		}
		System.out.println("***************");
	}

}
