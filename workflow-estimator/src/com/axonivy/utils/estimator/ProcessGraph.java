package com.axonivy.utils.estimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;

@SuppressWarnings("restriction")
public class ProcessGraph {
	public final Process process;

	public ProcessGraph(Process process) {
		this.process = process;
	}

	public RequestStart findStart() {
		return process.search().type(RequestStart.class).findOne();
	}

	public BaseElement findByElementName(String name) {
		return process.getElements().stream()
				.filter(el -> el.getName().equals(name))
				.findFirst()
				.orElse(null);
	} 
	
	private List<List<BaseElement>> findNextNodeElementPath(BaseElement from) {
		var nexts = new ArrayList<List<BaseElement>>();

		if (from != null && from instanceof NodeElement) {
			List<SequenceFlow> outs = ((NodeElement) from).getOutgoing();
			for (SequenceFlow out : outs) {

				NodeElement target = out.getTarget();
				List<List<BaseElement>> nextElements = findNextNodeElementPath(target);

				if (nextElements.isEmpty()) {
					nextElements.add(new ArrayList<BaseElement>(Arrays.asList(target, out)));
				} else {
					nextElements.forEach(nodes -> {
						nodes.add(0, target);
						nodes.add(1, out);
					});
				}

				nexts.addAll(nextElements);
			}
		}
		return nexts;
	}
}
