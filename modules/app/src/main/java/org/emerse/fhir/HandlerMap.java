package org.emerse.fhir;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import java.util.*;

public class HandlerMap extends AbstractHandler
{
	private List<MappedHandler> handlers = new ArrayList<>();

	@Override
	public void doHandle(
		String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response
	) throws Exception
	{
		if (target.startsWith("/"))
		{
			target = target.substring(1);
		}
		for (MappedHandler mapped : handlers)
		{
			var match = mapped.match(target);
			if (match != null)
			{
				mapped.handler.handle(match, baseRequest, request, response);
				return;
			}
		}
	}

	public void put(String path, Handler handler)
	{
		var mapped = new MappedHandler();
		mapped.path = path;
		mapped.handler = handler;
		handlers.add(mapped);
	}

	private static class MappedHandler
	{
		public String path;
		public Handler handler;

		public String match(String target)
		{
			if (target.startsWith(path))
			{
				var l = path.length();
				if (target.length() == l)
				{
					return "";
				}
				else if (target.length() > l && target.charAt(l) == '/')
				{
					return target.substring(l + 1);
				}
			}
			return null;
		}
	}
}
