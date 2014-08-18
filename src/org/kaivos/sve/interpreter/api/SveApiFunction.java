package org.kaivos.sve.interpreter.api;

import org.kaivos.sve.interpreter.core.SveValue;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.interpreter.exception.SveVariableNotFoundException;

public interface SveApiFunction {

	public SveValue call(SveValue[]args) throws SveVariableNotFoundException, SveRuntimeException;

}
