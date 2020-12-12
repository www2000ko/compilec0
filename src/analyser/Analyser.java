package analyser;
import error.*;
import instruction.*;
import tokenizer.*;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> cuinstructions=null;
    public SymbolTable _startTable=new SymbolTable();
    public SymbolEntry _start=null;
    public SymbolEntry cufn=null;
    public SymbolEntry main=null;
    public SymbolTable globalTable=new SymbolTable();
    private SymbolTable fnTable=_startTable;
    private SymbolTable varTable=globalTable;
    private SymbolTable paraTable =null;
    int stack=0;
    int stackTop=0;
    List <String> libs=Arrays.asList("getint","getdouble","getchar","putint","putdouble","putchar","putstr","putln");
    /** 当前偷看的 token */
    Token peekedToken = null;

    public Analyser(Tokenizer tokenizer) throws AnalyzeError {
        this.tokenizer = tokenizer;
        this.cuinstructions=new ArrayList<>();
        this._start=new SymbolEntry(null,false,fnTable.getNextVariableOffset(),SymbolKind.FN,
                IdentType.VOID,null,null,0,this.cuinstructions,null);
        fnTable.addSymbol(_start,null);
        for(String lib:libs){
            globalTable.addSymbol(new SymbolEntry(lib,true,globalTable.getNextVariableOffset(),SymbolKind.CONST,IdentType.STRING,lib),null);
        }
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
        else{
            throw new ExpectedTokenError(List.of(TokenType.CONST_KW,TokenType.LET_KW), next());
        }

    }
    private void analyseLetDeclaration() throws CompileError{
        if(nextIf(TokenType.LET_KW)!=null){
            var nameToken = expect(TokenType.IDENT);
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),false,varTable.getNextVariableOffset(),SymbolKind.LET
                    ,IdentType.INT,0L);
            expect(TokenType.COLON);
            expect(TokenType.INT);
            if(check(TokenType.ASSIGN)){
                expect(TokenType.ASSIGN);
                symbol.setInitialized(true);
                int off=symbol.getStackOffset();
                if(varTable.isStart()){
                    cuinstructions.add(new Instruction(Operation.globa,off));
                }
                else{
                    cuinstructions.add(new Instruction(Operation.loca ,off));
                }

                analyseExpression();
                cuinstructions.add(new Instruction(Operation.stroe64));
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
                    ,IdentType.INT,0L);
            varTable.addSymbol(symbol,nameToken.getStartPos());
        }
    }

    private void analyseFunction() throws CompileError {
        SymbolEntry symbol=new SymbolEntry(fnTable.getNextVariableOffset());
        this.cufn=symbol;
        ArrayList<Instruction> instructions=new ArrayList<Instruction>();
        cuinstructions=instructions;
        symbol.setInstruction(instructions);


        SymbolTable locTable=new SymbolTable();
        varTable=locTable;
        symbol.setLoc(locTable);


        symbol.setKind(SymbolKind.FN);

        symbol.setInitialized(true);

        IdentType type;
        expect(TokenType.FN_KW);
        var nameToken = expect(TokenType.IDENT);

        symbol.setName((String)nameToken.getValue());

        SymbolTable paratable=new SymbolTable();
        symbol.setParam(paratable);
        locTable.setLastTable(paratable);

        expect(TokenType.L_PAREN);
        if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
            this.paraTable=paratable;
            analyseParamList();
            this.paraTable=null;
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        if(nextIf(TokenType.INT)!=null){
            type=IdentType.INT;
            SymbolEntry returnsymbol=new SymbolEntry(null,true,cufn.getParam().getNextVariableOffset(),SymbolKind.LET
                    ,IdentType.INT,0L);
            cufn.getParam().addSymbol(returnsymbol,nameToken.getStartPos());
        }
        else if(nextIf(TokenType.VOID)!=null){
            type=IdentType.VOID;
        }
        else{
            throw new ExpectedTokenError(List.of(TokenType.INT,TokenType.VOID), next());
        }
        symbol.setType(type);
        if(((String)nameToken.getValue()).equals("main")){
            this.main=symbol;
            if(symbol.getType()==IdentType.VOID){
                _start.getInstruction().add(new Instruction(Operation.stackalloc,0));
            }
            else if(symbol.getType()==IdentType.INT){
                _start.getInstruction().add(new Instruction(Operation.stackalloc,1));
            }
            _start.getInstruction().add(new Instruction(Operation.call,symbol.getStackOffset()));
        }
        //System.out.println("addSymbol"+(String) nameToken.getValue()+nameToken.getStartPos());
        fnTable.addSymbol(symbol,nameToken.getStartPos());
        analyseBlockStatement();
        if(symbol.getType()==IdentType.VOID||symbol.getInstruction().get(symbol.getInstruction().size()-1).getOpt()!=Operation.ret){
            symbol.getInstruction().add(new Instruction(Operation.ret));
        }
    }

    private void analyseParamList() throws CompileError{
        SymbolEntry symbol=new SymbolEntry(cufn.getParam().getNextVariableOffset());
        SymbolKind isConstant=SymbolKind.LET;
        if(nextIf(TokenType.CONST_KW)!=null){
            isConstant=SymbolKind.CONST;
        }
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        expect(TokenType.INT);
        symbol.setName((String) nameToken.getValue());
        symbol.setInitialized(true);
        symbol.setKind(isConstant);
        symbol.setType(IdentType.INT);
        this.paraTable.addSymbol(symbol,nameToken.getStartPos());
        if(nextIf(TokenType.COMMA)!=null){
            analyseParamList();
        }
    }
    private void analyseBlockStatement() throws CompileError{
        expect(TokenType.L_BRACE);
        while(check(TokenType.IF_KW)||check(TokenType.WHILE_KW)||check(TokenType.RETURN_KW)
                ||check(TokenType.SEMICOLON)||check(TokenType.MINUS)||check(TokenType.IDENT)
                ||check(TokenType.L_PAREN)||check(TokenType.UINT_LITERAL)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)) {
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
            else if(check(TokenType.IDENT)){
                analyseIdentStatement();
            }
            else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                analyseDeclaration();
            }
            else{
                throw new ExpectedTokenError(List.of(TokenType.IF_KW,TokenType.WHILE_KW,TokenType.RETURN_KW,
                        TokenType.SEMICOLON,TokenType.IDENT, TokenType.LET_KW,TokenType.CONST_KW) ,next());
            }
    }
    private void analyseReturnStatement() throws CompileError {
        expect(TokenType.RETURN_KW);
        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_PAREN)
                ||check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)) {

            if(cufn.getType()==IdentType.VOID){
                throw new Error("gugu");
            }
            cuinstructions.add(new Instruction(Operation.arga,0));
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.stroe64));
        }

        cuinstructions.add(new Instruction(Operation.ret));
        expect(TokenType.SEMICOLON);
    }
    private void analyseIfStatement() throws CompileError {
        expect(TokenType.IF_KW);
        analyseBlooeanExpression();
        Instruction passblock1=new Instruction(Operation.br);
        cuinstructions.add(passblock1);
        int off1=cufn.getInstruction().size();
        analyseBlockStatement();
        Instruction passblock2=new Instruction(Operation.br,0);
        cuinstructions.add(passblock2);
        int off2=cufn.getInstruction().size();
        passblock1.setX(off2-off1);
        if(check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if (check(TokenType.L_BRACE)) {
                analyseBlockStatement();
            } else if (check(TokenType.IF_KW)) {
                analyseIfStatement();
            }
            int off3=cufn.getInstruction().size();
            passblock2.setX(off3-off2);
        }
    }
    private void analyseWhileStatement() throws CompileError {
        expect(TokenType.WHILE_KW);
        cuinstructions.add(new Instruction(Operation.br,0));
        int off1=cufn.getInstruction().size();

        analyseBlooeanExpression();

        Instruction passblock=new Instruction(Operation.br);
        cuinstructions.add(passblock);
        int off2=cufn.getInstruction().size();

        analyseBlockStatement();

        Instruction back=new Instruction(Operation.br);
        cuinstructions.add(back);
        int off3=cufn.getInstruction().size();

        back.setX(off1-off3);
        passblock.setX(off3-off2);
    }
    private void analyseBlooeanExpression() throws CompileError {
        nextIf(TokenType.L_PAREN);
        analyseExpression();
        if(check(TokenType.LT)){
            expect(TokenType.LT);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.cmpi));
            cuinstructions.add(new Instruction(Operation.setlt));
            cuinstructions.add(new Instruction(Operation.brtrue,1));
        }
        else if(check(TokenType.LE)){
            expect(TokenType.LE);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.cmpi));
            cuinstructions.add(new Instruction(Operation.setgt));
            cuinstructions.add(new Instruction(Operation.brfalse,1));
        }
        else if(check(TokenType.GE)){
            expect(TokenType.GE);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.cmpi));
            cuinstructions.add(new Instruction(Operation.setlt));
            cuinstructions.add(new Instruction(Operation.brfalse,1));
        }
        else if(check(TokenType.GT)){
            expect(TokenType.GT);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.cmpi));
            cuinstructions.add(new Instruction(Operation.setgt));
            cuinstructions.add(new Instruction(Operation.brtrue,1));
        }
        else if(check(TokenType.NEQ)){
            expect(TokenType.NEQ);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.cmpi));
            cuinstructions.add(new Instruction(Operation.brtrue,1));
        }
        else if(check(TokenType.EQ)){
            expect(TokenType.EQ);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.cmpi));
            cuinstructions.add(new Instruction(Operation.brfalse,1));
        }
        else{
            cuinstructions.add(new Instruction(Operation.brtrue,1));
        }
        nextIf(TokenType.R_PAREN);
    }
    private void analyseIdentStatement() throws CompileError{
        var nameToken=expect(TokenType.IDENT);
        if(check(TokenType.L_PAREN)){
                analysefn(nameToken);
        }
        else if(check(TokenType.ASSIGN)){
            SymbolEntry entry=getvar(nameToken);
            expect(TokenType.ASSIGN);
            entry.setInitialized(true);
            analyseExpression();
            cuinstructions.add(new Instruction(Operation.stroe64));
        }
        else{
            throw new ExpectedTokenError(List.of(TokenType.IDENT,TokenType.ASSIGN) ,next());
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseExpression() throws CompileError{
        analyseTerm();
        while(check(TokenType.MINUS)||check(TokenType.PLUS)){
            if(nextIf(TokenType.MINUS)!=null){
                analyseTerm();
                cuinstructions.add(new Instruction(Operation.subi));
            }
            else if(nextIf(TokenType.PLUS)!=null){
                analyseTerm();
                cuinstructions.add(new Instruction(Operation.addi));
            }
        }
        //throw new Error("Not implemented");
    }
    private void analyseTerm() throws CompileError {
        analyseFactor();
        while(check(TokenType.MUL)||check(TokenType.DIV)){
            if(nextIf(TokenType.MUL)!=null){
                analyseFactor();
                cuinstructions.add(new Instruction(Operation.muli));
            }
            else if(nextIf(TokenType.DIV)!=null){
                analyseFactor();
                cuinstructions.add(new Instruction(Operation.divi));
            }
        }
        //throw new Error("Not implemented");
    }

    private void analyseFactor() throws CompileError {
        int negate = 0;
        while (check(TokenType.MINUS) ||check(TokenType.PLUS)) {
            if(nextIf(TokenType.MINUS)!=null){
                negate = negate+1;
                // 计算结果需要被 0 减
                cuinstructions.add(new Instruction(Operation.push, 0));
            }
            else{
                nextIf(TokenType.PLUS);
            }
        }

        if (check(TokenType.IDENT)) {
            var nameToken=expect(TokenType.IDENT);
            if(check(TokenType.L_PAREN)){
                analysefn(nameToken);
            }
            else{
                SymbolEntry entry=getvar(nameToken);
                cuinstructions.add(new Instruction(Operation.load64,entry.stackOffset));
            }
        } else if (check(TokenType.UINT_LITERAL)) {
            var nameToken=next();
            cuinstructions.add(new Instruction(Operation.push,(Integer) nameToken.getValue()));
            // 调用相应的处理函数
        } else if(check(TokenType.STRING_LITERAL)){
            var nameToken=next();
            SymbolEntry symbol=new SymbolEntry((String)nameToken.getValue(),true,globalTable.getNextVariableOffset(),SymbolKind.CONST
                    ,IdentType.STRING,(String)nameToken.getValue());
            globalTable.addSymbol(symbol,nameToken.getStartPos());
            cuinstructions.add(new Instruction(Operation.push,globalTable.getOffset((String)nameToken.getValue(),nameToken.getStartPos())));
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
        for(int i=0;i<negate;i++){
            cuinstructions.add(new Instruction(Operation.subi));
        }
        //throw new Error("Not implemented");
    }
    private void analysefn(Token nameToken) throws CompileError {
        if(nameToken.getValue().equals("putint")||nameToken.getValue().equals("putdouble")
                ||nameToken.getValue().equals("putchar")||nameToken.getValue().equals("putstr")){

            SymbolEntry entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            cuinstructions.add(new Instruction(Operation.stackalloc,0));
            expect(TokenType.L_PAREN);
            if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.L_PAREN)
                    || check(TokenType.UINT_LITERAL) || check(TokenType.STRING_LITERAL)) {
                analyseExpression();
            }
            expect(TokenType.R_PAREN);

            cuinstructions.add(new Instruction(Operation.callname,entry.getStackOffset()));
        }
        else if(nameToken.getValue().equals("getint")||nameToken.getValue().equals("getdouble")||nameToken.getValue().equals("getchar")){
            SymbolEntry entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            expect(TokenType.L_PAREN);
            expect(TokenType.R_PAREN);
            cuinstructions.add(new Instruction(Operation.stackalloc,1));
            cuinstructions.add(new Instruction(Operation.callname,entry.getStackOffset()));
        }
        else if(nameToken.getValue().equals("putln")){
            SymbolEntry entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            expect(TokenType.L_PAREN);
            expect(TokenType.R_PAREN);
            cuinstructions.add(new Instruction(Operation.stackalloc,0));
            cuinstructions.add(new Instruction(Operation.callname,entry.getStackOffset()));
        }
        else{
            SymbolEntry entry=fnTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
            stackAlloc(entry);
            expect(TokenType.L_PAREN);
            if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.L_PAREN)
                    || check(TokenType.UINT_LITERAL) || check(TokenType.STRING_LITERAL)) {
                analyseExpression();
                for(int i=0;i<entry.getParam().getCount()-1;i++){
                    expect(TokenType.COMMA);
                    analyseExpression();
                }
            }
            expect(TokenType.R_PAREN);
            cuinstructions.add(new Instruction(Operation.call,entry.getStackOffset()));
        }
    }
    private SymbolEntry getvar(Token nameToken) throws AnalyzeError {
        SymbolEntry entry=varTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
        if(entry==null){
            entry=cufn.getParam().getsymbol(nameToken.getValue(),nameToken.getStartPos());
            if(entry==null){
                entry=globalTable.getsymbol(nameToken.getValue(),nameToken.getStartPos());
                if(entry==null){
                    throw new AnalyzeError(ErrorCode.NotDeclared,nameToken.getStartPos());
                }
                else{
                    cuinstructions.add(new Instruction(Operation.globa,entry.stackOffset));
                }
            }
            else{
                cuinstructions.add(new Instruction(Operation.arga,entry.stackOffset));
            }
        }
        else{
            cuinstructions.add(new Instruction(Operation.loca,entry.stackOffset));
        }
        return entry;
    }

    private void stackAlloc(SymbolEntry symbol){
        if(symbol.getType()==IdentType.VOID){
            cuinstructions.add(new Instruction(Operation.stackalloc,0));
        }
        else if(symbol.getType()==IdentType.INT){
            cuinstructions.add(new Instruction(Operation.stackalloc,1));
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
