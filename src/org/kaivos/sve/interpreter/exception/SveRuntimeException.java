package org.kaivos.sve.interpreter.exception;

public class SveRuntimeException extends Exception {

	public enum ExceptionType {
		NULL_PTR(1, "NullPointerException"),
		UNKNW_INDX(2, "UnknownIndexException"),
		WRONG_ARGS(3, "WrongArgumentsException"),
		TYPE_COER(4, "TypeConversionException"),
		OTHER(0, "Exception")
		;
		
		private int id;
		private String name;
		
		private ExceptionType(int id, String msg) {
			this.name = msg;
		}
		
		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6597933018452252432L;
	private String name;
	private int line;
	private ExceptionType type;
	
	public SveRuntimeException(int line, ExceptionType type, String n) {
		setName(n);
		this.line=(line);
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getLine() {
		return line;
	}
	
	public ExceptionType getType() {
		return type;
	}
	
	@Override
	public String getMessage() {
		return "[" + line + "] " + type.getId() + " " + type.getName() + ": " + name + "";
	}
	
}

