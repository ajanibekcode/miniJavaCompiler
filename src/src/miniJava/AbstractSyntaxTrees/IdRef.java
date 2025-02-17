/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IdRef extends BaseRef {
	
	public IdRef(Identifier id, SourcePosition posn){
		super(posn);
		this.id = id;
	}
		
	public <A,R> R visit(Visitor<A,R> v, A o) {
		return v.visitIdRef(this, o);
	}

	public Identifier id;
	public Declaration decl;

	@Override
	public TypeDenoter getType() {
		// TODO Auto-generated method stub
		return id.decl.type;
	}

	@Override
	public Declaration getDecl() {
		// TODO Auto-generated method stub
		return id.decl;
	}
}
