package org.kaivos.sve.interpreter.exception;

import org.kaivos.sve.interpreter.core.SveValue;

public class SveRaiseException extends SveRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6597933018452252432L;
	
	private SveValue val;
	
	public SveRaiseException(int line, SveValue val) {
		super(line, ExceptionType.OTHER, val.getValue_str());
		this.val = val;
	}
	
	public SveValue getSveValue() {
		return val;
	}
	
}
