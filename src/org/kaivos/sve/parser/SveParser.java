package org.kaivos.sve.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Set;

import jline.ConsoleReader;

import org.kaivos.sc.TokenScanner;
import org.kaivos.stg.error.SyntaxError;
import org.kaivos.stg.error.UnexpectedTokenSyntaxError;
import org.kaivos.sve.interpreter.SveInterpreter;
import org.kaivos.sve.interpreter.api.SveApiFunction;
import org.kaivos.sve.interpreter.core.SveValue;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.interpreter.exception.SveVariableNotFoundException;
import org.kaivos.sve.parser.SveTree.StartTree;

public class SveParser {
	
	private static SveInterpreter inter;
	public static final String SVE_VERSION = "Sve 1.5 Realtime Interpreter - (c) 2014 Iikka Hauhio - All rights reserved";
	
	public static final char[] OPERATORS = new char[]{';', '<', '>', '(', ')', ',', ':', '+', '-', '*', '/', '%', '=', '&', '|', '{', '}', '.', '!', '[', ']'};
	public static final String[] OPERATORS2 = new String[]{"->", "=>", "==", "!=", "&&", "||", "<=", ">=", "++", "--", "::"};
	
	/**
	 * @param args
	 * @throws SveRuntimeException 
	 * @throws SveVariableNotFoundException 
	 */
	public static void main(String[] args) throws SveVariableNotFoundException, SveRuntimeException {
		
		inter = new SveInterpreter(true);
		
		
		String file = "<stdin>";
		List<String> sveargs = new ArrayList<>();
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-f":
				file = args[++i];
				break;
			case "-D":
				String var = args[++i];
				String val = args[++i];
				SveValue sval;
				try {
					sval = new SveValue(Double.parseDouble(val));
				} catch (NumberFormatException ex) {
					sval = new SveValue(val);
				}
				inter.globalScope.setLocalVar(var, sval);
				break;
			case "-d":
				inter.debugger = true;
				break;
			case "-h":
			case "--help":
				System.out.println("usage: sve OPTIONS ARGUMENTS");
				System.out.println("options:");
				System.out.println("-f file     Uses <file> as the input");
				System.out.println("-D var val  Defines a variable");
				System.out.println("-d          Enable debugger mode");
				System.out.println("-h          Prints this help text");
				System.out.println("--help      --··--");
				System.out.println("-v          Prints the version number");
				System.out.println("--version   --··--");
				break;
			case "-v":
			case "--version":
				System.out.println(SVE_VERSION);
				break;
			default:
				sveargs.add(args[i]);
			}
		}
		
		for (int i = 0; i < sveargs.size(); i++) {
			inter.globalScope.setLocalVar("@"+(i+1), new SveValue(sveargs.get(i)));
		}

		inter.globalScope.setLocalVar("@@", inter.javaInterface.sve_GetFromJavaObject(sveargs.toArray(new String[sveargs.size()])));
		
		if (file.equals("<stdin>")) {
			try {
				openRealtimeInterpreter();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		String textIn = "";
		
		try (BufferedReader in = new BufferedReader(new FileReader(new File(file)))) {
			while (in.ready()) textIn += in.readLine() + "\n";
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		TokenScanner s = new TokenScanner();
		s.setSpecialTokens(OPERATORS);
		s.setBSpecialTokens(OPERATORS2);
		s.setComments(true);
		s.setFile(file);
		s.init(textIn);
		//System.out.println(s.getTokenList());
		SveTree.StartTree tree = new StartTree();
		
		try {
			tree.parse(s);
			
			try {
				inter.interpret(tree);
			} catch (SveRuntimeException e) {
				printError(inter, e);
			} catch (Exception e) {
				e.printStackTrace();
				inter.printStackTrace();
				inter.callStack.clear();
			}
		} catch (UnexpectedTokenSyntaxError e) {
			printError(s, e);
		} catch (SyntaxError e) {
			printError(s, e);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		{
			//SveCodeGenerator.CGStartTree gen = new SveCodeGenerator.CGStartTree(tree);
			//System.out.println(gen.generate(""));
		}
		
		
	}
	
	private static void openRealtimeInterpreter() throws IOException, SveVariableNotFoundException, SveRuntimeException {
		ConsoleReader r = new ConsoleReader();
        r.setBellEnabled(false);
		
		//BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		final PrintStream o = System.out;
		
		inter.addJavaFunction("help", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException,
					SveRuntimeException {
				
				if (args.length == 1) {
					o.println(inter.functionHelp.get(args[0].getValue_str()));
					return null;
				}
				
				o.println("-- Sve interpreter help --");
				o.println("Try  keep  commands one  line  long");
				o.println("If multiple lines needed, make sure");
				o.println("that the  interpreter   understands");
				o.println("that the  statement  does  not end.");
				o.println("Good  way is to  end  lines  with a");
				o.println("brace like '{'.");
				o.println();
				o.println(inter.functionHelp.get("help"));
				
				return null;
			}
		}, inter.globalScope, "help(function_name): Displays a small help text");
		
		inter.addJavaFunction("allhelp", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException,
					SveRuntimeException {
				
				Set<Entry<String, String>> s = inter.functionHelp.entrySet();
				List<Entry<String, String>> list = new ArrayList<>();
				for (Entry<String, String> e:s) list.add(e);
				Collections.sort(list, new Comparator<Entry<String, String>>() {

					@Override
					public int compare(Entry<String, String> o1,
							Entry<String, String> o2) {
						return o1.getKey().compareTo(o2.getKey());
					}

				});
				for (Entry<String, String> e:list) o.println(e.getValue());
				
				return null;
			}
		}, inter.globalScope, "allhelp(): Displays all function help texts");
		
		class exit {
			boolean exit = false;
		} final exit exit = new exit();
		
		inter.addJavaFunction("exit", new SveApiFunction() {
			
			@Override
			public SveValue call(SveValue[] args) throws SveVariableNotFoundException,
					SveRuntimeException {
				
				exit.exit = true;
				
				return null;
			}
		}, inter.globalScope, "allhelp(): Displays all function help texts");
		
		o.println(SVE_VERSION);
		o.println("Type \"help()\" or \"help(function_name)\" to get help.");
		
		String line = "";
		boolean addMode = false;
		
		//o.print("> ");
		while (!exit.exit) {
			if (!addMode) line = r.readLine("> ");
			else line += "\n" + r.readLine(">>> ");
			if (line == null) break;
			if (line.equals("exit")) {
				o.println("Use `exit()' or Control-D to exit.");
			}
			TokenScanner s = new TokenScanner();
			s.setSpecialTokens(OPERATORS);
			s.setBSpecialTokens(OPERATORS2);
			s.setComments(true);
			s.setFile("<stdin>");
			
			try {
				s.init(line);
				//System.out.println(s.getTokenList());
				SveTree.StartTree tree = new StartTree();
				
				tree.parse(s);
				SveValue val = inter.interpretStartTree(tree);
				if (val != null) o.println("= " + SveInterpreter.realtimeReturnVal(val));
				addMode = false;
				
			} catch (UnexpectedTokenSyntaxError e) {
				
				if (e.getToken().equals("<EOF>")) {
					addMode = true;
					//o.print(">> ");
					continue;
				} else addMode = false;
					
				printError(s, e);
			} catch (SyntaxError e) {
				printError(s, e);
			} catch (SveRuntimeException e) {
				printError(inter, e);
			} catch (Exception ex) {
				ex.printStackTrace();
				inter.printStackTrace();
				inter.callStack.clear();
			}
			
			System.err.flush();
			
			if (exit.exit) break;
			
			//o.print("> ");
		}
	}

	public static SveTree.StartTree parse(String file) {
		
		return parse(new File(file));
	}
	
	public static SveTree.StartTree parse(File file) {
		String textIn = "";
		
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			while (in.ready()) textIn += in.readLine() + "\n";
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		TokenScanner s = new TokenScanner();
		s.setSpecialTokens(new char[]{';', '<', '>', '(', ')', ',', ':', '+', '-', '*', '/', '%', '=', '&', '|', '{', '}', '.', '!', '[', ']', '$'});
		s.setBSpecialTokens(new String[]{"->", "=>", "==", "!=", "&&", "||", "<=", ">=", "++", "--", "::"});
		s.setComments(true);
		s.setFile(file.getAbsolutePath());
		s.init(textIn);
		//System.out.println(s.getTokenList());
		SveTree.StartTree tree = new StartTree();
		
		try {
			tree.parse(s);
			return tree;
		} catch (UnexpectedTokenSyntaxError e) {
			printError(s, e);
		} catch (SyntaxError e) {
			printError(s, e);
		} 
		return null;
	}
	
	public static SveTree.StartTree parseText(String txt) throws SyntaxError {
		
		TokenScanner s = new TokenScanner();
		s.setSpecialTokens(OPERATORS);
		s.setBSpecialTokens(OPERATORS2);
		s.setComments(true);
		s.init(txt);
		//System.out.println(s.getTokenList());
		SveTree.StartTree tree = new StartTree();
		
		tree.parse(s);
		return tree;
		
	}
	
	public static void printError(TokenScanner s, SyntaxError e) {
		System.err.println("; E: [" + e.getFile() + ":" + e.getLine() + "] " + e.getMessage());
		System.err.println(";    [" + e.getFile() + ":" + e.getLine() + "] Line: \t"
				+ s.getLine(e.getLine() - 1).trim());
	}
	
	public static void printError(TokenScanner s, UnexpectedTokenSyntaxError e) {
		if (e.getExceptedArray() == null) {
			System.err.println("; E: [" + e.getFile() + ":" + e.getLine()
					+ "] Syntax error on token '" + e.getToken()
					+ "', expected '" + e.getExcepted() + "'");
		} else {
			System.err.println("; E: [" + e.getFile() + ":" + e.getLine()
					+ "] Syntax error on token '" + e.getToken()
					+ "', expected one of:");
			for (String token : e.getExceptedArray()) {
				System.err.println(";    [Line " + e.getLine() + "] \t\t'"
						+ token + "'");
			}
		}

		System.err.println(";    [" + e.getFile() + ":" + e.getLine() + "] Line: '"
				+ s.getLine(e.getLine() - 1).trim() + "'");
	}
	
	public static void printError(SveInterpreter interpreter, SveRuntimeException e) {
		System.err.println("[E] " + e.getMessage());
		interpreter.printStackTrace();
		interpreter.callStack.clear();
	}
}
