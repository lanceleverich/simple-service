package org.kie.simple;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.runtime.process.WorkItem;

public class SLAInfoFactory {

	private static SLAInfoFactory factory;
	
	public SLAInfo createFrom(WorkItem workItem) {
		TimerMode slaMode = getParamAsEnum(TimerMode.class, workItem.getParameter("SLAMode"));
		ViolationAction violationAction = getParamAsEnum(ViolationAction.class,
				workItem.getParameter("ViolationAction"));
		Integer maxRetries = getParamAsInt(workItem.getParameter("MaxAutoRetry"));
		String retrySignalName = getParamAsString(workItem.getParameter("RetrySignalName"));
		String slaGroupId = getParamAsString(workItem.getParameter("SLAGroupId"));
		
		final long workItemId = workItem.getId();
		final long processInstanceId = workItem.getProcessInstanceId();
		long niid = ((WorkItemImpl) workItem).getNodeInstanceId();

		
		return new SLAInfo(processInstanceId, niid, workItemId, violationAction, slaMode, slaGroupId,
				maxRetries, retrySignalName);
	}
	
	protected String getParamAsString(Object param) {
		if (param == null) {
			return null;
		}
		if (param instanceof String && !((String) param).isEmpty()) {
			return (String) param;
		}

		return null;
	}

	protected Integer getParamAsInt(Object param) {
		if (param == null) {
			return null;
		}
		if (param instanceof String && !((String) param).isEmpty()) {
			return Integer.parseInt((String) param);
		}
		if (param instanceof Number) {
			return ((Number) param).intValue();
		}
		return null;
	}

	protected <E extends Enum<E>> E getParamAsEnum(Class<E> type, Object param) {
		if (param == null) {
			return null;
		}
		if (param instanceof String && !((String) param).isEmpty()) {
			return Enum.valueOf(type, (String) param);
		}
		return null;
	}

	public static SLAInfoFactory instance() {
		if(factory == null) {
			factory = new SLAInfoFactory();
		}
		return factory;
	}
}
