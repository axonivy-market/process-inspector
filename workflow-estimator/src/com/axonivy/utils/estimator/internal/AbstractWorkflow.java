package com.axonivy.utils.estimator.internal;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.internal.model.CommonElement;
import com.axonivy.utils.estimator.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public abstract class AbstractWorkflow {

	private enum FindType {
		ALL_TASKS, TASKS_ON_PATH
	};

	private ProcessGraph processGraph;

	protected AbstractWorkflow() {
		this.processGraph = new ProcessGraph();
	}

	protected abstract Map<String, Duration> getDurationOverrides();
	protected abstract Map<String, String> getProcessFlowOverrides();

	protected List<CommonElement> findPath(CommonElement... from) throws Exception {
		List<CommonElement> path = findPath(Arrays.asList(from), null, FindType.ALL_TASKS,  emptyList());		
		return path;
	}
	
	protected List<CommonElement> findPath(String flowName, CommonElement... from) throws Exception {
		List<CommonElement> path = findPath(Arrays.asList(from), flowName, FindType.TASKS_ON_PATH, emptyList());
		return path;
	}
	
	protected String getCustomInfoByCode(TaskConfig task) {
		String wfEstimateCode = processGraph.getCodeLineByPrefix(task, "WfEstimate.setCustomInfo");		
		String result = Optional.ofNullable(wfEstimateCode)
				.filter(StringUtils::isNotEmpty)
				.map(it -> StringUtils.substringBetween(it, "(\"", "\")"))
				.orElse(null);;
		
		return result;
	}
	
	protected String getTaskId(TaskAndCaseModifier task, TaskConfig taskConfig) {
		String id = task.getPid().getRawPid();
		if (task instanceof TaskSwitchGateway) {
			return id + "-" + taskConfig.getTaskIdentifier().getRawIdentifier();
		} else {
			return id;
		}
	}
	
	protected Duration getDuration(TaskAndCaseModifier task, TaskConfig taskConfig, UseCase useCase) {
		String key = getTaskId(task, taskConfig);		
		return getDurationOverrides().getOrDefault(key, getDurationByTaskScript(taskConfig, useCase));	
	}
	
	protected boolean isSystemTask(TaskAndCaseModifier task) {		
		return task.getAllTaskConfigs().stream().anyMatch(it -> "SYSTEM".equals(it.getActivator().getName()));
	}
	
	protected List<String> getParentElementNames(TaskAndCaseModifier task){
		List<String> parentElementNames = emptyList();
		if(task.getParent() instanceof EmbeddedProcessElement) {
			parentElementNames = processGraph.getParentElementNamesEmbeddedProcessElement(task.getParent());
		}		
		return parentElementNames ;
	}
	
	private List<CommonElement> findPath(List<CommonElement> froms, String flowName, FindType findType, List<CommonElement> previousElements) throws Exception {
		List<CommonElement> result = new ArrayList<>();
		for(CommonElement from : froms) {
			var path = findPath(from, flowName, findType, emptyList());
			result.addAll(correctPath(path));
		}
				
		return result;
	}
	
	private List<CommonElement> correctPath(List<CommonElement> elements){
		
		//Remove CommonElement with TaskSwitchGateway
		List<CommonElement> removeTaskSwitchGateway = elements.stream()
				.filter(it -> it instanceof TaskParallelGroup || it.getElement() instanceof TaskSwitchGateway == false)
				.toList();
		
		return removeTaskSwitchGateway;
	}
	/**
	 * Using Recursion Algorithm To Find Tasks On Graph.
	 */
	private List<CommonElement> findPath(CommonElement from, String flowName, FindType findType, List<CommonElement> previousElements) throws Exception {
		// Prevent loop
		if (isContains(previousElements, from)) {
			return emptyList();
		}
		
		List<CommonElement> path = new ArrayList<>();				
		path.add(from);
		
		if (from.getElement() instanceof NodeElement) {
				
			if (from.getElement() instanceof EmbeddedProcessElement) {
				List<CommonElement> pathFromSubProcess = findPathOfSubProcess(from, flowName, findType);				
				path.addAll(pathFromSubProcess);				
			}

			if(isEndTaskSwitchGatewayAndWaiting(previousElements, from)) {				
				return path;
			}
			
			while(isStartTaskSwitchGateway(from)) {
				//It will have a CommentElement for TaskSwitchGateway in list. We will remove it.
				var taskParallelGroup = getTaskParallelGroup(from, flowName, findType, previousElements);
				path.add(taskParallelGroup);
				from = getEndTaskSwithGateWay(taskParallelGroup);				
				if( from == null) {
					return  path;
				}
			}
			
			List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
			if (from instanceof Alternative && outs.isEmpty()) {
				Ivy.log().error("Can not found the out going from a alternative {0}", from.getPid().getRawPid());
				throw new Exception("Not found path");
			}
						
			Map<SequenceFlow, List<CommonElement>> paths = new LinkedHashMap<>();
			for (SequenceFlow out : outs) {
				List<CommonElement> currentPath = ListUtils.union(previousElements, Arrays.asList(from));
				List<CommonElement> nextOfPath = findPath(new CommonElement(out.getTarget()), flowName, findType, currentPath);
				paths.put(out, nextOfPath);
			}

			path.addAll(getPath(paths));
		}
		
		return path.stream().toList();
	}
	
	private boolean isContains(List<CommonElement> previousElements, final CommonElement element) {
		return previousElements.stream().map(CommonElement::getElement).anyMatch(it -> it.equals(element.getElement()));
	}
	
	private TaskParallelGroup getTaskParallelGroup(CommonElement from, String flowName, FindType findType, List<CommonElement> previousElements) throws Exception {
		TaskParallelGroup result = new TaskParallelGroup(from.getElement());		
		List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
		
		Map<SequenceFlow, List<CommonElement>> paths = new LinkedHashMap<>();
		for (SequenceFlow out : outs) {
			List<CommonElement> currentPath = ListUtils.union(previousElements, Arrays.asList(from));
			List<CommonElement> nextOfPath = findPath(new CommonElement(out.getTarget()), flowName, findType, currentPath);
			paths.put(out, nextOfPath);
		}

		result.setInternalPaths(paths);
		
		return result;
	}
	
	/**
	 * Find path on sub process
	 */	
	private List<CommonElement> findPathOfSubProcess(CommonElement subProcessElement, String flowName, FindType findType) throws Exception {
		// find start element EmbeddedProcessElement subProcessElement.getEmbeddedProcess()
		EmbeddedProcessElement processElement = (EmbeddedProcessElement)subProcessElement.getElement();
		BaseElement start = processGraph.findOneStartElementOfProcess(processElement.getEmbeddedProcess());
		List<CommonElement> path = findPath(new CommonElement(start), flowName, findType, emptyList());		
		return path;
	}	
	
	private List<SequenceFlow> getSequenceFlows(NodeElement from, String flowName, FindType findType) {
		if (findType == FindType.ALL_TASKS) {
			return from.getOutgoing();
		}

		if (from instanceof TaskSwitchGateway && from != null) {
			return from.getOutgoing();
		}
		
		Optional<SequenceFlow> flow = Optional.empty();

		// Always is priority check flow from flowOverrides first.
		if (from instanceof Alternative) {
			String flowIdFromOrverride = getProcessFlowOverrides().get(from.getPid().getRawPid());
			flow = from.getOutgoing().stream().filter(out -> out.getPid().getRawPid().equals(flowIdFromOrverride))
					.findFirst();
		}

		// If it don't find out the flow from flowOverrides, it is base on the default
		// flow in process
		if (flow.isEmpty()) {
			flow = getSequenceFlow(from, flowName);
		}

		return flow.map(Arrays::asList).orElse(emptyList());		
	}
	
	private Optional<SequenceFlow> getSequenceFlow(NodeElement nodeElement, String flowName) {
		List<SequenceFlow> outs = nodeElement.getOutgoing();
		if(CollectionUtils.isEmpty(outs)) {
			return Optional.empty();
		}
			
		if(nodeElement instanceof Alternative) {
			// High priority for checking default path if flowName is null
			if(isEmpty(flowName)) {
				return outs.stream().filter(out -> isDefaultPath(out)).findFirst();
			} else { 
				//If flowName is not null, check flowName first
				Optional<SequenceFlow> flow = outs.stream().filter(out -> hasFlowName(out, flowName)).findFirst();
				// Then check default path
				if(!flow.isPresent()) {
					flow = outs.stream().filter(out -> isDefaultPath(out)).findFirst();
				}
				return flow;
			}
		}
		
		Optional<SequenceFlow> flow = outs.stream()
				.filter(out -> hasFlowNameOrEmpty(out, flowName))
				.findFirst();
			
		return flow;
	}
	
	private boolean hasFlowNameOrEmpty(SequenceFlow sequenceFlow, String flowName) {		
		if(isEmpty(flowName)) {
			return true;
		}
		
		if(hasFlowName(sequenceFlow, flowName)) {
			return true;
		}
		
		if(isEmpty(sequenceFlow.getEdge().getLabel().getText())) {
			return true;
		};

		return false;
	}

	private boolean hasFlowName(SequenceFlow sequenceFlow, String flowName) {
		String label = sequenceFlow.getEdge().getLabel().getText();
		return isNotBlank(label) && label.contains(flowName);
	}
	
	private boolean isDefaultPath(SequenceFlow flow) {
		NodeElement sourceElement = flow.getSource();
		if(sourceElement instanceof Alternative) {
			return isDefaultPath((Alternative) sourceElement, flow);
		}
		
		return false;		
	}
	
	private boolean isDefaultPath(Alternative alternative, SequenceFlow sequenceFlow) {
		String currentElementId = sequenceFlow.getPid().getFieldId();
		String nextTargetId = processGraph.getNextTargetIdByCondition(alternative, EMPTY);
		return Objects.equals(currentElementId, nextTargetId);
	}
	
	private Duration getDurationByTaskScript(TaskConfig task, UseCase useCase) {
		List<String> prefixs = new ArrayList<String>(Arrays.asList("WfEstimate.setEstimate"));
		if(useCase != null) {
			prefixs.add("UseCase." + useCase.name());
		}

		String wfEstimateCode = processGraph.getCodeLineByPrefix(task, prefixs.toArray(new String[0]));
		if (isNotEmpty(wfEstimateCode)) {
			String result = StringUtils.substringBetween(wfEstimateCode, "(", "UseCase");
			int amount = Integer.parseInt(result.substring(0, result.indexOf(",")));
			String unit = result.substring(result.indexOf(".") + 1, result.lastIndexOf(","));

			switch (TimeUnit.valueOf(unit.toUpperCase())) {
				case DAYS:
	                return Duration.ofDays(amount);
	            case HOURS:
	                return Duration.ofHours(amount);
	            case MINUTES:
	                return Duration.ofMinutes(amount);
	            case SECONDS:
	                return Duration.ofSeconds(amount);
	            default:
	                // Handle any unexpected TimeUnit
	                break;
			}		
		}

		return Duration.ofHours(0);
	}
	
	private boolean isStartTaskSwitchGateway(CommonElement element) {
		return element.getElement() instanceof TaskSwitchGateway
				&& ((TaskSwitchGateway) element.getElement()).getOutgoing().size() > 1;
	}

	private boolean isEndTaskSwitchGatewayAndWaiting(List<CommonElement> elements, CommonElement element) {
		BaseElement baseElement = element.getElement();	
		if (baseElement instanceof TaskSwitchGateway) {
			boolean hasFullInComing = false;
			long count = elements.stream().filter(el -> el.equals(element)).count();
			if (baseElement instanceof TaskSwitchGateway && count == ((TaskSwitchGateway) baseElement).getIncoming().size()) {
				hasFullInComing = true;
			}

			boolean hasStartBefore = false;
			for (int i = elements.size() - 2; i >= 0; i--) {
				if (isStartTaskSwitchGateway(elements.get(i))) {
					hasStartBefore = true;
					break;
				}
			}

			return hasStartBefore && !hasFullInComing;
		}
		return false;
	}

	private CommonElement getEndTaskSwithGateWay(TaskParallelGroup taskParallelGroup) {
		List<CommonElement> elements = taskParallelGroup.getInternalPaths().entrySet().stream().findFirst()
				.map(it -> it.getValue()).orElse(emptyList());
		int size = elements.size();
		return size > 0 ? elements.get(size - 1) : null;
	}
	
	private List<CommonElement> getPath(Map<SequenceFlow, List<CommonElement>> paths) {
		List<CommonElement> result = new ArrayList<>();
		paths.entrySet().stream()				
				.forEach(entry -> {
					result.add(new CommonElement(entry.getKey()));
					result.addAll(entry.getValue());
				});
		
		return result;
	}
}
