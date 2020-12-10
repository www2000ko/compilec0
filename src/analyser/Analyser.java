package analyser;
import error.*;
import instruction.*;
import tokenizer.*;
import util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    SymbolTable fnTable=new SymbolTable();
    SymbolTable varTable=new SymbolTable();
    SymbolEntry _start;
    SymbolEntry cusymbol=new SymbolEntry();
    /** 当前偷看的 token */
    Token peekedToken = null;

    public Analyser(Tokenizer tokenizer) throws AnalyzeError {
        this.tokenizer = tokenizer;
        this.instructions=new ArrayList<>();
        fnTable.addSymbol(null,false,fnTable.getNextVariableOffset(),SymbolKind.FN,
                null,null,null,this.instructions,null);
    }

    public void analyse() throws CompileError {
        analyseProgram();
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expectAll(TokenType... tt) throws CompileError {
        var token = peek();
        for (TokenType tokens : tt){
            if (token.getTokenType() == tokens) {
                return next();
            }
        }
        throw new ExpectedTokenError(tt[0], token);
    }

    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }


    /**
     * <程序> ::= 'begin'<主过程>'end'
     */
    private void analyseProgram() throws CompileError {
        analyseMain();
        expect(TokenType.EOF);
    }

    private void analyseMain() throws CompileError {
        while(check(TokenType.CONST_KW)||check(TokenType.LET_KW) ){
            analyseDeclaration();
        }
        while (check(TokenType.FN_KW)) {
            analyseFunction();
        }

    }
    private void analyseDeclaration() throws CompileError{
        if(check(TokenType.CONST_KW) ==true){
            analyseConstantDeclaration();
        }
        else if(check(TokenType.LET_KW) ==true){
            analyseLetDeclaration();
        }

    }
    private void analyseLetDeclaration() throws CompileError{
        if(nextIf(TokenType.LET_KW)!=null){
            var nameToken = expect(TokenType.IDENT);
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),false,varTable.getNextVariableOffset(),SymbolKind.CONST
                    ,IdentType.INT,0,null,null);
            expect(TokenType.COLON);
            expect(TokenType.INT);
            boolean isInitialized=false;
            if(check(TokenType.ASSIGN)){
                expect(TokenType.ASSIGN);
                int off=symbol.getStackOffset();
                if(varTable.getLastTable()==null){
                    instructions.add(new Instruction(Operation.globa,off));
                }
                else{
                    instructions.add(new Instruction(Operation.loca ,off));
                }

                analyseExpression();
                instructions.add(new Instruction(Operation.stroe64));
                isInitialized=true;
            }
            expect(TokenType.SEMICOLON);
            //System.out.println("addSymbol"+(String) nameToken.getValue()+nameToken.getStartPos());

            varTable.addSymbol(symbol,nameToken.getStartPos());
        }
    }
    private void analyseConstantDeclaration() throws CompileError {
        if (nextIf(TokenType.CONST_KW) != null) {
            var nameToken = expect(TokenType.IDENT);

            expect(TokenType.COLON);
            expect(TokenType.INT);
            expect(TokenType.ASSIGN);
            analyseExpression();
            expect(TokenType.SEMICOLON);
            //System.out.println("addSymbol"+(String) nameToken.getValue()+nameToken.getStartPos());
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),true,varTable.getNextVariableOffset(),SymbolKind.CONST
                    ,IdentType.INT,0,null,null);
            varTable.addSymbol(symbol,nameToken.getStartPos());
        }
    }

    private void analyseFunction() throws CompileError {
        IdentType type;
        expect(TokenType.FN_KW);
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.L_PAREN);
        if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
            analyseParamList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        if(nextIf(TokenType.INT)!=null){
            type=IdentType.INT;
        }
        else if(nextIf(TokenType.VOID)!=null){
            type=IdentType.VOID;
        }
        else{
            //TODO error
        }
        System.out.println("addSymbol"+(String) nameToken.getValue()+nameToken.getStartPos());
        //TODO addSymbol();
        analyseBlockStatement();
    }

    private void analyseParamList() throws CompileError{
        boolean isConstant=false;
        if(nextIf(TokenType.CONST_KW)==null){
            isConstant=true;
        }
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.INT);
        System.out.println("addSymbol"+(String) nameToken.getValue()+nameToken.getStartPos());
        //TODO addsymbol()
        if(nextIf(TokenType.COMMA)!=null){
            analyseParamList();
        }

    }
    private void analyseBlockStatement() throws CompileError{
        expect(TokenType.L_BRACE);
        while(check(TokenType.IF_KW)||check(TokenType.WHILE_KW)||check(TokenType.RETURN_KW)
                ||check(TokenType.SEMICOLON)||check(TokenType.MINUS)||check(TokenType.IDENT)
                ||check(TokenType.R_PAREN)||check(TokenType.UINT_LITERAL)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)) {
            analyseStatement();
        }
        expect(TokenType.R_BRACE);
    }
    private void analyseStatement() throws CompileError {
            if(check(TokenType.IF_KW)){
                analyseIfStatement();
            }
            else if(check(TokenType.WHILE_KW)){
                analyseWhileStatement();
            }
            else if(check(TokenType.RETURN_KW)){
                analyseReturnStatement();
            }
            else if (check(TokenType.SEMICOLON)){
                expect(TokenType.SEMICOLON);
            }
            else if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.R_PAREN)||check(TokenType.UINT_LITERAL)){
                analyseExpression();
                expect(TokenType.SEMICOLON);
            }
            else if(check())
            else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                analyseDeclaration();
            }

    }
    private void analyseReturnStatement() throws CompileError {
        expect(TokenType.RETURN_KW);
        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.R_PAREN)
                ||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)) {
            analyseExpression();
        }
        expect(TokenType.SEMICOLON);
    }
    private void analyseIfStatement() throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpression();
        analyseBlockStatement();
        expect(TokenType.ELSE_KW);
        if (check(TokenType.R_BRACE)) {
            expect(TokenType.R_BRACE);
            analyseBlockStatement();
        } else if (check(TokenType.IF_KW)) {
            expect(TokenType.IF_KW);
            analyseIfStatement();
        }
    }
    private void analyseWhileStatement() throws CompileError {
        expect(TokenType.WHILE_KW);
        analyseExpression();
        analyseBlockStatement();
    }
    private void analyseExpression() throws CompileError {
        analyseC();
        analyseEP();
    }
    private void analyseEP() throws CompileError{
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            analyseC();;
            analyseEP();

            //TODO
        }
    }
    private void analyseC() throws CompileError {
        analyseD();
        analyseCP();
    }
    private void analyseCP() throws CompileError {
        if(check(TokenType.GE)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.LT)){
            expectAll(TokenType.GE,TokenType.GT,TokenType.LE,TokenType.LT);
            analyseD();
            analyseCP();
            //TODO
        }
    }
    private void analyseD() throws CompileError {
        analyseT();
        analyseDP();
    }
    private void analyseDP() throws CompileError {
        if(check(TokenType.MINUS)||check(TokenType.PLUS)){
            expectAll(TokenType.MINUS,TokenType.PLUS);
            analyseT();
            analyseDP();
            //TODO
        }
    }

    private void analyseT() throws CompileError {
        analyseF();
        analyseTP();
    }
    private void analyseTP() throws CompileError {
        if(check(TokenType.MUL)||check(TokenType.DIV)){
            expectAll(TokenType.MUL,TokenType.DIV);
            analyseF();
            analyseTP();
        }
    }
    private void analyseF() throws CompileError {
        boolean negate;
        if (nextIf(TokenType.MINUS) != null) {
            negate = true;
            // 计算结果需要被 0 减
            //instructions.add(new Instruction(Operation.LIT, 0));
        } else {
            nextIf(TokenType.PLUS);
            negate = false;
        }

        if (check(TokenType.IDENT)) {
            expect(TokenType.IDENT);
            if(check(TokenType.L_PAREN)){
                expect(TokenType.L_PAREN);
                if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.R_PAREN)
                        ||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)) {
                    analyseExpression();
                    while (check(TokenType.COMMA)){
                        expect(TokenType.COMMA);
                        analyseExpression();
                    }
                }
                expect(TokenType.R_PAREN);
            }
            else{

            }
            //instructions.add(new Instruction(Operation.LOD,entry.stackOffset));
        } else if (check(TokenType.UINT_LITERAL)) {
            var nameToken=expect(TokenType.UINT_LITERAL);
            //instructions.add(new Instruction(Operation.LIT,(Integer) nameToken.getValue()));
            // 调用相应的处理函数
        } else if (check(TokenType.L_PAREN)) {
            expect(TokenType.L_PAREN);
            analyseExpression();
            expect(TokenType.R_PAREN);
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.IDENT, TokenType.UINT_LITERAL, TokenType.L_PAREN), next());
        }

        if(check(TokenType.AS_KW)){
            expect(TokenType.AS_KW);
            expect(TokenType.INT);
            //TODO
        }

        if (negate) {
            //instructions.add(new Instruction(Operation.SUB));
        }
    }

//    private void analyseExpression() throws CompileError {
//        if(check(TokenType.MINUS)){
//            analyseNegateExpression();
//        }
//        else if(check(TokenType.IDENT)){
//            expect(TokenType.IDENT);
//            if(check(TokenType.L_PAREN)){
//                analyseCallExpression();
//            }
//            else if(check(TokenType.ASSIGN)){
//                analyseAssignExpression();
//            }
//            else{
//                analyseIdentExpression();
//            }
//        }
//        else if(check(TokenType.UINT_LITERAL)){
//            expect(TokenType.UINT_LITERAL);
//        }
//        else if(check(TokenType.STRING_LITERAL)){
//            expect(TokenType.STRING_LITERAL);
//        }
//        else if(check(TokenType.L_PAREN)){
//            expect(TokenType.L_PAREN);
//            analyseExpression();
//            expect(TokenType.R_PAREN);
//        }
//        //TODO
//
//        if(check(TokenType.AS_KW)){
//            analyseAsExpression();
//        }
//        else if(check(TokenType.PLUS)||check(TokenType.MINUS)||check(TokenType.DIV)||
//        check(TokenType.MUL)||check(TokenType.ASSIGN)||check(TokenType.NEQ)||check(TokenType.LT)
//        ||check(TokenType.LE)||check(TokenType.GE)||check(TokenType.GT)){
//            analyseOperatorExpression();
//        }
//    }
//    private void analyseOperatorExpression() throws CompileError{
//
//        analyseExpression();
//    }
//    private void analyseAsExpression() throws CompileError{
//        expect(TokenType.INT);
//    }
////    private void analyseLiteralExpression() throws CompileError{
////    }
//    private void analyseIdentExpression() throws CompileError{
//        expect(TokenType.IDENT);
//    }
//    private void analyseAssignExpression() throws CompileError{
//        expect(TokenType.IDENT);
//        expect(TokenType.ASSIGN);
//        analyseExpression();
//    }
//    private void analyseCallExpression() throws CompileError{
//        expect(TokenType.IDENT);
//        expect(TokenType.L_PAREN);
//        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.R_PAREN)
//                ||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)) {
//            analyseExpression();
//            while (check(TokenType.COMMA)){
//                analyseExpression();
//            }
//        }
//        expect(TokenType.R_PAREN);
//    }
//    private void analyseNegateExpression() throws CompileError{
//        expect(TokenType.MINUS);
//        analyseExpression();
//        //TODO
//    }


//    private void analyseConstantExpression() throws CompileError {
//        boolean negate;
//        if (nextIf(TokenType.Minus) != null) {
//            negate = true;
//        } else {
//            nextIf(TokenType.Plus);
//            negate = false;
//        }
//        var valToken = expect(TokenType.Uint);
//        if(negate){
//            instructions.add(new Instruction(Operation.LIT,-(int)valToken.getValue()));
//        }
//        else{
//            instructions.add(new Instruction(Operation.LIT,(int)valToken.getValue()));
//        }
//
//        //throw new Error("Not implemented");
//    }



//    private void analyseExpression() throws CompileError {
//        analyseTerm();
//        while(check(TokenType.Minus)||check(TokenType.Plus)){
//            if(nextIf(TokenType.Minus)!=null){
//                analyseTerm();
//                instructions.add(new Instruction(Operation.SUB));
//            }
//            else if(nextIf(TokenType.Plus)!=null){
//                analyseTerm();
//                instructions.add(new Instruction(Operation.ADD));
//            }
//        }
//        //throw new Error("Not implemented");
//    }
//
//    private void analyseAssignmentStatement() throws CompileError {
//        var nameToken=expect(TokenType.Ident);
//        SymbolEntry entry=symbolTable.get(nameToken.getValue());
//        if(entry==null){
//            throw new AnalyzeError(ErrorCode.NotDeclared,nameToken.getStartPos());
//        }
//        if(entry.isConstant){
//            throw new AnalyzeError(ErrorCode.AssignToConstant,nameToken.getStartPos());
//        }
//        expect(TokenType.Equal);
//        analyseExpression();
//        expect(TokenType.Semicolon);
//        instructions.add(new Instruction(Operation.STO,entry.stackOffset));
//        //throw new Error("Not implemented");
//    }
//
//    private void analyseOutputStatement() throws CompileError {
//        expect(TokenType.Print);
//        expect(TokenType.LParen);
//        analyseExpression();
//        expect(TokenType.RParen);
//        expect(TokenType.Semicolon);
//        instructions.add(new Instruction(Operation.WRT));
//    }
//
//    private void analyseTerm() throws CompileError {
//        analyseFactor();
//        while(check(TokenType.Mult)||check(TokenType.Div)){
//            if(nextIf(TokenType.Mult)!=null){
//                analyseFactor();
//                instructions.add(new Instruction(Operation.MUL));
//            }
//            else if(nextIf(TokenType.Div)!=null){
//                analyseFactor();
//                instructions.add(new Instruction(Operation.DIV));
//            }
//        }
//        //throw new Error("Not implemented");
//    }
//
//    private void analyseFactor() throws CompileError {
//        boolean negate;
//        if (nextIf(TokenType.Minus) != null) {
//            negate = true;
//            // 计算结果需要被 0 减
//            instructions.add(new Instruction(Operation.LIT, 0));
//        } else {
//            nextIf(TokenType.Plus);
//            negate = false;
//        }
//
//        if (check(TokenType.Ident)) {
//            var nameToken=next();
//            SymbolEntry entry=symbolTable.get(nameToken.getValue());
//            if(entry==null){
//                throw new AnalyzeError(ErrorCode.NotDeclared,nameToken.getStartPos());
//            }
//            if(entry.isInitialized==false){
//                throw new AnalyzeError(ErrorCode.NotInitialized,nameToken.getStartPos());
//            }
//            instructions.add(new Instruction(Operation.LOD,entry.stackOffset));
//        } else if (check(TokenType.Uint)) {
//            var nameToken=next();
//            instructions.add(new Instruction(Operation.LIT,(Integer) nameToken.getValue()));
//            // 调用相应的处理函数
//        } else if (check(TokenType.LParen)) {
//            var nameToken=next();
//            analyseExpression();
//            if(nextIf(TokenType.RParen)==null){
//                //throw new
//            }
//        } else {
//            // 都不是，摸了
//            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//        }
//
//        if (negate) {
//            instructions.add(new Instruction(Operation.SUB));
//        }
//        //throw new Error("Not implemented");
//    }
}
