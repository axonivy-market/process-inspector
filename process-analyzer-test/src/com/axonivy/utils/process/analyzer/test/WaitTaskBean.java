package com.axonivy.utils.process.analyzer.test;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;

import ch.ivyteam.ivy.process.intermediateevent.IProcessIntermediateEventBean;
import ch.ivyteam.ivy.service.ServiceException;

public class WaitTaskBean implements IProcessIntermediateEventBean {
	private boolean isRunning = false;

	public static String createEventIdentifierForTask() {
		return UUID.randomUUID().toString();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void start(IProgressMonitor monitor) throws ServiceException {
		isRunning = true;
	}

	@Override
	public void stop(IProgressMonitor monitor) throws ServiceException {
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void poll() {
	}

	@Override
	public boolean isMoreThanOneInstanceSupported() {
		return false;
	}

	@Override
	public Class<?> getResultObjectClass() {
		return null;
	}

}
