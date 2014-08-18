package org.kaivos.sve.interpreter.core;

import java.util.HashMap;

import org.kaivos.sve.interpreter.SveInterpreter;
import org.kaivos.sve.interpreter.core.SveValue;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.interpreter.exception.SveVariableNotFoundException;

public class SveScope {

	public SveScope superScope;
	HashMap<String, SveValue> variables = new HashMap<>();
	public SveScope prototypeScope;
	
	public SveValue prototypeScope_f;
	public SveInterpreter inter;
	
	public SveScope(SveScope scope) {
		superScope = scope;
	}
	
	public SveValue getVar(String name) throws SveVariableNotFoundException, SveRuntimeException {
		if (variables.containsKey(name)) return variables.get(name);
		SveValue tmp = null;
		
		if (superScope != null && (tmp = superScope.getVar(name)) != null) {
			return tmp;
		} else {
			return prototypeScope != null ? prototypeScope.getVar(name) : (prototypeScope_f != null ? inter.runFunction(prototypeScope_f, new SveValue(name)) : null);
		}
	}
	
	public SveValue getVarNPt(String name) throws SveVariableNotFoundException, SveRuntimeException {
		if (variables.containsKey(name)) return variables.get(name);
		SveValue tmp = null;
		
		if (superScope != null && (tmp = superScope.getVar(name)) != null) {
			return tmp;
		} else return null;
	}
	
	public SveValue setVar(String name, SveValue value) throws SveVariableNotFoundException, SveRuntimeException {
		if (variables.containsKey(name)) return variables.put(name, value);
		
		if (superScope != null && (superScope.getVarNPt(name) != null)) {
			return superScope.setVar(name, value);
		} else {
			return variables.put(name, value);
		}
	}
	
	public SveValue setLocalVar(String name, SveValue value) {
		return variables.put(name, value);
	}
	
	public HashMap<String, SveValue> variables() {
		return variables;
	}
	
}
