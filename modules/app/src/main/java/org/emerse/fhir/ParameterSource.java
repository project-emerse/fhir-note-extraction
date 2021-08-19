package org.emerse.fhir;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

public class ParameterSource
{
	private final HttpServletRequest request;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public ParameterSource(HttpServletRequest request)
	{
		this.request = request;
	}

	public <T> T getParameter(String name, Class<T> cls) throws Exception
	{
		var v = request.getParameter(name);
		return convert(v, cls);
	}

	public <T> List<T> getParameterList(String name, Class<T> componentType) throws Exception
	{
		var o = getParameter(name, componentType.arrayType());
		return new ArrayList<T>(Arrays.asList((T[]) o));
	}

	private <T> T convert(String v, Class<T> cls) throws Exception
	{
		if (cls == Integer.class || cls == int.class)
		{
			return (T) Integer.valueOf(v);
		}
		if (cls == String.class)
		{
			return (T) v;
		}
		else if (cls == Date.class)
		{
			return (T) dateFormat.parse(v);
		}
		else if (cls.isArray())
		{
			var vs = v.split(",");
			Class<?> elCls = cls.getComponentType();
			var o = Array.newInstance(elCls, vs.length);
			for (int i = 0; i < vs.length; i++)
			{
				Array.set(o, i, convert(vs[i], elCls));
			}
			return (T) o;
		}
		throw new RuntimeException("Unsupported type: " + cls);
	}
}
