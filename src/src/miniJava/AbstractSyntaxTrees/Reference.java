/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Reference extends AST
{
	public Reference(SourcePosition posn){
		super(posn);
	}
	
	public abstract TypeDenoter getType();
	public abstract Declaration getDecl();
	public Declaration decl;
	public Object spelling;

}
