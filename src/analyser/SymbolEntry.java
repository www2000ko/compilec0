package analyser;

import instruction.Instruction;

import java.util.ArrayList;

public class SymbolEntry {
    boolean isInitialized;
    int stackOffset;
    SymbolKind kind;
    IdentType type;
    Object value;
    SymbolTable param;
    ArrayList<Instruction> instruction;
    String name;
    public SymbolEntry(){
        this.name =null;
        this.isInitialized = false;
        this.stackOffset = -1;
        this.kind = null;
        this.type = null;
        this.value = null;
        this.param = null;
        this.instruction = null;
    }
    public SymbolEntry(String name,boolean isDeclared, int stackOffset, SymbolKind kind, IdentType type, Object value, SymbolTable param, ArrayList<Instruction>  instruction) {
        this.name =name;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.kind = kind;
        this.type = type;
        this.value = value;
        this.param = param;
        this.instruction = instruction;
    }
    public boolean isConstant(){
        if(this.kind ==SymbolKind.CONST){
            return true;
        }
        else{
            return false;
        }
    }
    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }


    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }

    public void setKind(SymbolKind kind) {
        this.kind = kind;
    }

    public void setType(IdentType type) {
        this.type = type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setParam(SymbolTable param) {
        this.param = param;
    }

    public void setInstruction(ArrayList<Instruction> instruction) {
        this.instruction = instruction;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public IdentType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public SymbolTable getParam() {
        return param;
    }

    public ArrayList<Instruction> getInstruction() {
        return instruction;
    }

    public String getName() {
        return name;
    }
}
