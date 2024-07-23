package com.axonivy.utils.process.inspector.internal.helper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;

import com.axonivy.utils.process.inspector.internal.model.AnalysisPath;
import com.axonivy.utils.process.inspector.internal.model.CommonElement;
import com.axonivy.utils.process.inspector.internal.model.ProcessElement;
import com.axonivy.utils.process.inspector.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;

public class AnalysisPathHelper {
	public static List<AnalysisPath> addToPath(AnalysisPath path, List<AnalysisPath> subPaths) {
		return addToPath(List.of(path), subPaths);
	}
	
	public static List<AnalysisPath> addToPath(List<AnalysisPath> paths, List<AnalysisPath> subPaths) {
		if (subPaths.isEmpty()) {
			return paths;
		}

		List<AnalysisPath> result = new ArrayList<>();
		for (AnalysisPath path : subPaths) {
			result.addAll(addAllToPath(paths, path.getElements()));
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
	
	public static ProcessElement getFirsElement(AnalysisPath path) {
		List<ProcessElement> elements = path.getElements();
		int size = elements.size();
		return size == 0 ? null : elements.get(0);
	}

	public static NodeElement getFirstNodeElement(List<AnalysisPath> paths) {
		NodeElement startNode = AnalysisPathHelper.getAllProcessElement(paths).stream()
				.map(ProcessElement::getElement)
				.filter(NodeElement.class::isInstance)
				.findFirst()
				.map(NodeElement.class::cast)
				.orElse(null);
		return startNode;
	}

	public static List<AnalysisPath> removeLastElementByClassType(List<AnalysisPath> paths, Class<?> clazz) {

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
				.flatMap(it -> getAllProcessElement(it).stream()).toList();
		return elements;
	}

	public static List<ProcessElement> getAllProcessElement(ProcessElement element) {
		if (element instanceof CommonElement) {
			return List.of(element);
		}

		if (element instanceof TaskParallelGroup) {
			List<ProcessElement> result = new ArrayList<>();

			TaskParallelGroup group = (TaskParallelGroup) element;
			if (group.getElement() != null) {
				result.add(new CommonElement(group.getElement()));
			}

			List<ProcessElement> allProcessElement = group.getInternalPaths().stream()
					.map(AnalysisPath::getElements)
					.flatMap(List::stream)
					.flatMap(it -> getAllProcessElement(it).stream())
					.toList();

			result.addAll(allProcessElement);
			return result;
		}

		return emptyList();
	}
	
	public static List<AnalysisPath> convertToAnalysisPath(Map<SequenceFlow, List<AnalysisPath>> interalPaths) {
		List<AnalysisPath> result = new ArrayList<>();
		interalPaths.entrySet().forEach(it -> {
			var paths = addToPath(new AnalysisPath(new CommonElement(it.getKey())), it.getValue());
			result.addAll(paths);
		});
		return result;
	}
	
	public static List<ProcessElement> getLastProcessElements(List<AnalysisPath> paths) {
		List<ProcessElement> result = new ArrayList<>();		
		paths.stream().map(AnalysisPathHelper::getLastElement).forEach(it -> {
			if (it instanceof TaskParallelGroup) {
				result.addAll(getLastProcessElements(((TaskParallelGroup) it).getInternalPaths()));
			} else {
				result.add(it);
			}
		});

		return result.stream().distinct().toList();
	}

	public static <K, V extends List<AnalysisPath>> Map<K, V> getPathByStartElements(Map<K, V> source, Set<K> keys) {
		Map<K, V> result = new LinkedHashMap<>();
		keys.forEach(key -> {
			V paths = source.get(key);
			if (isNotEmpty(paths)) {
				result.put(key, paths);
			}
		});
		return result;
	}
	
	public static List<AnalysisPath> getAnalysisPathFrom(Map<ProcessElement, List<AnalysisPath>> source, ProcessElement from) {
		Set<AnalysisPath> result = new HashSet<>();
		List<AnalysisPath> paths = source.values().stream().flatMap(Collection::stream).toList();
		
		for (AnalysisPath path : paths) {
			List<ProcessElement> elements = path.getElements();
			int index = elements.indexOf(from);
			if (index >= 0) {
				List<ProcessElement> afterFrom = elements.subList(index, elements.size() - 1);
				result.add(new AnalysisPath(afterFrom));
			}
		}

		return new ArrayList<>(result);	
	}
	
	public static Map<ProcessElement, List<AnalysisPath>> getAnalysisPathTo(Map<ProcessElement, List<AnalysisPath>> source, ProcessElement to) {
		Map<ProcessElement, List<AnalysisPath>> pathBeforeIntersection = new LinkedHashMap<>();
		for (Entry<ProcessElement, List<AnalysisPath>> entry : source.entrySet()) {
			List<AnalysisPath> beforeTo = subAnalysisPathTo(entry.getValue(), to);
			if (isNotEmpty(beforeTo)) {
				pathBeforeIntersection.put(entry.getKey(), beforeTo);
			}
		}

		return pathBeforeIntersection;
	}
	
	public static List<SequenceFlow> findIncomingsFromPaths(List<AnalysisPath> paths, ProcessElement from) {
		List<BaseElement> elements = getAllProcessElement(paths).stream()						
				.map(ProcessElement::getElement)
				.toList();
		
		List<SequenceFlow> sequenceFlows =	elements.stream()
				.filter(SequenceFlow.class::isInstance)
				.map(SequenceFlow.class::cast)
				.filter(it -> it.getTarget().equals(from.getElement()))			
				.distinct()				
				.toList();
		
		return sequenceFlows;
	}
	
	public static boolean isContains(List<AnalysisPath> currentPaths, final ProcessElement from) {
		boolean isContains = false;
		if (from.getElement() instanceof NodeElement && from.getElement() instanceof RequestStart == false) {
			NodeElement node = (NodeElement) from.getElement();
			
			if (node.getIncoming().size() > 0) {
				SequenceFlow sequenceFlow = node.getIncoming().get(0);
				List<AnalysisPath> pathWithConnectToFrom = currentPaths.stream().filter(path -> {
					int lastIndex = getLastIndex(path);
					return sequenceFlow.equals(path.getElements().get(lastIndex).getElement());
				}).toList();

				isContains = pathWithConnectToFrom.stream()
						.map(AnalysisPath::getElements)
						.flatMap(List::stream)
						.anyMatch(it -> it.getElement().equals(from.getElement()));
			}
		}
		return isContains;
	}
	
	public static Map<ProcessElement, Set<ProcessElement>> getAllStartElementOfTaskSwitchGateways(Map<ProcessElement, List<AnalysisPath>> source) {
		Map<ProcessElement, Set<ProcessElement>> result = new LinkedHashMap<>();

		for (ProcessElement startElement : source.keySet()) {			
			List<AnalysisPath> paths =  source.getOrDefault(startElement, emptyList());
			for (AnalysisPath path : paths) {
				for (ProcessElement element : path.getElements()) {
					if (element.getElement() instanceof TaskSwitchGateway) {						
						if (((TaskSwitchGateway) element.getElement()).getIncoming().size() > 1) {
							Set<ProcessElement> startElements = result.getOrDefault(element, emptySet());

							result.put(element, SetUtils.union(startElements, Set.of(startElement)));
						}
					}
				}
			}
		}
		
		return result;
	}
	
	public static List<AnalysisPath> getInternalPath(List<AnalysisPath> internalPaths, boolean withTaskEnd) {
		List<AnalysisPath> paths = new ArrayList<>();

		// Priority the path go to end first
		List<AnalysisPath> analysisPaths = internalPaths.stream().filter(it -> {
			ProcessElement last = AnalysisPathHelper.getLastElement(it);
			if (withTaskEnd && last.getElement() instanceof TaskEnd == true) {
				return true;
			} else if (!withTaskEnd && last.getElement() instanceof TaskEnd == false) {
				return true;
			} else {
				return false;
			}
		}).toList();

		if (isNotEmpty(analysisPaths)) {
			paths.addAll(analysisPaths);
		}

		return paths;
	}

	private static List<AnalysisPath> subAnalysisPathTo(List<AnalysisPath> source, ProcessElement to) {
		List<AnalysisPath> result = new ArrayList<>();
		for (AnalysisPath path : source) {
			int index = path.getElements().indexOf(to);
			if (index >= 0) {
				List<ProcessElement> beforeIntersection = path.getElements().subList(0, index);
				result.add(new AnalysisPath(beforeIntersection));
			}
		}
		return result;
	}
}
