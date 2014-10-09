package org.kaivos.sve.interpreter;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;

import org.kaivos.sve.interpreter.api.SveApiFunction;
import org.kaivos.sve.interpreter.core.SveValue;
import org.kaivos.sve.interpreter.core.SveValue.Type;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.interpreter.exception.SveVariableNotFoundException;
import org.kaivos.sve.interpreter.exception.SveRuntimeException.ExceptionType;

public class JavaInterface {

	private SveInterpreter interpreter;
	
	public JavaInterface(SveInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	public SveValue sve_GetFromJavaObject(final Object o) {
		
		if (o == null) return null;
		if (o.getClass().isPrimitive()) {
			switch (o.getClass().getName()) {
			case "byte":
			case "char":
			case "short":
			case "int":
			case "long":
			case "float":
			case "double":
				return new SveValue((double) o);
			case "boolean":
				return new SveValue((boolean) o);
			default:
				return null;
			}
		}
		
		if (o.getClass().isArray()) {
			Object[] ret = (Object[]) o;
			SveValue v = new SveValue(Type.TABLE);
			int i = 0;
			for (Object o2 : ret) {
				v.table.setLocalVar(""+i++, sve_GetFromJavaObject(o2));
			}
			v.value_obj = ret;
			return v;
		}
		
		SveValue v2 = new SveValue(o.toString());
		
		v2.value_obj = o;
		
		sve_MakeMethods(v2, o);
		
		v2.table.setLocalVar("@type", new SveValue("java:" + o.getClass().getName()));
		
		return v2;
	}
	
	public Object sve_CreateJavaObject(final SveValue val, Class<?> t) throws SveVariableNotFoundException, SveRuntimeException {
		
		if (val.type == Type.NIL) return null;
		
		if (t.getName().equals(int.class.getName())) return (int) val.getValue();
		else if (t.getName().equals(long.class.getName())) return (long) val.getValue();
		else if (t.getName().equals(boolean.class.getName())) return val.getValue_bool();
		else if (t.getName().equals(double.class.getName())) return val.getValue();
		else if (t.getName().equals(short.class.getName())) return (short) val.getValue();
		else if (t.getName().equals(float.class.getName())) return (float) val.getValue();
		else if (t.getName().equals(String.class.getName())) return val.getValue_str();
		else if (t.isArray() || (val.type == Type.TABLE && t.getName().equals(Object.class.getName()))) {
			ArrayList<Object> list = new ArrayList<>();
			for (int j = 0; val.table.getVar(""+j) != null; j++) {
				Object o = sve_CreateJavaObject(val.table.getVar(""+j), t.getComponentType());
				list.add(o);
			}
			
			Object array = Array.newInstance(t.getComponentType(), list.size());
			for (int j = 0; j < list.size(); j++) Array.set(array, j, list.get(j));
			
			return array;
		}
		else if (val.value_obj == null && t.isInterface()) {
			return java.lang.reflect.Proxy.newProxyInstance(getClassLoader(), new Class<?>[] {t}, new InvocationHandler() {
				
				@Override
				public Object invoke(Object obj, Method m, Object[] nullableArgs)
						throws Throwable {
					if (val.table.getVar(m.getName()) != null) {
						Object[] args = nullableArgs;
						if (args == null) args = new Object[0];
						SveValue[] sveargs = new SveValue[args.length];
						for (int i = 0; i < args.length; i++) {
							sveargs[i] = sve_GetFromJavaObject(args[i]);
						}
						return sve_GetFromJavaObject(interpreter.runFunction(val.table.getVar(m.getName()), sveargs));
					}
					return null;
				}
			});
		}
		else if (val.value_obj == null && t.getName().equals(Object.class.getName())) {
			if (val.type == Type.STRING) return val.getValue_str();
			if (val.type == Type.DOUBLE) return val.getValue();
			if (val.type == Type.BOOLEAN) return val.getValue_bool();
			else return null;
		}
		else return val.value_obj;
	}

	protected ClassLoader getClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

	protected void sve_MakeMethods(SveValue v2, final Object o) {
		
		Method[] methods = o.getClass().getMethods();
		Arrays.sort(methods, new Comparator<Method>() {

			@Override
			public int compare(Method o1, Method o2) {
				return o2.getParameterTypes().length-o1.getParameterTypes().length;
			}
			
		});
		
		HashSet<String> methodNames = new HashSet<>();
		
		for (final Method m : methods) {
			methodNames.add(m.getName());
			
			SveValue method = sve_GetMethod(o, m);
			
			String name = m.getName();
			
			v2.table.setLocalVar(name, method);
			
			for (Class<?> par : m.getParameterTypes()) {
				name += "_" + par.getSimpleName();
			}
			/*if (m.getParameterTypes().length == 0 && name.startsWith("get") && name.length() > 3) {
				char[] chars = name.substring(3).toCharArray();
				chars[0] = Character.toLowerCase(chars[0]);
				String fieldname = new String(chars);
				
				if (v2.table.variables().get(fieldname) == null) v2.table.setLocalVar(fieldname, method);
			}*/
			v2.table.setLocalVar(name, method);
		}
		
		//for (String name : methodNames)
		//	v2.table.setLocalVar(name, sve_UniversalMethod(o, name));
		
		v2.table.setLocalVar("@jclass", new SveValue(""+o.getClass().getName()));

		v2.value_obj = o;
	}
	
	@SuppressWarnings("unused")
	private SveValue sve_UniversalMethod(final Object o, final String name) {
		SveValue method = new SveValue(Type.FUNCTION_JAVA);
		method.method = new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveRuntimeException {
				Class<?>[] argTypes = new Class<?>[args.length];
				for (int i = 0; i < args.length; i++) {
					if (args[i].type == Type.BOOLEAN) argTypes[i] = boolean.class;
					else if (args[i].type == Type.DOUBLE) argTypes[i] = double.class;
					else if (args[i].type == Type.STRING) argTypes[i] = String.class;
					else if (args[i].value_obj != null) argTypes[i] = args[i].value_obj.getClass();
				}
				Method m = null;
				try {
					m = o.getClass().getMethod(name, argTypes);
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				System.err.println(m);
				return sve_callMethod(o, m, args);
			}
		};
		method.table.setLocalVar("@fullname", new SveValue(Type.NIL));
		return method;
	}

	protected SveValue sve_GetMethod(final Object o, final Method m) {
		SveValue method = new SveValue(Type.FUNCTION_JAVA);
		method.method = new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] arg0) throws SveRuntimeException {
				return sve_callMethod(o, m, arg0);
			}
		};
		method.table.setLocalVar("@fullname", new SveValue(m.toString()));
		return method;
	}
	
	protected SveValue sve_callMethod(Object o, Method m, SveValue[] arg0) throws SveVariableNotFoundException, SveRuntimeException {
		try {
			
			if (m == null) return new SveValue(Type.NIL);
			
			Object[] args = new Object[m.getParameterTypes().length];
			if (arg0.length != args.length) return new SveValue(Type.NIL);
			
			for (int i = 0; i < args.length; i++) {
				Class<?> t = m.getParameterTypes()[i];
				args[i] = sve_CreateJavaObject(arg0[i], t);
			}
			
			Class<?> r = m.getReturnType();
			if (r.getName().equals(int.class.getName())) return new SveValue((int) m.invoke(o, args));
			if (r.getName().equals(long.class.getName())) return new SveValue((long) m.invoke(o, args));
			if (r.getName().equals(boolean.class.getName())) return new SveValue((boolean) m.invoke(o, args));
			if (r.getName().equals(double.class.getName())) return new SveValue((double) m.invoke(o, args));
			if (r.getName().equals(short.class.getName())) return new SveValue((short) m.invoke(o, args));
			if (r.getName().equals(float.class.getName())) return new SveValue((float) m.invoke(o, args));
			if (r.getName().equals(String.class.getName())) return new SveValue((String) m.invoke(o, args));
			
			return sve_GetFromJavaObject(m.invoke(o, args));
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
			throw new SveRuntimeException(0, ExceptionType.OTHER, e.toString());
		}
	}

	protected SveValue sve_GetConstructor(final Object o, final Constructor<?> m) {
		SveValue method = new SveValue(Type.FUNCTION_JAVA);
		method.method = new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] arg0) throws SveRuntimeException {
				try {
					
					Object[] args = new Object[m.getParameterTypes().length];
					if (arg0.length != args.length) return new SveValue(Type.NIL);
					
					for (int i = 0; i < args.length; i++) {
						Class<?> t = m.getParameterTypes()[i];
						args[i] = sve_CreateJavaObject(arg0[i], t);
					}
					
					return sve_GetFromJavaObject(m.newInstance(args));
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | InstantiationException e) {
					e.printStackTrace();
					throw new SveRuntimeException(0, ExceptionType.OTHER, e.toString());
				}
			}
		};
		method.table.setLocalVar("@fullname", new SveValue(m.toString()));
		return method;
	}

	public void addJIFunctions() {
		
		interpreter.addJavaFunction("static", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] arg0) throws SveRuntimeException {
				if (arg0.length < 1) return new SveValue(Type.NIL);
				try {
					Class<?> cl = ClassLoader.getSystemClassLoader().loadClass(arg0[0].getValue_str());
					SveValue v = new SveValue(Type.TABLE);
					for (Field f : cl.getFields()) {
						if (((f.getModifiers() & Modifier.STATIC) != 0) || f.isEnumConstant())
							v.table.setLocalVar(f.getName(), sve_GetFromJavaObject(f.get(null)));
					}
					for (Method m : cl.getMethods()) {
						if ((m.getModifiers() & Modifier.STATIC) != 0)
						{
							SveValue method = sve_GetMethod(null, m);
							v.table.setLocalVar(m.getName(), method);
							
							String name = m.getName();
							for (Class<?> par : m.getParameterTypes()) {
								name += "_" + par.getSimpleName();
							}
							v.table.setLocalVar(name, method);
						}
					}
					for (Constructor<?> m : cl.getDeclaredConstructors()) {
						{
							SveValue method = sve_GetConstructor(null, m);
							v.table.setLocalVar(m.getName(), method);
							
							String name = "new";
							for (Class<?> par : m.getParameterTypes()) {
								name += "_" + par.getSimpleName();
							}
							v.table.setLocalVar(name, method);
						}
					}
					return v;
				} catch (ClassNotFoundException e) {
					throw new SveRuntimeException(0, ExceptionType.OTHER, e.toString());
				} catch (IllegalArgumentException e) {
					throw new SveRuntimeException(0, ExceptionType.OTHER, e.toString());
				} catch (IllegalAccessException e) {
					throw new SveRuntimeException(0, ExceptionType.OTHER, e.toString());
				}
			}
		}, interpreter.globalScope, "static(): Returns a table with all static values and enum constants");
		
		interpreter.addJavaFunction("proxy", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] arg0) throws SveRuntimeException {
				if (arg0.length < 2) return new SveValue(Type.NIL);
				try {
					Class<?> cl = getClassLoader().loadClass(arg0[0].getValue_str());
					
					return sve_GetFromJavaObject(sve_CreateJavaObject(arg0[1], cl));
				} catch (ClassNotFoundException e) {
					throw new SveRuntimeException(0, ExceptionType.OTHER, e.toString());
				} 
			}
		}, interpreter.globalScope, "proxy(class_name, table): Creates a proxy (anonymous class instance)");
		
	}
}
