package com.axonivy.utils.process.analyzer.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.PathFinder;
import com.axonivy.utils.process.analyzer.internal.model.AnalysisPath;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.test.ProcessGraphHelper;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;

@IvyTest
public class WorkflowFinderTest extends InternalAbstractTest{
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	
	private static PathFinder workflowPath;
	
	@BeforeAll
	public static void setup() {
		workflowPath = new PathFinder();
	}

	@Test
	void shouldFindPathAtStart() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var start = ProcessGraphHelper.findByElementName(process, "start");		
		Map<ProcessElement, List<AnalysisPath>> result = workflowPath.setFlowName("internal").setStartElements(List.of(new CommonElement(start))).findTaskOnPath();
		List<ProcessElement> elements = result.values().stream().flatMap(List::stream).map(AnalysisPath::getElements).flatMap(List::stream).toList();
		
		var expected = Arrays.asList(
				"RequestStartZ:start (18DC44E096FDFF75-f0)",
				"SequenceFlowZ:RequestStartZ->UserTaskZ",
				"UserTaskZ:Task A\n(Element Label) (18DC44E096FDFF75-f2)",
				"SequenceFlowZ:UserTaskZ->AlternativeZ",
				"AlternativeZ:Alter (18DC44E096FDFF75-f4)", 
				"SequenceFlowZ:AlternativeZ->AlternativeZ", 
				"AlternativeZ:int/ext? (18DC44E096FDFF75-f8)",
				"SequenceFlowZ:AlternativeZ->UserTaskZ",
				"UserTaskZ:Task B (18DC44E096FDFF75-f7)",
				"SequenceFlowZ:UserTaskZ->AlternativeZ",
				"AlternativeZ:Alter2 (18DC44E096FDFF75-f6)",
				"SequenceFlowZ:AlternativeZ->TaskEndZ",
				"TaskEndZ: (18DC44E096FDFF75-f1)");
		for(int i = 0; i <expected.size(); i++) {
			assertEquals(expected.get(i), elements.get(i).getElement().toString());	
		}
	}
}
 