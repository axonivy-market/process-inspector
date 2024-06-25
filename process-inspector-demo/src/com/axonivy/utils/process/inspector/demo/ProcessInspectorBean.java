package com.axonivy.utils.process.inspector.demo;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.component.selectoneradio.SelectOneRadio;
import org.primefaces.model.FilterMeta;

import com.axonivy.utils.process.inspector.APAConfig;
import com.axonivy.utils.process.inspector.ProcessInspector;
import com.axonivy.utils.process.inspector.demo.constant.FindType;
import com.axonivy.utils.process.inspector.demo.constant.UseCase;
import com.axonivy.utils.process.inspector.demo.helper.DateTimeHelper;
import com.axonivy.utils.process.inspector.demo.model.Analyzer;
import com.axonivy.utils.process.inspector.internal.AdvancedProcessInspector;
import com.axonivy.utils.process.inspector.model.DetectedAlternative;
import com.axonivy.utils.process.inspector.model.DetectedElement;
import com.axonivy.utils.process.inspector.model.DetectedTask;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.EmbeddedProcess;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.event.start.value.RequestPath;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

public class ProcessInspectorBean {

	private List<Process> processes = emptyList();
	
	private List<FilterMeta> filterBy;
	
	private Analyzer selectedAnalyzer = null;

	ProcessInspector processInspector;

	public ProcessInspectorBean() {
		this.processes = getAllProcesses();
	}

	public List<Process> getProcesses() {
		return processes;
	}

	public Analyzer getSelectedAnalyzer() {
		return selectedAnalyzer;
	}

	public void setSelectedAnalyzer(Analyzer selectedAnalyzer) {
		this.selectedAnalyzer = selectedAnalyzer;
	}
	
	public List<FilterMeta> getFilterBy() {
		return filterBy;
	}

	public void setFilterBy(List<FilterMeta> filterBy) {
		this.filterBy = filterBy;
	}

	@PostConstruct
	public void init() {
		filterBy = new ArrayList<>();
	}	 
	public List<SingleTaskCreator> getAllTaskModifier() {
		this.processInspector = new AdvancedProcessInspector();
		return getElementOfProcess(this.selectedAnalyzer.getProcess()).stream()
				.filter(item -> item instanceof SingleTaskCreator).map(SingleTaskCreator.class::cast).toList();
	}

	public List<FindType> getAllFindType() {
		return Arrays.stream(FindType.values()).toList();
	}

	public List<UseCase> getAllUseCases() {
		return Arrays.stream(UseCase.values()).toList();
	}

	public List<DetectedAlternative> getALternativeWithMoreThanOneOutgoing() throws Exception {
		APAConfig.handleAsTask();
		processInspector.enableDescribeAlternativeElements();

		List<DetectedAlternative> alternatives = processInspector.findAllTasks(selectedAnalyzer.getStartElement(), null)
				.stream().filter(item -> item instanceof DetectedAlternative).map(DetectedAlternative.class::cast)
				.filter(item -> item.getOptions().size() > 1).toList();
		return alternatives;
	}

	public void onSelectSequenceFlow(AjaxBehaviorEvent event) {
		if (event.getSource() != null) {
			DetectedElement newSequenceFlow = (DetectedElement) ((SelectOneRadio) event.getSource()).getValue();
			String sequenceFlowId = newSequenceFlow.getPid();

			SequenceFlow sequenceFlow = getElementOfProcess(selectedAnalyzer.getProcess()).stream()
					.filter(it -> it.getPid().getRawPid().equals(sequenceFlowId))
					.filter(it -> it instanceof SequenceFlow == true).map(SequenceFlow.class::cast).findFirst()
					.orElse(null);

			if (sequenceFlow != null) {
				if (sequenceFlow.getSource() instanceof Alternative) {
					Alternative alternative = (Alternative) sequenceFlow.getSource();
					selectedAnalyzer.getAlternativeFlows().put(alternative, sequenceFlow);
				}
			}
		}
	}

	public List<DetectedElement> getDetectedTask() throws Exception {
		UseCase useCase = selectedAnalyzer.getUseCase();
		String flowName = selectedAnalyzer.getFlowName();
		SingleTaskCreator startElement = selectedAnalyzer.getStartElement();
		processInspector = updateProcessInspector(selectedAnalyzer);

		long startTime = System.currentTimeMillis();
		List<?> detectedElements = null;
		if (FindType.ALL_TASK.equals(selectedAnalyzer.getFindType())) {
			detectedElements = processInspector.findAllTasks(startElement, useCase);
		} else {
			detectedElements = processInspector.findTasksOnPath(startElement, useCase, flowName);
		}

		long executionTime = System.currentTimeMillis() - startTime;
		selectedAnalyzer.setExecutionTime(executionTime);

		return detectedElements.stream().map(DetectedElement.class::cast).toList();
	}

	public Duration getDetectedTaskCalculate() throws Exception {
		UseCase useCase = selectedAnalyzer.getUseCase();
		String flowName = selectedAnalyzer.getFlowName();
		SingleTaskCreator startElement = selectedAnalyzer.getStartElement();
		processInspector = updateProcessInspector(selectedAnalyzer);
		Duration total = Duration.ZERO;
		if (FindType.ALL_TASK.equals(selectedAnalyzer.getFindType())) {
			total = processInspector.calculateWorstCaseDuration(startElement, useCase);
		} else {
			total = processInspector.calculateDurationOfPath(startElement, useCase, flowName);
		}

		return total;
	}

	public String getDisplayDuration(Duration duration) {
		return DateTimeHelper.getDisplayDuration(duration);
	}

	public String getDisplayDetectedElement(DetectedElement element) {
		String elementName = defaultIfEmpty(element.getElementName(), "No name");
		String shortPid = getShortPid(element.getPid());
		return String.format("%s (%s)", elementName, shortPid);
	}

	public String getProcessWebLink(SingleTaskCreator startElement) throws Exception {
		IWebStartable webStartable = null;		
		List<RequestStart> listStartElement = getStartElementsOfProcess(this.selectedAnalyzer.getProcess());

		RequestPath requestPath = null;
		if(startElement instanceof RequestStart) { 
			requestPath = listStartElement.stream()
					.filter(it -> it.getName().equals(startElement.getName()))
					.map(RequestStart::getRequestPath)
					.findFirst()
					.orElse(null);	
		} else {
			for (RequestStart startNode : listStartElement) {
				List<DetectedElement> listElements = this.processInspector.findAllTasks(startNode, null);
				boolean isExist = listElements.stream().anyMatch(it -> it.getElementName().equals(startElement.getName()));
				if (isExist) {
					requestPath = startNode.getRequestPath();
					break;
				}
			}	
		}
		
		final String startName = requestPath != null ? requestPath.getLinkPath() : StringUtils.EMPTY;
		webStartable = Ivy.session().getStartables().stream()
				.filter(it -> it.getLink().toRelativeUri().getPath().contains(startName)).findFirst()
				.orElse(null);

		if (webStartable != null) {
			return ProcessViewer.of((IProcessWebStartable) webStartable).url().toWebLink().getRelative();
		}
		return null;
	}

	public String getShortPid(String pid) {
		int index = pid.indexOf("-");
		return pid.substring(index + 1);
	}

	public List<String> getParentElementNames(){
		if(selectedAnalyzer != null) {
			return selectedAnalyzer.getTasks().stream()
					.filter(DetectedTask.class::isInstance)
					.map(DetectedTask.class::cast)
					.map(DetectedTask::getDisplayParentElementNames)
					.distinct()
					.toList();
		}
		return emptyList();
	}
	private ProcessInspector updateProcessInspector(Analyzer analyzer) {
		Map<String, String> flowOverrides = getProcessFlowOverride(analyzer);
		processInspector.disableDescribeAlternativeElements();
		processInspector.setProcessFlowOverrides(flowOverrides);
		return processInspector;
	}

	private Map<String, String> getProcessFlowOverride(Analyzer analyzer) {
		Map<Alternative, SequenceFlow> alternativeFlows = analyzer.getAlternativeFlows();
		Map<String, String> processFlowOverride = new HashMap<String, String>();

		for (Alternative item : alternativeFlows.keySet()) {
			if (alternativeFlows.get(item) != null) {
				processFlowOverride.put(item.getPid().getRawPid(), alternativeFlows.get(item).getPid().getRawPid());
			}
		}
		return processFlowOverride;
	}

	private List<Process> getAllProcesses() {
		List<Process> processes = IProcessManager.instance().getProjectDataModels().stream()
				.flatMap(project -> project.getProcesses().stream()).map(IProcess::getModel).toList();
		return processes;
	}

	private static List<BaseElement> getElementOfProcess(Process process) {
		var processElements = process.getProcessElements();
		var childElments = getElementOfProcesses(processElements);
		var elements = process.getElements();
		elements.addAll(childElments);

		return elements;
	}

	private List<RequestStart> getStartElementsOfProcess(Process process) {
		return getElementOfProcess(process).stream().filter(item -> item instanceof RequestStart).map(RequestStart.class::cast).toList();
	}

	private static List<BaseElement> getElementOfProcesses(List<ProcessElement> processElements) {
		if (processElements.isEmpty()) {
			return emptyList();
		}
		var embeddedProcess = processElements.stream().filter(it -> it instanceof EmbeddedProcessElement == true)
				.map(EmbeddedProcessElement.class::cast).map(it -> it.getEmbeddedProcess())
				.collect(Collectors.toList());

		var elememets = embeddedProcess.stream().map(EmbeddedProcess::getElements).flatMap(List::stream)
				.collect(Collectors.toList());

		var childProcessElements = embeddedProcess.stream().map(EmbeddedProcess::getProcessElements)
				.flatMap(List::stream).collect(Collectors.toList());

		var childElememts = getElementOfProcesses(childProcessElements);

		elememets.addAll(childElememts);

		return elememets;
	}
}
