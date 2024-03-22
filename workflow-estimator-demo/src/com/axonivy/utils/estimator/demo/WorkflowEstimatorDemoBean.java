package com.axonivy.utils.estimator.demo;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.demo.constant.FindType;
import com.axonivy.utils.estimator.demo.model.Estimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.EmbeddedProcess;
import ch.ivyteam.ivy.process.model.Process;
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

	public List<SingleTaskCreator> getAllTaskModifier(Process process) {
		return  getElementOfProcess(process).stream()
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
	
	public List<Alternative> getAllALternative(Process process){
		List<Alternative> alternatives = getElementOfProcess(process).stream()
				.filter(item -> item instanceof Alternative)
				.map(Alternative.class::cast)
				.filter(item -> item.getOutgoing().size() > 1)
				.toList();
		return alternatives;	
	}
	
	public List<EstimatedTask> getEstimatedTask(Estimator estimator) throws Exception{
		var workflowEstimator = new WorkflowEstimator(estimator.getProcess(), estimator.getUseCase(), estimator.getFlowName());
		List<EstimatedTask> estimatedTasks = null;
		long startTime = System.currentTimeMillis();
		if (FindType.ALL_TASK.equals(estimator.getFindType())) {
			estimatedTasks = workflowEstimator.findAllTasks(estimator.getStartElement()).stream()
					.map(EstimatedTask.class::cast).toList();
		} else {
			estimatedTasks = workflowEstimator.findTasksOnPath(estimator.getStartElement()).stream()
					.map(EstimatedTask.class::cast).toList();
		}
		
		long executionTime = System.currentTimeMillis() - startTime;
		estimator.setExecutionTime(executionTime);
		return estimatedTasks;
	}

	public Duration getEstimatedTaskCalculate(Estimator estimator) throws Exception{
		var workflowEstimator = new WorkflowEstimator(estimator.getProcess(), null, estimator.getFlowName());
		Duration total = Duration.ZERO;
		if(FindType.ALL_TASK.equals(estimator.getFindType())) {
			total = workflowEstimator.calculateEstimatedDuration(estimator.getStartElement());
		} else {
			total = workflowEstimator.calculateEstimatedDuration(estimator.getStartElement());
		}
		
		return total;
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
	
	public String getProcessWebLink(Process process) {
		String guid = process.getPid().getProcessGuid();
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
}
