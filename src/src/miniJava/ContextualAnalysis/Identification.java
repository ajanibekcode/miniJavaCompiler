package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.AbstractSyntaxTrees.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Identification implements Visitor<Object,Object> {
	private ErrorReporter _errors;
	private boolean inStaticContext = false;
	private ClassDecl currentClass = null;
	private MethodDecl currentMethod = null;
    private Stack<Map<String,Declaration>> scopes = new Stack<>();

	private Object currentInitializingVar;
//	private boolean isMain = false;
    
   
	public Identification(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse( Package prog ) {
//		System.out.println("Starting parse.");
		try {
			visitPackage(prog,null);
		} catch( IdentificationError e ) {
			_errors.reportError(e.toString());
		}
//		System.out.println("Parsing completed.");
	}
	
	private void closeScope() {
		// TODO Auto-generated method stub
//		System.out.println("Closing Scope.");
		scopes.pop();
	}
	private void openScope() {
		// TODO Auto-generated method stub
//		System.out.println("Opening new scope.");
		scopes.push(new HashMap<String, Declaration>());
	}
	
	public void addDeclaration(String id, Declaration decl) {
//	    System.out.println("Attempting to add declaration for ID: " + id);
		scopes.peek().put(id,decl);
//		System.out.println("Added declaration for ID: " + id);
	}

	private boolean findDeclarationLevelPlusTwo(String id) {
	    for (int i = scopes.size() - 1; i >= 2; i--) {
	        
	        if (scopes.get(i).containsKey(id)) {
	            return true;
	        }
	    }
	    return false;
	}
	
	public Declaration findDeclarationLevel1(String s, String context) {
		String name = context + "." + s;
		if (scopes.get(1).containsKey(name)) {
			return scopes.get(1).get(name);
		}
		return null;
	}
	
	public Declaration findDeclaration(String s, String context) {
//		System.out.println("Searching for declaration: " + s);
		if (context == null) {
			context = "";
		}
		for (int i = scopes.size() - 1; i >= 0; i--) {
			String k = i == 1 ? context + "." + s : s;			
            if (scopes.get(i).containsKey(k)) {
//            	System.out.println("Found declaration for: " + k + " in scope level " + (i));
//            	System.out.println("output " + scopes.get(i).get(k));
                return scopes.get(i).get(k);
            }
        }
        return null;
	}
	
	class IdentificationError extends Error {
		private static final long serialVersionUID = -441346906191470192L;
		private String _errMsg;
		
		public IdentificationError(AST ast, String errMsg) {
			super();
			this._errMsg = ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg;
		}
		
		@Override
		public String toString() {
			return _errMsg;
		}
	}

    @Override
    public Object visitPackage(Package prog, Object arg) throws IdentificationError {
        openScope();
        addPredefined();
        for (ClassDecl cd:prog.classDeclList) {
        	if (findDeclaration(cd.name, null) != null) {
        		throw new IdentificationError(cd, cd.name + " has already been declared.");
        	}
        	addDeclaration(cd.name, cd);
        }
        openScope();
        addDeclaration("_PrintStream.println", printStreamClass.methodDeclList.get(0));
        addDeclaration("System.out", systemClass.fieldDeclList.get(0));
        for (ClassDecl c:prog.classDeclList) {        
        	for (FieldDecl f:c.fieldDeclList) {
        		if (findDeclarationLevel1(f.name, c.name) != null){
        			throw new IdentificationError(c, c.name + " space " + f.name + " has already been encountered. IN FIELD DECL");
        		}
        		addDeclaration(c.name + "." + f.name, f);
        	}
        	for (MethodDecl m:c.methodDeclList) {
        		if (findDeclarationLevel1(m.name, c.name) != null) {
        			throw new IdentificationError(c, c.name + m.name + " has already been encountered. IN METHOD DECL");
        		}
        		addDeclaration(c.name + "." + m.name, m);
        	}
        	
        }
        for (ClassDecl c:prog.classDeclList) {
        	c.visit(this, null);
        }
//        if (!isMain) {
//        	throw new IdentificationError(prog, "no main method found");
//        }
        return null;
    }
    
    private ClassDecl printStreamClass;
    private ClassDecl systemClass;
    
    private void addPredefined() {
        ClassType printStreamType = new ClassType(new Identifier(new Token(TokenType.IDENTIFIER, "_PrintStream")), null);
        printStreamClass = new ClassDecl("_PrintStream", new FieldDeclList(), new MethodDeclList(), null);
        MethodDecl printlnMethod = new MethodDecl(
                new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null),
                new ParameterDeclList(),
                new StatementList(),
                null
        );
        printlnMethod.printlnMD = printlnMethod;
        printlnMethod.parameterDeclList.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
        printStreamClass.methodDeclList.add(printlnMethod);
        addDeclaration("_PrintStream", printStreamClass);

        ClassDecl stringClass = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);
        addDeclaration("String", stringClass);

        systemClass = new ClassDecl("System", new FieldDeclList(), new MethodDeclList(), null);
        FieldDecl outField = new FieldDecl(false, true, printStreamType, "out", null);
        systemClass.fieldDeclList.add(outField);
        addDeclaration("System", systemClass);
	}


	@Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
		currentClass = cd;
//		System.out.println(currentClass.name);
		for (FieldDecl fd:cd.fieldDeclList) {
			fd.visit(this, arg);
		}
        for (MethodDecl md:cd.methodDeclList) {
        	md.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, arg);
        return null;
    }

	@Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
		currentMethod = md;
		boolean prevContext = inStaticContext;
		inStaticContext = md.isStatic;
		md.type.visit(this, md);
		openScope();	  
	    for (ParameterDecl pd : md.parameterDeclList) {
	    	if ((findDeclarationLevelPlusTwo(pd.name))) {
	    		throw new IdentificationError(pd, "Already has been declared: " + pd.name);
	    	}
	    	addDeclaration(pd.name, pd);
	    	pd.visit(this, null);
	    }
	    
	    for (Statement s:md.statementList) {
	    	s.visit(this, null);
	    }
	    if (!(md.type.typeKind == TypeKind.VOID)) {
        	if (!(md.statementList.get(md.statementList.size()-1) instanceof ReturnStmt)) {
        		throw new IdentificationError(md, "Non-void method must have return statement.");
        	}
        }
	    
//	    if (md.name.equals("main") && !md.isPrivate && md.isStatic && md.type.typeKind == TypeKind.VOID && md.parameterDeclList.size() == 1 &&
//	    		md.parameterDeclList.get(0).type instanceof ArrayType && ((ArrayType)md.parameterDeclList.get(0).type).eltType instanceof ClassType && ((ClassType)((ArrayType)md.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")) {
//	    	if (isMain) {
//		    	throw new IdentificationError(md, "More than one main method");
//		    }
//	    	isMain = true;
//	    }
	    
	    closeScope();
	    inStaticContext = prevContext;
	    return null;
	}


	@Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
    	pd.type.visit(this, null);
    	addDeclaration(pd.name, pd);
        if (pd.type instanceof ClassType) {
            ClassType classType = (ClassType) pd.type;
            ClassDecl classDecl = findClassDeclaration(classType.className.spelling);
            if (classDecl == null) {
                throw new IdentificationError(pd, "Undeclared class: " + classType.className.spelling);
            }
        }

	    return null;

    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
    	decl.type.visit(this, (MethodDecl)arg);
        currentInitializingVar = decl.name;
//        System.out.println(decl.name + " visitVardecl");
        
        currentInitializingVar = null;
        if (findDeclarationLevelPlusTwo(decl.name)) {
        	throw new IdentificationError(decl, "Error in VisitVarDecl");
        }
        addDeclaration(decl.name, decl);
        
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
    	Declaration classDecl = findClassDeclaration(type.className.spelling);
//    	System.out.println(classDecl);
    	if (classDecl == null) {
    		throw new IdentificationError(type, "Undeclared class: " + type.className.spelling);
    	}
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, arg);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        openScope();
        for (Statement s : stmt.sl) {
            s.visit(this, (MethodDecl)arg);
        }
        closeScope();
        
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
    	stmt.varDecl.visit(this, (MethodDecl)arg);
    	currentInitializingVar = stmt.varDecl.name;
//        System.out.println(stmt.varDecl.name + " visitVardecl");
        
    	if (stmt.initExp != null) {
    		stmt.initExp.visit(this, arg);
    	}
    	currentInitializingVar = null;
    	return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
    	stmt.ref.decl = (Declaration) stmt.ref.visit(this, (MethodDecl)arg);
        Object obj = stmt.val.visit(this, (MethodDecl)arg);
        if (obj instanceof MethodDecl || obj instanceof ClassDecl) {
        	throw new IdentificationError(stmt, "ERROR IN VISITASSIGN STMT IDENTIFICATION");
        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, (MethodDecl)arg);
        stmt.ix.visit(this, (MethodDecl)arg);
        stmt.exp.visit(this, (MethodDecl)arg);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
    	stmt.methodRef.decl=(Declaration) stmt.methodRef.visit(this, (MethodDecl)arg);
        for (Expression e:stmt.argList) {
        	e.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null) {
        	stmt.returnExpr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
    	stmt.cond.visit(this, arg);
        if ((stmt.thenStmt instanceof VarDeclStmt)) {
            throw new IdentificationError(stmt,"Single statement as VARDECLSTMT in scope");
        } else {
            stmt.thenStmt.visit(this, arg);
        }
        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof VarDeclStmt) {
                throw new IdentificationError(stmt, "Else statement as VARDECLSTMT");
            } else {
                stmt.elseStmt.visit(this, arg);
            }
        }

        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, arg);
        if ((stmt.body instanceof VarDeclStmt)) {
        	throw new IdentificationError(stmt, "WHILE statement as VARDECLSTMT");
        } else {
        	stmt.body.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.decl = (Declaration) expr.operator.visit(this, null);
    	expr.decl = (Declaration) expr.expr.visit(this, arg);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
    	expr.decl = (Declaration) expr.operator.visit(this, null);
        expr.decl =	(Declaration) expr.left.visit(this, arg);
        expr.decl =	(Declaration) expr.right.visit(this, arg);
        
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.decl = (Declaration) expr.ref.visit(this, null);
        if (!(expr.ref instanceof ThisRef)) {
        	if (expr.ref.decl instanceof ClassDecl) {
        		throw new IdentificationError(expr, "Cannot reference ClassDecl VISITREFEXPR");
        	}
        }
        if (expr.ref.decl instanceof MethodDecl) {
        	throw new IdentificationError(expr, "Cannot be method decl");
        }
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.decl =	(Declaration) expr.ref.visit(this, arg);

        expr.ref.decl = (Declaration) expr.ixExpr.visit(this, arg);
        

        return null;
    }


	@Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.decl = (Declaration) expr.functionRef.visit(this, null);
//        System.out.println(expr.functionRef.decl + " in identification");
        
        for (Expression e : expr.argList) {
            e.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.decl =	(Declaration) expr.lit.visit(this, arg);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.decl = (Declaration) expr.classtype.visit(this,null);

        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.decl =	(Declaration) expr.eltType.visit(this,null);
        expr.decl =	(Declaration) expr.sizeExpr.visit(this,arg);
        
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
    	ref.decl = currentClass;
    	if (ref.decl == null) {
    		throw new IdentificationError(ref.decl, "Null reference error at visitThisRef.");
    	}
    	if (inStaticContext) {
            throw new IdentificationError(ref.decl, "Cannot reference \"this\" within a static context");
        }
    	return ref.decl;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
//    	System.out.println("ref: " + ref.id.spelling);
//    	System.out.println("initail: " + currentInitializingVar);
    	if (ref.id.spelling.equals(currentInitializingVar)) {
            throw new IdentificationError(ref, "Cannot reference \"" + ref.id.spelling + "\" within the initializing expression of the declaration for \"" + ref.id.spelling + "\"");
        }
    	Declaration decl = findDeclaration(ref.id.spelling, currentClass.name);
        if (decl == null) {
            throw new IdentificationError(ref, "Identifier " + ref.id.spelling + " is not declared.");
        }
        ref.id.decl = decl;
        ref.decl = decl;
        if (inStaticContext && ref.id.decl instanceof FieldDecl) {
        	FieldDecl fd = (FieldDecl)ref.id.decl;
        	if (!fd.isStatic) {
        		throw new IdentificationError(ref, "Cannot access field variable in static method");
        	}
        }
        return decl;
    }

	@Override
    public Object visitQRef(QualRef ref, Object arg) {
//		System.out.println("ENTERING: " + ref.id.spelling);
		ref.ref.decl =	(Declaration) ref.ref.visit(this, arg);
	    if (ref.ref.decl == null) {
	        throw new IdentificationError(ref, "Base declaration is null");
	    }
	    if (ref.ref.decl instanceof MethodDecl) {
	    	throw new IdentificationError(ref, "Error reference in visitQREF");
	    }
	    boolean baseIsCN = ref.ref.decl instanceof ClassDecl;
	    Declaration currentContext = ref.ref.decl;
	    if (currentContext instanceof ClassDecl) {
	    	ClassDecl classDecl = (ClassDecl) currentContext;
	    	Declaration memberDecl = findMemberInClass(ref.id.spelling, classDecl);
	    	if (memberDecl == null) {
	    		throw new IdentificationError(ref, "Member not found in class");
	    	}
	    	if (!(memberDecl instanceof MemberDecl)) {
	    		throw new IdentificationError(ref, "Must be MemberDecl");
	    	}
    		if (((MemberDecl)memberDecl).isPrivate && !currentClass.name.equals(classDecl.name)) {
    			throw new IdentificationError(ref, "Cannot access private within another class");
    		}
	    	
	    	
//	    	System.out.println("baseIsCN: " + baseIsCN + " memberDecl: " + memberDecl + " ref.ref: " + ref.ref);
	    	
	    	if (baseIsCN && !(ref.ref instanceof ThisRef)) {
	    	    if (memberDecl instanceof FieldDecl && !((FieldDecl)memberDecl).isStatic) {
	    	        throw new IdentificationError(ref, "Static field expected");
	    	    }
	    	    else if (memberDecl instanceof MethodDecl && !((MethodDecl)memberDecl).isStatic) {
	    	        throw new IdentificationError(ref, "Static method expected");
	    	    }
	    	}


	    	ref.id.decl = memberDecl;
	    	
	    } else {
	    	// is this local?
	    	 if (currentContext instanceof LocalDecl) {
	    	        LocalDecl varDecl = (LocalDecl) currentContext;
	    	        if (varDecl.type instanceof ClassType) {
	    	            ClassType classType = (ClassType) varDecl.type;
	    	            ClassDecl classDecl = findClassDeclaration(classType.className.spelling);
	    	            if (classDecl == null) {
	    	                throw new IdentificationError(ref, "Type " + classType.className.spelling + " not found.");
	    	            }

	    	            Declaration memberDecl = findMemberInClass(ref.id.spelling, classDecl);
	    	            if (memberDecl == null) {
	    	                throw new IdentificationError(ref, "Member " + ref.id.spelling + " not found in class " + classType.className.spelling);
	    	            }
	    	            if (!(memberDecl instanceof MemberDecl)) {
	    		    		throw new IdentificationError(ref, "Must be MemberDecl");
	    		    	}
	    	    		if (((MemberDecl)memberDecl).isPrivate && !currentClass.name.equals(classDecl.name)) {
	    	    			throw new IdentificationError(ref, "Cannot access private within another class");
	    	    		}
	    		    	
	    	          
	    	            ref.id.decl = memberDecl;
	    	            ref.decl = memberDecl;
	    	        } else {
	    	            throw new IdentificationError(ref, "The base of the qualified reference is not a class instance.");
	    	        }
	    	    } 
	    	 else if (currentContext instanceof FieldDecl) {
	    		 FieldDecl fieldDecl = (FieldDecl) currentContext;
	    		 if (!(fieldDecl.type instanceof ClassType)) {
	    			 throw new IdentificationError(ref, "Not classType");
	    		 }
	    		 ClassType fieldType = (ClassType)fieldDecl.type;
	    		 ClassDecl classDecl = findClassDeclaration(fieldType.className.spelling);
	    		 Declaration memberDecl = findMemberInClass(ref.id.spelling, classDecl);
	    		 if (memberDecl == null) {
	    			 throw new IdentificationError(ref, "Member " + ref.id.spelling + " not found in class " + fieldType.className.spelling);
	    		 }
	    		 if (!(memberDecl instanceof MemberDecl)) {
	 	    		throw new IdentificationError(ref, "Must be MemberDecl");
	 	    	}
	     		if (((MemberDecl)memberDecl).isPrivate && !currentClass.name.equals(classDecl.name)) {
	     			throw new IdentificationError(ref, "Cannot access private within another class");
	     		}
	 	    	
	    		 
	    		 ref.id.decl = memberDecl;
	    		 ref.decl = memberDecl;
	    	 }
	    	 else {
	    	        throw new IdentificationError(ref, "Unsupported base type for a qualified reference.");
	    	    }
	    }
	    
	    ref.decl = ref.id.decl;
	    return ref.decl;
	}

	private Declaration findMemberInClass(String spelling, ClassDecl classDecl) {
		// TODO Auto-generated method stub
		for (FieldDecl f : classDecl.fieldDeclList) {
	        if (f.name.equals(spelling)) return f;
	    }
	    for (MethodDecl m : classDecl.methodDeclList) {
	        if (m.name.equals(spelling)) return m;
	    }
	    return null;
	}

	@Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return findDeclaration(id.spelling, null);
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral bool, Object arg) {
        return null;
    }
    
    private ClassDecl findClassDeclaration(String className) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Declaration decl = scopes.get(i).get(className);
            if (decl instanceof ClassDecl) {
                return (ClassDecl)decl;
            }
        }
        return null;
    }
    
}