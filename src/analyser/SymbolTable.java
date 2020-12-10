package analyser;

import error.AnalyzeError;
import error.ErrorCode;
import instruction.Instruction;
import util.Pos;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    /** 符号表 */
    private HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
    private SymbolTable lastTable=null;
    /** 下一个变量的栈偏移 */
    private int nextOffset = 0;
    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    public int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    public void addSymbol(String name, boolean isDeclared, int stackOffset, SymbolKind kind, IdentType type, Object value, SymbolTable param, ArrayList<Instruction> instruction, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(name,isDeclared, stackOffset, kind, type, value, param, instruction));
        }
    }
    public void addSymbol(SymbolEntry symbol, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(symbol.getName()) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(symbol.getName() , symbol);
        }
    }
    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    public void declareSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }


    public SymbolTable getLastTable() {
        return lastTable;
    }

    public void setLastTable(SymbolTable lastTable) {
        this.lastTable = lastTable;
    }
}
