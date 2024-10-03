/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisRef extends BaseRef {
	
	public Declaration decl;
	public Identifier id;

	public ThisRef(SourcePosition posn) {
		super(posn);
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitThisRef(this, o);
	}

	@Override
	public TypeDenoter getType() {
		// TODO Auto-generated method stub
		return decl.type;
	}

	@Override
	public Declaration getDecl() {
		// TODO Auto-generated method stub
		return decl;
	}
	
}
