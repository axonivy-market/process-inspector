package com.axonivy.utils.estimator.demo;

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

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.demo.constant.FindType;
import com.axonivy.utils.estimator.demo.helper.DateTimeHelper;
import com.axonivy.utils.estimator.demo.model.Estimator;
import com.axonivy.utils.estimator.model.EstimatedElement;

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

@SuppressWarnings("restriction")
public class WorkflowEstimatorDemoBean {
	private static final List<String> PROCESS_FOLDERS = Arrays.asList("Bussiness Processes");

	private List<Estimator> estimators = new ArrayList<>();
	
	private List<Process> processes = emptyList();
	
	private Estimator selectedEstimator = null;

	public WorkflowEstimatorDemoBean() {
		processes = getAllProcesses();
	}
	
	public List<Estimator> getEstimators() {
		return estimators;
	}

	public void setEstimators(List<Estimator> estimators) {
		this.estimators = estimators;
	}

	public List<Process> getProcesses() {
		return processes;
	}
	
	public Estimator getSelectedEstimator() {
		return selectedEstimator;
	}

	public void setSelectedEstimator(Estimator selectedEstimator) {
		this.selectedEstimator = selectedEstimator;
	}

	public List<SingleTaskCreator> getAllTaskModifier() {
		return  getElementOfProcess(this.selectedEstimator.getProcess()).stream()
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
	
	public List<Alternative> getALternativeWithMoreThanOneOutgoing(){
		List<Alternative> alternatives = getElementOfProcess(this.selectedEstimator.getProcess()).stream()
				.filter(item -> item instanceof Alternative)
				.map(Alternative.class::cast)
				.filter(item -> item.getOutgoing().size() > 1)
				.toList();
		return alternatives;	
	}
	
	public void onSelectSequenceFlow(AjaxBehaviorEvent event) {
		if (event.getSource() != null) {
			SequenceFlow newSequenceFlow = (SequenceFlow) ((SelectOneRadio) event.getSource()).getValue();
			if (newSequenceFlow != null) {
				if (newSequenceFlow.getSource() instanceof Alternative) {
					Alternative alternative = (Alternative) newSequenceFlow.getSource();
					selectedEstimator.getAlternativeFlows().put(alternative, newSequenceFlow);
				}
			}
		}
	}
	
	public List<EstimatedElement> getEstimatedTask() throws Exception {
		WorkflowEstimator workflowEstimator = createWorkflowEstimator(selectedEstimator);

		long startTime = System.currentTimeMillis();
		List<EstimatedElement> estimatedElements = null;
		if (FindType.ALL_TASK.equals(selectedEstimator.getFindType())) {
			estimatedElements = workflowEstimator.findAllTasks(selectedEstimator.getStartElement()).stream()
					.map(EstimatedElement.class::cast).toList();
		} else {
			estimatedElements = workflowEstimator.findTasksOnPath(selectedEstimator.getStartElement()).stream()
					.map(EstimatedElement.class::cast).toList();
		}

		long executionTime = System.currentTimeMillis() - startTime;
		selectedEstimator.setExecutionTime(executionTime);

		return estimatedElements;
	}

	public Duration getEstimatedTaskCalculate() throws Exception{
		WorkflowEstimator workflowEstimator = createWorkflowEstimator(selectedEstimator);
		
		Duration total = Duration.ZERO;
		if(FindType.ALL_TASK.equals(selectedEstimator.getFindType())) {
			total = workflowEstimator.calculateEstimatedDuration(selectedEstimator.getStartElement());
		} else {
			total = workflowEstimator.calculateEstimatedDuration(selectedEstimator.getStartElement());
		}
		return total;
	}

	private WorkflowEstimator createWorkflowEstimator(Estimator estimator) {
		WorkflowEstimator workflowEstimator = new WorkflowEstimator(selectedEstimator.getProcess(), selectedEstimator.getUseCase(), selectedEstimator.getFlowName());

		HashMap<String, String> flowOverrides = getProcessFlowOverride(selectedEstimator);
		workflowEstimator.setProcessFlowOverrides(flowOverrides);
		
		return workflowEstimator;
	}

	private HashMap<String, String> getProcessFlowOverride(Estimator estimator) {		
		Map<Alternative, SequenceFlow> alternativeFlows = selectedEstimator.getAlternativeFlows();
		HashMap<String, String> processFlowOverride = new HashMap<String, String>();

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
		String guid = this.selectedEstimator.getProcess().getPid().getProcessGuid();
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
