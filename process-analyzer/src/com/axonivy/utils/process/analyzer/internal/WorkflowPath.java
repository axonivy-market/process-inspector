package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import com.axonivy.utils.process.analyzer.internal.model.AnalysisPath;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
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

	public Map<ProcessElement, List<AnalysisPath>> findPath(ProcessElement... from) throws Exception {
		Map<ProcessElement, List<AnalysisPath>> paths = findPath(Arrays.asList(from), null, FindType.ALL_TASKS,  emptyList());		
		return paths;
	}
	
	public Map<ProcessElement, List<AnalysisPath>> findPath(String flowName, ProcessElement... from) throws Exception {
		Map<ProcessElement, List<AnalysisPath>> paths = findPath(Arrays.asList(from), flowName, FindType.TASKS_ON_PATH, emptyList());
		return paths;
	}

	private<T> int getLastIndex(List<T> elements) {		
		return elements.size() == 0 ? 0 : elements.size() - 1;		
	}
	
	private Map<ProcessElement, List<AnalysisPath>> findPath(List<ProcessElement> froms, String flowName, FindType findType, List<AnalysisPath> previousElements) throws Exception {
		Map<ProcessElement, List<AnalysisPath>>  result = new LinkedHashMap<>();
		for(ProcessElement from : froms) {
			List<AnalysisPath> path = findPath(from, flowName, findType, emptyList());
			result.put(from, path);
		}
		
		ProcessElement intersectionTask  = findFirstIntersectionTaskSwitchGateway(result);
		if(intersectionTask == null) {
			return result;	
		}
		
		//Find again from intersection task
		List<AnalysisPath> subPath = findPath(new CommonElement(intersectionTask.getElement()), flowName, findType, emptyList());

		Map<ProcessElement, List<AnalysisPath>> fullPath = mergePath(result, intersectionTask, subPath);
				
		return fullPath;
	}
	
	private Map<ProcessElement, List<AnalysisPath>> mergePath(Map<ProcessElement, List<AnalysisPath>> source, ProcessElement intersection, List<AnalysisPath> subPath) {
		Map<ProcessElement, List<AnalysisPath>> pathBeforeIntersection = new LinkedHashMap<>();
		Map<ProcessElement, List<AnalysisPath>> pathNotIntersection = new LinkedHashMap<>();
		
		for (Entry<ProcessElement, List<AnalysisPath>> entry : source.entrySet()) {	
			List<AnalysisPath> beforeIntersections = getAnalysisPathBeforeIntersection(entry.getValue(), intersection);
			if(beforeIntersections.isEmpty()) {
				pathNotIntersection.put(entry.getKey(), entry.getValue());
			} else {
				pathBeforeIntersection.put(entry.getKey(), beforeIntersections);	
			}						
		}
		
		TaskParallelGroup taskGroup = new TaskParallelGroup(null);
		taskGroup.setInternalPaths(convertToTaskParallelGroup(pathBeforeIntersection));		
		List<AnalysisPath> fullPathWithIntersection = addToPath(List.of(new AnalysisPath(List.of(taskGroup))), subPath);

		Map<ProcessElement, List<AnalysisPath>> result = new LinkedHashMap<>();
		result.putAll(pathNotIntersection);
		result.put(pathBeforeIntersection.keySet().stream().findFirst().get(), fullPathWithIntersection);
		
		return result;
	}
	
	private List<AnalysisPath> getAnalysisPathBeforeIntersection(List<AnalysisPath> paths, ProcessElement intersection) {
		List<AnalysisPath> result = new ArrayList<>();
		for (AnalysisPath path : paths) {
			int index = path.getElements().indexOf(intersection);
			if (index >= 0) {
				List<ProcessElement> beforeIntersection = path.getElements().subList(0, index);
				result.add(new AnalysisPath(beforeIntersection));
			}
		}

		return result;
	}
	
	private Map<SequenceFlow, List<AnalysisPath>> convertToTaskParallelGroup(Map<ProcessElement, List<AnalysisPath>> internalPath) {
		Map<SequenceFlow, List<AnalysisPath >> result = new LinkedHashMap<>();
		internalPath.entrySet().forEach(it -> {
			result.put( ((TaskAndCaseModifier) it.getKey().getElement()).getIncoming().get(0), it.getValue());
		});		
		
		return result;
	}
	
	private ProcessElement findFirstIntersectionTaskSwitchGateway(Map<ProcessElement, List<AnalysisPath>> paths) {
		ProcessElement intersert = null;		
		if (paths.size() > 1) {
			Map<ProcessElement, List<ProcessElement>> taskSwitchGateways = new LinkedHashMap<>();
			for (Entry<ProcessElement, List<AnalysisPath>> path : paths.entrySet()) {
				AnalysisPath analysisPathWithTaskSwitchGateway =  findFirstAnalysisPathWithTaskSwitchGateway(path.getValue());
				if (analysisPathWithTaskSwitchGateway != null) {
					List<ProcessElement> taskGroup = analysisPathWithTaskSwitchGateway.getElements().stream()
							.filter(it -> it.getElement() instanceof TaskSwitchGateway == true)
							.toList();

					taskSwitchGateways.put(path.getKey(), taskGroup);
				}
			}
			
			List<ProcessElement> intersertsOfAll = taskSwitchGateways.values().stream().findFirst().orElse(emptyList());
			for (Entry<ProcessElement, List<ProcessElement>> entry : taskSwitchGateways.entrySet()) {
				List<ProcessElement> interserts = ListUtils.intersection(entry.getValue(), intersertsOfAll);
				if(interserts.size() == 0) {
					break;
				} else {
					intersertsOfAll = interserts;
				}
			}
			
			intersert = intersertsOfAll.stream()
					.filter(it -> ((TaskSwitchGateway) it.getElement()).getIncoming().size() > 1)
					.findFirst().orElse(null);
			
		}
		
		return intersert;
	}
	
	private AnalysisPath findFirstAnalysisPathWithTaskSwitchGateway(List<AnalysisPath> paths) {
		return paths.stream().filter(path -> containsTaskSwitchGateway(path)).findFirst().orElse(null);
	}
	
	private boolean containsTaskSwitchGateway(AnalysisPath path) {
		return path.getElements().stream().anyMatch(it -> processGraph.isTaskSwitchGateway(it.getElement()));
	}
	/**
	 * Using Recursion Algorithm To Find Tasks On Graph.
	 */
	private List<AnalysisPath> findPath(ProcessElement from, String flowName, FindType findType, List<AnalysisPath> currentPath) throws Exception {
		// Prevent loop
		if (isContains(currentPath, from)) {
			return emptyList();
		}

		List<AnalysisPath> path = new ArrayList<>();
		path = addToPath(path, from);
		
		if (from.getElement() instanceof NodeElement) {
				
			if (from.getElement() instanceof EmbeddedProcessElement) {
				List<AnalysisPath> pathFromSubProcess = findPathOfSubProcess(from, flowName, findType);
				path = addToPath(path, pathFromSubProcess);
			}

			if(isJoinTaskSwitchGateway(currentPath, from)) {				
				return path;
			}
			
			while(isStartTaskSwitchGateway(from)) {
				
				var taskParallelGroup = getTaskParallelGroup(from, flowName, findType, currentPath);
				
				//If last element is CommonElement(TaskSwitchGateway)-> We will remove it.				
				path = removeLastTaskSwitchGateway(path);

				path = addToPath(path, taskParallelGroup);
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
						
			Map<SequenceFlow, List<AnalysisPath>> pathOptions = new LinkedHashMap<>();
			for (SequenceFlow out : outs) {
				CommonElement outElement = new CommonElement(out);
				List<AnalysisPath> newPath = addAllToPath(currentPath, Arrays.asList(from, outElement));
				List<AnalysisPath> nextOfPath = findPath(new CommonElement(out.getTarget()), flowName, findType, newPath);
				pathOptions.put(out, nextOfPath);
			}

			path = addAllToPath(path, pathOptions);
		}
		
		return path;
	}
	
	private List<AnalysisPath> removeLastTaskSwitchGateway(List<AnalysisPath> paths){
		List<AnalysisPath> result = new ArrayList<>();
		for(AnalysisPath path : paths) {			
			int lastIndex =  getLastIndex(path.getElements());
			List<ProcessElement> pathElements =  new ArrayList<>(path.getElements());
			if(pathElements.get(lastIndex) instanceof CommonElement && pathElements.get(lastIndex).getElement() instanceof TaskSwitchGateway) {
				pathElements.remove(lastIndex);
			}
			result.add(new AnalysisPath(pathElements));
		}
		
		return result;
	}

	private List<AnalysisPath> addToPath(List<AnalysisPath> paths, ProcessElement from) {
		List<AnalysisPath> result = addAllToPath(paths, List.of(from));
		return result;
	}
	
	private List<AnalysisPath> addToPath(List<AnalysisPath> paths, List<AnalysisPath> subPaths) {
		if(subPaths.isEmpty()) {
			return paths;
		}
		
		List<AnalysisPath> result = paths;
		for(AnalysisPath path : subPaths) {
			result = addAllToPath(result, path.getElements());
		}
				
		return result;
	}
	
	private List<AnalysisPath> addAllToPath(List<AnalysisPath> paths, List<ProcessElement> elements) {
		List<AnalysisPath> result = new ArrayList<>();
		if(paths.isEmpty()) {
			if (isNotEmpty(elements)) {
				result.add(new AnalysisPath(elements));
			}
		} else {
			paths.forEach(it -> {
				result.add(new AnalysisPath(ListUtils.union(it.getElements(), elements)));
			});	
		}
		
		return result;
	}
	
	private List<AnalysisPath> addAllToPath(List<AnalysisPath> paths, Map<SequenceFlow, List<AnalysisPath>> pathOptions) {
		List<AnalysisPath> result = new ArrayList<>();
		if(pathOptions.isEmpty()) {
			result.addAll(paths);
		} else {
			pathOptions.entrySet().forEach(it -> {
				ProcessElement sequenceFlowElement = new CommonElement(it.getKey());
				if(it.getValue().isEmpty()) {
					result.addAll(addToPath(paths, sequenceFlowElement));	
				} else {
					it.getValue().forEach( path -> {	
						List<ProcessElement> elememts = ListUtils.union(List.of(sequenceFlowElement), path.getElements());
						
						result.addAll(addAllToPath(paths, elememts));				
					});	
				}
								
			});	
		}
		return result;
	}

	private boolean isContains(List<AnalysisPath> currentPath, final ProcessElement element) {
		return currentPath.stream().map(AnalysisPath::getElements).flatMap(List::stream).anyMatch(it -> it.getElement().equals(element.getElement()));
	}
	
	private TaskParallelGroup getTaskParallelGroup(ProcessElement from, String flowName, FindType findType, List<AnalysisPath> currentPath) throws Exception {
		TaskParallelGroup result = new TaskParallelGroup(from.getElement());		
		List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
		
		Map<SequenceFlow, List<AnalysisPath>> paths = new LinkedHashMap<>();
		for (SequenceFlow out : outs) {
			CommonElement outElement = new CommonElement(out);
			List<AnalysisPath> newPath = addAllToPath(currentPath, Arrays.asList(from, outElement));
			List<AnalysisPath> nextOfPath = findPath(new CommonElement(out.getTarget()), flowName, findType, newPath);
			paths.put(out, nextOfPath);
		}

		result.setInternalPaths(paths);
		
		return result;
	}
	
	/**
	 * Find path on sub process
	 */	
	private List<AnalysisPath> findPathOfSubProcess(ProcessElement subProcessElement, String flowName, FindType findType) throws Exception {
		// find start element EmbeddedProcessElement subProcessElement.getEmbeddedProcess()
		EmbeddedProcessElement processElement = (EmbeddedProcessElement)subProcessElement.getElement();
		BaseElement start = processGraph.findOneStartElementOfProcess(processElement.getEmbeddedProcess());
		List<AnalysisPath> path = findPath(new CommonElement(start), flowName, findType, emptyList());		
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

	private boolean isJoinTaskSwitchGateway(List<AnalysisPath> paths, ProcessElement from) {
		List<ProcessElement> elements = paths.stream().map(AnalysisPath::getElements).flatMap(List::stream).toList();
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
		List<ProcessElement> elements = taskParallelGroup.getInternalPaths().entrySet().stream()
				.flatMap(it -> it.getValue().stream()).findFirst().map(AnalysisPath::getElements).orElse(emptyList());

		int size = elements.size();
		return size > 0 ? elements.get(size - 1) : null;
	}
}
