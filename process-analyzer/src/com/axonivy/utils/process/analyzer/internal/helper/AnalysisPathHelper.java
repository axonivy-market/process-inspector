package com.axonivy.utils.process.analyzer.internal.helper;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.ListUtils;

import com.axonivy.utils.process.analyzer.internal.model.AnalysisPath;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;

public class AnalysisPathHelper {
	public static List<AnalysisPath> addToPath(List<AnalysisPath> paths, List<AnalysisPath> subPaths) {
		if (subPaths.isEmpty()) {
			return paths;
		}

		List<AnalysisPath> result = paths;
		for (AnalysisPath path : subPaths) {
			result = addAllToPath(result, path.getElements());
		}

		return result;
	}

	public static List<AnalysisPath> addAllToPath(List<AnalysisPath> paths, List<ProcessElement> elements) {
		List<AnalysisPath> result = new ArrayList<>();
		if (paths.isEmpty()) {
			if (isNotEmpty(elements)) {
				result.add(new AnalysisPath(elements));
			}
		} else {
			paths.forEach(it -> {
				result.add(new AnalysisPath(ListUtils.union(it.getElements(), elements)));
			});
		}

		return result;
	}

	public static List<AnalysisPath> addAllToPath(List<AnalysisPath> paths, Map<SequenceFlow, List<AnalysisPath>> pathOptions) {
		List<AnalysisPath> result = new ArrayList<>();
		if (pathOptions.isEmpty()) {
			result.addAll(paths);
		} else {
			pathOptions.entrySet().forEach(it -> {
				ProcessElement sequenceFlowElement = new CommonElement(it.getKey());
				if (it.getValue().isEmpty()) {
					result.addAll(addAllToPath(paths, List.of(sequenceFlowElement)));
				} else {
					it.getValue().forEach(path -> {
						List<ProcessElement> elememts = ListUtils.union(List.of(sequenceFlowElement),
								path.getElements());

						result.addAll(addAllToPath(paths, elememts));
					});
				}
			});
		}
		return result;
	}
	

	public static List<AnalysisPath> replaceFirstElement(ProcessElement element, List<AnalysisPath> subPaths) {
		if (subPaths.isEmpty()) {
			return List.of(new AnalysisPath(List.of(element)));
		}

		List<AnalysisPath> result = new ArrayList<>();
		for (AnalysisPath path : subPaths) {
			int size = path.getElements().size();
			List<ProcessElement> withoutStartElement = size > 0 ? path.getElements().subList(1, size) : emptyList();
			result.add(new AnalysisPath(ListUtils.union(List.of(element), withoutStartElement)));
		}

		return result;
	}
	
	public static <T> int getLastIndex(AnalysisPath path) {
		List<ProcessElement> elements = path.getElements();
		return elements.size() == 0 ? 0 : elements.size() - 1;
	}
	
	public static ProcessElement getLastElement(AnalysisPath path) {
		List<ProcessElement> elements = path.getElements();
		int size = elements.size();
		return size == 0 ? null : elements.get(size - 1);
	}
	
	public static List<AnalysisPath> removeLastElementByClassType(List<AnalysisPath> paths , Class<?> clazz) {

		List<AnalysisPath> result = new ArrayList<>();
		for (AnalysisPath path : paths) {
			int lastIndex = AnalysisPathHelper.getLastIndex(path);
			List<ProcessElement> pathElements = new ArrayList<>(path.getElements());
			ProcessElement lastElement = pathElements.get(lastIndex);
			
			if (lastElement instanceof CommonElement && clazz.isInstance(lastElement.getElement())) {
				pathElements.remove(lastIndex);
			}
			
			result.add(new AnalysisPath(pathElements));
		}

		return result;
	}
	
	public static List<ProcessElement> getAllProcessElement(List<AnalysisPath> paths) {
		List<ProcessElement> elements = paths.stream()
				.map(AnalysisPath::getElements)
				.flatMap(List::stream)
				.flatMap(it -> getAllProcessElement(it).stream())
				.toList();
		return elements;
	}
	
	public static List<ProcessElement> getAllProcessElement(ProcessElement element) {
		if(element instanceof CommonElement) {
			return List.of(element);
		}
		
		if(element instanceof TaskParallelGroup) {
			List<ProcessElement> result = new ArrayList<>();
			
			TaskParallelGroup group = (TaskParallelGroup) element;
			if(group.getElement() != null) {
				result.add(new CommonElement(group.getElement()));
			}
						
			for(Entry<SequenceFlow, List<AnalysisPath>> entry : group.getInternalPaths().entrySet()) {
				List<ProcessElement> allProcessElement = entry.getValue().stream()
						.map(AnalysisPath::getElements)
						.flatMap(List::stream)
						.flatMap(it -> getAllProcessElement(it).stream())
						.toList();
				
				result.add(new CommonElement(entry.getKey()));				
				result.addAll(allProcessElement);
			}
			
			return result;
		}
		
		return emptyList();
	}
}
