package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.event.start.StartEvent;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.IvyScriptExpression;

@SuppressWarnings("restriction")
public class ProcessGraph {
	public final Process process;

	public ProcessGraph(Process process) {
		this.process = process;
	}
	
	/**
	 * Using Recursion Algorithm To Find Tasks On Graph.
	 * @throws Exception 
	 */
	public List<BaseElement> findPath(BaseElement from) throws Exception {
		List<BaseElement> path = findPath(from, EMPTY, true,  emptyList());		
		return path;
	}
	
	public List<BaseElement> findPath(BaseElement from, String flowName) throws Exception {
		List<BaseElement> path = findPath(from, flowName, false, emptyList());
		return path;
	}

	public List<BaseElement> findPath(BaseElement from, String flowName, boolean isFindAllTasks, List<BaseElement> previousElements) throws Exception {
		// Prevent loop
		if (previousElements.indexOf(from) >= 0) {
			return emptyList();
		}

		List<BaseElement> path = new ArrayList<>();
		path.add(from);

		if (from instanceof NodeElement) {
			if(from instanceof EmbeddedProcessElement) {
				List<BaseElement> pathFromSubProcess = findPathOfSubProcess((EmbeddedProcessElement) from, flowName, isFindAllTasks);
				path.addAll(pathFromSubProcess);
			}
			
			List<SequenceFlow> outs = getSequenceFlows((NodeElement) from, flowName, isFindAllTasks);			
			
			if (from instanceof Alternative && outs.isEmpty()) {
				Ivy.log().error("Can not found the out going from a alternative {0}", from.getPid().getRawPid());
				throw new Exception("Not found path");
			}
			
			Map<SequenceFlow, List<BaseElement>> paths = new HashedMap<>();
			for (SequenceFlow out : outs) {
				List<BaseElement> currentPath = ListUtils.union(previousElements, Arrays.asList(from));
				List<BaseElement> nextOfPath = findPath(out.getTarget(), flowName, isFindAllTasks, currentPath);
				paths.put(out, nextOfPath);
			}

			paths.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Comparator.comparing(List::size, Comparator.reverseOrder())))
					.forEach(entry -> {
						path.add(entry.getKey());
						path.addAll(entry.getValue());
					});
		}
		
		return path.stream().distinct().toList();
	}
	
	/**
	 * Find path on sub process
	 */	
	private List<BaseElement> findPathOfSubProcess(EmbeddedProcessElement subProcessElement, String flowName, boolean isFindAllTasks) throws Exception {
		// find start element
		BaseElement start = findStartElementOfProcess(subProcessElement.getEmbeddedProcess());
		var listElement = findPath(start, flowName, isFindAllTasks, emptyList());
		
		return listElement;
	}
	
	private BaseElement findStartElementOfProcess(Process subProcess) {
		BaseElement start = subProcess.getElements().stream().filter(item -> item instanceof StartEvent).findFirst().orElse(null);
		return start;
	}
	
	private List<SequenceFlow> getSequenceFlows(NodeElement from, String flowName, boolean isFindAllTasks) {
		if (isFindAllTasks || from instanceof TaskSwitchGateway) {
			return from.getOutgoing();
		} else {
			return getSequenceFlow(from, flowName).map(Arrays::asList).orElse(emptyList());
		}
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
		if (isNotBlank(label) && label.contains(flowName)) {
			return true;
		}

		return false;
	}
	
	private boolean isDefaultPath(BaseElement currentElement, Alternative previousElement) {
		String currentElementId = currentElement.getPid().getFieldId();
		String nextTargetId = getNextTargetIdByCondition((Alternative) previousElement, EMPTY);
		return Objects.equals(currentElementId, nextTargetId);
	}
	
	private boolean isDefaultPath(SequenceFlow flow) {
		NodeElement sourceElement = flow.getSource();
		if(sourceElement instanceof Alternative) {
			return isDefaultPath(flow, (Alternative) sourceElement);
		}
		
		return false;		
	}
	
	private String getNextTargetIdByCondition(Alternative alternative, String condition) {
		IvyScriptExpression script = IvyScriptExpression.script(defaultString(condition));
		String nextTargetId = alternative.getConditions().conditions().entrySet().stream()
				.filter(entry -> script.equals(entry.getValue()))
				.findFirst()
				.map(Entry::getKey)
				.orElse(null);		
		
		return nextTargetId;
	}
	
	private boolean isAcceptedTask(BaseElement element) {
		return Optional.ofNullable(element)
				// filter to get task only
				.filter(node -> {
					return node instanceof TaskAndCaseModifier;
				}).map(TaskAndCaseModifier.class::cast)
				.filter(node -> {
					return node instanceof RequestStart == false;
				})
				// Remove SYSTEM task
				.filter(node -> {
					return isSystemTask(node) == false;
				}).isPresent();
	}
	
	private boolean isSystemTask(TaskAndCaseModifier task) {		
		return task.getAllTaskConfigs().stream().anyMatch(it -> "SYSTEM".equals(it.getActivator().getName()));
	}
}
