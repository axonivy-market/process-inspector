package com.axonivy.utils.process.inspector.internal;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.inspector.model.ElementTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.EmbeddedProcess;
import ch.ivyteam.ivy.process.model.HierarchicElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.diagram.edge.DiagramEdge;
import ch.ivyteam.ivy.process.model.diagram.value.Label;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.IvyScriptExpression;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;
import ch.ivyteam.ivy.process.model.element.value.task.TaskIdentifier;

public class ProcessGraph {

	private enum Role {
		SYSTEM
	};

	public String getCodeLineByPrefix(TaskConfig task, String... prefix) {
		// strongly typed!
		String script = Optional.of(task.getScript()).orElse(EMPTY);
		return getCodeLineByPrefix(script, prefix);
	}

	public String getCodeLineByPrefix(String script, String... prefix) {
		String[] codeLines = script.split("\\n");
		String wfEstimateCode = Arrays.stream(codeLines).filter(line -> containPrefixs(line, prefix)).findFirst()
				.orElse(EMPTY);
		return wfEstimateCode;
	}

	public List<String> getParentElementNamesEmbeddedProcessElement(BaseElement parentElement) {	
		List<String> result = new ArrayList<>();
		if (parentElement instanceof EmbeddedProcessElement) {
			EmbeddedProcessElement processElement = (EmbeddedProcessElement) parentElement;
			List<String> parentElementNames = getParentElementNamesEmbeddedProcessElement(processElement.getParent());

			// Add parent element name at first
			result.addAll(parentElementNames);
			// Add child at last
			result.add(processElement.getName());
		}

		return result;
	}

	public EmbeddedStart findStartElementOfProcess(SequenceFlow sequenceFlow, EmbeddedProcessElement embeddedProcessElement) {		
		EmbeddedStart start = findStartElementOfProcess(embeddedProcessElement).stream()				
				.filter(it -> sequenceFlow == null || ((EmbeddedStart) it).getConnectedOuterSequenceFlow().equals(sequenceFlow))
				.findFirst()
				.map(EmbeddedStart.class::cast)
				.orElse(null);		
		return start;
	}

	public TaskConfig getStartTaskConfig(SequenceFlow sequenceFlow) {
		BaseElement taskSwitchGateway = sequenceFlow.getSource();
		TaskConfig taskConfig = null;
		if (taskSwitchGateway instanceof TaskSwitchGateway) {
			// ivp=="TaskA.ivp"
			String condition = sequenceFlow.getCondition();
			// "TaskA.ivp"
			String startTask = Arrays.stream(condition.split("==")).skip(1).limit(1).findFirst().orElse(null);

			taskConfig = ((TaskSwitchGateway) taskSwitchGateway).getAllTaskConfigs().stream()
					.filter(it -> startTask.contains(it.getTaskIdentifier().getTaskIvpLinkName())).findFirst()
					.orElse(null);
		}
		return taskConfig;
	}

	public boolean isSystemTask(BaseElement task) {
		if (task instanceof TaskAndCaseModifier) {
			return ((TaskAndCaseModifier) task).getAllTaskConfigs().stream()
					.anyMatch(it -> Role.SYSTEM.name().equals(it.getActivator().getName()));
		}
		return false;
	}

	public ElementTask getElementTask(SingleTaskCreator task) {
		return ElementTask.createSingle(task.getPid().getRawPid());
	}

	public ElementTask createElementTask(TaskAndCaseModifier task, TaskConfig taskConfig) {
		String pid = task.getPid().getRawPid();
		if (task instanceof TaskSwitchGateway) {
			String taskIdentifier = Optional.ofNullable(taskConfig).map(TaskConfig::getTaskIdentifier)
					.map(TaskIdentifier::getRawIdentifier).orElse(EMPTY);

			return ElementTask.createGateway(pid, taskIdentifier);
		} else {
			return ElementTask.createSingle(pid);
		}
	}

	public boolean isRequestStart(BaseElement element) {
		return element instanceof RequestStart;
	}

	public boolean isTaskAndCaseModifier(BaseElement element) {
		return element instanceof TaskAndCaseModifier;
	}

	public boolean isSingleTaskCreator(BaseElement element) {
		return element instanceof SingleTaskCreator;
	}

	public boolean isSequenceFlow(BaseElement element) {
		return element instanceof SequenceFlow;
	}

	public boolean isTaskSwitchGateway(BaseElement element) {
		return element instanceof TaskSwitchGateway;
	}

	public boolean isAlternative(BaseElement element) {
		return element instanceof Alternative;
	}

	public boolean isSubProcessCall(BaseElement element) {
		return element instanceof SubProcessCall;
	}

	public boolean isTaskEnd(BaseElement element) {
		return element instanceof TaskEnd;
	}

	public boolean isEmbeddedEnd(BaseElement element) {
		return element instanceof EmbeddedEnd;
	}
	
	public boolean isHandledAsTask(SubProcessCall subProcessCall) {
		return containPrefixs(subProcessCall.getParameters().getCode(), "APAConfig.handleAsTask");
	}

	public String getAlternativeNameId(BaseElement alternative) {
		return Stream.of(alternative.getName(), alternative.getPid().getRawPid()).filter(StringUtils::isNotEmpty)
				.collect(Collectors.joining("-"));
	}
	
	public SequenceFlow getFirstIncoming(BaseElement element) {		
		if(element instanceof NodeElement) {
			return ((NodeElement) element).getIncoming().stream().findFirst().orElse(null);			
		}
		return null;		
	}

	public List<SequenceFlow> getOutgoing(BaseElement element) {		
		if(element instanceof NodeElement) {
			return ((NodeElement) element).getOutgoing();			
		}
		return emptyList();		
	}
	
	public boolean isMultiIncoming(BaseElement element) {		
		if(element instanceof NodeElement) {
			return ((NodeElement) element).getIncoming().size() > 1;			
		}
		return false;		
	}
	
	public boolean isMultiIOutgoing(BaseElement element) {		
		if(element instanceof NodeElement) {
			return ((NodeElement) element).getOutgoing().size() > 1;			
		}
		return false;		
	}
	
	public List<SequenceFlow> getSequenceFlowOf(NodeElement from,  NodeElement startNode) {
		long numberOfStarts = countStartElement(from);
		List<SequenceFlow> incomings = from.getIncoming();
		
		List<SequenceFlow> sequenceFlows = emptyList();				
		if(numberOfStarts == 1) {
			//If there are only on start node -> just get incoming
			sequenceFlows = incomings;
		} else {			
			//TODO: Should find another solution to check
			sequenceFlows = incomings.stream().filter(it -> isStartedFromOf(startNode, it, emptyList())).toList();
		}
		return sequenceFlows;
	}
	

	public boolean hasFlowName(SequenceFlow sequenceFlow, String flowName) {
		String label = Optional.ofNullable(sequenceFlow)
				.map(SequenceFlow::getEdge)
				.map(DiagramEdge::getLabel)
				.map(Label::getText)
				.orElse(null);
		
		return isNotBlank(label) && label.contains(flowName);
	}

	public boolean isDefaultPath(SequenceFlow flow) {
		NodeElement sourceElement = flow.getSource();
		if (sourceElement instanceof Alternative) {
			return isDefaultPath((Alternative) sourceElement, flow);
		}

		return false;
	}

	public EmbeddedProcessElement getParentElement(BaseElement element) {
		if (element instanceof HierarchicElement) {
			var parentElement = ((HierarchicElement) element).getParent();
			if (parentElement instanceof EmbeddedProcessElement) {
				return (EmbeddedProcessElement) parentElement;
			}
		}
		return null;
	}
	
	public boolean isConnectOuterOf(EmbeddedEnd end, SequenceFlow sequenceFlow) {
		BaseElement outerSequenceFlow = ((EmbeddedEnd) end).getConnectedOuterSequenceFlow();
		return outerSequenceFlow.getPid().equals(sequenceFlow.getPid());
	}
	
	private boolean isDefaultPath(Alternative alternative, SequenceFlow sequenceFlow) {
		String currentElementId = sequenceFlow.getPid().getFieldIds();
		List<String> nextTargetIds = getNextTargetIdsByCondition(alternative, EMPTY);
		return nextTargetIds.contains(currentElementId);
	}
	
	private List<String> getNextTargetIdsByCondition(Alternative alternative, String condition) {
		IvyScriptExpression script = IvyScriptExpression.script(defaultString(condition));
		List<String> nextTargetIds = alternative.getConditions().conditions().entrySet().stream()
				.filter(entry -> script.equals(entry.getValue())).map(Entry::getKey).toList();
		
		return nextTargetIds;
	}
	
	private boolean isStartedFromOf(NodeElement startNode, SequenceFlow sequenceFlow, List<BaseElement> pathChecked) {
		if(startNode == null) {
			return false;
		}
		NodeElement node = sequenceFlow.getSource();
		if(pathChecked.contains(node)) {
			return false;
		}
		
		if(startNode.equals(node)) {
			return true;
		} 
		
		// Maybe have a loop here.
		List<SequenceFlow> sequenceFlows = node.getIncoming();
		for (SequenceFlow flow : sequenceFlows) {
			List<BaseElement> newPathChecked = union(pathChecked, List.of(node, flow));
			if (isStartedFromOf(startNode, flow, newPathChecked)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean containPrefixs(String content, String... prefix) {
		return List.of(prefix).stream().allMatch(it -> content.contains(it));
	}
	
	private List<EmbeddedStart> findStartElementOfProcess(EmbeddedProcessElement embeddedProcessElement) {
		EmbeddedProcess process = embeddedProcessElement.getEmbeddedProcess();
		List<EmbeddedStart> starts = process.getElements().stream()
				.filter(EmbeddedStart.class::isInstance)
				.map(EmbeddedStart.class::cast)
				.toList();	
		return starts;
	}
	
	private long countStartElement(BaseElement element) {
		if(element instanceof NodeElement) {
			BaseElement parent = ((NodeElement) element).getParent();
			if(parent instanceof  EmbeddedProcessElement) {
				return findStartElementOfProcess((EmbeddedProcessElement)parent).stream()
						.map(EmbeddedStart.class::cast)
						.filter(it -> it.getOutgoing().size() > 0)
						.count();
			} else {
				return ((NodeElement) element).getRootProcess().getElements().stream()
						.filter(RequestStart.class::isInstance)
						.count();
			}
		}
		return 0;
	}
}