package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;

import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;

@SuppressWarnings("restriction")
public class WorkflowPath {

	private enum FindType {
		ALL_TASKS, TASKS_ON_PATH
	};

	private ProcessGraph processGraph;
	private Map<String, String> processFlowOverrides;
	
	public WorkflowPath(Map<String, String> processFlowOverrides) {
		this.processGraph = new ProcessGraph();
		this.processFlowOverrides = processFlowOverrides;
	}

	protected List<ProcessElement> findPath(ProcessElement... from) throws Exception {
		List<ProcessElement> path = findPath(Arrays.asList(from), null, FindType.ALL_TASKS,  emptyList());		
		return path;
	}
	
	public List<ProcessElement> findPath(String flowName, ProcessElement... from) throws Exception {
		List<ProcessElement> path = findPath(Arrays.asList(from), flowName, FindType.TASKS_ON_PATH, emptyList());
		return path;
	}

	private<T> int getLastIndex(List<T> elements) {		
		return elements.size() == 0 ? 0 : elements.size() - 1;		
	}
	
	private List<ProcessElement> findPath(List<ProcessElement> froms, String flowName, FindType findType, List<ProcessElement> previousElements) throws Exception {
		List<ProcessElement> result = new ArrayList<>();
		for(ProcessElement from : froms) {
			var path = findPath(from, flowName, findType, emptyList());
			result.addAll(path);
		}
				
		return result;
	}

	/**
	 * Using Recursion Algorithm To Find Tasks On Graph.
	 */
	private List<ProcessElement> findPath(ProcessElement from, String flowName, FindType findType, List<ProcessElement> previousElements) throws Exception {
		// Prevent loop
		if (isContains(previousElements, from)) {
			return emptyList();
		}

		List<ProcessElement> path = new ArrayList<>();
		path.add(from);
		
		if (from.getElement() instanceof NodeElement) {
				
			if (from.getElement() instanceof EmbeddedProcessElement) {
				List<ProcessElement> pathFromSubProcess = findPathOfSubProcess(from, flowName, findType);				
				path.addAll(pathFromSubProcess);				
			}

			if(isJoinTaskSwitchGateway(previousElements, from)) {				
				return path;
			}
			
			while(isStartTaskSwitchGateway(from)) {
				
				var taskParallelGroup = getTaskParallelGroup(from, flowName, findType, previousElements);
				
				//If last element is CommonElement(TaskSwitchGateway)-> We will remove it.
				int lastIndex =  getLastIndex(path);
				if(path.get(lastIndex) instanceof CommonElement && path.get(lastIndex).getElement() instanceof TaskSwitchGateway) {
					path.remove(lastIndex);
				}
				
				path.add(taskParallelGroup);
				from = getJoinTaskSwithGateWay(taskParallelGroup);
				
				if( from == null) {
					return  path;
				}
			}

			List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
			if (from.getElement() instanceof Alternative && outs.isEmpty()) {
				Ivy.log().error("Can not found the out going from a alternative {0}", from.getPid().getRawPid());
				throw new Exception("Not found path");
			}
						
			Map<SequenceFlow, List<ProcessElement>> paths = new LinkedHashMap<>();
			for (SequenceFlow out : outs) {
				CommonElement outElement = new CommonElement(out);
				List<ProcessElement> currentPath = ListUtils.union(previousElements, Arrays.asList(from, outElement));
				List<ProcessElement> nextOfPath = findPath(new CommonElement(out.getTarget()), flowName, findType, currentPath);
				paths.put(out, nextOfPath);
			}

			path.addAll(getPath(paths));
		}
		
		return path;
	}
		
	private boolean isContains(List<ProcessElement> previousElements, final ProcessElement element) {
		return previousElements.stream().map(ProcessElement::getElement).anyMatch(it -> it.equals(element.getElement()));
	}
	
	private TaskParallelGroup getTaskParallelGroup(ProcessElement from, String flowName, FindType findType, List<ProcessElement> previousElements) throws Exception {
		TaskParallelGroup result = new TaskParallelGroup(from.getElement());		
		List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
		
		Map<SequenceFlow, List<ProcessElement>> paths = new LinkedHashMap<>();
		for (SequenceFlow out : outs) {
			CommonElement outElement = new CommonElement(out);
			List<ProcessElement> currentPath = ListUtils.union(previousElements, Arrays.asList(from, outElement));
			List<ProcessElement> nextOfPath = findPath(new CommonElement(out.getTarget()), flowName, findType, currentPath);
			paths.put(out, nextOfPath);
		}

		result.setInternalPaths(paths);
		
		return result;
	}
	
	/**
	 * Find path on sub process
	 */	
	private List<ProcessElement> findPathOfSubProcess(ProcessElement subProcessElement, String flowName, FindType findType) throws Exception {
		// find start element EmbeddedProcessElement subProcessElement.getEmbeddedProcess()
		EmbeddedProcessElement processElement = (EmbeddedProcessElement)subProcessElement.getElement();
		BaseElement start = processGraph.findOneStartElementOfProcess(processElement.getEmbeddedProcess());
		List<ProcessElement> path = findPath(new CommonElement(start), flowName, findType, emptyList());		
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
			String flowIdFromOrverride = this.processFlowOverrides.get(from.getPid().getRawPid());
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
	
	private boolean isStartTaskSwitchGateway(ProcessElement element) {
		return element.getElement() instanceof TaskSwitchGateway
				&& ((TaskSwitchGateway) element.getElement()).getOutgoing().size() > 1;
	}

	private boolean isJoinTaskSwitchGateway(List<ProcessElement> elements, ProcessElement from) {
		BaseElement baseElement = from.getElement();
		boolean result =  false;
		if (baseElement instanceof TaskSwitchGateway && ((TaskSwitchGateway) baseElement).getIncoming().size() > 1) {
			boolean hasFullInComing = false;
			long count = elements.stream().filter(el -> el.equals(from)).count();
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

			result = hasStartBefore && !hasFullInComing;
			
		}
		return result;
	}

	private ProcessElement getJoinTaskSwithGateWay(TaskParallelGroup taskParallelGroup) {
		List<ProcessElement> elements = taskParallelGroup.getInternalPaths().entrySet().stream().findFirst()
				.map(it -> it.getValue()).orElse(emptyList());
		int size = elements.size();
		return size > 0 ? elements.get(size - 1) : null;
	}
	
	private List<ProcessElement> getPath(Map<SequenceFlow, List<ProcessElement>> paths) {
		List<ProcessElement> result = new ArrayList<>();
		paths.entrySet().stream()				
				.forEach(entry -> {
					result.add(new CommonElement(entry.getKey()));
					result.addAll(entry.getValue());
				});
		
		return result;
	}	
}
