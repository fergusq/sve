package org.kaivos.sve.parser;

import java.util.ArrayList;
import java.util.Arrays;

import org.kaivos.parsertools.ParserTree;
import org.kaivos.sc.TokenScanner;
import org.kaivos.stg.error.SyntaxError;
import org.kaivos.stg.error.UnexpectedTokenSyntaxError;

public class SveTree extends ParserTree {


	
	/*
	 * Start = {
	 * 		LINE*
	 * }
	 */
	public static class StartTree extends TreeNode {
		
		public ArrayList<LineTree> lines = new ArrayList<LineTree>();
		public ArrayList<FunctionTree> functions = new ArrayList<FunctionTree>();
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			while (!seek(s).equals("<EOF>")) {
				/*if (seek(s).equals("def")) {
					FunctionTree t = new FunctionTree();
					t.parse(s);
					functions.add(t);
					continue;
				}*/
				LineTree t = new LineTree();
				t.parse(s);
				lines.add(t);
			}
			accept("<EOF>", s);
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}

	public static boolean TraditionalSyntax = true;
	
	/*
	 * Function = {
	 * 		"def" ":" NAME "." PARAMETERS LINE
	 * }
	 */
	public static class FunctionTree extends TreeNode {

		public String name = null;
		
		public ArrayList<ParameterTree> paramters = new ArrayList<ParameterTree>();
		public LineTree line = new LineTree();
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			if (TraditionalSyntax) {
				accept("def", s);
				
				accept(":", s);
				
				name = next(s);
				while (seek(s).equals(".")) {
					accept(".", s);
					name += ("." + next(s));
				}
				
				if (seek(s).equals("(")) { accept("(", s);
					if (!seek(s).equals(")"))
						while (true) {
							ParameterTree t = new ParameterTree();
							t.parse(s);
							paramters.add(t);

							String str = accept(new String[] { ",", ")" }, s);

							if (str.equals(")"))
								break;
						}
					else
						accept(")", s);
				} else {
					
				}

				line = new LineTree();
				line.parse(s);
				return;
			}
			accept("def", s);
			
			accept(":", s);
			
			name = next(s);
			
			accept(".", s);
			
			if (seek(s).equals("[")) { accept("[", s);
				if (!seek(s).equals("]"))
					while (true) {
						ParameterTree t = new ParameterTree();
						t.parse(s);
						paramters.add(t);

						String str = accept(new String[] { ",", "]" }, s);

						if (str.equals("]"))
							break;
					}
				else
					accept("]", s);
			} else {
				ParameterTree t = new ParameterTree();
				t.parse(s);
				paramters.add(t);
			}

			line = new LineTree();
			line.parse(s);
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Parameter = {
	 * 		NAME
	 * }
	 */
	public static class ParameterTree extends TreeNode {

		public String name = null, valueType = null;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			name = next(s);
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Line = {
	 * 		Expression
	 * 		| While
	 * 		| If
	 * 		| "return" EXPRESSION
	 * 		| "break"
	 * }
	 */
	public static class LineTree extends TreeNode {

		public enum LineType {
			EXPRESSION,
			WHILE,
			DO_WHILE,
			FOR,
			IF,
			BLOCK,
			RETURN,
			BREAK,
			NOP,
			IFWHILE,
			DEF
		}
		
		public LineType type;
		
		public WhileTree _while;
		public ExpressionTree expression;
		public DoWhileTree doWhile;
		public ForTree _for;
		public IfWhileTree ifWhile;
		public IfTree _if;
		public BlockTree block;
		public FunctionTree func;
		//public ReturnTree _return;
		
		public int line;
		public String file;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			String str = seek(s);

			this.line = s.line()+1;
			this.file = s.file();
			
			if (str.equals(";")) {
				type = LineType.NOP;
			} else if (str.equals("while") && seek(s, 2).equals(":")) {
				type = LineType.WHILE;
				_while = new WhileTree();
				_while.parse(s);
				
			} else if (str.equals("do") && seek(s, 2).equals(":")) {
				type = LineType.DO_WHILE;
				doWhile = new DoWhileTree();
				doWhile.parse(s);
				
			} else if (str.equals("for") && seek(s, 2).equals(":")) {
				type = LineType.FOR;
				_for = new ForTree();
				_for.parse(s);
				
			} else if (str.equals("if") && seek(s, 2).equals(":")) {
				type = LineType.IF;
				_if = new IfTree();
				_if.parse(s);
				
			} else if (str.equals("if") && seek(s, 2).equals("while") && seek(s, 3).equals(":")) {
				type = LineType.IFWHILE;
				ifWhile = new IfWhileTree();
				ifWhile.parse(s);
				
			} else if (str.equals("return")) {
				type = LineType.RETURN;
				accept("return", s);
				if (seek(s).equals(":")) accept(":", s);
				expression = new ExpressionTree();
				expression.parse(s);
			} else if (str.equals("=")) {
				type = LineType.RETURN;
				accept("=", s);
				expression = new ExpressionTree();
				expression.parse(s);
				
			} else if (str.equals("break")) {
				type = LineType.BREAK;
				accept("break", s);
				
			} else if (str.equals("{"))
			{
				type = LineType.BLOCK;
				block = new BlockTree();
				block.parse(s);
			} else if (str.equals("def") && seek(s, 2).equals(":"))
			{
				type = LineType.DEF;
				func = new FunctionTree();
				func.parse(s);
			} else
			{
				type = LineType.EXPRESSION;
				expression = new ExpressionTree();
				expression.parse(s);
			}
			
			if (seek(s).equals(";")) accept(";", s);
			
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}

	/*
	 * Block = {
	 * 		"{" LINE* "}"
	 * }
	 */
	public static class BlockTree extends TreeNode {

		public ExpressionTree condition;
		public ArrayList<LineTree> lines = new ArrayList<LineTree>();
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			accept("{", s);
			while (!seek(s).equals("}")) {
				LineTree t = new LineTree();
				t.parse(s);
				lines.add(t);
			}
			accept("}", s);
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	
	/*
	 * While = {
	 * 		"while" ":" EXPRESSION LINE
	 * }
	 */
	public static class WhileTree extends TreeNode {

		public ExpressionTree condition;
		public LineTree line;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			accept("while", s);
			
			accept(":", s);
			condition = new ExpressionTree();
			condition.parse(s);
			
			line = new LineTree();
			line.parse(s);
			
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	public static class IfWhileTree extends TreeNode {

		public ExpressionTree condition;
		public LineTree line;
		public LineTree line_else;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			accept("if", s);
			
			accept("while", s);
			
			accept(":", s);
			condition = new ExpressionTree();
			condition.parse(s);
			
			line = new LineTree();
			line.parse(s);
			
			if (seek(s).equals("else")) {
				accept("else", s);
				accept(":", s);
				line_else = new LineTree();
				line_else.parse(s);
			}
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * DoWhile = {
	 * 		"do" ":" LINE "while" ":" EXPRESSION
	 * }
	 */
	public static class DoWhileTree extends TreeNode {

		public ExpressionTree condition;
		public LineTree line;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			accept("do", s);
			accept(":", s);
			
			line = new LineTree();
			line.parse(s);
			
			accept("while", s);
			
			accept(":", s);
			condition = new ExpressionTree();
			condition.parse(s);
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * For = {
	 * 		"for" ":" EXPRESSION "," EXPRESSION "," EXPRESSION LINE
	 * }
	 */
	public static class ForTree extends TreeNode {

		public ExpressionTree assign;
		
		public ExpressionTree condition;
		public ExpressionTree increment;
		public LineTree line;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			accept("for", s);
			
			accept(":", s);
			assign = new ExpressionTree();
			assign.parse(s);
			accept(",", s);
			condition = new ExpressionTree();
			condition.parse(s);
			accept(",", s);
			increment = new ExpressionTree();
			increment.parse(s);
			
			line = new LineTree();
			line.parse(s);
			
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * If = {
	 * 		"if" ":" EXPRESSION LINE ("else" ":" LINE)?
	 * }
	 */
	public static class IfTree extends TreeNode {

		public ExpressionTree condition;
		public LineTree line;
		public LineTree line_else;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			accept("if", s);
			
			accept(":", s);
			condition = new ExpressionTree();
			condition.parse(s);
			
			line = new LineTree();
			line.parse(s);
			
			if (seek(s).equals("else")) {
				accept("else", s);
				accept(":", s);
				line_else = new LineTree();
				line_else.parse(s);
			}
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Expression2 = {
	 * 		EXPRESSION3 (((("->"|"=>") EXPRESSION3)|"[" EXPRESSION "]")* (("."|"::") ("[" (EXPRESSION ("," EXPRESSION)*)? "]"|EXPRESSION) )?)?
	 * 		| EXPRESSION3
	 * }
	 */
	public static class Expression2Tree extends TreeNode {

		public enum Operator {
			NEXT,
			OPERATOR
		}
		public Operator operator = Operator.NEXT;
		public PrimaryTree first;
		
		public ArrayList<String> op = new ArrayList<>();
		public ArrayList<PrimaryTree> second = new ArrayList<>();
		public ExpressionTree exp;
		
		public ArrayList<ArrayList<ExpressionTree>> arguments = new ArrayList<>();
		
		public int line;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			if (TraditionalSyntax) {
				first = new PrimaryTree();
				first.parse(s);
				
				line = s.line()+1;
				
				String operator;
				label1: while (Arrays.asList("[", ":", ".", "(", "::", "->").contains((operator = seek(s))) || (op.size() > 0 && seek(s).equals("="))){
					switch (operator) {
					case "[":
						op.add(next(s));
						this.operator = Operator.OPERATOR;
					
						PrimaryTree e2 = new PrimaryTree();
						e2.operator = PrimaryTree.Operator.EXPRESSION;
						e2.second = new ExpressionTree();
						e2.second.parse(s);
						second.add(e2);
						
						accept("]", s);
						
						arguments.add(null);
						break;
					case ":":
						op.add(next(s));
						this.operator = Operator.OPERATOR;
					
						exp = new ExpressionTree();
						exp.parse(s);
						second.add(null);
						
						arguments.add(null);
						break label1;
					case "=":
						accept("=", s);
						op.add(":");
						this.operator = Operator.OPERATOR;
					
						exp = new ExpressionTree();
						exp.parse(s);
						second.add(null);
						
						arguments.add(null);
						break label1;
					case ".":
						accept(".", s);
						op.add("->");
						this.operator = Operator.OPERATOR;
					
						PrimaryTree e3 = new PrimaryTree();
						e3.operator = PrimaryTree.Operator.STRING;
						e3.first = next(s);
						second.add(e3);
						
						arguments.add(null);
						break;
					case "(":
						{	
							this.operator = Operator.OPERATOR;
							
							op.add(".");
							accept("(", s);
							ArrayList<ExpressionTree> args = new ArrayList<>();
							
							if (!seek(s).equals(")")) while (true) {
								ExpressionTree t = new ExpressionTree();
								t.parse(s);
								args.add(t);
								if (accept(new String[]{")", ","}, s).equals(")")) break;
							} else accept(")", s);
							
							arguments.add(args);
							second.add(null);
						}
						break;
					case "::":
					{	
						this.operator = Operator.OPERATOR;
						
						op.add(".");
						accept("::", s);
						ExpressionTree t = new ExpressionTree();
						t.parse(s);
						
						ArrayList<ExpressionTree> args = new ArrayList<>();
						args.add(t);
						arguments.add(args);
						second.add(null);
						
						
					}
					break;
					case "->":
					{	
						this.operator = Operator.OPERATOR;
						
						op.add("o->m");
						accept("->", s);
						
						PrimaryTree e = new PrimaryTree();
						e.parse(s);
						second.add(e);
						
						ArrayList<ExpressionTree> args = new ArrayList<>();
						
						if (seek(s).equals("::")) {
							ExpressionTree t = new ExpressionTree();
							t.parse(s);
							args.add(t);
							
						} else if (seek(s).equals("(")) {
							accept("(", s);
							
							if (!seek(s).equals(")")) while (true) {
								ExpressionTree t = new ExpressionTree();
								t.parse(s);
								args.add(t);
								if (accept(new String[]{")", ","}, s).equals(")")) break;
							} else accept(")", s);
							
						
						}
						
						arguments.add(args);
						
						
					}
					break;
					default:
						this.operator = Operator.NEXT;
						break label1;
					}
				}
				return;
			}
			
			first = new PrimaryTree();
			first.parse(s);
			
			line = s.line()+1;
			
			String operator;
			label1: while (Arrays.asList(new String[]{"->", ":", ".", "::", "["}).contains((operator = seek(s)))){
				switch (operator) {
				case "[":
					op.add(next(s));
					this.operator = Operator.OPERATOR;
				
					PrimaryTree e2 = new PrimaryTree();
					e2.operator = PrimaryTree.Operator.EXPRESSION;
					e2.second = new ExpressionTree();
					e2.second.parse(s);
					second.add(e2);
					
					accept("]", s);
					
					arguments.add(null);
					break;
				case ":":
					op.add(next(s));
					this.operator = Operator.OPERATOR;
				
					exp = new ExpressionTree();
					exp.parse(s);
					second.add(null);
					
					arguments.add(null);
					break label1;
				case "->":
					op.add(next(s));
					this.operator = Operator.OPERATOR;
				
					PrimaryTree e3 = new PrimaryTree();
					e3.operator = PrimaryTree.Operator.STRING;
					e3.first = next(s);
					second.add(e3);
					
					arguments.add(null);
					break;
				case ".":
				case "::":
					{	
						this.operator = Operator.OPERATOR;
						
						op.add(accept(new String[]{".", "::"}, s));
						if (seek(s).equals("[")) {
							ArrayList<ExpressionTree> args = new ArrayList<>();
							accept("[", s);
							if (!seek(s).equals("]")) while (true) {
								ExpressionTree t = new ExpressionTree();
								t.parse(s);
								args.add(t);
								if (accept(new String[]{"]", ","}, s).equals("]")) break;
							} else accept("]", s);
							arguments.add(args);
						} else {
							ExpressionTree t = new ExpressionTree();
							t.parse(s);
							
							ArrayList<ExpressionTree> args = new ArrayList<>();
							args.add(t);
							arguments.add(args);
						}
						second.add(null);
						
					}
					break;
				default:
					this.operator = Operator.NEXT;
					break label1;
				}
			}
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Expression1 = {
	 * 		EXPRESSION2 ("*"|"/"|"%") EXPRESSION2
	 * 		| EXPRESSION2
	 * }
	 */
	public static class Expression1Tree extends TreeNode {

		public enum Operator {
			NEXT,
			OPERATOR
		}
		public Operator operator = Operator.NEXT;
		public ArrayList<String> op = new ArrayList<>();
		public Expression2Tree first;
		public ArrayList<Expression2Tree> second = new ArrayList<>();
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			first = new Expression2Tree();
			first.parse(s);
			
			String operator;
			label1: while (Arrays.asList(new String[]{"*", "/", "%"}).contains((operator = seek(s)))){
				switch (operator) {
				case "*":
				case "/":
				case "%":
					op.add(next(s));
					this.operator = Operator.OPERATOR;
				
					Expression2Tree e = new Expression2Tree();
					e.parse(s);
					second.add(e);
					break;
				default:
					this.operator = Operator.NEXT;
					break label1;
				}
			}
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Expression0 = {
	 * 		EXPRESSION1 ("+"|"-") EXPRESSION1
	 * 		| EXPRESSION1
	 * }
	 */
	public static class Expression0Tree extends TreeNode {

		public enum Operator {
			NEXT,
			OPERATOR
		}
		public Operator operator = Operator.NEXT;
		public ArrayList<String> op = new ArrayList<>();
		public Expression1Tree first;
		public ArrayList<Expression1Tree> second = new ArrayList<>();
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			first = new Expression1Tree();
			first.parse(s);
			
			parse_(s);
			
		}

		private void parse_(TokenScanner s) throws SyntaxError {
			String operator;
			label1: while (Arrays.asList(new String[]{"+", "-"}).contains((operator = seek(s)))){
				switch (operator) {
				case "+":
				case "-":
					op.add(next(s));
					this.operator = Operator.OPERATOR;
				
					Expression1Tree e = new Expression1Tree();
					e.parse(s);
					second.add(e);
					break;
				default:
					this.operator = Operator.NEXT;
					break label1;
				}
			}
		}
		
		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Expression = {
	 * 		| EXPRESSION_LOGIC ("&&"|"||") EXPRESSION_LOGIC
	 * 		| EXPRESSION_LOGIC
	 * }
	 */
	public static class ExpressionTree extends TreeNode {

		public enum Operator {
			NEXT,
			OPERATOR
		}
		public Operator operator = Operator.NEXT;
		public ArrayList<String> op = new ArrayList<>();
		public ExpressionLogicTree first;
		public ArrayList<ExpressionLogicTree> second = new ArrayList<>();
		
		@SuppressWarnings("unused")
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			first = new ExpressionLogicTree();
			first.parse(s);
			
			String operator;
			label1: while (Arrays.asList(new String[]{"&&", "||"}).contains((operator = seek(s)))){
			switch (operator) {
			case "&&":
			case "||":
				op.add(next(s));
				this.operator = Operator.OPERATOR;
				
				ExpressionLogicTree e = new ExpressionLogicTree();
				e.parse(s);
				second.add(e);
				break;
			default:
				this.operator = Operator.NEXT;
			
			}
			}
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * ExpressionLogic = {
	 * 		| EXPRESSION0 ("=="|"<"|"<="|">"|">=") EXPRESSION0
	 * 		| EXPRESSION0
	 * }
	 */
	public static class ExpressionLogicTree extends TreeNode {

		public enum Operator {
			NEXT,
			OPERATOR
		}
		public Operator operator = Operator.NEXT;
		public ArrayList<String> op = new ArrayList<>();
		public Expression0Tree first;
		public ArrayList<Expression0Tree> second = new ArrayList<>();
		
		@SuppressWarnings("unused")
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			first = new Expression0Tree();
			first.parse(s);
			
			String operator;
			label1: while (Arrays.asList(new String[]{"==", "<", "<=", ">", ">=", "!="}).contains((operator = seek(s)))){
			switch (operator) {
			case "==":
			case "<":
			case "<=":
			case ">":
			case ">=":
			case "!=":
				op.add(next(s));
				this.operator = Operator.OPERATOR;
				
				Expression0Tree e = new Expression0Tree();
				e.parse(s);
				second.add(e);
				break;
			default:
				this.operator = Operator.NEXT;
			
			}
			}
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * Primary = {
	 * 		NUMBER
	 * 		| NAME "=" EXPRESSION
	 * 		| "local" NAME "=" EXPRESSION
	 * 		| NAME
	 * 		| "!" NAME
	 * 		| "++" NAME
	 * 		| "--" NAME
	 * 	 	| NAME "++"
	 * 		| NAME "--"
	 * 		| "(" EXPRESSION ")"
	 * 		| "{" (EXPRESSION ("," EXPRESSION)*)? "}"
	 * 		| "!" EXPRESSION 
	 * 		| "-" EXPRESSION
	 * 		| ":" EXPRESSION
	 * 		| STRING
	 * 		| ANONYMOUS_FUNCTION
	 * 		| "table" ":" "{" (NAME "=" EXPRESSION ("," NAME "=" EXPRESSION)*)? "}"
	 * }
	 */
	public static class PrimaryTree extends TreeNode {

		public enum Operator {
			NUMBER_VALUE,
			ASSIGN,
			VARIABLE,
			EXPRESSION,
			PREFIX,
			POSTFIX,
			NOT,
			REVERSE,
			LIST,
			STRING,
			ANONYMOUS_FUNCTION,
			LOCAL_ASSIGN,
			TABLE,
			IF
		}
		public Operator operator;
		public String fix;
		public String first;
		public ExpressionTree second;
		public ExpressionTree third;
		public ExpressionTree fourth;
		public Expression2Tree expr;
		public ArrayList<ExpressionTree> arguments = new ArrayList<>();
		public AnonymousFunctionTree function;
		public ArrayList<String> names = new ArrayList<>();
		public int line;
		public String file;
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			
			line = s.line()+1;
			file = s.file();
			
			if (seek(s).equals("(")) {
				accept("(", s);
				this.operator = Operator.EXPRESSION;
				second = new ExpressionTree();
				second.parse(s);
				accept(")", s);
				return;
			} else if (seek(s).equals("++") || seek(s).equals("--")) {
				operator = Operator.PREFIX;
				fix = accept(new String[]{"++", "--"}, s);
				first = next(s);
				return;
			} else if (seek(s).equals("!")) {
				operator = Operator.NOT;
				accept("!", s);
				expr = new Expression2Tree();
				expr.parse(s);
				return;
			} else if (seek(s).equals("-")) {
				operator = Operator.REVERSE;
				accept("-", s);
				expr = new Expression2Tree();
				expr.parse(s);
				return;
			} else if (seek(s).equals(":")) { 
				this.operator = Operator.STRING;
				accept(":", s);
				first = next(s);
				return;
			} else if (seek(s).equals("def")) { 
				this.operator = Operator.ANONYMOUS_FUNCTION;
				function = new AnonymousFunctionTree();
				function.parse(s);
				return;
			} else if (seek(s).equals("local")) {	
				this.operator = Operator.LOCAL_ASSIGN;
				
				accept("local", s);
				
				first = next(s);
				
				accept("=", s);
				
				second = new ExpressionTree();
				second.parse(s);
				return;
			} else if (seek(s).equals("if") && seek(s, 2).equals(":")) {	
				this.operator = Operator.IF;
				
				accept("if", s);
				
				accept(":", s);
				
				second = new ExpressionTree();
				second.parse(s);
				
				third = new ExpressionTree();
				third.parse(s);
				
				accept("else", s);
				accept(":", s);
				fourth = new ExpressionTree();
				fourth.parse(s);
				
				return;
				
			} else if (seek(s).equals("{")) {	
				this.operator = Operator.TABLE;
				
				accept("{", s);
				if (!seek(s).equals("}")) while (true) {
					names.add(next(s));
					accept("=", s);
					ExpressionTree t = new ExpressionTree();
					t.parse(s);
					arguments.add(t);
					if (accept(new String[]{"}", ","}, s).equals("}")) break;
				} else accept("}", s);
				return;
				
			} else if (seek(s).equals("[") && TraditionalSyntax) {	
				this.operator = Operator.LIST;
				
				accept("[", s);
				if (!seek(s).equals("]")) while (true) {
					ExpressionTree t = new ExpressionTree();
					t.parse(s);
					arguments.add(t);
					if (accept(new String[]{"]", ","}, s).equals("]")) break;
				} else accept("]", s);
				return;
				
			} /*else if (seek(s).equals("{")) {	
				this.operator = Operator.LIST;
				
				accept("{", s);
				if (!seek(s).equals("}")) while (true) {
					ExpressionTree t = new ExpressionTree();
					t.parse(s);
					arguments.add(t);
					if (accept(new String[]{"}", ","}, s).equals("}")) break;
				} else accept("}", s);
				return;
				
			}*/ 
			
			/*else if (seek(s).equals("[{")) {	
				operator = Operator.LIST;
				
				accept("[{", s);
				if (!seek(s).equals("}]")) while (true) {
					ExpressionTree t = new ExpressionTree();
					t.parse(s);
					arguments.add(t);
					if (accept(new String[]{"}]", ","}, s).equals("}]")) break;
				} else accept("}]", s);
				return;
			} else if (seek(s).equals("new")) {
				operator = Operator.NEW;
				accept("new", s);
				first = next(s);
				accept("(", s);
				if (!seek(s).equals(")")) while (true) {
					ExpressionTree t = new ExpressionTree();
					t.parse(s);
					arguments.add(t);
					if (accept(new String[]{")", ","}, s).equals(")")) break;
				} else accept(")", s);
				return;
			}*/
			
			first = next(s);
			if (Character.isDigit(first.charAt(0)) && seek(s, 1).equals(".") && Character.isDigit(seek(s,2).charAt(0))) {
				accept(".", s);
				first = first + "." + next(s);
			} else if (seek(s).equals("$")) {
				accept("$", s);
				first += "$";
				if (seek(s).equals("$")) {
					accept("$", s);
					first = "$" + next(s);
				}
			}
			
			String operator = seek(s);

			if (operator.equals("=")) {	
				this.operator = Operator.ASSIGN;
				
				accept("=", s);
				
				second = new ExpressionTree();
				second.parse(s);
			}  /*else if (operator.equals("[")) {	
				this.operator = Operator.ARRAY;
				
				accept("[", s);
				second = new ExpressionTree();
				second.parse(s);
				accept("]", s);
				
			} else if (operator.equals("[]")) {	
				this.operator = Operator.ARRAY;
				
				accept("[]", s);
				
			} else if (seek(s).equals("[{")) {	
				this.operator = Operator.LIST;
				
				accept("[{", s);
				if (!seek(s).equals("}]")) while (true) {
					ExpressionTree t = new ExpressionTree();
					t.parse(s);
					arguments.add(t);
					if (accept(new String[]{"}]", ","}, s).equals("}]")) break;
				} else accept("}]", s);
				return;
			} */else if (operator.equals("++") || operator.equals("--")) {
				this.operator = Operator.POSTFIX;
				fix = accept(new String[]{"++", "--"}, s);
				return;
			} else {
				if (first.equals("<EOF>")) {
					throw new UnexpectedTokenSyntaxError(s.file(), s.line()+1, "<EOF>", "<ident>", "Unexpected EOF");
				}
				if (first.startsWith("\"") && first.endsWith("\"")) {
					this.operator = Operator.STRING;
					first = first.substring(1, first.length()-1);
					return;
				}
				this.operator = Operator.VARIABLE;
			}
			
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
	/*
	 * AnanymousFunction = {
	 * 		"def" "." PARAMETERS LINE
	 * }
	 */
	public static class AnonymousFunctionTree extends TreeNode {
		
		public ArrayList<ParameterTree> paramters = new ArrayList<ParameterTree>();
		public LineTree line = new LineTree();
		
		public String name = "";
		
		@Override
		public void parse(TokenScanner s) throws SyntaxError {
			if (TraditionalSyntax) {
				accept("def", s);
				
				/*if (seek(s).equals(":")) {
					accept(":", s);
					name = next(s);
				}*/ // TODO Mieti asiaa
				
				if (seek(s).equals("(")) {
					accept("(", s);
					if (!seek(s).equals(")"))
						while (true) {
							ParameterTree t = new ParameterTree();
							t.parse(s);
							paramters.add(t);
	
							String str = accept(new String[] { ",", ")" }, s);
	
							if (str.equals(")"))
								break;
							}
						else
							accept(")", s);
				}
				line = new LineTree();
				line.parse(s);
				return;
			}
			
			accept("def", s);
			
			accept(".", s);
			
			if (seek(s).equals("[")) { accept("[", s);
				if (!seek(s).equals("]"))
					while (true) {
						ParameterTree t = new ParameterTree();
						t.parse(s);
						paramters.add(t);

						String str = accept(new String[] { ",", "]" }, s);

						if (str.equals("]"))
							break;
					}
				else
					accept("]", s);
			} else {
				ParameterTree t = new ParameterTree();
				t.parse(s);
				paramters.add(t);
			}

			line = new LineTree();
			line.parse(s);
		}

		@Override
		public String generate(String a) {
			return null;
		}
		
	}
	
}

