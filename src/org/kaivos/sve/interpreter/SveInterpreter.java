package org.kaivos.sve.interpreter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import org.kaivos.bcx.data.BCXByte;
import org.kaivos.bcx.data.BCXDataList;
import org.kaivos.bcx.data.BCXDataType;
import org.kaivos.bcx.data.BCXFloat;
import org.kaivos.bcx.data.BCXString;
import org.kaivos.sc.TokenScanner;
import org.kaivos.stg.error.SyntaxError;
import org.kaivos.sve.SveUtil;
import org.kaivos.sve.interpreter.api.SveApiFunction;
import org.kaivos.sve.interpreter.core.SveScope;
import org.kaivos.sve.interpreter.core.SveValue;
import org.kaivos.sve.interpreter.core.SveValue.Type;
import org.kaivos.sve.interpreter.exception.SveRaiseException;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.interpreter.exception.SveRuntimeException.ExceptionType;
import org.kaivos.sve.interpreter.exception.SveVariableNotFoundException;
import org.kaivos.sve.parser.SveParser;
import org.kaivos.sve.parser.SveTree;
import org.kaivos.sve.parser.SveTree.BlockTree;
import org.kaivos.sve.parser.SveTree.DoWhileTree;
import org.kaivos.sve.parser.SveTree.Expression0Tree;
import org.kaivos.sve.parser.SveTree.Expression1Tree;
import org.kaivos.sve.parser.SveTree.Expression2Tree;
import org.kaivos.sve.parser.SveTree.ExpressionLogicTree;
import org.kaivos.sve.parser.SveTree.ExpressionTree;
import org.kaivos.sve.parser.SveTree.ForTree;
import org.kaivos.sve.parser.SveTree.FunctionTree;
import org.kaivos.sve.parser.SveTree.IfTree;
import org.kaivos.sve.parser.SveTree.IfWhileTree;
import org.kaivos.sve.parser.SveTree.LineTree;
import org.kaivos.sve.parser.SveTree.PrimaryTree;
import org.kaivos.sve.parser.SveTree.StartTree;
import org.kaivos.sve.parser.SveTree.WhileTree;

public class SveInterpreter {

	public SveScope globalScope;
	private Random rnd = new Random();
	public JavaInterface javaInterface;
	
	public SveInterpreter(boolean includeJavaAPI) throws SveVariableNotFoundException, SveRuntimeException {
		globalScope = new SveScope(null);
		
		//globalScope.setVar("print", new SveValue(-38));
		
		addJavaFunction("sethelp", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				functionHelp.put(args[0].getValue_str(), args[1].getValue_str());
				return new SveValue(Type.NIL);
				
			}
		}, globalScope, "sethelp(function_name, string): Set's help text");
		
		addJavaFunction("include", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException, SveRuntimeException {
				if (args.length < 1) return new SveValue(Type.NIL);
				interpret(SveParser.parse(args[0].getValue_str()));
				return new SveValue(Type.NIL);
				
			}
		}, globalScope, "include(file_string): Executes sve code from a file");
		addJavaFunction("require", new SveApiFunction() {
			
			private ArrayList<String> required = new ArrayList<>();
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException, SveRuntimeException {
				if (args.length < 1 || required.contains(args[0].getValue_str())) return new SveValue(Type.NIL);
				required.add(args[0].getValue_str());
				File f = null;
				int i = 0;
				File[] path = SveUtil.getSveDirectory();
				do {
					f = new File(path[i++], args[0].getValue_str());
				} while (!f.exists() && i < path.length);
				if (!f.exists()) f = new File(args[0].getValue_str());
				interpret(SveParser.parse(f));
				return new SveValue(Type.NIL);
				
			}
		}, globalScope, "require(file_string): Executes sve code from a file except if the file is already executed");
		
		addJavaFunction("eval", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException, SveRuntimeException {
				if (args.length < 1) return new SveValue(Type.NIL);
				
				SveScope scope = globalScope;
				if (args.length == 2) {
					scope = args[1].table;
				}
				try {
					return interpretStartTree(SveParser.parseText(args[0].getValue_str()), scope);
				} catch (SyntaxError e) {
					e.printStackTrace();
					throw new SveRuntimeException(-1, ExceptionType.OTHER, e.getMessage());
				}
				
			}
		}, globalScope, "eval(sve_code, [scope=$]): Executes sve code in a scope");
		
		addJavaFunction("print", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length > 0) System.out.println(args[0].getValue_str());
				return new SveValue(Type.NIL);
				
			}
		}, globalScope, "print(string): Prints string to the standard output");
		addJavaFunction("puts", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length > 0) System.out.print(args[0].getValue_str());
				return new SveValue(Type.NIL);
				
			}
		}, globalScope, "print(string): Prints string to the standard output");
		addJavaFunction("readln", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				String s = "";
				do {
					char c = 0;
					try {
						c = (char) System.in.read();
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					if (c == '\n') break;
					else s += c;
				} while (true);
				return new SveValue(s);
				
			}
		}, globalScope, "print(string): Prints string to the standard output");
		addJavaFunction("str", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length > 0) return new SveValue(args[0].getValue_str());
				return new SveValue(Type.NIL);
			}
		}, globalScope, "str(number): Returns the string representation of the number");
		addJavaFunction("number", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length > 0) return new SveValue(Double.parseDouble(args[0].getValue_str()));
				return new SveValue(Type.NIL);
			}
		}, globalScope, "number(string): Parsers a float number from a string");
		addJavaFunction("sysclock", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] args) {
				return new SveValue(System.currentTimeMillis());
			}
		}, globalScope, "sysclock(): Returns the system time in milliseconds");
		addJavaFunction("pcall", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				if (args[0].type != Type.FUNCTION) {
					return new SveValue(Type.NIL);
				}
				try {
					SveValue val = new SveValue(true);
					callStack.push("<pcall:"+args.hashCode()+">");
					val.table.setLocalVar("value", runFunction(args[0], Arrays.copyOfRange(args, 1, args.length)));
					callStack.pop();
					return val;
				} catch (SveRuntimeException ex) {
					callStack.push("<pcall catch:" + args.hashCode() + ">");
					SveValue val = new SveValue(false);
					val.table.setLocalVar("eid", new SveValue(ex.getType().getId()));
					val.table.setLocalVar("ename", new SveValue(ex.getType().getName()));
					val.table.setLocalVar("msg", new SveValue(ex.getName()));
					String stack = "";
					for (int i = callStack.size()-1; i >= 0; i--) {
						stack += ("\t" + callStack.get(i));
					}
					val.table.setLocalVar("stack", new SveValue(stack));
					return val;
				} catch (Exception ex) {
					callStack.push("<pcall catch:" + args.hashCode() + ">");
					SveValue val = new SveValue(false);
					val.table.setLocalVar("eid", new SveValue(ExceptionType.OTHER));
					val.table.setLocalVar("ename", new SveValue(ex.getClass().getName()));
					val.table.setLocalVar("msg", new SveValue(ex.getMessage()));
					String stack = "";
					for (int i = callStack.size()-1; i >= 0; i--) {
						stack += ("\t" + callStack.get(i));
					}
					val.table.setLocalVar("stack", new SveValue(stack));
					return val;
				}
			}
		}, globalScope, "pcall(function, arguments): Calls the function and returns 1 if success and 0 if error");
		addJavaFunction("raise", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] args) throws SveRuntimeException {
				if (args.length < 1) return new SveValue(Type.NIL);
				throw new SveRaiseException(-1, args[0]);
				
			}
		}, globalScope, "raise(error): Raise an error");
		addJavaFunction("acall", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException, SveRuntimeException {
				if (args.length < 2) return new SveValue(Type.NIL);
				if (args[0].type != Type.FUNCTION && args[0].type != Type.FUNCTION_JAVA) {
					return new SveValue(Type.NIL);
				}
				int length = 0;
				List<SveValue> vals = new ArrayList<SveValue>();
				while (args[1].table.getVar((length++)+"") != null) vals.add(args[1].table.getVar((length-1)+""));
					
				return runFunction(args[0], args.length>2 ? args[2].table : globalScope, vals.toArray(new SveValue[vals.size()]));
			}
		}, globalScope, "acall(function, arguments, [parent_env=$]): Calls the function with custom parameters and environment");
		addJavaFunction("rnd", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length > 0) return new SveValue(rnd.nextInt((int) args[0].getValue()));
				return new SveValue(Type.NIL);
			}
		}, globalScope, "rnd(max): Returns a new random number");
		addJavaFunction("type", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				SveValue v = null;
				if (args.length > 0) v = args[0];
				else return new SveValue(Type.NIL);
				
				switch (v.type) {
				case FUNCTION:
					return new SveValue("function");
				case DOUBLE:
					return new SveValue("number");
				case STRING:
					return new SveValue("string");
				case BREAK:
					return new SveValue("break");
				case FUNCTION_JAVA:
					return new SveValue("nfunction");
				case TABLE:
					return new SveValue("table");
				case NIL:
					return new SveValue("nil");
				case BOOLEAN:
					return new SveValue("boolean");
				case USER:
					return new SveValue("user");
				default:
					return new SveValue("undefined");
				}
			}
		}, globalScope, "type(obj): Returns the type of the object");
		addJavaFunction("hash", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				SveValue v = null;
				if (args.length > 0) v = args[0];
				else return new SveValue(Type.NIL);
				
				return new SveValue(v.hashCode());
			}
		}, globalScope, "hash(obj): Returns the hash code of the object");
		
		addJavaFunction("pow", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(Math.pow(v.getValue(), v2.getValue()));
			}
		}, globalScope, "pow(a, b): Returns the value of the first argument raised to the power of the second argument.");
		
		addJavaFunction("andb", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(((long)v.getValue())&((long)v2.getValue()));
			}
		}, globalScope, "andb(int, int): Bitwise AND");
		addJavaFunction("orb", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(((long)v.getValue())|((long)v2.getValue()));
			}
		}, globalScope, "orb(int, int): Bitwise OR");
		addJavaFunction("xorb", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(((long)v.getValue())^((long)v2.getValue()));
			}
		}, globalScope, "xorb(int, int): Bitwise XOR");
		addJavaFunction("shlb", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(((long)v.getValue())<<((long)v2.getValue()));
			}
		}, globalScope, "shlb(int, int): Bitwise left shift");
		addJavaFunction("shrb", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(((long)v.getValue())>>((long)v2.getValue()));
			}
		}, globalScope, "shrb(int, int): Signed bitwise right shift");
		addJavaFunction("ushrb", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(((long)v.getValue())>>>((long)v2.getValue()));
			}
		}, globalScope, "ushrb(int, int): Unsigned bitwise right shift");
		
		addJavaFunction("charat", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				SveValue v;
				v = args[0];
				
				SveValue v2;
				v2 = args[1];
				
				return new SveValue(v.getValue_str().charAt((int) v2.getValue()));
			}
		}, globalScope, "charat(string, index): Returns the character at the index");
		addJavaFunction("chr", new SveApiFunction() {
			@Override
			public SveValue call(SveValue[] arg0) {
				if (arg0.length < 1) return new SveValue(Type.NIL);
				return new SveValue("" + (char) arg0[0].getValue());
			}
		}, globalScope, "chr(chr_id): Returns a string that contains the right character");
		
		addJavaFunction("strlen", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].getValue_str().length());
			}
		}, globalScope, "strlen(string): Returns the length of the string");
		addJavaFunction("substr", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				if (args.length == 2)
					return new SveValue(args[0].getValue_str().substring((int) args[1].getValue()));
				
				return new SveValue(args[0].getValue_str().substring((int) args[1].getValue(), (int)args[2].getValue()));
			}
		}, globalScope, "substr(string, start, end): Creates a new substring from the parameter string");
		
		addJavaFunction("split", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				
				String[] splitted = args[0].getValue_str().split(args[1].getValue_str());
				
				SveValue table = new SveValue(Type.TABLE);
				
				for (int i = 0; i < splitted.length; i++) {
					SveValue value = new SveValue(splitted[i]);
					table.table.setLocalVar(i+"", value);
				}
				
				return table;
			}
		}, globalScope, "split(string, delimeter): Splits the string");
	
		addJavaFunction("toupper", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].getValue_str().toUpperCase());
			}
		}, globalScope, "toupper(string): Changes all characters to upper case");
		
		addJavaFunction("tolower", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].getValue_str().toLowerCase());
			}
		}, globalScope, "tolower(string): Changes all characters to lower case");
		
		addJavaFunction("table", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				SveValue table = new SveValue(Type.TABLE);
				for (int i = 0; i < args.length; i++) {
					SveValue value = args[i];
					table.table.setLocalVar(i+"", value);
				}
				return table;
			}
		}, globalScope, "table(elements): Creates a new array");
		addJavaFunction("len", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException, SveRuntimeException {
				if (args.length < 1) return new SveValue(Type.NIL);
				int length = 0;
				while (args[0].table.getVar((length++)+"") != null && args[0].table.getVar((length-1)+"").type != Type.NIL);
				return new SveValue(length-1);
			}
		}, globalScope, "len(table): Returns the length of the parameter table (not the size of the table)");
		addJavaFunction("keys", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				
				Collection<String> values = args[0].table.variables().keySet();
				
				SveValue table = new SveValue(Type.TABLE);
				int i = 0;
				for (String val : values) {
					table.table.setLocalVar(i++ +"", new SveValue(val));
				}
				return table;
			}
		}, globalScope, "keys(table): Returns an array that contains all keys from the parameter table");
		addJavaFunction("values", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				
				Collection<SveValue> values = args[0].table.variables().values();
				
				SveValue table = new SveValue(Type.TABLE);
				int i = 0;
				for (SveValue val : values) {
					table.table.setLocalVar(i++ +"", val);
				}
				return table;
			}
		}, globalScope, "values(table): Returns an array that contains all values of the parameter table");
		addJavaFunction("defined", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				if (args.length == 1) {
					return new SveValue((globalScope.variables().containsKey(args[0].getValue_str()))?true:false);
				}
				return new SveValue((args[0].table.variables().containsKey(args[1].getValue_str()))?true:false);
			}
		}, globalScope, "defined(table, index): Contains the parameter table the parameter index");
		addJavaFunction("define", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				if (args.length == 2) {
					globalScope.variables().put(args[0].getValue_str(), args[1]);
				}
				args[0].table.variables().put(args[1].getValue_str(), args[2]);
				return args[0];
			}
		}, globalScope, "define(table, index, val): Adds a new index to the parameter table");
		addJavaFunction("undefine", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				if (args.length == 1) {
					globalScope.variables().remove(args[0].getValue_str());
				}
				args[0].table.variables().remove(args[1].getValue_str());
				return new SveValue(Type.NIL);
			}
		}, globalScope, "undefine(table, index): Removes an index from the paremeter table");
		addJavaFunction("new", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return args[0].copy();
			}
		}, globalScope, "new(table): Creates a new copy of the parameter table");
		addJavaFunction("setpt", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				args[0].table.prototypeScope = args[1].type == Type.NIL ? null : args[1].table;
				return args[0];
			}
		}, globalScope, "setpt(table, prototype): Sets the prototype of the parameter table");
		addJavaFunction("getpt", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].table.prototypeScope);
			}
		}, globalScope, "getpt(table): Returns the prototype of the parameter table");
		
		addJavaFunction("setsup", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				args[0].table.superScope = args[1].type == Type.NIL ? null : args[1].table;
				return args[0];
			}
		}, globalScope, "setsup(scope, scope): Sets the super scope of the parameter scope");
		addJavaFunction("getsup", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].table.superScope);
			}
		}, globalScope, "getsup(scope): Returns the super scope of the parameter scope");
		
		addJavaFunction("setptf", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				args[0].table.inter = SveInterpreter.this;
				args[0].table.prototypeScope_f = args[1].type == Type.NIL ? null : args[1];
				return args[0];
			}
		}, globalScope, "setptf(table, function): Sets the prototype of the parameter table");
		addJavaFunction("getptf", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return args[0].table.prototypeScope_f;
			}
		}, globalScope, "getptf(table): Returns the prototype of the parameter table");
		
		addJavaFunction("setls", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 2) return new SveValue(Type.NIL);
				args[0].localScope = args[1].type == Type.NIL ? null : args[1].table;
				return args[0];
			}
		}, globalScope, "setls(function, scope): Sets the defination scope of a function instance");
		addJavaFunction("getls", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].localScope);
			}
		}, globalScope, "getls(function): Returns the defination scope of a function instance");
		
		globalScope.setVar("disk", new SveValue(Type.TABLE));
		
		addJavaFunction("disk.save", new SveApiFunction() {
			
			private BCXDataType createBCX(SveValue val) throws SveRuntimeException {
				
				switch (val.type) {
				case BOOLEAN:
				{
					BCXByte byt = (BCXByte) BCXByte.get();
					byt.setValue((byte) (val.getValue_bool()?1:0));
					return byt;
				}
				case DOUBLE:
				{
					BCXFloat byt = (BCXFloat) BCXFloat.get();
					byt.setValue((float) val.getValue());
					return byt;
				}
				case STRING:
				{
					BCXString byt = (BCXString) BCXString.get();
					byt.setValue(val.getValue_str());
					return byt;
				}
				case TABLE:
				{
					BCXDataList byt = (BCXDataList) BCXDataList.get();
					for (Entry<String, SveValue> e : val.table.variables().entrySet()) {
						byt.addChild(e.getKey(), createBCX(e.getValue()));
					}
					return byt;
				}
				
				default:
					throw new SveRuntimeException(-1, ExceptionType.OTHER, "Illegal data");
				}
			}
			
			@Override
			public SveValue call(SveValue[] args) throws SveRuntimeException {
				if (args.length < 2) return new SveValue(Type.NIL);
				try {
					createBCX(args[1]).write(new DataOutputStream(System.out));
				} catch (IOException e) {
					e.printStackTrace();
					throw new SveRuntimeException(-1, ExceptionType.OTHER, e.getMessage());
				}
				return new SveValue(Type.NIL);
			}
		}, globalScope, "disk.save(name, data): Saves table to the disk");
		
		addJavaFunction("disk.load", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) {
				if (args.length < 1) return new SveValue(Type.NIL);
				return new SveValue(args[0].localScope);
			}
		}, globalScope, "disk.load(name): Loads a table from the disk");
		
		// java-rajapinta
		
		if (includeJavaAPI) (this.javaInterface = new JavaInterface(this)).addJIFunctions();
		
		// "vakiot"
		
		globalScope.setVar("true", new SveValue(true));
		globalScope.setVar("false", new SveValue(false));
		
		globalScope.setVar("nil", new SveValue(Type.NIL));
		
		// scopet
		
		SveValue global = new SveValue(Type.TABLE);
		global.table = globalScope;
		
		globalScope.setVar("$", global);
		globalScope.setVar("$$", global);
		
	}
	
	public Stack<String> callStack = new Stack<>();
	public Stack<String> functionStack = new Stack<>();
	
	private void callStackPush(String file, int line) {
		if (functionStack.peek() != null)
			callStack.push(file+":"+line + " in " + functionStack.peek());
		else
			callStack.push(file+":"+line);
	}
	
	private void callStackPop() {
		callStack.pop();
	}
	
	public void printStackTrace() {	
		for (int i = callStack.size()-1; i >= 0; i--) {
			System.err.println("\t" + callStack.get(i));
		}
	}
	
	public void interpret(StartTree tree) throws SveVariableNotFoundException, SveRuntimeException {

		/*try {
			addJavaFunction(System.class.getField("out").getClass().getMethod("print", String.class), scope);
		} catch (NoSuchMethodException | SecurityException | NoSuchFieldException e) {
			e.printStackTrace();
		}*/
		
		callStack.push("<runtime>");
		functionStack.push("<runtime>");
		
		for (FunctionTree t : tree.functions) {
			interpretFunction(t, globalScope);
		}
		
		functionStack.push("<main>");
		
		
		for (LineTree t : tree.lines) {
			interpretLine(t, globalScope);
		}
		
		functionStack.pop();
		callStack.pop();
	}
	
	public SveValue interpretStartTree(StartTree tree) throws SveVariableNotFoundException, SveRuntimeException {
		
		return interpretStartTree(tree, globalScope);
	}
	
	public SveValue interpretStartTree(StartTree tree, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {

		/*try {
			addJavaFunction(System.class.getField("out").getClass().getMethod("print", String.class), scope);
		} catch (NoSuchMethodException | SecurityException | NoSuchFieldException e) {
			e.printStackTrace();
		}*/
		
		callStack.push("<runtime>");
		functionStack.push("<runtime>");
		
		for (FunctionTree t : tree.functions) {
			interpretFunction(t, scope);
		}
		
		functionStack.push("<main>");
		
		SveValue val = null;
		
		for (LineTree t : tree.lines) {
			val = interpretLine(t, scope);
		}
		
		functionStack.pop();
		
		functionStack.pop();
		callStack.pop();
		
		return val;
	}
	
	public SveValue interpretFunction(FunctionTree f, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		callStack.push("<runtime/registerFunction>");
		SveValue l = new SveValue(Type.FUNCTION);
		l.line = f.line;
		l.parameters = f.paramters;
		l.localScope = scope;
		l.fname = f.name;
		setToTable(f.name, l, scope);
		callStack.pop();
		return l;
	}
	
	public void setToTable(String name, SveValue val, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		while (name.contains(".")) {
			SveValue s = scope.getVar(name.substring(0, name.indexOf('.')));
			if (s == null) {
				throw new SveVariableNotFoundException(-1, name.substring(0, name.indexOf('.')));
			}
			scope = s.table;
			name = name.substring(name.indexOf('.')+1);
		}
		scope.setLocalVar(name, val);
	}
	
	public HashMap<String, String> functionHelp = new HashMap<>();
	
	public void addJavaFunction(String name, SveApiFunction m, SveScope scope, String help) {
		SveValue l = new SveValue(Type.FUNCTION_JAVA);
		l.method = m;
		try {
			setToTable(name, l, scope);
		} catch (SveVariableNotFoundException e) {
			e.printStackTrace();
		} catch (SveRuntimeException e) {
			e.printStackTrace();
		}
		
		functionHelp.put(name, help);
	}
	
	private SveValue returnValue = null;
	
	public SveValue runFunction(String name, SveValue...args) throws SveVariableNotFoundException, SveRuntimeException {
		SveValue function = globalScope.getVar(name);
		
		return runFunction(function, args);
	}
	
	public SveValue runFunction(SveValue function, SveValue...args) throws SveVariableNotFoundException, SveRuntimeException {
		
		return runFunction(function, new SveScope(globalScope), args);
	}
	
	public SveValue runFunction(SveValue function, SveScope scope, SveValue...args) throws SveVariableNotFoundException, SveRuntimeException {
		
		return runFunction(function, null, scope, args);
	}
	
	public SveValue runFunction(SveValue function, SveValue self, SveScope scope, SveValue...args) throws SveVariableNotFoundException, SveRuntimeException {
		if (function.type == Type.FUNCTION) {
			functionStack.push(function.fname);
			
			SveScope s = new SveScope(function.localScope==null?globalScope:function.localScope);
			
			for (int j = 0; j < function.parameters.size(); j++) {	// TODO unchecked array
				if (j >= args.length) s.setLocalVar(function.parameters.get(j).name, new SveValue(Type.NIL));
				//if (j >= args.length) throw new SveRuntimeException(-1, ExceptionType.WRONG_ARGS, "Wrong number of arguments!");
				else s.setLocalVar(function.parameters.get(j).name, (SveValue) args[j]);
			}
			SveValue f2 = new SveValue(Type.FUNCTION);
			f2.line = function.line;
			f2.parameters = function.parameters;
			f2.localScope = function.localScope;
			s.setLocalVar("$$", new SveValue(s));
			s.setLocalVar("$self", f2);
			s.setLocalVar("$parent", new SveValue(scope));
			
			if (self != null) {
				s.setLocalVar("$obj", self);
			}
			
			// argument table
			SveValue args1 = new SveValue(Type.TABLE);
			for (int j = 0; j < args.length; j++) {
				args1.table.setVar(""+j, args[j]);
			}
			s.setLocalVar("$args", args1);
			
			s.setLocalVar("$freturn", new SveValue(Type.NIL));
			returnValue = null;
			
			interpretLine(function.line, s);
			
			functionStack.pop();
			
			SveValue a = null;
			a = returnValue == null ? s.getVar("$freturn") : returnValue;
			returnValue = null;
			return a;
		} else if (function.type == Type.FUNCTION_JAVA) {
			//ArrayList<SveValue> l = new ArrayList<>();
			//l.addAll(Arrays.asList(args));
			//return function.method.call(l.toArray(new SveValue[l.size()]));
			functionStack.push(function.fname);
			SveValue ret = function.method.call(args);
			functionStack.pop();
			return ret;
		}
		
		else throw new SveRuntimeException(-1, ExceptionType.TYPE_COER, "Can't cast to function");
	}
	
	private static int recursion_meter = 0;
	
	public static String realtimeReturnVal(SveValue val) {
		if (val.type == Type.STRING) return "\"" + val.getValue_str() + "\"";
		if (val.type == Type.NIL) return "nil";
		if (val.type == Type.TABLE) {
			
			if (recursion_meter > 3) return "{...}";
			
			String s = "{";
			
			Set<Entry<String,SveValue>> a = val.table.variables().entrySet();
			List<Entry<String, SveValue>> list = new ArrayList<>();
			for (Entry<String, SveValue> e:a) list.add(e);
			Collections.sort(list, new Comparator<Entry<String, SveValue>>() {

				@Override
				public int compare(Entry<String, SveValue> o1,
						Entry<String, SveValue> o2) {
					return o1.getKey().compareTo(o2.getKey());
				}

			});
			boolean first = true;
			for (Entry<String, SveValue> e:list) {
				recursion_meter++;
				if (!first) s += ", "; first = false;
				s += e.getKey() + "=" + realtimeReturnVal(e.getValue());
				recursion_meter--;
			}
			
			return s + "}";
				
			
		}
		return val.getValue_str();
	}
	
	/** Onko tulkki debugger-tilassa? */
	public boolean debugger;
	
	private List<String> breakpoints = new ArrayList<>();
	private boolean linebyline = true;
	private Scanner scanner = new Scanner(System.in);
	
	private SveValue expreval(String code, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		
		TokenScanner s = new TokenScanner();
		s.setSpecialTokens(SveParser.OPERATORS);
		s.setBSpecialTokens(SveParser.OPERATORS2);
		s.setComments(true);
		s.setFile("<stdin>");
		s.init(code);
		
		SveTree.ExpressionTree tree = new ExpressionTree();
		try {
			tree.parse(s);
		} catch (SyntaxError e) {
			e.printStackTrace();
		}
		
		return interpretExpression(tree, scope);
	}
	
	private void debugger(String file, int line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		PrintStream out = System.out;
		
		if (linebyline || breakpoints.contains(file + ":" + line)) {
			linebyline = true;
			
			loop: while (true) {
			
				out.print(file + ":" + line + " in " + functionStack.peek() + " dbg> ");
				
				String cmd = scanner.next();
				switch (cmd) {
				case "dump":
					for (Entry<String, SveValue> val : scope.variables().entrySet()) {
						
						out.println(val.getKey() + "=" + (val.getKey().startsWith("$") ? "{...}" : realtimeReturnVal(val.getValue())));
					}
					break;
				case "scope":
					switch (scanner.next()) {
					case "local":
						for (Entry<String, SveValue> val : scope.variables().entrySet()) {
						
							out.println(val.getKey() + "=" + (val.getKey().startsWith("$") ? "{...}" : realtimeReturnVal(val.getValue())));
						}
						break;
					case "global":
						for (Entry<String, SveValue> val : globalScope.variables().entrySet()) {
						
							out.println(val.getKey() + "=" + (val.getKey().startsWith("$") ? "{...}" : realtimeReturnVal(val.getValue())));
						}
						break;
					case "function":
						SveScope sc = scope;
						do {
							for (Entry<String, SveValue> val : sc.variables().entrySet()) {
								
								out.println(val.getKey() + "=" + (val.getKey().startsWith("$") ? "{...}" : realtimeReturnVal(val.getValue())));
							}
						} while (!sc.variables().containsKey("$parent") && (sc = scope.superScope) != null);
					}
					break;
				case "sve":
					out.println("= " + realtimeReturnVal(expreval(scanner.nextLine(), scope)));
					break;
				case "breakpoint":
				case "break":
					String breakpoint = scanner.next();
					if (!breakpoint.contains(":")) breakpoint = file + ":" + breakpoint;
					breakpoints.add(breakpoint);
					break;
				case "stack":
					for (int i = callStack.size()-1; i >= 0; i--) {
						System.err.println("\t" + callStack.get(i));
					}
					break;
				case "run":
					linebyline = false;
					break loop;
				case "next":
				case "n":
				case "step":
					linebyline = true;
					break loop;
				case "help":
					System.err.println("Available commands: breakpoint, dumb, help, next, scope, stack, step, sve");
					break;
				}
			}
		}
	}
	
	public SveValue interpretLine(LineTree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		
		if (debugger) {
			debugger(line.file, line.line, scope);
		}
		
		switch (line.type) {
		case EXPRESSION:
			try {
				return interpretExpression(line.expression, scope);
			} catch (Exception ex) {
				if (debugger) {
					ex.printStackTrace();
					linebyline=true;
					debugger(line.file, line.line, scope);
					break;
				} else throw ex;
			}
		case BLOCK:{
			BlockTree t = line.block;
			SveScope newScope = new SveScope(scope);
			newScope.setLocalVar("$$", new SveValue(newScope));
			for (int i = 0; i < t.lines.size(); i++) {
				SveValue v = interpretLine(t.lines.get(i), newScope);
				if (v != null && v.type == Type.BREAK) {
					return v;
				}
				if (v != null && v.type == Type.RETURN) return v;
			}
			break;}
		case IF:{
			IfTree t = line._if;
			SveValue c = interpretExpression(t.condition, scope);
			if (c.getValue_bool()) {
				return interpretLine(t.line, scope);
			} else if (t.line_else != null) {
				return interpretLine(t.line_else, scope);
			}
		} break;
		case IFWHILE:{
			IfWhileTree t = line.ifWhile;
			SveValue c = interpretExpression(t.condition, scope);
			if (c.getValue_bool()) {
				do {
					SveValue v = interpretLine(t.line, scope);
					if (v != null && v.type == Type.BREAK) break;
					if (v != null && v.type == Type.RETURN) return v;
					c = interpretExpression(t.condition, scope);
				} while (c.getValue_bool());
			} else if (t.line_else != null) {
				return interpretLine(t.line_else, scope);
			}
		} break;
		case WHILE:{
			WhileTree t = line._while;
			SveValue c = interpretExpression(t.condition, scope);
			while (c.getValue_bool()) {
				SveValue v = interpretLine(t.line, scope);
				if (v != null && v.type == Type.BREAK) break;
				if (v != null && v.type == Type.RETURN) return v;
				c = interpretExpression(t.condition, scope);
			}
		} break;
		case DO_WHILE:{
			DoWhileTree t = line.doWhile;
			SveValue c = null;
			do {
				SveValue v = interpretLine(t.line, scope);
				if (v != null && v.type == Type.BREAK) break;
				if (v != null && v.type == Type.RETURN) return v;
				c = interpretExpression(t.condition, scope);
			} while (c.getValue_bool());
		} break;
		case FOR:{
			ForTree t = line._for;
			interpretExpression(t.assign, scope);
			SveValue c = interpretExpression(t.condition, scope);
			while (c.getValue_bool()) {
				SveValue v = interpretLine(t.line, scope);
				if (v != null && v.type == Type.BREAK) break;
				if (v != null && v.type == Type.RETURN) return v;
				
				interpretExpression(t.increment, scope);
				
				c = interpretExpression(t.condition, scope);
			}
		} break;
		case DEF:{
			return interpretFunction(line.func, scope);
		}
		case RETURN:{
			SveValue tmp = interpretExpression(line.expression, scope);
			returnValue = tmp;
			scope.setVar("$freturn", tmp);
			SveValue v = new SveValue(Type.RETURN);
			return v;
		}
		case BREAK:
			SveValue v = new SveValue(Type.BREAK);
			return v;
		default:
			break;
		}
		return null;
	}
	
	public SveValue interpretExpression(ExpressionTree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		switch (line.operator) {
		case OPERATOR:
			SveValue value;
			ExpressionLogicTree first = line.first;
			value = interpretExpressionLogic(first, scope);
			for (int i = 0; i < line.op.size(); i++) {
				switch (line.op.get(i)) {
				case "||":
					{
						if (value.getValue_bool()) value = new SveValue(true);
						else value = new SveValue(interpretExpressionLogic(line.second.get(i), scope).getValue_bool());
					}
					break;
				case "&&":
					{
						if (!value.getValue_bool()) value = new SveValue(false);
						else value = new SveValue(interpretExpressionLogic(line.second.get(i), scope).getValue_bool());
					}
					break;

				default:
					break;
				}
			}
			return value;

		default:
			return interpretExpressionLogic(line.first, scope);
		}
	}
	
	public SveValue interpretExpressionLogic(ExpressionLogicTree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		switch (line.operator) {
		case OPERATOR:
			SveValue value;
			Expression0Tree first = line.first;
			value = interpretExpression0(first, scope);
			for (int i = 0; i < line.op.size(); i++) {
				SveValue second =  interpretExpression0(line.second.get(i), scope);
				switch (line.op.get(i)) {
				case "==":
					if (value.type == Type.DOUBLE && second.type == Type.DOUBLE) value = new SveValue(value.getValue() == second.getValue());
					else if (value.type == Type.STRING || second.type == Type.STRING) {
						SveValue v = new SveValue(value.getValue_str().equals(second.getValue_str()));
						value = v;
					} else value = new SveValue(value.equals(second));
					break;
				case "!=":
					if (value.type == Type.DOUBLE && second.type == Type.DOUBLE) value = new SveValue(value.getValue() != second.getValue());
					else if (value.type == Type.STRING || second.type == Type.STRING) {
						SveValue v = new SveValue(!value.getValue_str().equals(second.getValue_str()));
						value = v;
					} else value = new SveValue(!value.equals(second));
					break;
				case "<":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.first.first.file, line.first.first.first.first.line);
						throw new SveRuntimeException(line.first.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue() < second.getValue());
					break;
				case ">":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.first.first.file, line.first.first.first.first.line);
						throw new SveRuntimeException(line.first.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue() > second.getValue());
					break;
				case "<=":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.first.first.file, line.first.first.first.first.line);
						throw new SveRuntimeException(line.first.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue() <= second.getValue());
					break;
				case ">=":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.first.first.file, line.first.first.first.first.line);
						throw new SveRuntimeException(line.first.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue() >= second.getValue());
					break;

				default:
					break;
				}
			}
			return value;

		default:
			return interpretExpression0(line.first, scope);
		}
	}
	
	public SveValue interpretExpression0(Expression0Tree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		switch (line.operator) {
		case OPERATOR:
			SveValue value;
			Expression1Tree first = line.first;
			value = interpretExpression1(first, scope);
			for (int i = 0; i < line.op.size(); i++) {
				SveValue second =  interpretExpression1(line.second.get(i), scope);
				switch (line.op.get(i)) {
				case "+":
					if (value.type == Type.STRING || second.type == Type.STRING){
						value = new SveValue(value.getValue_str()+second.getValue_str());
					} else if (value.type == Type.DOUBLE && second.type == Type.DOUBLE) {
						value = new SveValue(value.getValue()+second.getValue());
					} else {
						callStackPush(line.first.first.first.file, line.first.first.first.line);
						throw new SveRuntimeException(line.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					}
					break;
				case "-":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.first.file, line.first.first.first.line);
						throw new SveRuntimeException(line.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue()-second.getValue());
					break;

				default:
					break;
				}
			}
			return value;

		default:
			return interpretExpression1(line.first, scope);
		}
	}
	
	public SveValue interpretExpression1(Expression1Tree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		switch (line.operator) {
		case OPERATOR:
			SveValue value;
			Expression2Tree first = line.first;
			value = interpretExpression2(first, scope);
			for (int i = 0; i < line.op.size(); i++) {
				SveValue second =  interpretExpression2(line.second.get(i), scope);
				switch (line.op.get(i)) {
				case "*":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.file, line.first.first.line);
						throw new SveRuntimeException(line.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue()*second.getValue());
					break;
				case "/":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.file, line.first.first.line);
						throw new SveRuntimeException(line.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue()/second.getValue());
					break;
				case "%":
					if (value.type != Type.DOUBLE || second.type != Type.DOUBLE){
						callStackPush(line.first.first.file, line.first.first.line);
						throw new SveRuntimeException(line.first.first.line, ExceptionType.TYPE_COER, "Can't convert to number");
					} else value = new SveValue(value.getValue()%second.getValue());
					break;

				default:
					break;
				}
			}
			return value;

		default:
			return interpretExpression2(line.first, scope);
		}
	}
	
	public SveValue interpretExpression2(Expression2Tree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		switch (line.operator) {
		case OPERATOR:
			SveValue value, value2 = null;
			PrimaryTree first = line.first;
			value = interpretPrimary(first, scope);
			for (int i = 0; i < line.op.size(); i++) {
				switch (line.op.get(i)) {
				case "->":
				case "[":
					value2 = value;
					SveValue second =  interpretPrimary(line.second.get(i), scope);
					
					if (i < line.op.size()-1 && line.op.get(i+1).equals(":")) {
						value = value.table.setVar(second.getValue_str(), interpretExpression(line.exp, scope));
					} else {
						value = value.table.getVar(second.getValue_str());
						if (value == null) {
							callStackPush(first.file, first.line);
							throw new SveRuntimeException(line.line, ExceptionType.UNKNW_INDX, "Unknown index '" + second.getValue_str() + "'");
						}
					}
					break;
				case ":":
					
					break;
				case "::":
				case ".":
					if (value == null) {
						callStackPush(first.file, first.line);
						throw new SveRuntimeException(line.line, ExceptionType.NULL_PTR, "Can't call a null value");
					}
					
					{
						SveValue function = value;
						
						SveValue[] args = new SveValue[line.arguments.get(i).size()];
						for (int j = 0; j < args.length; j++) {
							args[j] = interpretExpression(line.arguments.get(i).get(j), scope);
						}
						
						callStackPush(first.file, first.line);
						
						value = runFunction(function, value2, scope, args);
						
						callStackPop();
					}
					break;
				case "o->m":
					if (value == null) {
						callStack.push(""+first.file+":"+first.line);
						
						throw new SveRuntimeException(line.line, ExceptionType.NULL_PTR, "Can't apply a function to a null value");
					}
					
					{
						SveValue arg1 = value;
						SveValue function =  interpretPrimary(line.second.get(i), scope);
						
						SveValue[] args = new SveValue[1+line.arguments.get(i).size()];
						args[0] = arg1;
						for (int j = 1; j < args.length; j++) {
							args[j] = interpretExpression(line.arguments.get(i).get(j-1), scope);
						}
						
						callStackPush(first.file, first.line);
						
						value = runFunction(function, scope, args);
						
						callStackPop();
					}
					
					break;
				default:
					break;
				}
				
			}
			return value;

		default:
			return interpretPrimary(line.first, scope);
		}
	}
	
	public SveValue interpretPrimary(PrimaryTree line, SveScope scope) throws SveVariableNotFoundException, SveRuntimeException {
		switch (line.operator) {
		case ASSIGN:
			SveValue v1 = interpretExpression(line.second, scope);
			if (v1 != null) {
				scope.setVar(line.first, v1);
				return v1;
			}
			else {
				callStackPush(line.file, line.line);
				throw new SveRuntimeException(line.line, ExceptionType.NULL_PTR, "Can't assign a null value.");
			}
		case EXPRESSION:
			return interpretExpression(line.second, scope);
		case VARIABLE:
			if (scope.getVar(line.first) != null) {
				return scope.getVar(line.first);
			} else {
				try {
					return new SveValue(Double.parseDouble(line.first));
				} catch (NumberFormatException ex) {
					callStackPush(line.file, line.line);
					throw new SveVariableNotFoundException(line.line, line.first);
				}
			}
		case LOCAL_ASSIGN:{
			SveValue v2 = interpretExpression(line.second, scope);
			if (v2 != null) {
				scope.setLocalVar(line.first, v2);
				return v2;
			}
			else {
				callStackPush(line.file, line.line);
				throw new SveRuntimeException(line.line, ExceptionType.NULL_PTR, "Can't assign a null value.");
			}
		}
		case PREFIX:
			if (scope.getVar(line.first) != null) {
				SveValue v = scope.getVar(line.first);
				SveValue n = new SveValue(line.fix.equals("++")?v.getValue()+1:v.getValue()-1);
				scope.setVar(line.first, n);
				return n;
			}
			else {
				callStackPush(line.file, line.line);
				throw new SveRuntimeException(line.line, ExceptionType.NULL_PTR, "Can't modify a null value");
			}
		case POSTFIX:
			if (scope.getVar(line.first) != null) {
				SveValue v = scope.getVar(line.first);
				SveValue n = new SveValue(line.fix.equals("++")?v.getValue()+1:v.getValue()-1);
				scope.setVar(line.first, n);
				return v;
			}
			else {
				callStackPush(line.file, line.line);
				throw new SveRuntimeException(line.line, ExceptionType.NULL_PTR, "Can't modify a null value");
			}
		case NOT:
			SveValue v = interpretExpression2(line.expr, scope);
			return new SveValue(!v.getValue_bool());
		case REVERSE:
			SveValue v2 = interpretExpression2(line.expr, scope);
			return new SveValue(-v2.getValue());
		case STRING:
			return new SveValue(line.first);
		case ANONYMOUS_FUNCTION:
			SveValue l = new SveValue(Type.FUNCTION);
			l.line = line.function.line;
			l.parameters = line.function.paramters;
			l.localScope = scope;
			return l;
		case TABLE:
			SveValue ret1 = new SveValue(Type.TABLE);
			ret1.localScope = scope;
			for (int i = 0; i < line.arguments.size(); i++) {
				ret1.table.setVar(line.names.get(i), interpretExpression(line.arguments.get(i), scope));
			}
			return ret1;
		case LIST:
			SveValue ret = new SveValue(Type.TABLE);
			ret.localScope = scope;
			for (int i = 0; i < line.arguments.size(); i++) {
				ret.table.setVar(""+i, interpretExpression(line.arguments.get(i), scope));
			}
			return ret;
		case IF:
			SveValue cond = interpretExpression(line.second, scope);
			if (cond.getValue_bool()) {
				return interpretExpression(line.third, scope);
			}
			else {
				return interpretExpression(line.fourth, scope);
			}
		default:
			break;
		}
		return new SveValue(Type.NIL);
	}
}
