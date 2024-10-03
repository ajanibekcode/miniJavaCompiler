package miniJava;

import miniJava.SyntacticAnalyzer.Scanner;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.CodeGeneration.ELFMaker;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecking;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) {
		// TODO: Instantiate the ErrorReporter object
		
		// TODO: Check to make sure a file path is given in args
		
		// TODO: Create the inputStream using new FileInputStream
		
		// TODO: Instantiate the scanner with the input stream and error object
		
		// TODO: Instantiate the parser with the scanner and error object
		
		// TODO: Call the parser's parse function
		
		// TODO: Check if any errors exist, if so, println("Error")
		//  then output the errors
		
		// TODO: If there are no errors, println("Success")
		ErrorReporter errorReporter = new ErrorReporter();
		if (args.length != 1){
			return;
		}
		InputStream file;
		try {
			file = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			return;
		}
		Scanner scanner = new Scanner(file, errorReporter);
		Parser parser = new Parser(scanner, errorReporter);
		Package p = parser.parse();
//		ASTDisplay astDisplay = new ASTDisplay();
//		astDisplay.showTree(p);

		if (errorReporter.hasErrors()){
			System.out.println("Error");
			errorReporter.outputErrors();
			return;
		} 
		Identification i = new Identification(errorReporter);
		i.parse(p);
		if (errorReporter.hasErrors()){
			System.out.println("Error");
			errorReporter.outputErrors();
			return;
		}
		TypeChecking typeCheck = new TypeChecking(errorReporter);
		typeCheck.parse(p);
		if (errorReporter.hasErrors()){
			System.out.println("Error");
			errorReporter.outputErrors();
			return;
		}
		CodeGenerator codeGen = new CodeGenerator(errorReporter);
		codeGen.parse(p);
		
		if (errorReporter.hasErrors()) {
			System.out.println("Error");
			errorReporter.outputErrors();
			return;
		}
		else {
//			ASTDisplay display = new ASTDisplay();
//			display.showTree(programAST);
			codeGen.makeElf("a.out");
		}
	}
}
