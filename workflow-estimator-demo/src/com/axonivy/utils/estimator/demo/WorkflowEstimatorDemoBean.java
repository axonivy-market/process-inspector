package com.axonivy.utils.estimator.demo;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.demo.constant.FindType;
import com.axonivy.utils.estimator.demo.model.Estimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@SuppressWarnings("restriction")
public class WorkflowEstimatorDemoBean {
	private static final List<String> PROCESS_FOLDERS = Arrays.asList("Bussiness Processes");

	private List<Estimator> estimators = new ArrayList<>();
	
	private List<Process> processes = emptyList();
	
	private Estimator seletedEstimator = null;

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

	public void onAddEstimator() {
		estimators.add(new Estimator());
	}
	
	public void onDeleteEstimator(Estimator estimator) {
		estimators.remove(estimator);
	}	
	
	public Estimator getSeletedEstimator() {
		return seletedEstimator;
	}

	public void setSeletedEstimator(Estimator seletedEstimator) {
		this.seletedEstimator = seletedEstimator;
	}

	public void onSelectedProcess() {
		
	}

	public List<SingleTaskCreator> getAllTaskModifier(Process process) {
		return process.getElements().stream()
			.filter(item -> item instanceof SingleTaskCreator)
			.map(SingleTaskCreator.class::cast)
			.toList();
	}
	
	public List<FindType> getAllFindType(){
		return  Arrays.stream(FindType.values()).toList();
	}
	
	public List<EstimatedTask> getEstimatedTask(Estimator estimator) throws Exception{
		var workflowEstimator = new WorkflowEstimator(estimator.getProcess(), null, estimator.getFlowName());
		List<EstimatedTask> estimatedTasks = null;
		if(FindType.ALL_TASK.equals(estimator.getFindType())) {
			estimatedTasks = workflowEstimator.findAllTasks(estimator.getStartElement());
		} else {
			estimatedTasks = workflowEstimator.findTasksOnPath(estimator.getStartElement());
		}
		
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
	
	private boolean isAcceptedProcess(List<String>folders, String fullQualifiedName) {
		return folders.stream().anyMatch(folder -> fullQualifiedName.contains(folder));
	}
}
