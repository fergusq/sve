package org.kaivos.sve.interpreter.exception;

public class SveVariableNotFoundException extends SveRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6597933018452252432L;
	
	public SveVariableNotFoundException(int line, String n) {
		super(line, ExceptionType.UNKNW_INDX, "variable '" + n + "' not found");
	}
	
}
