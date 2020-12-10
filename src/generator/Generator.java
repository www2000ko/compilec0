package generator;

import analyser.Analyser;
import analyser.IdentType;
import analyser.SymbolEntry;
import analyser.SymbolTable;
import error.CompileError;
import instruction.Instruction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Generator {
    DataOutputStream output;
    Analyser analyser;
    int magic=0x72303b3e;
    int version=1;
    public Generator(DataOutputStream output, Analyser analyser){
        this.output=output;
        this.analyser=analyser;
    }
    public void generateo0() throws IOException {
        output.writeInt(this.magic);
        output.writeInt(this.version);
        generateGlobals();
        generateFn();
    }
    private void generateGlobals() throws IOException {
        int count=analyser.globalTable.getCount();
        output.writeInt(count);
        LinkedHashMap<String, SymbolEntry> table=analyser.globalTable.getSymbolTable();
        for(Map.Entry<String, SymbolEntry> entry : table.entrySet()) {
            output.writeBoolean(entry.getValue().isConstant());
            output.writeInt(8);
            output.writeLong((Long) entry.getValue().getValue());
        }

    }

    private void generateFn() throws IOException{
        int count=analyser._startTable.getCount();
        output.writeInt(count);
        LinkedHashMap<String, SymbolEntry> table=analyser._startTable.getSymbolTable();
        for(Map.Entry<String, SymbolEntry> entry : table.entrySet()) {
            output.writeInt(entry.getValue().getStackOffset());
            if(entry.getValue().getType() == IdentType.VOID){
                output.writeInt(0);
            }
            else if(entry.getValue().getType() ==IdentType.INT){
                output.writeInt(8);
            }
            if(entry.getValue().getParam() == null){
                output.writeInt(0);
            }
            else{
                //TODO
            }
            if(entry.getValue().getLoc()==null){
                output.writeInt(0);
            }else{
                SymbolTable loc=entry.getValue().getLoc();
                output.writeInt(loc.getCount());
            }
            ArrayList<Instruction> instructions=entry.getValue().getInstruction();
            output.writeInt(instructions.size());
            generateInstruction(instructions);
        }
    }

    private void generateInstruction(ArrayList<Instruction> instructions) throws IOException {
        for(Instruction instruction:instructions){
            output.write(instruction.toByte());
        }
    }
};