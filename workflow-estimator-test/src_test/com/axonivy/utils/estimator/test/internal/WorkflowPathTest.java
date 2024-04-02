package com.axonivy.utils.estimator.test.internal;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.internal.WorkflowPath;
import com.axonivy.utils.estimator.internal.model.CommonElement;
import com.axonivy.utils.estimator.test.ProcessGraphHelper;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;

@IvyTest
@SuppressWarnings("restriction")
public class WorkflowPathTest extends InternalAbstractTest{
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	
	private static WorkflowPath workflowPath;
	
	@BeforeAll
	public static void setup() {
		workflowPath = new WorkflowPath(emptyMap());
	}

	@Test
	void shouldFindPathAtStart() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var start = ProcessGraphHelper.findByElementName(process, "start");		
		var result = workflowPath.findPath("internal", new CommonElement(start));
		
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
			assertEquals(expected.get(i), result.get(i).toString());	
		}
	}
}
 