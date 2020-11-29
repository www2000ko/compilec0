package tokenizer;

public enum TokenType {
    /** 空 */
    None,
    FN_KW     ,//-> 'fn'
    LET_KW    ,//-> 'let'
    CONST_KW  ,//-> 'const'
    AS_KW     ,//-> 'as'
    WHILE_KW  ,//-> 'while'
    IF_KW     ,//-> 'if'
    ELSE_KW   ,//-> 'else'
    RETURN_KW ,//-> 'return'

    UINT_LITERAL,
    STRING_LITERAL,

    IDENT,

    PLUS    ,// -> '+'
    MINUS   , //-> '-'
    MUL     , //-> '*'
    DIV     , //-> '/'
    ASSIGN  ,// -> '='
    EQ      ,// -> '=='
    NEQ     , //-> '!='
    LT      , //-> '<'
    GT      , //-> '>'
    LE      , //-> '<='
    GE      , //-> '>='
    L_PAREN , //-> '('
    R_PAREN , //-> ')'
    L_BRACE , //-> '{'
    R_BRACE , //-> '}'
    ARROW   , //-> '->'
    COMMA   , //-> ','
    COLON   , //-> ':'
    SEMICOLO, //-> ';'
    /** 文件尾 */
    EOF;


}
