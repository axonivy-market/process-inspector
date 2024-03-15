package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.estimator.constant.UseCase;

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
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public class ProcessGraph {
	public final Process process;
	
	private Map<String, Duration> durationOverrides = emptyMap();
	// It only impart to find task base in flowName
	private Map<String, String> processFlowOverrides = emptyMap() ;
	
	public ProcessGraph(Process process) {
		this.process = process;
	}
	
	public void setProcessFlowOverrides(HashMap<String, String> processFlowOverrides) {
		this.processFlowOverrides = processFlowOverrides;
	}
	
	public void setDurationOverrides(HashMap<String, Duration> durationOverrides) {
		this.durationOverrides = durationOverrides;
	}
	
	public List<BaseElement> findPath(BaseElement... from) throws Exception {
		List<BaseElement> path = findPath(Arrays.asList(from), null, true,  emptyList());		
		return path;
	}
	
	public List<BaseElement> findPath(String flowName, BaseElement... from) throws Exception {
		List<BaseElement> path = findPath(Arrays.asList(from), flowName, false, emptyList());
		return path;
	}
	
	public String getCustomInfoByCode(TaskConfig task) {
		String wfEstimateCode = getCodeLineByPrefix(task, "WfEstimate.setCustomInfo");
		String result = null;
		if (isNotEmpty(wfEstimateCode)) {
			result = StringUtils.substringBetween(wfEstimateCode, "(\"", "\")");
		}
		return result;
	}
	
	public String getTaskId(TaskAndCaseModifier task, TaskConfig taskConfig) {
		String id = task.getPid().getRawPid();
		if (task instanceof TaskSwitchGateway) {
			return id + "-" + taskConfig.getTaskIdentifier().getRawIdentifier();
		} else {
			return id;
		}
	}
	
	public Duration getDuration(TaskAndCaseModifier task, TaskConfig taskConfig, UseCase useCase) {
		String key = getTaskId(task, taskConfig);		
		return durationOverrides.getOrDefault(key, getDurationByTaskScript(taskConfig, useCase));	
	}
	
	public static boolean isSystemTask(TaskAndCaseModifier task) {		
		return task.getAllTaskConfigs().stream().anyMatch(it -> "SYSTEM".equals(it.getActivator().getName()));
	}
	
	public List<String> getParentElementNames(TaskAndCaseModifier task){
		List<String> parentElementNames = emptyList();
		if(task.getParent() instanceof EmbeddedProcessElement) {
			parentElementNames = getParentElementNamesEmbeddedProcessElement(task.getParent());
		}
		
		Collections.reverse(parentElementNames);
		return parentElementNames ;
	}
	
	private List<String> getParentElementNamesEmbeddedProcessElement(BaseElement parentElement){
		List<String> result = new ArrayList<>();	
		if(parentElement instanceof EmbeddedProcessElement) {
			result.add(parentElement.getName());
			
			List<String> parentElementNames = getParentElementNamesEmbeddedProcessElement(((EmbeddedProcessElement)parentElement).getParent());
			result.addAll(parentElementNames);
		}
		
		return result;
	}

	private List<BaseElement> findPath(List<BaseElement> froms, String flowName, boolean isFindAllTasks, List<BaseElement> previousElements) throws Exception {
		List<BaseElement> result = new ArrayList<>();
		for(BaseElement from : froms) {
			var path = findPath(from, flowName, isFindAllTasks, emptyList());
			result.addAll(path);
		}
				
		return result.stream().distinct().toList();
	}
	
	/**
	 * Using Recursion Algorithm To Find Tasks On Graph.
	 */
	private List<BaseElement> findPath(BaseElement from, String flowName, boolean isFindAllTasks, List<BaseElement> previousElements) throws Exception {
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
					//Consider to count all element of remove this sort (what flow is drawn first it will go first  
					.sorted(Map.Entry.comparingByValue(Comparator.comparing(ProcessGraph::countNumberAcceptedTasks, Comparator.reverseOrder())))
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
			Optional<SequenceFlow> flow = Optional.empty();
			
			//Always is priority check flow from flowOverrides first.
			if(from instanceof Alternative) {
				String flowIdFromOrverride = processFlowOverrides.get(from.getPid().getRawPid());
				flow = from.getOutgoing().stream().filter(out -> out.getPid().getRawPid().equals(flowIdFromOrverride)).findFirst();				
			}
			
			//If it don't find out the flow from flowOverrides, it is base on the default flow in process
			if(flow.isEmpty()) {
				flow = getSequenceFlow(from, flowName);
			}
			
			return flow.map(Arrays::asList).orElse(emptyList());
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
	
	private static long countNumberAcceptedTasks(List<BaseElement> listElements) {
		return listElements.stream().filter(item -> isAcceptedTask(item)).count();
	}
	
	private static boolean isAcceptedTask(BaseElement element) {
		return Optional.ofNullable(element)
				// filter to get task only
				.filter(node -> {
					return node instanceof TaskAndCaseModifier;
				}).map(TaskAndCaseModifier.class::cast)
				.filter(node -> {
					return node instanceof RequestStart == false;
				})
				// Remove SYSTEM task
				.filter(node -> isSystemTask(node) == false)
				.isPresent();
	}
	
	private String getCodeLineByPrefix(TaskConfig task, String... prefix) {
		// strongly typed!
		String script = Optional.of(task.getScript()).orElse(EMPTY);
		String[] codeLines = script.split("\\n");
		String wfEstimateCode = Arrays.stream(codeLines)
				.filter(line -> containtPrefixs(line, prefix))
				.findFirst()
				.orElse(EMPTY);
		return wfEstimateCode;
	}
	
	private boolean containtPrefixs(String content, String... prefix) {
		return List.of(prefix).stream().allMatch(it -> content.contains(it));
	}
	private Duration getDurationByTaskScript(TaskConfig task, UseCase useCase) {
		List<String> prefixs = new ArrayList<String>(Arrays.asList("WfEstimate.setEstimate"));
		if(useCase != null) {
			prefixs.add("UseCase." + useCase.name());
		}

		String wfEstimateCode = getCodeLineByPrefix(task, prefixs.toArray(new String[0]));
		if (StringUtils.isNotEmpty(wfEstimateCode)) {
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
}
