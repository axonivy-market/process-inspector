package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
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
			pathByFlowName = paths.stream().filter(path -> hasFlowName(path, flowName)).toList();
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
	
	private boolean hasFlowName(List<BaseElement> elements, String flowName) {
		if (StringUtils.isBlank(flowName)) {
			return true;
		}

		boolean existsFlowName = false;
		for (BaseElement el : elements) {
			if (el instanceof SequenceFlow) {
				String label = ((SequenceFlow) el).getEdge().getLabel().getText();
				if(StringUtils.isNotBlank(label)) {
					//Have label but not contain flowName -> Exit check
					if(!label.contains(flowName)) {
						return false;
					} else {
						existsFlowName = true;
					}
				}
			}
		}

		return existsFlowName;
	}
	
	private boolean isAllEmptyCondition(List<BaseElement> elements) {

		for (int i = 1; i < elements.size(); i++) {
			var currentElement = elements.get(i);
			var previousElement = elements.get(i - 1);
			if (previousElement instanceof Alternative) {
				String currentElementId = currentElement.getPid().getFieldId();
				String nextTargetId = getNextTargetIdByCondition((Alternative) previousElement, StringUtils.EMPTY);
				if (!Objects.equal(currentElementId, nextTargetId)) {
					return false;
				}
			}
		}
		
		return true;
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
}
