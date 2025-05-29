package lexer;

/**
 * 词法单元类，表示从源代码中识别出的单个单词
 */
public class Token {
    // 单词类型枚举
    public enum Type {
        IDENFR,     // 标识符
        INTCON,     // 整型常量
        STRCON,     // 字符串常量
        CHRCON,     // 字符常量
        MAINTK,     // main关键字
        CONSTTK,    // const关键字
        INTTK,      // int关键字
        CHARTK,     // char关键字
        BREAKTK,    // break关键字
        CONTINUETK, // continue关键字
        IFTK,       // if关键字
        ELSETK,     // else关键字
        NOT,        // !
        AND,        // &&
        OR,         // ||
        FORTK,      // for关键字
        GETINTTK,   // getint关键字
        GETCHARTK,  // getchar关键字
        PRINTFTK,   // printf关键字
        RETURNTK,   // return关键字
        PLUS,       // +
        MINU,       // -
        MULT,       // *
        DIV,        // /
        MOD,        // %
        LSS,        // <
        LEQ,        // <=
        GRE,        // >
        GEQ,        // >=
        EQL,        // ==
        NEQ,        // !=
        ASSIGN,     // =
        SEMICN,     // ;
        COMMA,      // ,
        LPARENT,    // (
        RPARENT,    // )
        LBRACK,     // [
        RBRACK,     // ]
        LBRACE,     // {
        RBRACE,     // }
        VOIDTK      // void关键字
    }
    
    private Type type;        // 单词类型
    private String value;     // 单词的值
    private int lineNumber;   // 单词在源代码中的行号
    
    public Token(Type type, String value, int lineNumber) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    @Override
    public String toString() {
        // 字符串常量输出时需要带双引号
        if (type == Type.STRCON) {
            return type + " \"" + value + "\"";
        }
        // 字符常量输出时需要带单引号
        else if (type == Type.CHRCON) {
            return type + " '" + value + "'";
        }
        // 其他类型按原样输出
        else {
            return type + " " + value;
        }
    }
} 