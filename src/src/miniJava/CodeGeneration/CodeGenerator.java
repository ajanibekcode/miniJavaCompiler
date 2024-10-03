package miniJava.CodeGeneration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
public class CodeGenerator implements Visitor<Object, Object> {
	private ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section
	private ClassDecl currentClass;
	private MethodDecl currentMethod;
	private int currentOffset;
	private int _rbpOffset;
	private Stack<Integer> blockScopeStack;
	private List<resolver> unknownAddresses;
	private MethodDecl mainMD;
	class CodeGenerationError extends Error {
		private static final long serialVersionUID = -441346906191470192L; // taken from identification
		
		public CodeGenerationError(String errMsg) {
			super(errMsg);
		}
	}
	
	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}
	public static class resolver{
		enum Type{
			CondJmp,
			Jmp,
			Call
		}
		public static Map<String, Integer> labels = new HashMap<>();
		private InstructionList _asm;
		private Type type;
		private Object obj;
		private Instruction instruction;
		public resolver(InstructionList _asm, Object obj, int index){
			this._asm = _asm;
			this.obj = obj;
			this.instruction = _asm.get(index);
			System.out.println("Current instruction: " + this.instruction.size());
			if (instruction instanceof Jmp) {
				type = Type.Jmp;
			} else if (instruction instanceof CondJmp) {
				type = Type.CondJmp;
			} else if (instruction instanceof Call) {
				type = Type.Call;
			} else {
//				throw new CodeGenerationError("Unsupported type");
				return;
				
			}
		}
			
		public void needsResolve(){
			int tempOffset;
			Instruction newInstruction = null;
			if (obj instanceof MethodDecl) {
				tempOffset = ((MethodDecl)obj)._asmOffset;
				System.out.println("tempOffset: " + tempOffset);
			}
			else {
				String label = (String)obj;
				System.out.println("labels: " + labels.get(label));
				tempOffset = labels.get(label);
				System.out.println("tempOffset after labels: " + tempOffset);
			}
			
			if (type == Type.Call) {
				newInstruction = new Call((int)instruction.startAddress, tempOffset);
			} else if (type == Type.CondJmp) {
				newInstruction = new CondJmp(((CondJmp)instruction).cond, instruction.startAddress, tempOffset, false);
			} else if (type == Type.Jmp) {
				newInstruction = new Jmp(instruction.startAddress, tempOffset, false);
			}
			_asm.patch(this.instruction.listIdx, newInstruction);
		}
		
	}
	
	
	int arg_offset = 0x10;
	
	
	public void parse(Package prog){
		_asm = new InstructionList();
		unknownAddresses = new ArrayList<>();
		List<ClassDecl> mainList = new ArrayList<>();
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new ModRMSIB(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new ModRMSIB(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new ModRMSIB( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new ModRMSIB(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		for (ClassDecl cd:prog.classDeclList) {
			for (MethodDecl md:cd.methodDeclList) {
				if (md.name.equals("main") && !md.isPrivate && md.type.typeKind == TypeKind.VOID && md.parameterDeclList.size() == 1 && md.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY && ((ArrayType)md.parameterDeclList.get(0).type).eltType.typeKind == TypeKind.CLASS && ((ClassType)((ArrayType)md.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")){
					mainMD = md;
					mainList.add(cd);
					break;
				}
			}
		}
		
		if (mainList.size() != 1) {
			throw new CodeGenerationError("More than one or none main method found");
		}
		
		blockScopeStack = new Stack<>();
		resolver.labels.clear();
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.R15, Reg64.RSP)));

		prog.visit(this, null);
		_asm.outputFromMark();
		for (resolver r: unknownAddresses) {
			r.needsResolve();
		}
		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
		
	}
	private int makeExit() {
		// TODO Auto-generated method stub
		int idxStart = _asm.getSize();
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX, true), 60));
		_asm.add(new Xor(new ModRMSIB(Reg64.RDI, Reg64.RDI)));
		_asm.add(new Syscall());
		return idxStart;
	}
	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO: visit relevant parts of our AST
		prog._asmOffset = _asm.getSize();
		for (ClassDecl cd:prog.classDeclList) {
			cd.visit(this, arg);
		}
		
		return null;
	}
	
	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), 0); // TODO: set the location of the main method
	}
	
	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new ModRMSIB(Reg64.RAX,true),0x09) ); // mmap
		
		_asm.add( new Xor(		new ModRMSIB(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.RDX,true),0x03) 	); // prot read|write
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		_asm.add( new Mov_rmi(	new ModRMSIB(Reg64.R8, true),-1) 	); // fd= -1
		_asm.add( new Xor(		new ModRMSIB(Reg64.R9,Reg64.R9)) 	); // offset=0
		_asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}
	
	private int makePrintln() {
		// TODO: how can we generate the assembly to println?
		int idxStart = _asm.getSize();
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSI, Reg64.RSP)));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RAX, true), 1));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDI, true), 1));
		_asm.add(new Mov_rmi(new ModRMSIB(Reg64.RDX, true), 1));
		_asm.add(new Syscall());
		_asm.add(new Pop(Reg64.RAX));
		return idxStart;
	}
	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		currentClass = cd;
		cd._asmOffset = _asm.getSize();
		for (MethodDecl md:cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}
	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		throw new CodeGenerationError("Should not visit FieldDecl");
//		return null;
	}
	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		//push rbp	-> start every method
		//mov rbp, rsp
		List<Instruction> Unknowns = new ArrayList<>();
		currentMethod = md;
		md._asmOffset = _asm.getSize();
		md.methodAddress = _asm.getSize();
		System.out.println("Method name: " + md.name + " address: " + md._asmOffset);
		
		_asm.add(new Push(Reg64.RBP));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, Reg64.RSP)));
		
		int _paramOffset = 16;
		if( !md.isStatic )
			_paramOffset += 8;
		
		for (ParameterDecl pd:md.parameterDeclList) {
			pd._memOffset = _paramOffset;
			_paramOffset += 8;
			System.out.println("paramOffset: " + _paramOffset);
		}
		
		_rbpOffset = 0;
		blockScopeStack.clear();
		blockScopeStack.push(0);
		for (Statement stmt:md.statementList) {
			stmt.visit(this, null);
		}
		
		addInsLabel(md.name);
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RSP, Reg64.RBP)));
		_asm.add(new Pop(Reg64.RBP));
		
		if (md == mainMD) {
			makeExit();
		}
		
		_asm.add(new Ret());
		return null;
	}
	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		throw new CodeGenerationError("Should not visit ParameterDecl");
//		return null;
	}
	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		throw new CodeGenerationError("Should not visit VarDecl");
//		return null;
		
	}
	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		throw new CodeGenerationError("Should not visit BaseType");
//		return null;
	}
	@Override
	public Object visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		throw new CodeGenerationError("Should not visit ClassType");
//		return null;
	}
	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		throw new CodeGenerationError("Should not visit ArrayType");
//		return null;
	}
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt._asmOffset = _asm.getSize();
		blockScopeStack.push(0);
		for (Statement st:stmt.sl) {
			st.visit(this, null);
		}
		int pop = blockScopeStack.pop();
		_rbpOffset += pop;
		_asm.add(new Lea(new ModRMSIB(Reg64.RSP, pop, Reg64.RSP)));
		return null;
	}
	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		_asm.add(new Push(0));
		stmt._asmOffset = _asm.getSize();
		stmt.initExp.visit(this, arg);
		_rbpOffset -= 8;
		stmt.varDecl._memOffset = _rbpOffset;
		blockScopeStack.push(blockScopeStack.pop() + 8);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, stmt.varDecl._memOffset, Reg64.RAX)));
		return null;
	}
	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt._asmOffset = _asm.getSize();
		stmt.ref.visit(this, Boolean.TRUE);
		stmt.val.visit(this, arg);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RDI));
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RDI, 0, Reg64.RAX)));
		return null;
	}
	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt._asmOffset = _asm.getSize();
		stmt.exp.visit(this, null);
		stmt.ref.visit(this, null);
		stmt.ix.visit(this,null);
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RBX));
		_asm.add(new Pop(Reg64.RAX));
		
		_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, Reg64.RCX, 0, 8, Reg64.RBX)));
		
		return null;
	}
	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt._asmOffset = _asm.getSize();
		for (int i = stmt.argList.size()-1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, null);
			System.out.println("Number of args: " + stmt.argList.size());
		}
		if (stmt.methodRef instanceof IdRef) {
			MethodDecl md = (MethodDecl)((IdRef)stmt.methodRef).id.decl;
			if (!md.isStatic) {
//				For IdRef: how can I get "y, x, this" on the stack? 
				_asm.add(new Push(new ModRMSIB(Reg64.RBP, 16)));
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), stmt.methodRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)stmt.methodRef.decl).methodAddress));
				}
			} else {
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), stmt.methodRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)stmt.methodRef.decl).methodAddress));
				}
			}
			
		}
		
		if (((MethodDecl)stmt.methodRef.decl) == MethodDecl.printlnMD) {
			System.out.println("entered here");
			makePrintln();
			return null;
		}
		
		if (stmt.methodRef instanceof QualRef) {
			MethodDecl md = (MethodDecl)((QualRef)stmt.methodRef).id.decl;
			if (!md.isStatic) {
//				For QualRef: how can I get "y, x, a" on the stack (where the full qualref is "a.b")?
				QualRef qr = (QualRef)stmt.methodRef;
				qr.ref.visit(this, null);
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), stmt.methodRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)stmt.methodRef.decl).methodAddress));
				}
			} else {
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), stmt.methodRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)stmt.methodRef.decl).methodAddress));
				}
			}
			
		}
			
		
		
		return null;
	}
	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt._asmOffset = _asm.getSize();
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
			_asm.add(new Pop(Reg64.RAX));
		}
		addUnknown(_asm.add(new Jmp(0, 0, false)), stmt.methodDecl);
		return null;
		
	}
	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt._asmOffset = _asm.getSize();
		//condition
		stmt.cond.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, true), 0));
		int start = _asm.getSize();
		int condJmp = _asm.add(new CondJmp(Condition.E, 0, 0, false));
		//then
		stmt.thenStmt.visit(this, null);
		int end = _asm.getSize();
		//else

		if (stmt.elseStmt != null) {
			int startElse = _asm.getSize();
			int jump = _asm.add(new Jmp(0));
			end = _asm.getSize();
			stmt.elseStmt.visit(this, null);
			int sizeElse = _asm.getSize();
			_asm.patch(jump, new Jmp(startElse, sizeElse, false));
		}
		_asm.patch(condJmp, new CondJmp(Condition.E, start, end, false));
		return null;
		
	}
	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
//		stmt._asmOffset = _asm.getSize();
//		stmt.cond.visit(this, null);
//		_asm.add(new Pop(Reg64.RAX));
//		_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RAX)));
//		
//		int currentAddress = _asm.getSize();
//		CondJmp condJmp = new CondJmp(Condition.NE, 0);
//		_asm.add(condJmp);
//		
//		stmt.body.visit(this, null);
//		_asm.add(new Jmp(_asm.getSize(), condJmp.startAddress, false));
//		
//		_asm.patch(condJmp.listIdx, new CondJmp(Condition.NE, currentAddress, _asm.getSize(), false));
//
//		return null;
		stmt._asmOffset = _asm.getSize();
		
		String condJmpLabel = "condJmpLabel";
		addUnknown(_asm.add(new Jmp(0, 0, false)), condJmpLabel);
		
		
		int tempAddress = _asm.getSize();
		stmt.body.visit(this, null);
		
		
		addInsLabel(condJmpLabel);
		stmt.cond.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, true), 1));
		_asm.add(new CondJmp(Condition.E, _asm.getSize(), tempAddress, false));
		return null;
	}
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		// visit left, visit right
		expr._asmOffset = _asm.getSize();
		expr.expr.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		
		if (expr.operator.spelling.equals("!")) {
			_asm.add(new Xor(new ModRMSIB(Reg64.RAX, true), 1));
		} else if (expr.operator.spelling.equals("-")) {
			_asm.add(new Xor(new ModRMSIB(Reg64.RCX, Reg64.RCX))); // set RCX to 0
			_asm.add(new Sub(new ModRMSIB(Reg64.RCX, Reg64.RAX)));
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
			
		} else {
			throw new CodeGenerationError("Unary operator expected");
		}
	
		_asm.add(new Push(Reg64.RAX));
		return null;
		
	}
	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RAX));
		Condition cond = null;
		
		if (expr.operator.kind == TokenType.BINOP || expr.operator.kind == TokenType.UNOP) {
			if (expr.operator.spelling.equals("*")) {
				_asm.add(new Imul(Reg64.RAX, new ModRMSIB(Reg64.RCX, true)));
			} else if (expr.operator.spelling.equals("/")) {
				_asm.add(new Xor(new ModRMSIB(Reg64.RDX, Reg64.RDX)));
				_asm.add(new Idiv(new ModRMSIB(Reg64.RCX, true)));
			} else if (expr.operator.spelling.equals("&&")) {
				_asm.add(new And(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
			} else if (expr.operator.spelling.equals("||")) {
				_asm.add(new Or(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
			} else if (expr.operator.spelling.equals("-")) {
				_asm.add(new Sub(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
			} else if (expr.operator.spelling.equals("+")) {
				_asm.add(new Add(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
			} else if (expr.operator.spelling.equals(">")) {
				cond = Condition.GT;
			} else if (expr.operator.spelling.equals("<")) {
				cond = Condition.LT;
			} else if (expr.operator.spelling.equals("<=")) {
				cond = Condition.LTE;
			} else if (expr.operator.spelling.equals(">=")) {
				cond = Condition.GTE;
			} else if (expr.operator.spelling.equals("==")) {
				cond = Condition.E;
			} else if (expr.operator.spelling.equals("!=")) {
				cond = Condition.NE;
			} else {
				_errors.reportError("Expected binary operator");
			}
		}
		System.out.println("Current cond: " + cond);
		if (cond != null) {
			_asm.add(new Cmp(new ModRMSIB(Reg64.RAX, Reg64.RCX)));
			_asm.add(new SetCond(cond, Reg8.AL));
			_asm.add(new And(new ModRMSIB(Reg64.RAX, true), 0x1));
		}
		_asm.add(new Push(Reg64.RAX));
		return null;
		
	}
	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		expr.ref.visit(this, Boolean.FALSE);
		return null;
	}
	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));
		_asm.add(new Lea(new ModRMSIB(Reg64.RAX, Reg64.RCX, 8, 8, Reg64.RAX)));
		_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, 0, Reg64.RAX)));
		_asm.add(new Push(Reg64.RAX));
		return null;
	}
	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		// 
		for (int i = expr.argList.size()-1; i >= 0; i--) {
			expr.argList.get(i).visit(this, null);
		}
		if (expr.functionRef instanceof IdRef) {
			MethodDecl md = (MethodDecl)((IdRef)expr.functionRef).id.decl;
			if (!md.isStatic) {
//				For IdRef: how can I get "y, x, this" on the stack? 
				_asm.add(new Push(new ModRMSIB(Reg64.RBP, 16)));
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), expr.functionRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)expr.functionRef.decl).methodAddress));
				}
			} else {
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), expr.functionRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)expr.functionRef.decl).methodAddress));
				}
			}
			
		}
		
		if (expr.functionRef instanceof QualRef) {
			MethodDecl md = (MethodDecl)((QualRef)expr.functionRef).id.decl;
			if (!md.isStatic) {
//				For QualRef: how can I get "y, x, a" on the stack (where the full qualref is "a.b")?
				QualRef qr = (QualRef)expr.functionRef;
				qr.ref.visit(this, null);
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), expr.functionRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)expr.functionRef.decl).methodAddress));
				}
			} else {
				if (md.methodAddress == -1) {
					addUnknown(_asm.add(new Call(0, 0)), expr.functionRef.decl);
				} else {
					_asm.add(new Call(_asm.getSize(), ((MethodDecl)expr.functionRef.decl).methodAddress));
				}
			}
			
		}
		
		_asm.add(new Push(Reg64.RAX));
		return null;
	}
	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		expr.lit.visit(this, arg);
		return null;
	}
	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		makeMalloc();
		_asm.add(new Push(Reg64.RAX));
		return null;
	}
	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr._asmOffset = _asm.getSize();
		makeMalloc();
		_asm.add(new Push(Reg64.RAX));
		return null;
	}
	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		_asm.add(new Push(new ModRMSIB(Reg64.RBP,16)));
		return null;
	}
	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		ref._asmOffset = _asm.getSize();
		if (ref.decl instanceof LocalDecl) {
			if (arg == Boolean.TRUE) {
				_asm.add(new Lea(new ModRMSIB(Reg64.RBP, ref.decl._memOffset, Reg64.RAX)));
			} else {
				_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RBP, ref.decl._memOffset, Reg64.RAX)));
			}
			_asm.add(new Push(Reg64.RAX));
			return null;
		}
//		TODO: Figure out field variables
		if (ref.decl instanceof FieldDecl) {
			System.out.println("entered field decl id ref");
			FieldDecl fd = (FieldDecl)ref.decl;
			if (fd.isStatic) {
				if ((Boolean)arg == Boolean.TRUE && arg instanceof Boolean) {
					_asm.add(new Lea(new ModRMSIB(Reg64.R15, fd._memOffset, Reg64.RBX)));
					_asm.add(new Push(Reg64.RBX));
				} else {
					_asm.add(new Mov_rrm(new ModRMSIB(Reg64.R15, fd._memOffset, Reg64.RBX)));
					_asm.add(new Push(Reg64.RBX));
				}
				return null;
			}
			_asm.add(new Mov_rmr(new ModRMSIB(Reg64.RBP, 16, Reg64.RAX)));
			if (arg == Boolean.TRUE) {
				_asm.add(new Lea(new ModRMSIB(Reg64.RAX, fd._memOffset, Reg64.RBP)));
			} else {
				_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, fd._memOffset, Reg64.RBP)));
			}
			_asm.add(new Push(Reg64.RAX));
			return null;
		}
		return null;
	}
	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		ref._asmOffset = _asm.getSize();
		FieldDecl fd = (FieldDecl)ref.id.decl;
		if (fd.isStatic) {
			if ((Boolean)arg == Boolean.TRUE && arg instanceof Boolean) {
				_asm.add(new Lea(new ModRMSIB(Reg64.R15, fd._memOffset, Reg64.RBX)));
				_asm.add(new Push(Reg64.RBX));
			} else {
				_asm.add(new Mov_rrm(new ModRMSIB(Reg64.R15, fd._memOffset, Reg64.RBX)));
				_asm.add(new Push(Reg64.RBX));
			}
			return null;
		}
		ref.ref.visit(this, (Object)Boolean.FALSE);
		
		_asm.add(new Pop(Reg64.RAX));
		
		
		System.out.println("ref.id.decl: " + ref.id.decl);

		
		if ((Boolean)arg == Boolean.TRUE && arg instanceof Boolean) {
			_asm.add(new Lea(new ModRMSIB(Reg64.RAX, fd._memOffset, Reg64.RBX)));
			_asm.add(new Push(Reg64.RBX));
		} else {
			_asm.add(new Mov_rrm(new ModRMSIB(Reg64.RAX, fd._memOffset, Reg64.RBX)));
			_asm.add(new Push(Reg64.RBX));
		}
		return null;
	}
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		_errors.reportError("Should not visit identifier");
		return null;
	}
	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		_errors.reportError("Should not visit operator");
		return null;
	}
	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		num._asmOffset = _asm.getSize();
		_asm.add(new Push(Integer.parseInt(num.spelling)));
		return null;
	}
	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		bool._asmOffset = _asm.getSize();
		if (bool.spelling.equals("true")){
			_asm.add(new Push(1));
		} else {
			_asm.add(new Push(0));
		}
		return null;
	}
	@Override
	public Object visitNullLiteral(NullLiteral n1, Object arg) {
		// TODO Auto-generated method stub
		n1._asmOffset = _asm.getSize();
		_asm.add(new Push(0));
		return null;
	}
	
	public void addUnknown(int index, Object obj) {
		unknownAddresses.add(new resolver(_asm, obj, index));
	}
	
	public void addInsLabel(String label) {
		// adds instruction labels of all unsresolved addresses
		resolver.labels.put(label, _asm.getSize());
		System.out.println("Added instruction label: " + label);
	}
	
}
