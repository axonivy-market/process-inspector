package com.axonivy.utils.process.inspector.internal;

import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.addAllToPath;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.addToPath;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.findIncomingsFromPaths;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.getAllStartElementOfTaskSwitchGateways;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.getAnalysisPathFrom;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.getAnalysisPathTo;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.getPathByStartElements;
import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.replaceFirstElement;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper;
import com.axonivy.utils.process.inspector.internal.model.AnalysisPath;
import com.axonivy.utils.process.inspector.internal.model.CommonElement;
import com.axonivy.utils.process.inspector.internal.model.ProcessElement;
import com.axonivy.utils.process.inspector.internal.model.SubProcessGroup;
import com.axonivy.utils.process.inspector.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;

public class PathFinder {

	private enum FindType {
		ALL_TASKS, TASKS_ON_PATH
	};

	private ProcessGraph processGraph;
	private List<ProcessElement> froms;
	private String flowName;
	private Map<String, String> processFlowOverrides = emptyMap();
	
	public PathFinder() {
		this.processGraph = new ProcessGraph();
	}

	public PathFinder setProcessFlowOverrides(Map<String, String> processFlowOverrides) {
		this.processFlowOverrides = processFlowOverrides;
		return this;
	}

	public PathFinder setStartElements(List<ProcessElement> froms) {
		this.froms = froms;
		return this;
	}

	public PathFinder setFlowName(String flowName) {
		this.flowName = flowName;
		return this;
	}

	public Map<ProcessElement, List<AnalysisPath>> findAllTask() throws Exception {
		Map<ProcessElement, List<AnalysisPath>> paths = emptyMap();
		if (froms != null) {
			paths = findPathWithParentElement(this.froms, null, FindType.ALL_TASKS);
		}
		return paths;
	}

	public Map<ProcessElement, List<AnalysisPath>> findTaskOnPath() throws Exception {
		Map<ProcessElement, List<AnalysisPath>> paths = emptyMap();
		if (froms != null) {
			paths = findPathWithParentElement(this.froms, this.flowName, FindType.TASKS_ON_PATH);
		}
		return paths;
	}
	
	private Map<ProcessElement, List<AnalysisPath>> findPathWithParentElement(List<ProcessElement> startElement, String flowName, FindType findType) throws Exception {
		Map<ProcessElement, List<AnalysisPath>> paths = findPath(startElement, flowName, findType);
		
		//This block to find the parent's path which is  EmbeddedProcessElement
		Map<ProcessElement, List<AnalysisPath>> result = new LinkedHashMap<>();
		for(Entry<ProcessElement, List<AnalysisPath>>  entry : paths.entrySet()) {
			var pathWithParent = getParentPathOf(entry.getKey(), flowName, findType, entry.getValue());
			result.put(entry.getKey(), pathWithParent);
		}
		
		//Fix internal path for subprocess with multiple end event
		result = correctInternalPathByNextSequence(result);
		return result;
	}
	
	private List<AnalysisPath> getParentPathOf(ProcessElement startElement,String flowName, FindType findType, List<AnalysisPath> paths) throws Exception {				
		List<AnalysisPath> result = paths;
		
		EmbeddedProcessElement parentElement = processGraph.getParentElement(startElement.getElement());
		if(parentElement != null) {
			SubProcessGroup subProcess = new SubProcessGroup(parentElement, paths);			
			ProcessElement parentProcessElement = new CommonElement(parentElement);
			
			Map<ProcessElement, List<AnalysisPath>> parentPaths = findPath(List.of(parentProcessElement), flowName, findType);			
			List<AnalysisPath> subPaths = parentPaths.getOrDefault(parentProcessElement, emptyList());			
			List<AnalysisPath> fullParentPath = replaceFirstElement(subProcess, subPaths);
			
			result = getParentPathOf(parentProcessElement, flowName, findType, fullParentPath);
		}

		return result;
	}

	private Map<ProcessElement, List<AnalysisPath>> findPath(List<ProcessElement> froms, String flowName, FindType findType) throws Exception {
		Map<ProcessElement, List<AnalysisPath>> result = new LinkedHashMap<>();
		for (ProcessElement from : froms) {
			List<AnalysisPath> path = findAnalysisPaths(from, flowName, findType, emptyList());
			result.put(from, path);
		}

		ProcessElement intersectionTask = findIntersectionTaskSwitchGateway(result);
		if (intersectionTask == null) {
			return result;
		}

		// Find again from intersection task
		ProcessElement startElement = new CommonElement(intersectionTask.getElement());
		Map<ProcessElement, List<AnalysisPath>> subPaths = findPath(List.of(startElement), flowName, findType);

		Map<ProcessElement, List<AnalysisPath>> fullPath = mergePath(result, intersectionTask, subPaths.getOrDefault(startElement, emptyList()));

		return fullPath;
	}

	private ProcessElement findIntersectionTaskSwitchGateway(Map<ProcessElement, List<AnalysisPath>> paths) {
		if (paths.size() > 1) {
			var intersections = getAllIntersectionTaskSwitchGatewayWithStartElement(paths);
			return intersections.entrySet().stream()
					.max((a, b) -> Integer.compare(a.getValue().size(), b.getValue().size())).map(Entry::getKey)
					.orElse(null);
		}
		return null;
	}

	private Map<ProcessElement, List<AnalysisPath>> mergePath(Map<ProcessElement, List<AnalysisPath>> source,
			ProcessElement intersection, List<AnalysisPath> subPath) {
		Map<ProcessElement, List<AnalysisPath>> pathBeforeIntersection = getAnalysisPathTo(source, intersection);

		Map<ProcessElement, List<AnalysisPath>> pathNotIntersection = source.entrySet().stream()
				.filter(entry -> !pathBeforeIntersection.keySet().contains(entry.getKey())).collect(toMap(
						Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

		TaskParallelGroup taskGroup = convertToTaskParallelGroup(pathBeforeIntersection);

		List<AnalysisPath> fullPathWithIntersection = addToPath(List.of(new AnalysisPath(List.of(taskGroup))), subPath);

		Map<ProcessElement, List<AnalysisPath>> result = new LinkedHashMap<>();
		result.putAll(pathNotIntersection);
		result.put(pathBeforeIntersection.keySet().stream().findFirst().get(), fullPathWithIntersection);

		return result;
	}

	private TaskParallelGroup convertToTaskParallelGroup(Map<ProcessElement, List<AnalysisPath>> pathBeforeIntersection) {
		Map<ProcessElement, List<AnalysisPath>> pathHaveNoIntersection = emptyMap();
		Map<ProcessElement, List<AnalysisPath>> pathHaveIntersection = pathBeforeIntersection;

		var haveNextIntersection = getLastIntersectionByStartElements(pathBeforeIntersection);
		final Set<ProcessElement> startEleWithoutIntersection = new HashSet<>();
		if (haveNextIntersection.size() > 0) {
			pathHaveNoIntersection = getPathHaveNoIntersection(pathBeforeIntersection, haveNextIntersection);

			startEleWithoutIntersection.addAll(pathHaveNoIntersection.keySet());

			pathHaveIntersection = pathBeforeIntersection.entrySet().stream()
					.filter(entry -> !startEleWithoutIntersection.contains(entry.getKey()))
					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
		}

		List<AnalysisPath> pathsWithIntersection = convertToAnalysisPaths(pathHaveIntersection);

		Map<ProcessElement, List<AnalysisPath>> internalPath = new LinkedHashMap<>();
		internalPath.putAll(pathHaveNoIntersection);

		if (isNotEmpty(pathsWithIntersection)) {
			ProcessElement key = pathBeforeIntersection.keySet().stream()
					.filter(it -> !startEleWithoutIntersection.contains(it))
					.findFirst().get();
			internalPath.put(key, pathsWithIntersection);
		}

		TaskParallelGroup taskGroup = convertToTaskParallelGroupWithInternalPath(internalPath);
		return taskGroup;
	}

	private List<AnalysisPath> convertToAnalysisPaths(Map<ProcessElement, List<AnalysisPath>> source) {
		List<AnalysisPath> result = new ArrayList<AnalysisPath>();

		Map<ProcessElement, Set<ProcessElement>> intersections = getLastIntersectionByStartElements(source);

		// End recursion point
		if (intersections.size() == 0) {
			TaskParallelGroup taskGroup = convertToTaskParallelGroupWithInternalPath(source);
			return List.of(new AnalysisPath(List.of(taskGroup)));
		}

		for (Entry<ProcessElement, Set<ProcessElement>> intersectionEntry : intersections.entrySet()) {
			ProcessElement intersection = intersectionEntry.getKey();
			Set<ProcessElement> startElements = intersectionEntry.getValue();

			Map<ProcessElement, List<AnalysisPath>> paths = getPathByStartElements(source, startElements);

			Map<ProcessElement, List<AnalysisPath>> pathBeforeIntersection = getAnalysisPathTo(paths, intersection);

			// Call recursion inside convertToTaskParallelGroup
			TaskParallelGroup taskGroup = convertToTaskParallelGroup(pathBeforeIntersection);

			List<AnalysisPath> subPathAfterIntersection = getAnalysisPathFrom(paths, intersection);

			result.addAll(addToPath(List.of(new AnalysisPath(List.of(taskGroup))), subPathAfterIntersection));
		}

		return result;
	}

	private Map<ProcessElement, List<AnalysisPath>> getPathHaveNoIntersection(
			Map<ProcessElement, List<AnalysisPath>> source, 
			Map<ProcessElement, Set<ProcessElement>> intersections) {
		
		List<ProcessElement> startElementWithIntersection = intersections.values().stream()
				.flatMap(Collection::stream)
				.toList();

		return source.entrySet().stream()
				.filter(entry -> !startElementWithIntersection.contains(entry.getKey()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	private TaskParallelGroup convertToTaskParallelGroupWithInternalPath(Map<ProcessElement, List<AnalysisPath>> internalPaths) {
		TaskParallelGroup taskGroup = new TaskParallelGroup(null);
		
		Map<SequenceFlow, List<AnalysisPath>> result = new LinkedHashMap<>();
		internalPaths.entrySet().forEach(it -> {
			SequenceFlow sequenceFlow = processGraph.getFirstIncoming(it.getKey().getElement());
			result.put(sequenceFlow, it.getValue());
		});
		
		taskGroup.setInternalPaths(result);
		return taskGroup;
	}

	private Map<ProcessElement, Set<ProcessElement>> getLastIntersectionByStartElements(Map<ProcessElement, List<AnalysisPath>> paths) {
		var intersections = getAllIntersectionTaskSwitchGatewayWithStartElement(paths);

		Map<ProcessElement, Set<ProcessElement>> keepIntersections = new LinkedHashMap<>();
		intersections.forEach((intersection, startElements) -> {
			boolean isContains = keepIntersections.values().stream().anyMatch(it -> it.containsAll(startElements));
			if (!isContains) {
				keepIntersections.put(intersection, startElements);
			}
		});
		return keepIntersections;
	}

	private Map<ProcessElement, Set<ProcessElement>> getAllIntersectionTaskSwitchGatewayWithStartElement(Map<ProcessElement, List<AnalysisPath>> source) {
		Map<ProcessElement, Set<ProcessElement>> intersectNodes = getAllStartElementOfTaskSwitchGateways(source);

		return intersectNodes.entrySet().stream()
				.sorted(Map.Entry
						.<ProcessElement, Set<ProcessElement>>comparingByValue(
								(Set<ProcessElement> a, Set<ProcessElement> b) -> Integer.compare(a.size(), b.size()))
						.reversed())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));
	}

	/**
	 * Using Recursion Algorithm To Find Tasks On Graph.
	 */
	private List<AnalysisPath> findAnalysisPaths(ProcessElement startElement, String flowName, FindType findType, List<AnalysisPath> currentPath) throws Exception {
		List<AnalysisPath> path = emptyList();
		ProcessElement from = startElement;
		// Prevent loop
		if (AnalysisPathHelper.isContains(currentPath, from)) {
			return path;
		}

		path = addAllToPath(path, List.of(from));

		if (from.getElement() instanceof NodeElement) {

			if (from.getElement() instanceof EmbeddedProcessElement) {
				SubProcessGroup subProcessGroup = findPathOfSubProcess(from, flowName, findType, currentPath);
				path = AnalysisPathHelper.removeLastElementByClassType(path, EmbeddedProcessElement.class);	
				path = addAllToPath(path, List.of(subProcessGroup));
			}

			if (isJoinTaskSwitchGateway(currentPath, from)) {
				return path;
			}

			if (isStartTaskSwitchGateway(from)) {
				var taskParallelGroup = getTaskParallelGroup(from, flowName, findType, currentPath);

				// If last element is CommonElement(TaskSwitchGateway)-> We will remove it.
				path = AnalysisPathHelper.removeLastElementByClassType(path, TaskSwitchGateway.class);

				path = addAllToPath(path, List.of(taskParallelGroup));
				
				from = getJoinTaskSwithGateWay(taskParallelGroup);
				
				var newPath = addToPath(currentPath, path);
				if (from == null || !haveFullInComingJoinTaskSwitchGateway(newPath, from)) {
					return path;
				}
				
				if (processGraph.isTaskSwitchGateway(from.getElement())) {
					//In case only one out going -> it should run as normal node					 
					if(processGraph.isMultiIOutgoing(from.getElement())) {
						// Call recursion for next TasksSwitchGateway
						List<AnalysisPath> nextPathOfTaskSwitchGateway = findAnalysisPaths(from, flowName, findType, newPath);
						path = addToPath(path, nextPathOfTaskSwitchGateway);
					}
				}
			}
			

			var newPath = addToPath(currentPath, path);
			//It stop finding tasks when the end node is TaskEnd for TASKS_ON_PATH case
			if (shouldStopFindTask(newPath, findType)) {
				return path;
			}
			// Call recursion for next normal node
			var pathOptions = findAnalysisPathForNextNode(from, flowName, findType, newPath);
			path = addAllToPath(path, pathOptions);	
		}
		
		return path;
	}

	private Map<SequenceFlow, List<AnalysisPath>> findAnalysisPathForNextNode(ProcessElement from, String flowName, 
			FindType findType, List<AnalysisPath> currentPath) throws Exception {
		
		List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
		if (from.getElement() instanceof Alternative && outs.isEmpty()) {
			String mgs = String.format("Not found path after element: \"%s\"", processGraph.getAlternativeNameId(from.getElement()));
			throw new Exception(mgs);
		}

		Map<SequenceFlow, List<AnalysisPath>> pathOptions = new LinkedHashMap<>();
		for (SequenceFlow out : outs) {
			CommonElement outElement = new CommonElement(out);
			List<AnalysisPath> newPath = addAllToPath(currentPath, Arrays.asList(outElement));
			
			ProcessElement nextStartElement = new CommonElement(out.getTarget());
			List<AnalysisPath> nextOfPath = findAnalysisPaths(nextStartElement, flowName, findType, newPath);
			pathOptions.put(out, nextOfPath);
		}

		return pathOptions;
	}

	private TaskParallelGroup getTaskParallelGroup(ProcessElement from, String flowName, FindType findType,
			List<AnalysisPath> currentPath) throws Exception {
		TaskParallelGroup result = new TaskParallelGroup(from.getElement());
		List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);

		Map<SequenceFlow, List<AnalysisPath>> paths = new LinkedHashMap<>();
		for (SequenceFlow out : outs) {
			CommonElement outElement = new CommonElement(out);
			List<AnalysisPath> newPath = addAllToPath(currentPath, Arrays.asList(from, outElement));
			
			ProcessElement nextStartElement = new CommonElement(out.getTarget());
			List<AnalysisPath> nextOfPath = findAnalysisPaths(nextStartElement, flowName, findType, newPath);
			paths.put(out, nextOfPath);
		}

		result.setInternalPaths(paths);

		return result;
	}

	/**
	 * Find path on sub process
	 */
	private SubProcessGroup findPathOfSubProcess(ProcessElement subProcessElement, String flowName, FindType findType, List<AnalysisPath> currentPath) throws Exception {
		EmbeddedProcessElement processElement = (EmbeddedProcessElement) subProcessElement.getElement();
		
		List<ProcessElement> currentElements = AnalysisPathHelper.getAllProcessElement(currentPath);
		SequenceFlow lastElement = currentElements.stream()
				.reduce((a, b) -> b)
				.map(ProcessElement::getElement)
				.map(SequenceFlow.class::cast).orElse(null);
		
		EmbeddedStart start = processGraph.findStartElementOfProcess((SequenceFlow)lastElement, processElement);
		List<AnalysisPath> path = findAnalysisPaths(new CommonElement(start), flowName, findType, emptyList());
		
		SubProcessGroup subProcessGroup = new SubProcessGroup(processElement, path);		
		return subProcessGroup;
	}

	private List<SequenceFlow> getSequenceFlows(NodeElement from, String flowName, FindType findType) throws Exception {
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
			flow = from.getOutgoing().stream()
					.filter(out -> out.getPid().getRawPid().equals(flowIdFromOrverride))
					.findFirst();
		}

		// If it don't find out the flow from flowOverrides, it is base on the default
		// flow in process
		if (flow.isEmpty()) {
			flow = getSequenceFlow(from, flowName);
		}

		return flow.map(Arrays::asList).orElse(emptyList());
	}

	private Optional<SequenceFlow> getSequenceFlow(NodeElement nodeElement, String flowName) throws Exception {
		List<SequenceFlow> outs = nodeElement.getOutgoing();
		if (isEmpty(outs)) {
			return Optional.empty();
		}

		if (nodeElement instanceof Alternative) {
			// High priority for checking default path if flowName is null
			if (isEmpty(flowName)) {
				return findSequenceFlowByDefaultPath(outs);
			} else {
				// If flowName is not null, check flowName first
				Optional<SequenceFlow> flow = findSequenceFlowByFlowName(outs, flowName);
				// Then check default path
				if (!flow.isPresent()) {
					flow = findSequenceFlowByDefaultPath(outs);
				}
				return flow;
			}
		}
		
		//Sequence flow after Task
		Optional<SequenceFlow> flow = outs.stream().filter(out -> hasFlowNameOrEmpty(out, flowName)).findFirst();

		return flow;
	}

	private Optional<SequenceFlow> findSequenceFlowByDefaultPath(List<SequenceFlow> outs) throws Exception {
		List<SequenceFlow> defaultPathOuts = outs.stream().filter(out -> processGraph.isDefaultPath(out)).toList();
		if (defaultPathOuts.size() > 1) {
			// Throw exception
			throw new Exception("Have more than one out going with default path");
		} else {
			return defaultPathOuts.stream().findFirst();
		}
	}

	private Optional<SequenceFlow> findSequenceFlowByFlowName(List<SequenceFlow> outs, String flowName) throws Exception {
		List<SequenceFlow> flowNameOuts = outs.stream().filter(out -> processGraph.hasFlowName(out, flowName)).toList();
		if (flowNameOuts.size() > 1) {
			// Throw exception
			throw new Exception("Have more than one out going with flowname " + flowName);
		} else {
			return flowNameOuts.stream().findFirst();
		}
	}

	private boolean hasFlowNameOrEmpty(SequenceFlow sequenceFlow, String flowName) {
		if (isEmpty(flowName)) {
			return true;
		}

		if (processGraph.hasFlowName(sequenceFlow, flowName)) {
			return true;
		}

		if (isEmpty(sequenceFlow.getEdge().getLabel().getText())) {
			return true;
		}

		return false;
	}

	private boolean isStartTaskSwitchGateway(ProcessElement element) {
		return processGraph.isTaskSwitchGateway(element.getElement()) && processGraph.isMultiIOutgoing(element.getElement());
	}

	private boolean isJoinTaskSwitchGateway(List<AnalysisPath> paths, ProcessElement from) {		
		BaseElement baseElement = from.getElement();
		boolean result = false;
		
		if (processGraph.isTaskSwitchGateway(baseElement) && processGraph.isMultiIncoming(baseElement)) {

			boolean hasFullInComing = haveFullInComingJoinTaskSwitchGateway(paths, from);
			
			boolean hasStartBefore = false;
			List<ProcessElement> elements = paths.stream().map(AnalysisPath::getElements).flatMap(List::stream).toList();
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

	private boolean haveFullInComingJoinTaskSwitchGateway(List<AnalysisPath> paths, ProcessElement from) {
		if(from == null) {
			return false;
		}

		BaseElement baseElement = from.getElement();
		boolean hasFullInComing = false;
		//Make sure it is join task switch  
		if (processGraph.isTaskSwitchGateway(baseElement) && processGraph.isMultiIncoming(baseElement)) {

			List<SequenceFlow> sequenceFlowToParalletTasks = findIncomingsFromPaths(paths, from);

			NodeElement startNode = AnalysisPathHelper.getFirstNodeElement(paths);
			List<SequenceFlow> sequenceFlows = processGraph.getSequenceFlowOf((NodeElement) baseElement, startNode);

			long count = sequenceFlowToParalletTasks.stream().filter(el -> sequenceFlows.contains(el)).count();
			if (count >= sequenceFlows.size()) {
				hasFullInComing = true;
			}
		}
		return hasFullInComing;
	}

	private ProcessElement getJoinTaskSwithGateWay(TaskParallelGroup taskParallelGroup) {
		List<ProcessElement> elements = AnalysisPathHelper.getAllProcessElement(taskParallelGroup);
		return AnalysisPathHelper.getLastElement(new AnalysisPath(elements));		
	}
	
	private boolean shouldStopFindTask(SubProcessGroup element, FindType findType) {
		List<AnalysisPath> paths = element.getInternalPaths();
		return shouldStopFindTask(paths, findType);
	}
	
	private boolean shouldStopFindTask(List<AnalysisPath> paths, FindType findType) {
		
		if (FindType.TASKS_ON_PATH.equals(findType) && isNotEmpty(paths)) {
			ProcessElement lastElement = AnalysisPathHelper.getLastElement(paths.get(0));
			if (lastElement.getElement() instanceof TaskEnd) {
				return true;
			} else if (lastElement instanceof SubProcessGroup) {
				return shouldStopFindTask((SubProcessGroup) lastElement, findType);
			}
		}
		return false;
	}

	private Map<ProcessElement, List<AnalysisPath>> correctInternalPathByNextSequence(Map<ProcessElement, List<AnalysisPath>> source) {
		Map<ProcessElement, List<AnalysisPath>> result = new LinkedHashMap<>();

		for (Entry<ProcessElement, List<AnalysisPath>> entry : source.entrySet()) {
			List<AnalysisPath> paths = correctInternalPathByNextSequence(entry.getValue());
			result.put(entry.getKey(), paths);
		}
		return result;
	}
	
	private List<AnalysisPath> correctInternalPathByNextSequence(List<AnalysisPath> paths) {
		List<AnalysisPath> result = new ArrayList<>();
		
		for (AnalysisPath path : paths) {
			int lengh = path.getElements().size();
			List<ProcessElement> elements = new ArrayList<>();
			for (int i = 0; i < lengh; i++) {
				ProcessElement element = path.getElements().get(i); 
				ProcessElement nextElement = i < lengh - 1 ? path.getElements().get(i + 1) : null;
				
				if (element instanceof SubProcessGroup) {
					SubProcessGroup subProcessGroup = (SubProcessGroup) element;
					List<AnalysisPath> subPaths = subProcessGroup.getInternalPaths();
					subPaths = getAnalysisPathBaseOnNextSequenceFlow(subPaths, nextElement);
					
					SubProcessGroup subElement = new SubProcessGroup((EmbeddedProcessElement) element.getElement(), subPaths);
					elements.add(subElement);
					
				} else {
					elements.add(element);
				}
			}
			result.add(new AnalysisPath(elements));
		}
		
		return result;
	}
	private List<AnalysisPath> getAnalysisPathBaseOnNextSequenceFlow(List<AnalysisPath> internalPaths, ProcessElement nextElement) {		
		if(nextElement == null) {
			return internalPaths;
		}
		
		List<AnalysisPath> result = new ArrayList<>();
		BaseElement sequenceFlow = nextElement.getElement();
		if (sequenceFlow instanceof SequenceFlow) {
			for (AnalysisPath path : internalPaths) {				
				ProcessElement lastProcessElement = AnalysisPathHelper.getLastElement(path);
				BaseElement lastElement = lastProcessElement.getElement();

				if (processGraph.isTaskEnd(lastElement)) {
					result.add(path);
					continue;
				}

				if (processGraph.isTaskSwitchGateway(lastElement)) {
					Map<SequenceFlow, List<AnalysisPath>> validInternalPaths = new LinkedHashMap<>();
					((TaskParallelGroup) lastProcessElement).getInternalPaths().forEach((key, value) -> {
						var validPaths = getAnalysisPathBaseOnNextSequenceFlow(value, nextElement);
						if (isNotEmpty(validPaths)) {
							validInternalPaths.put(key, validPaths);
						}
					});

					int size = path.getElements().size();
					List<ProcessElement> newPath = path.getElements().stream().limit(size - 1).collect(Collectors.toList());
					if (MapUtils.isNotEmpty(validInternalPaths)) {
						TaskParallelGroup taskGroup = new TaskParallelGroup(lastElement);
						taskGroup.setInternalPaths(validInternalPaths);
						newPath.add(taskGroup);	
					}
					result.add(new AnalysisPath(newPath));
					continue;
				}
				
				if (processGraph.isEmbeddedEnd(lastElement)) {
					if (processGraph.isConnectOuterOf((EmbeddedEnd) lastElement, (SequenceFlow) sequenceFlow)) {
						result.add(path);
					}
					continue;
				}
			}
		}
		return result;
	}
}
