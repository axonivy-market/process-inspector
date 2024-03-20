package com.axonivy.utils.estimator;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.event.start.StartEvent;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.value.IvyScriptExpression;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
class ProcessGraph {

	protected ProcessGraph() {
	}
	
	protected String getCodeLineByPrefix(TaskConfig task, String... prefix) {
		// strongly typed!
		String script = Optional.of(task.getScript()).orElse(EMPTY);
		String[] codeLines = script.split("\\n");
		String wfEstimateCode = Arrays.stream(codeLines)
				.filter(line -> containtPrefixs(line, prefix))
				.findFirst()
				.orElse(EMPTY);
		return wfEstimateCode;
	}
	
	protected List<String> getParentElementNamesEmbeddedProcessElement(BaseElement parentElement){
		List<String> result = new ArrayList<>();
		if(parentElement instanceof EmbeddedProcessElement) {
			EmbeddedProcessElement processElement = (EmbeddedProcessElement)parentElement;
			List<String> parentElementNames = getParentElementNamesEmbeddedProcessElement(processElement.getParent());
			
			// Add parent element name at first
			result.addAll(parentElementNames);
			// Add child at last
			result.add(processElement.getName());
		}
		
		return result;
	}
	
	protected BaseElement findOneStartElementOfProcess(Process process) {
		BaseElement start = process.getElements().stream()
				.filter(item -> item instanceof StartEvent)
				.findFirst()
				.orElse(null);
		
		return start;
	}
	
	protected String getNextTargetIdByCondition(Alternative alternative, String condition) {
		IvyScriptExpression script = IvyScriptExpression.script(defaultString(condition));
		String nextTargetId = alternative.getConditions().conditions().entrySet().stream()
				.filter(entry -> script.equals(entry.getValue()))
				.findFirst()
				.map(Entry::getKey)
				.orElse(null);		
		
		return nextTargetId;
	}
	
	
	private boolean containtPrefixs(String content, String... prefix) {
		return List.of(prefix).stream().allMatch(it -> content.contains(it));
	}
	
}
