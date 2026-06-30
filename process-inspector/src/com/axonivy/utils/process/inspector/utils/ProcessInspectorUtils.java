package com.axonivy.utils.process.inspector.utils;

import java.util.List;

import ch.ivyteam.ivy.process.loader.ProcessLoader;
import ch.ivyteam.ivy.process.model.Process;

public class ProcessInspectorUtils {

	public static List<Process> getAllProcesses() {
		return loader().loadAll().toList();
	}

	public static Process getProcessByName(String processName) {
		return loader().loadByPath(processName).orElse(null);
	}

	private static ProcessLoader loader() {
		return ProcessLoader.current();
	}
}
