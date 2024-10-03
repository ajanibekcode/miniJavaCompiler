package miniJava.ContextualAnalysis;


import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class TypeChecking implements Visitor<Object, TypeDenoter> {
	private ErrorReporter _errors;
	
	public TypeChecking(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse(Package prog) {
//		System.out.println("Starting type checking...");
		prog.visit(this, null);
//		System.out.println("Type checking completed.");

	}

	private void reportTypeError(AST ast, String errMsg) {
		_errors.reportError( ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg );
	}

	@Override
	public TypeDenoter visitPackage(Package prog, Object arg) {
//		System.out.println("Visiting package...");
		for (ClassDecl cd:prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
		for (FieldDecl fd:cd.fieldDeclList) {
			fd.visit(this, null);
		}
		for (MethodDecl md:cd.methodDeclList) {
			md.visit(this, null);
		}
		
		return cd.type;
	}


	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		for(ParameterDecl pd: md.parameterDeclList) {
            pd.visit(this, md);
        }
        for(Statement s : md.statementList) {
            s.visit(this, md);
        }
        return md.type;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		return pd.type;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		if (decl.name.equals("String")){
			if (decl.type instanceof ClassType) {
				return new BaseType(TypeKind.UNSUPPORTED, null);
			}
		}
		return decl.type;
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		for (Statement s:stmt.sl) {
			s.visit(this, (MethodDecl)arg);
		}
		
		return null; 
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeDenoter lhs = stmt.varDecl.visit(this, null);
		TypeDenoter rhs = stmt.initExp.visit(this, null);
//		System.out.println("Lefthand side: " + lhs);
//		System.out.println("Righthand side: " + rhs);
	    if (typeCheck(lhs, rhs) == null) {
	        reportTypeError(stmt, "Type of the initialization expression does not match the variable declaration type");
	    }
	    return null;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
//		System.out.println("Visiting AssignStmt for reference: " + stmt.ref.decl.type);
		TypeDenoter currentType = stmt.ref.decl.type;
		TypeDenoter t2 = stmt.ref.visit(this, null);
		TypeDenoter t3 = stmt.val.visit(this, null);
		if (currentType instanceof ArrayType || currentType instanceof BaseType || currentType.typeKind == TypeKind.CLASS) {
			TypeDenoter t1 = typeCheck(t2, t3);
			if (t1 == null) {
				reportTypeError(stmt, "Invalid type for assigning reference: " + t1);
			}
		}
		return null;
	}


	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		if (stmt.ref.visit(this, null) instanceof ArrayType) {
			if (typeCheck(stmt.ix.visit(this, null), new BaseType(TypeKind.INT, null)) != null) {
				return null;
			}
			reportTypeError(stmt, "Invalid expr in visit IXassignSTMT");
		} else {
			reportTypeError(stmt, "Invalid expr in visi IXassignSTM IF STATEMENT FAILED");
		}
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
//		System.out.println("My stmt.methodRef.decl: " + stmt.methodRef.decl);
		if (!(stmt.methodRef.decl instanceof MethodDecl)) {
			reportTypeError(stmt, "need to access method.");
			return null;
		}
		if (stmt.argList.size() == ((MethodDecl)stmt.methodRef.decl).parameterDeclList.size()) {
			for (int i = 0; i < stmt.argList.size(); i++) {
				TypeDenoter t1 = typeCheck(stmt.argList.get(i).visit(this, null), ((MethodDecl)stmt.methodRef.decl).parameterDeclList.get(i).visit(this, null));
				if (t1 == null) {
					reportTypeError(stmt, "Arg does not match parameter.");
				}
			}
		} else {
			reportTypeError(stmt, "Arg number does not match method number of params.");
		}
		return null;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		if (((MethodDecl)arg).type.typeKind == TypeKind.VOID) {
			if (stmt.returnExpr != null) {
				reportTypeError(stmt, "Returning a VOID method.");
			}
		} else {
			if (stmt.returnExpr != null) {
				 TypeDenoter t1 = typeCheck(((MethodDecl)arg).type, stmt.returnExpr.visit(this, (MethodDecl)arg));
			     if (t1 == null) {
			    	 reportTypeError(stmt, "INVALID RETURN type.");
			     }
			} else {
				reportTypeError(stmt, "Not returning a method that needs return stmt.");
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1 = typeCheck(((TypeDenoter)stmt.cond.visit(this, null)), new BaseType(TypeKind.BOOLEAN, null));
	    if (t1 == null) {
	        reportTypeError(stmt, "Condition in if statement is not of type boolean");
	    }
	    stmt.thenStmt.visit(this, (MethodDecl)arg);
	    if (stmt.elseStmt != null) {
	        stmt.elseStmt.visit(this, (MethodDecl)arg);
	    }
	    return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1 = typeCheck(((TypeDenoter)stmt.cond.visit(this, null)), new BaseType(TypeKind.BOOLEAN, null));
	    if (t1 == null) {
	        reportTypeError(stmt, "Condition in if statement is not of type boolean");
	    }
	    stmt.body.visit(this, (MethodDecl)arg);
	    return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter operandType = expr.expr.visit(this, (MethodDecl)arg);
	    Operator op = expr.operator;

	    switch (op.spelling) {
	        case "-":
	            if (operandType.typeKind != TypeKind.INT) {
	                reportTypeError(expr, "Unary minus applied to a non-int operand");
	                return new BaseType(TypeKind.ERROR, null);
	            }
	            return new BaseType(TypeKind.INT, null);
	            
	        case "!":
	            if (operandType.typeKind != TypeKind.BOOLEAN) {
	                reportTypeError(expr, "Logical negation applied to a non-boolean operand");
	                return new BaseType(TypeKind.ERROR, null);
	            }
	            return new BaseType(TypeKind.BOOLEAN, null);
	            
	        default:
	            reportTypeError(expr, "Unknown unary operator " + op.spelling);
	            return new BaseType(TypeKind.ERROR, null);
	    }
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter lhs = expr.left.visit(this, null);
	    TypeDenoter rhs = expr.right.visit(this, null);
	    Operator op = expr.operator;
	    String spelling = op.spelling;

	    switch (spelling) {
	        // Arithmetic operations
	        case "+":
	        case "-":
	        case "*":
	        case "/":
	            if (lhs.typeKind == TypeKind.INT && rhs.typeKind == TypeKind.INT) {
	                return new BaseType(TypeKind.INT, null);
	            } else {
	                reportTypeError(expr, "Arithmetic operation " + spelling + " applied to non-int operands");
	                return new BaseType(TypeKind.ERROR, null);
	            }

	        // Relational operations
	        case ">":
	        case ">=":
	        case "<":
	        case "<=":
	            if (lhs.typeKind == TypeKind.INT && rhs.typeKind == TypeKind.INT) {
	                return new BaseType(TypeKind.BOOLEAN, null);
	            } else {
	                reportTypeError(expr, "Relational operation " + spelling + " applied to non-int operands");
	                return new BaseType(TypeKind.ERROR, null);
	            }

	        // Logical operations
	        case "&&":
	        case "||":
	            if (lhs.typeKind == TypeKind.BOOLEAN && rhs.typeKind == TypeKind.BOOLEAN) {
	                return new BaseType(TypeKind.BOOLEAN, null);
	            } else {
	                reportTypeError(expr, "Logical operation " + spelling + " applied to non-boolean operands");
	                return new BaseType(TypeKind.ERROR, null);
	            }

	        // Equality operations
	        case "==":
	        case "!=":
	            if (typeCheck(lhs, rhs) != null) {
	                return new BaseType(TypeKind.BOOLEAN, null);
	            } else {
	                reportTypeError(expr, "Equality operation " + spelling + " applied to operands of different types");
	                return new BaseType(TypeKind.ERROR, null);
	            }

	        default:
	            reportTypeError(expr, "Unknown operator " + spelling);
	            return new BaseType(TypeKind.UNSUPPORTED, null);
	    }
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.ref.visit(this, null);
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		if (expr.ref.visit(this, null) instanceof ArrayType) {
//			System.out.println("In Array Type");
			if (typeCheck(expr.ixExpr.visit(this, null), new BaseType(TypeKind.INT, null)) != null) {
//				System.out.println("In Array Type SECOND");
				//return new BaseType(((ArrayType)expr.ref.visit(this, null)).eltType.typeKind, null);
				return ((ArrayType)expr.ref.visit(this, null)).eltType;
			} else {
//				reportTypeError(expr, "Invalid expression in visitIXexpr");
				return new BaseType(TypeKind.ERROR, null);
			}
		} else {
//			System.out.println("In Array Type FAILED IF");
			reportTypeError(expr, "Reference not ArrayType");
			return new BaseType(TypeKind.ERROR, null);
		}
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.functionRef.decl.type = expr.functionRef.visit(this, null);
		if (expr.argList.size() == ((MethodDecl)expr.functionRef.decl).parameterDeclList.size()) {
			for (int i = 0; i < expr.argList.size(); i++) {
				TypeDenoter t1 = typeCheck(expr.argList.get(i).visit(this, null), ((MethodDecl)expr.functionRef.decl).parameterDeclList.get(i).visit(this, null));
				if (t1 == null) {
					reportTypeError(expr, "arg does not match param type.");
				}
			}
		} else {
			reportTypeError(expr, "num does not match in visitCALLEXPR");
			return new BaseType(TypeKind.ERROR, null);
		}
		return ((MethodDecl)expr.functionRef.decl).type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		if (expr.classtype.className.spelling.equals("String")) {
			return new BaseType(TypeKind.UNSUPPORTED, null);
		}
		return expr.classtype;
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		typeCheck(new BaseType(TypeKind.INT, null), expr.sizeExpr.visit(this, null));
		ArrayType t1 = new ArrayType(expr.eltType, null);
		return t1;
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
//		System.out.println("Visiting Identifier: " + id.spelling);
//		id.decl.visit(this, null);
		// TODO Auto-generated method stub
		return id.decl.visit(this, null);
	}

	@Override
	public TypeDenoter visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.INT, null);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.BOOLEAN, null);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral n1, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.CLASS, null);
		
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		if (fd.type.typeKind == TypeKind.VOID) {
			reportTypeError(fd, "needs to be method decl.");
		}
		return null;
	}
	
	private TypeDenoter typeCheck(TypeDenoter t1, TypeDenoter t2) {
//		System.out.println("This is t1: " + t1 + " and this is t2: " + t2);
		if (t1.typeKind == TypeKind.UNSUPPORTED || t2.typeKind == TypeKind.UNSUPPORTED) {
			return null;
		}
		if (t1.typeKind.equals(t2.typeKind)) {
//			System.out.println("t2 typekind: " + t2.typeKind + " t1 typekind: " + t1.typeKind);
//			System.out.println("t1: " + t1.getClass().getName() + " t2: " + t2.getClass().getName());
			if (t1 instanceof BaseType || t2 instanceof BaseType) {
				if (t1 instanceof BaseType) {
					return t1;
				} else {
					return t2;
				}
			}
			if (t1 instanceof ClassType) {
				if (((ClassType)t1).className.spelling.equals(((ClassType)t2).className.spelling)){
//					System.out.println("made it to the conditional if");
					return t1;
				}
//				System.out.println("Made it inside loop");
				return null;
			}
			if (t1 instanceof ArrayType) {
				ArrayType t3 = (ArrayType)t1;
				ArrayType t4 = (ArrayType)t2;
				if (typeCheck(t3.eltType, t4.eltType) != null) {
					return t1;
				} else {
					return null;
				}
					
			}
//			System.out.println("Made it here to return t1");
			return t1;
		}
//		System.out.println("Didnt pass IF STATEMENT.");
		return null;
	}
}