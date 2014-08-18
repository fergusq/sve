package org.kaivos.sve.interpreter.core;

import java.util.ArrayList;


import org.kaivos.sve.interpreter.api.SveApiFunction;
import org.kaivos.sve.parser.SveTree.LineTree;
import org.kaivos.sve.parser.SveTree.ParameterTree;

public class SveValue {

	public enum Type {
		DOUBLE,
		STRING,
		BOOLEAN,
		FUNCTION, 
		FUNCTION_JAVA,
		BREAK, RETURN,
		NIL,
		TABLE,
		USER
	}
	
	public Type type;
	
	private double value;
	private String value_str;
	private boolean value_bool;
	public Object value_obj;
	
	public SveScope table = new SveScope(null);
	
	public LineTree line;
	public ArrayList<ParameterTree> parameters = new ArrayList<ParameterTree>();
	public SveScope localScope;
	public String fname;
	
	public SveApiFunction method;
	
	public SveValue returnValue;
	
	public SveValue(double v) {
		type = Type.DOUBLE;
		value = v;
	}
	
	public SveValue(boolean v) {
		type = Type.BOOLEAN;
		value_bool = v;
	}
	
	public SveValue(String v) {
		type = Type.STRING;
		value_str = v;
	}
	
	public SveValue(Object v) {
		type = Type.USER;
		value_obj = v;
	}
	
	public SveValue(Type v) {
		type = v;
	}

	public SveValue(SveScope t) {
		if (t == null) {
			type = Type.NIL;
		} else {
			type = Type.TABLE;
			table = t;
		}
	}

	public double getValue() {
		if (type == Type.STRING) {
			try {
				return Double.parseDouble(value_str);
			} catch (NumberFormatException ex) {
				return Double.MIN_VALUE;
			}
		}
		if (type == Type.FUNCTION) {
			return hashCode();
		}
		if (type == Type.FUNCTION_JAVA) {
			return method.hashCode();
		}
		if (type == Type.NIL) return 0;
		
		if (type == Type.TABLE) return 1;
		
		if (type == Type.USER) return value_obj.hashCode();
		
		if (type == Type.BOOLEAN) return value_bool ? 1 : 0;
		return value;
	}

	@SuppressWarnings("unused")
	private void setValue(double value) {
		if (type == Type.STRING) this.value_str = "" + value;
		this.value = value;
	}

	public String getValue_str() {
		if (type == Type.DOUBLE && ((int)value) == value) return "" + (int)value;
		if (type == Type.DOUBLE && ((int)value) != value) return "" + value;
		if (type == Type.FUNCTION) {
			if (fname != null)
				return "function:"+fname+"@"+hashCode();
			return "function@" + hashCode();
		}
		if (type == Type.FUNCTION_JAVA) {
			if (fname != null)
				return "nfunction:"+fname+"@"+hashCode();
			return "nfunction@" + method.hashCode();
		}
		if (type == Type.NIL) return "nil";
		if (type == Type.TABLE) {
			/*Set<Entry<String, SveValue>> s = table.variables.entrySet();
			List<Entry<String, SveValue>> list = new ArrayList<>();
			for (Entry<String, SveValue> e:s) list.add(e);
			Collections.sort(list, new Comparator<Entry<String, SveValue>>() {

				@Override
				public int compare(Entry<String, SveValue> o1,
						Entry<String, SveValue> o2) {
					return o1.getKey().compareTo(o2.getKey());
				}

			});
			return list.toString();*/
			return table.variables.toString();
		}
		
		if (type == Type.BOOLEAN) return value_bool ? "true" : "false";
		
		if (type == Type.USER) return value_obj.toString();
		
		return value_str;
	}

	@SuppressWarnings("unused")
	private void setValue_str(String value_str) {
		if (type == Type.DOUBLE) {
			try {
				this.value = Double.parseDouble(value_str);
			} catch (NumberFormatException ex) {
				this.value = Double.MIN_VALUE;
			}
		}
		if (type == Type.FUNCTION) {
			this.value = hashCode();
		}
		this.value_str = value_str;
	}
	
	public boolean getValue_bool() {
		if (type == Type.NIL) return false;
		if (type == Type.BOOLEAN) return value_bool;
		return true;
	}

	@SuppressWarnings("unchecked")
	public SveValue copy() {
		SveValue l = new SveValue(getValue());
		l.type = type;
		l.value = value;
		l.value_str = value_str;
		l.value_bool = value_bool;
		l.value_obj = value_obj;
		l.line = line;
		l.parameters = (ArrayList<ParameterTree>) parameters.clone();
		l.method = method;
		l.table = table;
		l.localScope = localScope;
		l.returnValue = returnValue;
		return l;
	}
	
	@Override
	public String toString() {
		return getValue_str();
	}
	
}
