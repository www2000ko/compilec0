package tokenizer;



import error.ErrorCode;
import error.TokenizeError;
import util.Pos;

import java.util.HashMap;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrUnknown();
        }
    }


    private Token lexUInt() throws TokenizeError {
        String arr = "";
        arr+=it.nextChar();
        Pos startPos=it.currentPos();
        while(Character.isDigit(it.peekChar())){
            arr+=it.nextChar();
        }
        Pos endPos=it.currentPos();
        return new Token(TokenType.UINT_LITERAL, Integer.valueOf(arr), startPos, endPos);
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
        //throw new Error("Not implemented");
    }
//    FN_KW     ,//-> 'fn'
//    LET_KW    ,//-> 'let'
//    CONST_KW  ,//-> 'const'
//    AS_KW     ,//-> 'as'
//    WHILE_KW  ,//-> 'while'
//    IF_KW     ,//-> 'if'
//    ELSE_KW   ,//-> 'else'
//    RETURN_KW ,//-> 'return'
    public final static HashMap<String,TokenType> keywordmap=new HashMap(){{
        put("fn", TokenType.FN_KW);
        put("let", TokenType.LET_KW);
        put("const", TokenType.CONST_KW);
        put("as", TokenType.AS_KW);
        put("while", TokenType.WHILE_KW);
        put("if", TokenType.IF_KW);
        put("else", TokenType.ELSE_KW);
        put("return", TokenType.RETURN_KW);
    }};

    private Token lexIdentOrKeyword() throws TokenizeError {
        String arr = "";
        arr+=it.nextChar();
        Pos startPos=it.currentPos();
        while(Character.isDigit(it.peekChar())||Character.isAlphabetic(it.peekChar())){
            arr+=it.nextChar();
        }
        Pos endPos=it.currentPos();
        for(String key:keywordmap.keySet()){
            if(arr.equals(key)){
                return new Token(keywordmap.get(key), arr, startPos, endPos);
            }
        }
        return new Token(TokenType.IDENT, arr, startPos, endPos);
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        //throw new Error("Not implemented");
    }
//    PLUS    ,// -> '+'
//    MINUS   , //-> '-'
//    MUL     , //-> '*'
//    DIV     , //-> '/'
//    ASSIGN  ,// -> '='
//    EQ      ,// -> '=='
//    NEQ     , //-> '!='
//    LT      , //-> '<'
//    GT      , //-> '>'
//    LE      , //-> '<='
//    GE      , //-> '>='
//    L_PAREN , //-> '('
//    R_PAREN , //-> ')'
//    L_BRACE , //-> '{'
//    R_BRACE , //-> '}'
//    ARROW   , //-> '->'
//    COMMA   , //-> ','
//    COLON   , //-> ':'
//    SEMICOLO, //-> ';'
    private Token lexOperatorOrUnknown() throws TokenizeError {
        char ch=it.nextChar();
        Pos prePos=it.previousPos();
        if(ch=='+'){
            return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
        }
        else if(ch=='-'){
            return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
        }
        else if(ch=='*'){
            return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
        }
        else if(ch=='/'){
            return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
        }
        else if(ch=='='){
            if(it.peekChar()=='='){
                ch=it.nextChar();
                Pos cuPos=it.currentPos();
                return new Token(TokenType.EQ, "==", prePos, cuPos);
            }
            else{
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
            }
        }
        else if(ch=='!'&&it.peekChar()=='='){
            ch=it.nextChar();
            Pos cuPos=it.currentPos();
            return new Token(TokenType.NEQ, "!=", prePos, cuPos);
        }
        else if(ch=='<'){
            if(it.peekChar()=='='){
                ch=it.nextChar();
                Pos cuPos=it.currentPos();
                return new Token(TokenType.LE, "<=", prePos, cuPos);
            }
            else{
                return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());
            }
        }
        else if(ch=='>'){
            if(it.peekChar()=='='){
                ch=it.nextChar();
                Pos cuPos=it.currentPos();
                return new Token(TokenType.GE, ">=", prePos, cuPos);
            }
            else{
                return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());
            }
        }
        else if(ch=='('){
            return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());
        }
        else if(ch==')'){
            return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());
        }
        else if(ch=='{'){
            return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
        }
        else if(ch=='}'){
            return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
        }
        else if(ch=='-'&&it.peekChar()=='>'){
            ch=it.nextChar();
            Pos cuPos=it.currentPos();
            return new Token(TokenType.ARROW, "->", prePos, cuPos);
        }
        else if(ch==','){
            return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
        }
        else if(ch==':'){
            return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
        }
        else if(ch==';'){
            return new Token(TokenType.COLON, ';', it.previousPos(), it.currentPos());
        }
//        switch (it.nextChar()) {
//            case '+':
//                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
//            case '-':
//                // 填入返回语句
//                return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
//                //throw new Error("Not implemented");
//            case '*':
//                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
//                //throw new Error("Not implemented");Div
//            case '/':
//                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
//                //throw new Error("Not implemented");
//            case '=':
//                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
//            case ';':
//                return new Token(TokenType.Semi, ';', it.previousPos(), it.currentPos());
//            case '(':
//                return new Token(TokenType.LParen, '(', it.previousPos(), it.currentPos());
//            case ')':
//                return new Token(TokenType.RParen, ')', it.previousPos(), it.currentPos());
//            // 填入更多状态和返回语句
//            default:
//                // 不认识这个输入，摸了
//                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
//        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
