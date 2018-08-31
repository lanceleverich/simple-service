package org.kie.simple;

import java.util.Map;
import java.util.Optional;

import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.SLAViolatedEvent;

public class SLAViolationHandler extends DefaultProcessEventListener {

	public SLAViolationHandler() {
		System.out.println("SLAViolationHandler event listener loaded");
	}

	@Override
	public void beforeSLAViolated(SLAViolatedEvent event) {
		System.out.println("About to trigger SLA violation event for " + event.getProcessInstance().getProcessName());
	}

	private Optional<SLAInfo> getSLAInfo(SLAViolatedEvent event) {

		if (event != null && event.getProcessInstance() != null && event.getNodeInstance() != null) {
			Object obj = ((WorkflowProcessInstance) event.getProcessInstance()).getVariable("slaMap");
			Long procInstId = event.getProcessInstance().getId();
			Long nodeInstId = event.getNodeInstance().getId();
			if (obj != null && obj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> slaMap = (Map<String, Object>) obj;
				String key = SLAInfo.generateKey(procInstId, nodeInstId);
				Object innerObj = slaMap.get(key);

				if (innerObj != null && innerObj instanceof SLAInfo) {
					return Optional.of((SLAInfo) innerObj);
				}
			}
		}
		return Optional.empty();
	}

	private boolean retrySignal(SLAViolatedEvent event, String signalName, Integer maxRetries) {
		WorkflowProcessInstance spi = ((WorkflowProcessInstance) event.getProcessInstance());
		Integer counter = (Integer) spi.getVariable(signalName + "_counter");
		counter = (counter == null) ? 0 : counter + 1;
		spi.setVariable(signalName + "_counter", counter);
		spi.setVariable(signalName + "_end", counter >= maxRetries);
		return counter < maxRetries;
	}

	@Override
	public void afterSLAViolated(SLAViolatedEvent event) {

		System.out.println(event.getProcessInstance().getProcessName() + " has violated an SLA");
		if (event.getNodeInstance() == null) {
			System.out.println("Unable to determine the violating node");
		}

		System.out.println("***** SLA Violated Event ***** NODE: " + event.getNodeInstance().getNodeName());
		WorkItemNodeInstance nii = (WorkItemNodeInstance) event.getNodeInstance();
		Optional<SLAInfo> info = getSLAInfo(event);
		if (!info.isPresent()) {
			System.out.println("didn't get the SLAInfo");
			return;
		}

		SLAInfo slaInfo = info.get();
		System.out.println("***** SLA Violated Action ***** NODE: " + event.getNodeInstance().getNodeName() + " ACTION "
				+ slaInfo.getAction());
		
		// cancel de work item
		switch (slaInfo.getAction()) {
		case AUTO:
			if (retrySignal(event, slaInfo.getRetrySignalName(), slaInfo.getMaxRetries())) {
				nii.cancel();
				event.getKieRuntime().signalEvent(slaInfo.getRetrySignalName(), event,
						event.getProcessInstance().getId());
//				event.getKieRuntime().getWorkItemManager().abortWorkItem(nii.getWorkItemId());
				
			} else {
				System.out.println(event.getNodeInstance().getNodeName() + " MAXIMUM RETRIES REACHED, WAITING");
			}
			break;
		case ABORTPROC:
			// cancel the task and the process. It should not follow the current path
			nii.cancel();
			event.getKieRuntime().abortProcessInstance(slaInfo.getProcessId());
			break;
		case SKIP: // NONE
			// abort the current wih
			nii.cancel();
			event.getKieRuntime().getWorkItemManager().abortWorkItem(nii.getWorkItemId());
			break;
		case WAIT:
			// we do nothing and wait
			nii.cancel();
			break;
		}

		System.out.println("***************");
	}

}
