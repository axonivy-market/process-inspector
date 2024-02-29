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
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;

import com.google.common.base.Objects;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
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
	 * Using Recursion Algorithm To Find All The Path On Graph.
	 * Return all paths
	 */
	public List<List<BaseElement>> findPaths(BaseElement from) {
		List<BaseElement> path = findPath(from, emptyList());
		if (path.isEmpty()) {
			return emptyList();
		}
		return Arrays.asList(path);
	}
	
	public List<List<BaseElement>> findPaths(BaseElement from, String flowName) {
		List<BaseElement> path = findPathByFlowName(from, flowName);
		if (path.isEmpty()) {
			return emptyList();
		}
		return Arrays.asList(path);
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
		return Objects.equal(currentElementId, nextTargetId);
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
	
	
	/**
	 * Find base on start node and check condition at each
	 */
	public List<BaseElement> findPath(BaseElement from, List<BaseElement> previousElements) {
	
		//Prevent loop
		if (previousElements.indexOf(from) >= 0) {
			return emptyList();
		}

		List<BaseElement> path = new ArrayList<>();
		path.add(from);
		
		if (from instanceof NodeElement) {
			List<SequenceFlow> outs = ((NodeElement) from).getOutgoing();
			
			Map<SequenceFlow, List<BaseElement>> paths =  new HashedMap<>();
			for (SequenceFlow out : outs) {
				List<BaseElement> currentPath = ListUtils.union(previousElements, Arrays.asList(from));
				paths.put(out, findPath(out.getTarget(), currentPath));			
			}
			
			var longestPath = paths.entrySet().stream()
					.max(Comparator.comparingInt(entry -> entry.getValue().size()))
					.orElse(null);
			
			if(longestPath != null) {
				path.add(longestPath.getKey());
				path.addAll(longestPath.getValue());
			}
		}
		
		return path;
	}

	public List<BaseElement> findPathByFlowName(BaseElement from, String flowName) {
		List<BaseElement> path = new ArrayList<>();
		path.add(from);
		
		if (from instanceof NodeElement) {
			NodeElement target = (NodeElement) from;
			while (target.getOutgoing().size() > 0) {
				
				SequenceFlow flow = null;			
				if(target instanceof EmbeddedProcessElement) {
					List<BaseElement> subPaths = findPathOfSubProcessByFlowName((EmbeddedProcessElement)target, flowName);
					path.addAll(subPaths);
					flow = findCorrectSequenceFlow(target, flowName);
				} else if(target instanceof TaskSwitchGateway) {					
					List<BaseElement> parallelTasks = findPathOfTaskSwitchGatewayByFlowName((TaskSwitchGateway) target, flowName);
					path.addAll(parallelTasks);
					flow = ((NodeElement) parallelTasks.get(parallelTasks.size() - 1)).getOutgoing().stream().findFirst().orElse(null);					
				} else {					
					flow = findCorrectSequenceFlow(target, flowName);
				}
				 
				if(flow == null) {
					if(!(path.get(path.size() - 1) instanceof TaskEnd)) {
						//Can not find any way
						path.clear();	
					}	
					break;
				}
				
				path.add(flow);
				
				target = flow.getTarget();
				//Prevent loop
				if (path.indexOf(target) >= 0) {
					break;
				}
				path.add(target);
			}	
		}
		
		return path.stream().distinct().toList();
	}
	
	/**
	 * Find path on sub process
	 */
	private List<BaseElement> findPathOfSubProcessByFlowName(EmbeddedProcessElement subProcessElement, String flowName) {
		// find start element
		BaseElement start = findStartElementOfProcess(subProcessElement.getEmbeddedProcess());
		var listElement = findPathByFlowName(start, flowName);
		
		return listElement;
	}
	
	private BaseElement findStartElementOfProcess(Process subProcess) {
		BaseElement start = subProcess.getElements().stream().filter(item -> item instanceof StartEvent).findFirst().orElse(null);
		return start;
	}
	
	private List<BaseElement> findPathOfTaskSwitchGatewayByFlowName(TaskSwitchGateway from, String flowName) {
		List<List<BaseElement>> paths = new ArrayList<List<BaseElement>>();

		List<SequenceFlow> outs = from.getOutgoing();
		for (SequenceFlow out : outs) {

			List<BaseElement> path = new ArrayList<>();
			path.add(out);

			NodeElement target = out.getTarget();
			while (target.getOutgoing().size() > 0) {
				path.add(target);
				
				SequenceFlow flow = null;
				if(target instanceof TaskSwitchGateway) {
					List<BaseElement> subTasks = findPathOfTaskSwitchGatewayByFlowName((TaskSwitchGateway) target, flowName);					
					path.addAll(subTasks);					
				} else {
					flow = findCorrectSequenceFlow(target, flowName);					
				}
				
				if(flow == null) {
					target = null;
					break;
				}
				
				path.add(flow);
				
				target = flow.getTarget();
				// Prevent loop
				if (path.indexOf(target) >= 0) {
					break;
				}
			}
			
			if(target != null) {
				path.add(target);	
			}
			
			paths.add(path);
		}

		List<BaseElement> elements = paths.stream()
				.sorted(Comparator.comparing(List::size,  Comparator.reverseOrder()))
				.flatMap(List::stream)
				.collect(Collectors.toList());
		
		return elements;
	}
	
	private SequenceFlow findCorrectSequenceFlow(NodeElement nodeElement, String flowName) {
		List<SequenceFlow> outs = nodeElement.getOutgoing();
		//High priority for checking default path if flowName is null
		if(nodeElement instanceof Alternative) {
			if(isEmpty(flowName)) {
				return outs.stream().filter(out -> isDefaultPath(out)).findFirst().orElse(null);
			} else {
				SequenceFlow flow = outs.stream().filter(out -> hasFlowName(out, flowName)).findFirst().orElse(null);
				if(flow == null) {
					flow = outs.stream().filter(out -> isDefaultPath(out)).findFirst().orElse(null);
				}
				return flow;
			}
		}
		
		SequenceFlow flow = outs.stream()
				.filter(out -> hasFlowNameOrEmpty(out, flowName))
				.findFirst()
				.orElse(null);
			
		return flow;
	}
}
