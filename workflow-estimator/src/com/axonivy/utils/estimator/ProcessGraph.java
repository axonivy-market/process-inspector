package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
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
	 * Exception: Throw stack overflow error if there are loop node 
	 */
	public List<List<BaseElement>> findPaths(BaseElement from) {
		return findAllPaths(from);
	}
	
	public List<List<BaseElement>> findPaths(BaseElement from, String flowName) {
		List<List<BaseElement>> paths = findAllPaths(from);

		List<List<BaseElement>> pathByFlowName = emptyList();
		if (isNotBlank(flowName)) {
			pathByFlowName = paths.stream().filter(path -> hasFlowNameOrDefaultPath(path, flowName)).toList();
		} else {
			pathByFlowName = paths.stream().filter(path -> isAllEmptyCondition(path)).toList();
		}
		//Maybe throw an exception if there are not path
		return pathByFlowName;
	}
	
	private List<List<BaseElement>> findAllPaths(BaseElement from) {
		List<List<BaseElement>> paths = findNextNodeElementPath(from);
		
		//Insert from element at first position
		paths.forEach(path -> {
			path.add(0, from);
		});
		
		return paths;
	}
	
	private List<List<BaseElement>> findNextNodeElementPath(BaseElement from) {
		var nexts = new ArrayList<List<BaseElement>>();

		if (from != null && from instanceof NodeElement) {
			List<SequenceFlow> outs = ((NodeElement) from).getOutgoing();
			for (SequenceFlow out : outs) {

				NodeElement target = out.getTarget();
				List<List<BaseElement>> nextElements = findNextNodeElementPath(target);

				if (nextElements.isEmpty()) {
					nextElements.add(new ArrayList<BaseElement>(Arrays.asList(out, target)));
				} else {
					nextElements.forEach(nodes -> {
						nodes.add(0, out);
						nodes.add(1, target);
					});
				}

				nexts.addAll(nextElements);
			}
		}
		return nexts;
	}
	
	private boolean hasFlowNameOrDefaultPath(List<BaseElement> elements, String flowName) {
		if (StringUtils.isBlank(flowName)) {
			return true;
		}

		for (int i = 0; i < elements.size(); i++) {
			var currentElement = elements.get(i);
			var previousElement = i > 0 ? elements.get(i - 1) : null;
			if (currentElement instanceof SequenceFlow) {
				var passedFlowNameCheck = hasFlowNameOrEmpty((SequenceFlow) currentElement, flowName);

				var passedDefaultPathCheck = false;
				if (previousElement instanceof Alternative) {
					passedDefaultPathCheck = isDefaultPath(currentElement, (Alternative) previousElement);
				}

				if (!passedFlowNameCheck && !passedDefaultPathCheck) {
					return false;
				}
			}
		}

		return true;
	}
	
	private boolean hasFlowNameOrEmpty(SequenceFlow sequenceFlow, String flowName) {

		String label = sequenceFlow.getEdge().getLabel().getText();
		if (isNotBlank(label)) {
			if (!label.contains(flowName)) {
				return false;
			}
		}

		return true;
	}

	private boolean isAllEmptyCondition(List<BaseElement> elements) {

		for (int i = 1; i < elements.size(); i++) {
			var currentElement = elements.get(i);
			var previousElement = elements.get(i - 1);
			if (previousElement instanceof Alternative) {				
				if (!isDefaultPath(currentElement, (Alternative) previousElement)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean isDefaultPath(BaseElement currentElement, Alternative previousElement) {
		String currentElementId = currentElement.getPid().getFieldId();
		String nextTargetId = getNextTargetIdByCondition((Alternative) previousElement, EMPTY);
		return Objects.equal(currentElementId, nextTargetId);
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
	 * @param from
	 * @param flowName
	 * @return
	 */
//	public List<BaseElement> findPathByFlowName(BaseElement from, String flowName) {
//		List<BaseElement> path = new ArrayList<>();
//		path.add(from);
//		
//		
//		if (from instanceof NodeElement) {
//			NodeElement currentNode = (NodeElement) from;
//			List<SequenceFlow> outs = currentNode.getOutgoing();
//			while (outs.size() > 0) {
//				SequenceFlow flow = findCorrectSequenceFlow(outs, flowName);
//				
//				if(flow == null) {
//					break;
//				}
//
//				path.add(from);
//				
//			}
//		}
//		return emptyList();
//	}
//	
//	private SequenceFlow findCorrectSequenceFlow(List<SequenceFlow> outs, String flowName) {
//		return outs.stream().filter(out -> hasFlowNameOrEmpty(out, flowName)).findFirst().orElse(null);
//	}
}
