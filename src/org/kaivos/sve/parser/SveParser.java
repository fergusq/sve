package org.kaivos.sve.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.kaivos.sc.TokenScanner;
import org.kaivos.stg.error.SyntaxError;
import org.kaivos.stg.error.UnexpectedTokenSyntaxError;
import org.kaivos.sve.interpreter.SveInterpreter;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.parser.SveTree.StartTree;

public class SveParser {
	
	public static final String SVE_VERSION = "Sve 1.6 Realtime Interpreter - (c) 2015 Iikka Hauhio - All rights reserved";
	
	public static final char[] OPERATORS = new char[]{';', '<', '>', '(', ')', ',', ':', '+', '-', '*', '/', '%', '^', '=', '&', '|', '{', '}', '.', '!', '[', ']'};
	public static final String[] OPERATORS2 = new String[]{"->", "=>", "==", "!=", "&&", "||", "<=", ">=", "++", "--", "::"};
	
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
		s.setSpecialTokens(new char[]{';', '<', '>', '(', ')', ',', ':', '+', '-', '*', '/', '%', '^', '=', '&', '|', '{', '}', '.', '!', '[', ']'});
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
