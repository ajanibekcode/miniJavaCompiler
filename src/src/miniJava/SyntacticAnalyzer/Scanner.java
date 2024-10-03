package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import miniJava.ErrorReporter;
import static java.lang.Character.isDigit;


public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	private boolean eot = false;
	private static final HashMap<String, TokenType> reservedWords = new HashMap<>();
	
	
	static {
		reservedWords.put("if", TokenType.IF);
		reservedWords.put("while", TokenType.WHILE);
	    reservedWords.put("else", TokenType.ELSE);
	    reservedWords.put("return", TokenType.RETURN);
	    reservedWords.put("false", TokenType.BOOL_LITERAL);
	    reservedWords.put("true", TokenType.BOOL_LITERAL);
	    reservedWords.put("int", TokenType.INT);
	    reservedWords.put("new", TokenType.NEW);
	    reservedWords.put("class", TokenType.CLASS);
	    reservedWords.put("private", TokenType.VISIBILITY);
	    reservedWords.put("public", TokenType.VISIBILITY);
	    reservedWords.put("static", TokenType.ACCESS);
	    reservedWords.put("void", TokenType.VOID);
	    reservedWords.put("id", TokenType.IDENTIFIER);
	    reservedWords.put("boolean", TokenType.BOOLEAN);
	    reservedWords.put("this", TokenType.THIS);
	    reservedWords.put("null", TokenType.NULL_LITERAL);
	}
	
	
	public Scanner(InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		this._currentText = new StringBuilder();
		nextChar();
	}
	
	public Token scan() {
		// TODO: This function should check the current char to determine what the token could be.
		// TODO: Consider what happens if the current char is whitespace
		// TODO: Consider what happens if there is a comment (// or /* */)
		// TODO: What happens if there are no more tokens?
		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.
		if (eot) {
			return null;
		}
		_currentText = new StringBuilder();
		TokenType token = scanToken();
		return makeToken(token);
	}
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentChar = (char) c;
			// TODO: What happens if c == -1? REACHED END OF INPUT
			if (c == -1){
				eot = true;
			} 
			// TODO: What happens if c is not a regular ASCII character?
			if (c > 127){
				throw new IOException("Non-ASCII character was found.");
			}
		} catch( IOException e ) {
			// TODO: Report an error here
			_errors.reportError("I/O Error in scanner: " + e.getMessage());
		}
	}
	
	private Token makeToken( TokenType toktype) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in 
		String spelling = _currentText.toString();
		return new Token(toktype, spelling);
	}
	
	private void checkWhitespace(){
		while ((_currentChar == '\n' || _currentChar == '\r' || _currentChar == '\t' || _currentChar == ' ') && !eot){
			skipIt();
		}
	}
		
	public TokenType scanToken(){
		checkWhitespace();
		// check if end input is reached
		if (eot){
			return TokenType.EOT;
		}
		if (_currentChar == '+'){
			takeIt();
			return TokenType.BINOP;
		}
		
		else if (_currentChar == '-'){
			takeIt();
			return TokenType.UNOP;
		}
		else if (_currentChar == '.') {
			takeIt();
			return TokenType.DOT;
		}
		else if (_currentChar == ';'){
			takeIt();
			return TokenType.SEMICOLON;
		}
		else if (_currentChar == ','){
			takeIt();
			return TokenType.COMMA;
		}
		else if (_currentChar == ')'){
			takeIt();
			return TokenType.RPAREN;
		}
		else if (_currentChar == '('){
			takeIt();
			return TokenType.LPAREN;
		}
		else if (_currentChar == '{'){
			takeIt();
			return TokenType.LBRACE;
		}
		else if (_currentChar == '}'){
			takeIt();
			return TokenType.RBRACE;
		}
		else if (_currentChar == '['){
			takeIt();
			return TokenType.LBRACKET;
		}
		else if (_currentChar == ']'){
			takeIt();
			return TokenType.RBRACKET;
		}
		else if (_currentChar == '*'){
			takeIt();
			return TokenType.BINOP;
		}
		// What happens when we have == || >= !=
		else if (_currentChar == '='){
			takeIt();
			if (_currentChar == '='){
				takeIt();
				return TokenType.BINOP;
			}
			return TokenType.EQUAL;
		}
		else if (_currentChar == '&'){
			takeIt();
			if (_currentChar == '&'){
				takeIt();
				return TokenType.BINOP;
			}
			return TokenType.ERROR;
		}
		else if (_currentChar == '>' || _currentChar == '<'){
			takeIt();
			if (_currentChar == '='){
				takeIt();
			}
			return TokenType.BINOP;
		}
		else if (_currentChar == '|'){
			takeIt();
			if (_currentChar == '|'){
				takeIt();
				return TokenType.BINOP;
			}
			return TokenType.ERROR;
		}
		else if (_currentChar == '!'){
			takeIt();
			if (_currentChar == '='){
				takeIt();
				return TokenType.BINOP;
			}
			return TokenType.UNOP;
		}
		// TODO: Numbers 0-9 -> Account for multiple numbers -> import isDigit method
		else if (_currentChar == '0' || _currentChar == '1' || _currentChar == '2' || _currentChar == '3' || 
				_currentChar == '4' || _currentChar == '5' || _currentChar == '6' || _currentChar == '7' || _currentChar == '8' || _currentChar == '9'){
			while (isDigit(_currentChar)){
				takeIt(); // keep taking the int literal
			}
			return TokenType.INT_LITERAL;
		}
		// TODO: comments
		else if (_currentChar == '/') {
			takeIt();
			// Differentiate between single line commments and block comments
			if (!eot && _currentChar == '/') {
				//single line
				skipIt();
				
				while( !eot && _currentChar != '\n' )
					skipIt();
				_currentText = new StringBuilder();
				return scanToken();
				
			} else if (!eot && _currentChar == '*') {
				//block comment
				/*while (_currentChar != '*') {
					skipIt();
				}
				if (_currentChar == '/') {
					skipIt();
				}*/
				skipIt();
				for(;;) {
					if( eot ) {
						// TODO: report error
						return TokenType.ERROR;
					}
					
					if( _currentChar == '*' ) {
						skipIt();
						if( _currentChar == '/' ) {
							skipIt();
							_currentText = new StringBuilder();
							return scanToken();
						}
					} else skipIt();
				}
			} else {
				return TokenType.BINOP;
			}
		}
		// reserved keywords
		while (!eot && (Character.isLetter(_currentChar) || Character.isDigit(_currentChar) || _currentChar == '_')) {
			takeIt();
		}
		String key = _currentText.toString();
		if (reservedWords.containsKey(key)) {
			return reservedWords.get(key);
		} else {
			if (_currentText.length() == 0 || !Character.isLetter(_currentText.charAt(0)) ) {
				return TokenType.ERROR;
			} else {
				return TokenType.IDENTIFIER;
			}
		}
		
		
	}
		
}
