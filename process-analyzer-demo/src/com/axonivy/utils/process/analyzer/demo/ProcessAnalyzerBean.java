package com.axonivy.utils.process.analyzer.demo;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.event.AjaxBehaviorEvent;

import org.primefaces.component.selectoneradio.SelectOneRadio;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import com.axonivy.utils.process.analyzer.demo.constant.FindType;
import com.axonivy.utils.process.analyzer.demo.constant.UseCase;
import com.axonivy.utils.process.analyzer.demo.helper.DateTimeHelper;
import com.axonivy.utils.process.analyzer.demo.model.Analyzer;
import com.axonivy.utils.process.analyzer.model.DetectedAlternative;
import com.axonivy.utils.process.analyzer.model.DetectedElement;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.EmbeddedProcess;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.start.IProcessWebStartable;
import ch.ivyteam.ivy.workflow.start.IWebStartable;

public class ProcessAnalyzerBean {
	private static final List<String> PROCESS_FOLDERS = Arrays.asList("Bussiness Processes");

	private List<Analyzer> analyzers = new ArrayList<>();
	
	private List<Process> processes = emptyList();
	
	private Analyzer selectedAnalyzer = null;
	
	AdvancedProcessAnalyzer processAnalyzer;

	public ProcessAnalyzerBean() {
		this.processes = getAllProcesses();
	}
	
	public List<Analyzer> getAnalyzers() {
		return analyzers;
	}

	public void setAnalyzers(List<Analyzer> analyzers) {
		this.analyzers = analyzers;
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

	public List<SingleTaskCreator> getAllTaskModifier() {
		this.processAnalyzer = new AdvancedProcessAnalyzer();
		return  getElementOfProcess(this.selectedAnalyzer.getProcess()).stream()
			.filter(item -> item instanceof SingleTaskCreator)
			.map(SingleTaskCreator.class::cast)
			.toList();
	}
	
	public List<FindType> getAllFindType(){
		return  Arrays.stream(FindType.values()).toList();
	}
	
	public List<UseCase> getAllUseCases(){
		return  Arrays.stream(UseCase.values()).toList();
	}
	
	public List<DetectedAlternative> getALternativeWithMoreThanOneOutgoing() throws Exception {
		
		processAnalyzer.enableDescribeAlternativeElements();
		
		List<DetectedAlternative> alternatives = processAnalyzer.findAllTasks(selectedAnalyzer.getStartElement(), null).stream()
				.filter(item -> item instanceof DetectedAlternative)
				.map(DetectedAlternative.class::cast)
				.filter(item -> item.getOptions().size() > 1)
				.toList();
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
		processAnalyzer = updateProcessAnalyzer(selectedAnalyzer);
		
		long startTime = System.currentTimeMillis();
		List<?> detectedElements = null;
		if (FindType.ALL_TASK.equals(selectedAnalyzer.getFindType())) {
			detectedElements = processAnalyzer.findAllTasks(startElement, useCase);
		} else {
			detectedElements = processAnalyzer.findTasksOnPath(startElement, useCase, flowName);
		}

		long executionTime = System.currentTimeMillis() - startTime;
		selectedAnalyzer.setExecutionTime(executionTime);

		return detectedElements.stream().map(DetectedElement.class::cast).toList();
	}

	public Duration getDetectedTaskCalculate() throws Exception{
		UseCase useCase = selectedAnalyzer.getUseCase();
		String flowName = selectedAnalyzer.getFlowName();
		SingleTaskCreator startElement = selectedAnalyzer.getStartElement();
		processAnalyzer = updateProcessAnalyzer(selectedAnalyzer);
		Duration total = Duration.ZERO;
		if (FindType.ALL_TASK.equals(selectedAnalyzer.getFindType())) {
			total = processAnalyzer.calculateWorstCaseDuration(startElement, useCase);	
		} else {
			total = processAnalyzer.calculateDurationOfPath(startElement, useCase, flowName);
		}
				
		return total;
	}

	private AdvancedProcessAnalyzer updateProcessAnalyzer(Analyzer analyzer) {
		Map<String, String> flowOverrides = getProcessFlowOverride(analyzer);
		processAnalyzer.disableDescribeAlternativeElements();
		processAnalyzer.setProcessFlowOverrides(flowOverrides);		
		return processAnalyzer;
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
		var manager = IProcessManager.instance().getProjectDataModelFor(IProcessModelVersion.current());
		List<Process> processes = manager.search().find().stream()
				.map(start -> start.getRootProcess())
				.filter(process -> isAcceptedProcess(PROCESS_FOLDERS, process.getFullQualifiedName().getName()))
				.distinct()
				.collect(Collectors.toList());
		return processes;
	}
	
	public String getProcessWebLink() {
		String guid = this.selectedAnalyzer.getProcess().getPid().getProcessGuid();
		IWebStartable webStartable = Ivy.session().getStartables().stream().filter(it -> it.getLink().toRelativeUri().getPath().contains(guid)).findFirst().orElse(null);
		
		if(webStartable != null) {
			return ProcessViewer.of((IProcessWebStartable) webStartable).url().toWebLink().getRelative();	
		}	
		return null;
	}
	
	private boolean isAcceptedProcess(List<String>folders, String fullQualifiedName) {
		return folders.stream().anyMatch(folder -> fullQualifiedName.contains(folder));
	}
	
	private static List<BaseElement> getElementOfProcess (Process process) {
		var processElements = process.getProcessElements();
		var childElments = getElementOfProcesses(processElements);
		var elements = process.getElements();
		elements.addAll(childElments);
		
		return elements;
	}
	
	private static List<BaseElement> getElementOfProcesses (List<ProcessElement> processElements) {
		if(processElements.isEmpty()) {
			return emptyList();
		}
		var embeddedProcess = processElements.stream()
				.filter(it -> it instanceof EmbeddedProcessElement == true)
				.map(EmbeddedProcessElement.class::cast)
				.map(it -> it.getEmbeddedProcess())
				.collect(Collectors.toList());

		var elememets = embeddedProcess.stream()
				.map(EmbeddedProcess::getElements)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		
		var childProcessElements = embeddedProcess.stream()
				.map(EmbeddedProcess::getProcessElements)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		
		var childElememts = getElementOfProcesses(childProcessElements);
		
		elememets.addAll(childElememts);
		
		return elememets;
	}
	
	public String getDisplayDuration(Duration duration) {
		return DateTimeHelper.getDisplayDuration(duration);
	}
}
