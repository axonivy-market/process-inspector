package com.axonivy.utils.estimator.demo;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.estimator.demo.model.Estimator;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskModifier;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@SuppressWarnings("restriction")
public class WorkflowEstimatorDemoBean {

	private List<Estimator> estimators = new ArrayList<>();

	public WorkflowEstimatorDemoBean() {

	}

	public void onAddEstimator() {
		estimators.add(new Estimator());
	}

	public List<TaskModifier> getAllTaskModifier(Process process) {

		return emptyList();
	}

	public List<Process> getAllProcesses() {
		var manager = IProcessManager.instance().getProjectDataModelFor(IProcessModelVersion.current());
		List<Process> templates = manager.search().find().stream()
				.map(start -> start.getRootProcess())
				// .map(process ->
				// process.search().type(EmbeddedProcessElement.class).findOne().getEmbeddedProcess())
				.collect(Collectors.toList());
		
		return templates;
	}
}
