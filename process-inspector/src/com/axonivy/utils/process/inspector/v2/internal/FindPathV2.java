//package com.axonivy.utils.process.inspector.v2.internal;
//
//import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.addAllToPath;
//import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.addToPath;
//import static com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper.convertToAnalysisPath;
//import static java.util.Collections.emptyList;
//import static java.util.Collections.emptyMap;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import com.axonivy.utils.process.inspector.internal.ProcessGraph;
//import com.axonivy.utils.process.inspector.internal.PathFinder.FindType;
//import com.axonivy.utils.process.inspector.internal.helper.AnalysisPathHelper;
//import com.axonivy.utils.process.inspector.internal.model.AnalysisPath;
//import com.axonivy.utils.process.inspector.internal.model.CommonElement;
//import com.axonivy.utils.process.inspector.internal.model.ProcessElement;
//import com.axonivy.utils.process.inspector.internal.model.SubProcessGroup;
//import com.axonivy.utils.process.inspector.v2.model.ProcessNode;
//
//import ch.ivyteam.ivy.process.model.BaseElement;
//import ch.ivyteam.ivy.process.model.NodeElement;
//import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
//import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
//import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
//import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
//
//public class FindPathV2 {
//	private enum FindType {
//		ALL_TASKS, TASKS_ON_PATH
//	};
//	
//	
//	private ProcessGraph processGraph;
//	private List<ProcessElement> froms;
//	private String flowName;
//	private Map<String, String> processFlowOverrides = emptyMap();
//	
//	private List<ProcessNode> allPath = new ArrayList<>();
//	
//	public FindPathV2() {
//		this.processGraph = new ProcessGraph();
//	}
//	
//	private void findAnalysisPaths(NodeElement from, String flowName, FindType findType) throws Exception {
//		
//		// Prevent loop
////		if (AnalysisPathHelper.isContains(currentPath, from)) {
////			return path;
////		}
//
//		path = addAllToPath(path, List.of(from));
//
//		if (from.getElement() instanceof NodeElement) {
//		
//			
//			var newPath = addToPath(currentPath, path);
//			//It stop finding tasks when the end node is TaskEnd for TASKS_ON_PATH case
//	
//			// Call recursion for next normal node
//			var pathOptions = findAnalysisPathForNextNode(from, flowName, findType, newPath);
//			path = addToPath(path, pathOptions);	
//		}
//		
//		return path;
//	}
//	
//	private List<AnalysisPath> findAnalysisPathForNextNode(ProcessNode from, String flowName, FindType findType) throws Exception {	
//		List<SequenceFlow> outs = getSequenceFlows((NodeElement) from.getElement(), flowName, findType);
//		
//		if (from.getElement() instanceof Alternative && outs.isEmpty()) {
//			String mgs = String.format("Not found path after element: \"%s\"", processGraph.getAlternativeNameId(from.getElement()));
//			throw new Exception(mgs); 
//		}
//
//		var paths = findAnalysisPaths(outs, flowName, findType, currentPath);
//
//		return convertToAnalysisPath(paths);
//	}
//	
//	private Map<SequenceFlow, List<AnalysisPath>> findAnalysisPaths (List<SequenceFlow> outs, String flowName, FindType findType,  List<AnalysisPath> currentPath) throws Exception {
//		Map<SequenceFlow, List<AnalysisPath>> pathOptions = new LinkedHashMap<>();
//		for (SequenceFlow out : outs) {
//			CommonElement outElement = new CommonElement(out);
//			List<AnalysisPath> newPath = addAllToPath(currentPath, Arrays.asList(outElement));
//			
//			ProcessElement nextStartElement = new CommonElement(out.getTarget());
//			List<AnalysisPath> nextPaths = findAnalysisPaths(nextStartElement, flowName, findType, newPath);
//			pathOptions.put(out, nextPaths);
//		}
//		return pathOptions;
//	}
//	
//	private List<SequenceFlow> getSequenceFlows(NodeElement from, String flowName, FindType findType) throws Exception {
//		if (findType == FindType.ALL_TASKS) {
//			return from.getOutgoing();
//		}
//
//		if (from instanceof TaskSwitchGateway && from != null) {
//			return from.getOutgoing();
//		}
//
//		Optional<SequenceFlow> flow = Optional.empty();
//
//		// Always is priority check flow from flowOverrides first.
//		if (from instanceof Alternative) {
//			String flowIdFromOrverride = this.processFlowOverrides.get(from.getPid().getRawPid());
//			flow = from.getOutgoing().stream()
//					.filter(out -> out.getPid().getRawPid().equals(flowIdFromOrverride))
//					.findFirst();
//		}
//
//		// If it don't find out the flow from flowOverrides, it is base on the default
//		// flow in process
//		if (flow.isEmpty()) {
//			flow = getSequenceFlow(from, flowName);
//		}
//
//		return flow.map(Arrays::asList).orElse(emptyList());
//	}
//	
//	private ProcessNode findFrom()
//}
