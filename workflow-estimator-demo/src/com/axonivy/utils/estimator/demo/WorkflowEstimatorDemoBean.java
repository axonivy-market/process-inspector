package com.axonivy.utils.estimator.demo;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.estimator.demo.model.Estimator;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskModifier;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@SuppressWarnings("restriction")
public class WorkflowEstimatorDemoBean {
	private static final List<String> PROCESS_FOLDERS = Arrays.asList("Bussiness Processes");

	private List<Estimator> estimators = new ArrayList<>();
	private List<Process> processes = emptyList();

	public WorkflowEstimatorDemoBean() {
		
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


	public void setProcesses(List<Process> processes) {
		this.processes = processes;
	}


	public void initData() {
		processes = getAllProcesses();
	}

	public void onAddEstimator() {
		estimators.add(new Estimator());
	}
	
	public void onSelectedProcess() {
		
	}

	public List<TaskModifier> getAllTaskModifier(Process process) {
		
		return emptyList();
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
