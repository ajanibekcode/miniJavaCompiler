package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;

		public SyntaxError(String message){
			super(message);
		}
	}
	
	public Package parse() {
		try {
			// The first thing we need to parse is the Program symbol
			return parseProgram();
			
		} catch( SyntaxError e ) {
			_errors.reportError(e.getMessage());
		}
		return null;
	}
	
	// Program ::= (ClassDeclaration)* eot
	private Package parseProgram() throws SyntaxError {
		ClassDeclList classList = new ClassDeclList();
		// TODO: Keep parsing class declarations until eot
		while (_currentToken != null && _currentToken.getTokenType() != TokenType.EOT){
			classList.add(parseClassDeclaration());
		}
		if (_currentToken != null) {
			accept(TokenType.EOT);
		}
		return new Package(classList, null);
	}
	
	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		// TODO: Take in a "class" token (check by the TokenType)
		accept(TokenType.CLASS);
		String cn = _currentToken.getTokenText();
		//  What should be done if the first token isn't "class"?
		// TODO: Take in an identifier token
		accept(TokenType.IDENTIFIER);
		// TODO: Take in a {
		accept(TokenType.LBRACE);
		
		// TODO: Parse either a FieldDeclaration or MethodDeclaration
		while (_currentToken.getTokenType() != TokenType.RBRACE){
			MemberDecl member = parseMethodOrFieldDeclaration();
			if (member instanceof FieldDecl) {
				fdl.add((FieldDecl)member);
			} else {
				mdl.add((MethodDecl)member);
			}
		}
		// TODO: Take in a }
		accept(TokenType.RBRACE);
		return new ClassDecl(cn, fdl, mdl, _currentToken.getTokenPosition());
	}
	private MemberDecl parseMethodOrFieldDeclaration() throws SyntaxError{
		ParameterDeclList parameters = new ParameterDeclList();
		StatementList statements = new StatementList();
		boolean isPrivate = parseVisibility();
		boolean isStatic = parseAccess();
		if (_currentToken.getTokenType() == TokenType.VOID) {
			TypeDenoter type = new BaseType(TypeKind.VOID, _currentToken.getTokenPosition());
			accept(TokenType.VOID);
			String name = _currentToken.getTokenText();
			accept(TokenType.IDENTIFIER);
			MemberDecl md = new FieldDecl(isPrivate, isStatic, type, name, _currentToken.getTokenPosition());
			accept(TokenType.LPAREN);
			if (_currentToken.getTokenType() != TokenType.RPAREN) {
				parameters = parseParameterList();
			}
			accept(TokenType.RPAREN);
			accept(TokenType.LBRACE);
			while (_currentToken.getTokenType() != TokenType.RBRACE) {
				statements.add(parseStatement());
			}
			accept(TokenType.RBRACE);
			return new MethodDecl(md, parameters, statements, _currentToken.getTokenPosition());
		}
		TypeDenoter type = parseType();
		String id = _currentToken.getTokenText();
		accept(TokenType.IDENTIFIER);
		if (_currentToken.getTokenType() == TokenType.SEMICOLON) {
			accept(TokenType.SEMICOLON);
			return new FieldDecl(isPrivate, isStatic, type, id, _currentToken.getTokenPosition());
		}
		else {
			MemberDecl md = new FieldDecl(isPrivate, isStatic, type, id, _currentToken.getTokenPosition());
			accept(TokenType.LPAREN);
			if (_currentToken.getTokenType() != TokenType.RPAREN) {
				parameters = parseParameterList();
			}
			accept(TokenType.RPAREN);
			accept(TokenType.LBRACE);
			while (_currentToken.getTokenType() != TokenType.RBRACE) {
				statements.add(parseStatement());
			}
			accept(TokenType.RBRACE);
			return new MethodDecl(md, parameters, statements, _currentToken.getTokenPosition());
		}
	}
	
	private boolean parseVisibility() throws SyntaxError{
		if (_currentToken.getTokenType() == TokenType.VISIBILITY) {
			if (_currentToken.getTokenText().equals("private")) {
				accept(TokenType.VISIBILITY);
				return true;
			}
			accept(TokenType.VISIBILITY);
		}
		return false;
	}

	private boolean parseAccess() throws SyntaxError{
		if (_currentToken.getTokenType() == TokenType.ACCESS) {
			accept(TokenType.ACCESS);
			return true;
		}
		return false;
	}

	private TypeDenoter parseType() throws SyntaxError{
		if (_currentToken.getTokenType() == TokenType.INT) {
			TypeDenoter type = new BaseType(TypeKind.INT, _currentToken.getTokenPosition());
			accept(TokenType.INT);
			if (_currentToken.getTokenType() == TokenType.LBRACKET) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
				type = new ArrayType(type, _currentToken.getTokenPosition());
				return type;
			}
			return type;
		}
		else if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
			TypeDenoter type = new ClassType(new Identifier(_currentToken), _currentToken.getTokenPosition());
			accept(TokenType.IDENTIFIER);
			if (_currentToken.getTokenType() == TokenType.LBRACKET) {
				accept(TokenType.LBRACKET);
				accept(TokenType.RBRACKET);
				type = new ArrayType(type, _currentToken.getTokenPosition());
				return type;
			}
			return type;
		}
		else if (_currentToken.getTokenType() == TokenType.BOOLEAN) {
			TypeDenoter type = new BaseType(TypeKind.BOOLEAN, _currentToken.getTokenPosition());
			accept(TokenType.BOOLEAN);
			return type;
		}
		
		throw new SyntaxError("Type error");
	}

	private ParameterDeclList parseParameterList() throws SyntaxError{
		ParameterDeclList list = new ParameterDeclList();
		TypeDenoter type = parseType();
		String name = _currentToken.getTokenText();
		ParameterDecl s = new ParameterDecl(type, name, _currentToken.getTokenPosition());
		list.add(s);
		accept(TokenType.IDENTIFIER);
		while (_currentToken.getTokenType() == TokenType.COMMA){
			accept(TokenType.COMMA);
			type = parseType();
			name = _currentToken.getTokenText();
			ParameterDecl s1 = new ParameterDecl(type, name, _currentToken.getTokenPosition());
			list.add(s1);
			accept(TokenType.IDENTIFIER);
		}
		return list;
	}

	private Statement parseStatement() throws SyntaxError{
		// {Statement*}
		if (_currentToken.getTokenType() == TokenType.LBRACE){
			accept(TokenType.LBRACE);
			StatementList s = new StatementList();
			while (_currentToken.getTokenType() != TokenType.RBRACE){
				s.add(parseStatement());
			}
			accept(TokenType.RBRACE);
			return new BlockStmt(s, _currentToken.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.THIS) {
			Reference ref = parseReference();
			if (_currentToken.getTokenType() == TokenType.EQUAL) {
				accept(TokenType.EQUAL);
				Expression exp = parseExpression();
				accept(TokenType.SEMICOLON);
				return new AssignStmt(ref, exp, _currentToken.getTokenPosition());
			} else if (_currentToken.getTokenType() == TokenType.LBRACKET){
				accept(TokenType.LBRACKET);
				Expression exp = parseExpression();
				accept(TokenType.RBRACKET);
				accept(TokenType.EQUAL);
				Expression exp1 = parseExpression();
				accept(TokenType.SEMICOLON);
				return new IxAssignStmt(ref, exp, exp1, _currentToken.getTokenPosition());
			} else if (_currentToken.getTokenType() == TokenType.LPAREN) {
				accept(TokenType.LPAREN);
				ExprList args = new ExprList();
				if (_currentToken.getTokenType() != TokenType.RPAREN) {
					args = parseArgList();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.SEMICOLON);
				return new CallStmt(ref, args, _currentToken.getTokenPosition());
			}
			
		}
		else if (_currentToken.getTokenType() == TokenType.BOOLEAN || _currentToken.getTokenType() == TokenType.INT) {
			TypeDenoter type = parseType();
			String name = _currentToken.getTokenText();
			VarDecl var = new VarDecl(type, name, _currentToken.getTokenPosition());
			accept(TokenType.IDENTIFIER);
			accept(TokenType.EQUAL);
			Expression e = parseExpression();
			accept(TokenType.SEMICOLON);
			return new VarDeclStmt(var, e, _currentToken.getTokenPosition());
			
		}
		else if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
			Token firstIDToken = _currentToken;
			Identifier firstID = new Identifier(firstIDToken);
			accept(TokenType.IDENTIFIER);
			
			if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
				String id = _currentToken.getTokenText();
				accept(TokenType.IDENTIFIER);
				ClassType ct = new ClassType(firstID, _currentToken.getTokenPosition());
//				String secondID = _currentToken.getTokenText();
				accept(TokenType.EQUAL);
				Expression e = parseExpression();
				accept(TokenType.SEMICOLON);
				return new VarDeclStmt(new VarDecl(ct, id, null), e, _currentToken.getTokenPosition());
			}
			else if (_currentToken.getTokenType() == TokenType.DOT) {
				Reference ref = new IdRef(firstID, null);
				accept(TokenType.DOT);
				Identifier secondID = new Identifier(_currentToken);
				accept(TokenType.IDENTIFIER);
				ref = new QualRef(ref, secondID, null);
				while (_currentToken.getTokenType() == TokenType.DOT) {
					accept(TokenType.DOT);
					secondID = new Identifier(_currentToken);
					accept(TokenType.IDENTIFIER);
					ref = new QualRef(ref, secondID, null);
				}
				if (_currentToken.getTokenType() == TokenType.EQUAL) {
					accept(TokenType.EQUAL);
					Expression e = parseExpression();
					accept(TokenType.SEMICOLON);
					return new AssignStmt(ref, e, _currentToken.getTokenPosition());
				} else if (_currentToken.getTokenType() == TokenType.LBRACKET){
					accept(TokenType.LBRACKET);
					Expression exp = parseExpression();
					accept(TokenType.RBRACKET);
					accept(TokenType.EQUAL);
					Expression exp1 = parseExpression();
					accept(TokenType.SEMICOLON);
					return new IxAssignStmt(ref, exp, exp1, _currentToken.getTokenPosition());
				} else if (_currentToken.getTokenType() == TokenType.LPAREN) {
					accept(TokenType.LPAREN);
					ExprList args = new ExprList();
					if (_currentToken.getTokenType() != TokenType.RPAREN) {
						args = parseArgList();
					}
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOLON);
					return new CallStmt(ref, args, _currentToken.getTokenPosition());
				}
			}
			else if (_currentToken.getTokenType() == TokenType.EQUAL) {
				accept(TokenType.EQUAL);
				Expression e = parseExpression();
				accept(TokenType.SEMICOLON);
				return new AssignStmt(new IdRef(firstID, null), e, _currentToken.getTokenPosition());
			}
			else if (_currentToken.getTokenType() == TokenType.LPAREN) {
				accept(TokenType.LPAREN);
				ExprList args = new ExprList();
				if (_currentToken.getTokenType() != TokenType.RPAREN) {
					args = parseArgList();
				}
				accept(TokenType.RPAREN);
				accept(TokenType.SEMICOLON);
				return new CallStmt(new IdRef(firstID, null), args, _currentToken.getTokenPosition());
			}
			else if (_currentToken.getTokenType() == TokenType.LBRACKET) {
				accept(TokenType.LBRACKET);
				if (_currentToken.getTokenType() == TokenType.RBRACKET) {
					accept(TokenType.RBRACKET);
					TypeDenoter type = new ClassType(firstID, _currentToken.getTokenPosition());
					type = new ArrayType(type, _currentToken.getTokenPosition());
					String name = _currentToken.getTokenText();
					VarDecl var = new VarDecl(type, name, _currentToken.getTokenPosition());
					accept(TokenType.IDENTIFIER);
					accept(TokenType.EQUAL);
					Expression e = parseExpression();
					accept(TokenType.SEMICOLON);
					return new VarDeclStmt(var, e, _currentToken.getTokenPosition());
				} else {
					Expression e = parseExpression();
					accept(TokenType.RBRACKET);
					accept(TokenType.EQUAL);
					Expression e1 = parseExpression();
					accept(TokenType.SEMICOLON);
					return new IxAssignStmt(new IdRef(firstID, null), e, e1, _currentToken.getTokenPosition());
				}
			}
			
		}
		else if (_currentToken.getTokenType() == TokenType.RETURN) {
			accept(TokenType.RETURN);
			Expression ref = null;
			if (_currentToken.getTokenType() != TokenType.SEMICOLON) {
				ref = parseExpression();
			}
			accept(TokenType.SEMICOLON);
			return new ReturnStmt(ref, _currentToken.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.IF) {
			accept(TokenType.IF);
			accept(TokenType.LPAREN);
			Expression exp = parseExpression();
			accept(TokenType.RPAREN);
			Statement state = parseStatement();
			if (_currentToken.getTokenType() == TokenType.ELSE) {
				accept(TokenType.ELSE);
				Statement state2 = parseStatement();
				return new IfStmt(exp, state, state2, _currentToken.getTokenPosition());
			}
			return new IfStmt(exp, state, _currentToken.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.WHILE){
			accept(TokenType.WHILE);
			accept(TokenType.LPAREN);
			Expression exp = parseExpression();
			accept(TokenType.RPAREN);
			Statement state = parseStatement();
			return new WhileStmt(exp, state, _currentToken.getTokenPosition());
		}
		throw new SyntaxError("Statement error.");
	}

	private Reference parseReference() throws SyntaxError{
		Reference ref = null;
		Identifier id = null;
		if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
			id = new Identifier(_currentToken);
			accept(TokenType.IDENTIFIER);
			ref = new IdRef(id, _currentToken.getTokenPosition());
			if (_currentToken.getTokenType() != TokenType.DOT) {
				return ref;
			}
		}
		else if (_currentToken.getTokenType() == TokenType.THIS){
			accept(TokenType.THIS);
			ref = new ThisRef(_currentToken.getTokenPosition());
			if (_currentToken.getTokenType() != TokenType.DOT) {
				return ref;
			}
		}
		while (_currentToken.getTokenType() == TokenType.DOT) {
			accept(TokenType.DOT);
			ref = new QualRef(ref, new Identifier(_currentToken), _currentToken.getTokenPosition());
			accept(TokenType.IDENTIFIER);
		}
//		return new Ref(ref, id, _currentToken.getTokenPosition());
		return ref;
	}

	private Expression parseExpressionMAX() throws SyntaxError{
		if (_currentToken.getTokenType() == TokenType.IDENTIFIER || _currentToken.getTokenType() == TokenType.THIS) {
			Reference ref = parseReference();
			ExprList el = new ExprList();
			if (_currentToken.getTokenType() == TokenType.LBRACKET) {
				accept(TokenType.LBRACKET);
				Expression e = parseExpression();
				accept(TokenType.RBRACKET);
				return new IxExpr(ref, e, _currentToken.getTokenPosition());
			}
			else if (_currentToken.getTokenType() == TokenType.LPAREN) {
				accept(TokenType.LPAREN);
				if (_currentToken.getTokenType() != TokenType.RPAREN) {
					el = parseArgList();
				}
				accept(TokenType.RPAREN);
				return new CallExpr(ref, el, _currentToken.getTokenPosition());
			}
			return new RefExpr(ref, _currentToken.getTokenPosition());
		}
		
		
		else if (_currentToken.getTokenType() == TokenType.UNOP) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.UNOP);
			Expression exp = parseExpression();
			return new UnaryExpr(op, exp, _currentToken.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.LPAREN) {
			accept(TokenType.LPAREN);
			Expression exp = parseExpression();
			accept(TokenType.RPAREN);
			return exp;
		}
		else if (_currentToken.getTokenType() == TokenType.INT_LITERAL) {
			IntLiteral token = new IntLiteral(_currentToken);
			accept(TokenType.INT_LITERAL);
			return new LiteralExpr(token, _currentToken.getTokenPosition());	
		}
		else if (_currentToken.getTokenType() == TokenType.BOOL_LITERAL) {
			BooleanLiteral token = new BooleanLiteral(_currentToken);
			accept(TokenType.BOOL_LITERAL);
			return new LiteralExpr(token, _currentToken.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.NULL_LITERAL) {
			NullLiteral token = new NullLiteral(_currentToken);
			accept(TokenType.NULL_LITERAL);
			return new LiteralExpr(token, _currentToken.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.NEW) {
			accept(TokenType.NEW);
			if (_currentToken.getTokenType() == TokenType.IDENTIFIER) {
				ClassType ct = new ClassType(new Identifier(_currentToken), _currentToken.getTokenPosition());
				accept(TokenType.IDENTIFIER);
				if (_currentToken.getTokenType() == TokenType.LPAREN) {
					accept(TokenType.LPAREN);
					accept(TokenType.RPAREN);
					return new NewObjectExpr(ct, _currentToken.getTokenPosition());
				}
				else {
					accept(TokenType.LBRACKET);
					Expression exp = parseExpression();
					accept(TokenType.RBRACKET);
					return new NewArrayExpr(ct, exp, _currentToken.getTokenPosition());
				}
			} else {
				TypeDenoter et = new BaseType(TypeKind.INT, _currentToken.getTokenPosition());
				accept(TokenType.INT);
				accept(TokenType.LBRACKET);
				Expression exp = parseExpression();
				accept(TokenType.RBRACKET);
				return new NewArrayExpr(et, exp, _currentToken.getTokenPosition());
			}
		}
		
		throw new SyntaxError("Expr error.");
	}
	
	private Expression parseExpression() throws SyntaxError{
		return parseExpressionLogicalOr();
	}
	
	private Expression parseExpressionLogicalOr() throws SyntaxError{
		Expression LHS = parseExpressionLogicalAnd();
		while (_currentToken.getTokenText().equals("||")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.BINOP);
			Expression RHS = parseExpressionLogicalAnd();
			LHS = new BinaryExpr(op, LHS, RHS, _currentToken.getTokenPosition());
		}
		return LHS;
	}
	
	private Expression parseExpressionLogicalAnd() throws SyntaxError{
		Expression LHS = parseExpressionEquality();
		while (_currentToken.getTokenText().equals("&&")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.BINOP);
			Expression RHS = parseExpressionEquality();
			LHS = new BinaryExpr(op, LHS, RHS, _currentToken.getTokenPosition());
		}
		return LHS;
	}
	
	private Expression parseExpressionEquality() throws SyntaxError{
		Expression LHS = parseExpressionRelational();
		while (_currentToken.getTokenText().equals("==") || _currentToken.getTokenText().equals("!=")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.BINOP);
			Expression RHS = parseExpressionRelational();
			LHS = new BinaryExpr(op, LHS, RHS, _currentToken.getTokenPosition());
		}
		return LHS;
	}
	
	private Expression parseExpressionRelational() throws SyntaxError{
		Expression LHS = parseExpressionAdditive();
		while (_currentToken.getTokenText().equals("<=") || _currentToken.getTokenText().equals("<") || _currentToken.getTokenText().equals(">") || _currentToken.getTokenText().equals(">=")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.BINOP);
			Expression RHS = parseExpressionAdditive();
			LHS = new BinaryExpr(op, LHS, RHS, _currentToken.getTokenPosition());
		}
		return LHS;
	}
	
	private Expression parseExpressionAdditive() throws SyntaxError{
		Expression LHS = parseExpressionMultiplicative();
		while (_currentToken.getTokenText().equals("+")|| _currentToken.getTokenText().equals("-")) {
			Operator op = new Operator(_currentToken);
			if (_currentToken.getTokenText().equals("-")){
				accept(TokenType.UNOP);
			}
			else {
				accept(TokenType.BINOP);
			}
			Expression RHS = parseExpressionMultiplicative();
			LHS = new BinaryExpr(op, LHS, RHS, _currentToken.getTokenPosition());
		}
		return LHS;
	}
	
	private Expression parseExpressionMultiplicative() throws SyntaxError{
		Expression LHS = parseExpressionUnary();
		while (_currentToken.getTokenText().equals("*") || _currentToken.getTokenText().equals("/")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.BINOP);
			Expression RHS = parseExpressionUnary();
			LHS = new BinaryExpr(op, LHS, RHS, _currentToken.getTokenPosition());
		}
		return LHS;
	}
	
	private Expression parseExpressionUnary() throws SyntaxError{
		if (_currentToken.getTokenText().equals("-") || _currentToken.getTokenText().equals("!")) {
			Operator op = new Operator(_currentToken);
			accept(TokenType.UNOP);
			return new UnaryExpr(op, parseExpressionUnary(), _currentToken.getTokenPosition());
		}
		return parseExpressionMAX();
	}
		

	private ExprList parseArgList() throws SyntaxError{
		ExprList list = new ExprList();
		Expression ref = parseExpression();
		list.add(ref);
		while (_currentToken.getTokenType() == TokenType.COMMA){
			accept(TokenType.COMMA);
			ref = parseExpression();
			list.add(ref);
		}
		return list;
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if ( _currentToken.getTokenType() == expectedType ) {
//			System.out.println("Expected: " + expectedType + ", Accepted this " + _currentToken.getTokenType() + " " + _currentToken.getTokenText());
////			System.out.println(_currentToken.getTokenText());
			_currentToken = _scanner.scan();
			return;
		} else {
			_errors.reportError("Expected token " + expectedType + ", but got " + _currentToken.getTokenType());
			// TODO: Report an error here.
			//  "Expected token X, but got Y"
			throw new SyntaxError("Expected token " + expectedType + ", but got " + _currentToken.getTokenType());
		}
	}
	
}