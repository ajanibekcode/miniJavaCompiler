package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;

public class Rep extends Instruction{
	public Rep() {
		opcodeBytes.write(0xf3);
		opcodeBytes.write(0x48);
		opcodeBytes.write(0xab);
	}
}
