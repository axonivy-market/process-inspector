package com.axonivy.utils.estimator.demo;

import static java.util.Collections.emptyList;

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
	
	public List<EstimatedTask> getEstimatedtask(Estimator estimator) throws Exception{
		var workflowEstimator = new WorkflowEstimator(estimator.getProcess(), null, estimator.getFlowName());
		List<EstimatedTask> estimatedTasks = null;
		if(FindType.ALL_TASK.equals(estimator.getFindType())) {
			estimatedTasks = workflowEstimator.findAllTasks(estimator.getStartElement());
		} else {
			estimatedTasks = workflowEstimator.findTasksOnPath(estimator.getStartElement());
		}
		
		return estimatedTasks;
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
