package org.emerse.fhir;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.util.*;

public abstract class AbstractHandler implements Handler
{
	protected Server server;
	protected RunningState runningState = RunningState.STOPPED;

	@Override
	public void handle(
		String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response
	) throws IOException, ServletException
	{
		try
		{
			doHandle(target, baseRequest, request, response);
		}
		catch (Exception e)
		{
			if (e instanceof IOException ioe)
			{
				throw ioe;
			}
			if (e instanceof ServletException se)
			{
				throw se;
			}
			if (e instanceof RuntimeException re)
			{
				throw re;
			}
			throw new RuntimeException(e);
		}
	}

	protected abstract void doHandle(
		String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response
	) throws Exception;

	@Override
	public void setServer(Server server)
	{
		this.server = server;
	}

	@Override
	public Server getServer()
	{
		return server;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void start() throws Exception
	{
		runningState = RunningState.RUNNING;
	}

	@Override
	public void stop() throws Exception
	{
		runningState = RunningState.STOPPED;
	}

	@Override
	public boolean isRunning()
	{
		return runningState == RunningState.RUNNING;
	}

	@Override
	public boolean isStarted()
	{
		return runningState == RunningState.RUNNING;
	}

	@Override
	public boolean isStarting()
	{
		return runningState == RunningState.STARTING;
	}

	@Override
	public boolean isStopping()
	{
		return runningState == RunningState.STOPPING;
	}

	@Override
	public boolean isStopped()
	{
		return runningState == RunningState.STOPPED;
	}

	@Override
	public boolean isFailed()
	{
		return runningState == RunningState.FAILED;
	}

	@Override
	public boolean addEventListener(EventListener listener)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeEventListener(EventListener listener)
	{
		throw new UnsupportedOperationException();
	}

	public enum RunningState
	{
		STOPPED, STARTING, RUNNING, STOPPING, FAILED
	}
}
