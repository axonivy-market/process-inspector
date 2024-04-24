package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.model.ElementTask;

public class WorkflowDuration {
	private ProcessGraph processGraph;
	private Map<ElementTask, Duration> durationOverrides = emptyMap();

	public WorkflowDuration() {
		this.processGraph = new ProcessGraph();
	}

	public WorkflowDuration setDurationOverrides (Map<ElementTask, Duration> durationOverrides) {
		this.durationOverrides = durationOverrides;
		return this;
	}

	public Duration getDuration(ElementTask elementTask, String script, Enum<?> useCase) {		
		Duration overriderDuration = this.durationOverrides.get(elementTask);
		Duration taskDuration = ofNullable(overriderDuration)
				.orElse(getDurationByTaskScript(script, useCase));
		
		return taskDuration;
	}

	private Duration getDurationByTaskScript(String script, Enum<?> useCase) {
		if (useCase != null) {
			String useCasePrefix = getUseCasePrefix(useCase);
			List<String> prefixs = Arrays.asList("APAConfig.setEstimate", useCasePrefix);

			String wfEstimateCode = processGraph.getCodeLineByPrefix(script, prefixs.toArray(new String[0]));
			if (isNotEmpty(wfEstimateCode)) {
				String[] result = StringUtils.substringsBetween(wfEstimateCode, "(", ")")[0].split(",");
				int amount = Integer.parseInt(result[0]);
				TimeUnit unit = getTimeUnit(result[1]);

				switch (unit) {
				case DAYS:
					return Duration.ofDays(amount);
				case HOURS:
					return Duration.ofHours(amount);
				case MINUTES:
					return Duration.ofMinutes(amount);
				case SECONDS:
					return Duration.ofSeconds(amount);
				default:
					// Handle any unexpected TimeUnit
					break;
				}
			}
		}
		return Duration.ofHours(0);
	}

	private TimeUnit getTimeUnit(String timeUnit) {
		if (isBlank(timeUnit)) {
			return null;
		}

		if (timeUnit.startsWith("TimeUnit.")) {
			// TimeUnit.HOURS
			return TimeUnit.valueOf(timeUnit.split("\\.")[1]);
		} else {
			// HOURS
			return TimeUnit.valueOf(timeUnit);
		}
	}

	private String getUseCasePrefix(Enum<?> useCase) {
		String usePrefix = StringUtils.EMPTY;
		if (useCase != null) {
			usePrefix = "." + useCase.name();
		}

		return usePrefix;
	}


}
