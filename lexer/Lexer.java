package lexer;

import utils.SimpleErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词法分析器，将源代码解析为词法单元序列
 */
public class Lexer {
    private static final Map<String, Token.Type> KEYWORDS = new HashMap<>();
    
    // 初始化关键字映射
    static {
        KEYWORDS.put("main", Token.Type.MAINTK);
        KEYWORDS.put("const", Token.Type.CONSTTK);
        KEYWORDS.put("int", Token.Type.INTTK);
        KEYWORDS.put("char", Token.Type.CHARTK);
        KEYWORDS.put("void", Token.Type.VOIDTK);
        KEYWORDS.put("if", Token.Type.IFTK);
        KEYWORDS.put("else", Token.Type.ELSETK);
        KEYWORDS.put("for", Token.Type.FORTK);
        KEYWORDS.put("break", Token.Type.BREAKTK);
        KEYWORDS.put("continue", Token.Type.CONTINUETK);
        KEYWORDS.put("return", Token.Type.RETURNTK);
        KEYWORDS.put("getint", Token.Type.GETINTTK);
        KEYWORDS.put("getchar", Token.Type.GETCHARTK);
        KEYWORDS.put("printf", Token.Type.PRINTFTK);
    }
    
    private String source;       // 源代码
    private int position;        // 当前位置
    private int line;            // 当前行号
    private char currentChar;    // 当前字符
    private List<Token> tokens;  // 识别出的词法单元列表
    
    /**
     * 创建词法分析器
     * @param source 源代码
     */
    public Lexer(String source) {
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.tokens = new ArrayList<>();
        if (!source.isEmpty()) {
            this.currentChar = source.charAt(0);
        }
    }
    
    /**
     * 执行词法分析，将源代码转换为词法单元序列
     * @return 词法单元序列
     */
    public List<Token> tokenize() {
        while (position < source.length()) {
            // 跳过空白字符
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                }
                advance();
                continue;
            }
            
            // 跳过注释
            if (currentChar == '/' && peekNext() == '/') {
                skipLineComment();
                continue;
            }
            
            if (currentChar == '/' && peekNext() == '*') {
                skipBlockComment();
                continue;
            }
            
            // 识别标识符和关键字
            if (Character.isLetter(currentChar) || currentChar == '_') {
                tokens.add(identifier());
                continue;
            }
            
            // 识别数字
            if (Character.isDigit(currentChar)) {
                tokens.add(number());
                continue;
            }
            
            // 识别字符常量
            if (currentChar == '\'') {
                tokens.add(character());
                continue;
            }
            
            // 识别字符串常量
            if (currentChar == '"') {
                tokens.add(string());
                continue;
            }
            
            // 识别操作符和分隔符
            switch (currentChar) {
                case '+':
                    tokens.add(new Token(Token.Type.PLUS, "+", line));
                    advance();
                    break;
                case '-':
                    tokens.add(new Token(Token.Type.MINU, "-", line));
                    advance();
                    break;
                case '*':
                    tokens.add(new Token(Token.Type.MULT, "*", line));
                    advance();
                    break;
                case '/':
                    tokens.add(new Token(Token.Type.DIV, "/", line));
                    advance();
                    break;
                case '%':
                    tokens.add(new Token(Token.Type.MOD, "%", line));
                    advance();
                    break;
                case '<':
                    if (peekNext() == '=') {
                        advance();
                        tokens.add(new Token(Token.Type.LEQ, "<=", line));
                    } else {
                        tokens.add(new Token(Token.Type.LSS, "<", line));
                    }
                    advance();
                    break;
                case '>':
                    if (peekNext() == '=') {
                        advance();
                        tokens.add(new Token(Token.Type.GEQ, ">=", line));
                    } else {
                        tokens.add(new Token(Token.Type.GRE, ">", line));
                    }
                    advance();
                    break;
                case '=':
                    if (peekNext() == '=') {
                        advance();
                        tokens.add(new Token(Token.Type.EQL, "==", line));
                    } else {
                        tokens.add(new Token(Token.Type.ASSIGN, "=", line));
                    }
                    advance();
                    break;
                case '!':
                    if (peekNext() == '=') {
                        advance();
                        tokens.add(new Token(Token.Type.NEQ, "!=", line));
                    } else {
                        tokens.add(new Token(Token.Type.NOT, "!", line));
                    }
                    advance();
                    break;
                case '&':
                    if (peekNext() == '&') {
                        advance();
                        tokens.add(new Token(Token.Type.AND, "&&", line));
                        advance();
                    } else {
                        // 错误：单个&不是有效的操作符
                        SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
                        // 添加一个AND token作为替代，避免解析器陷入无限循环
                        tokens.add(new Token(Token.Type.AND, "&&", line));
                        advance();
                    }
                    break;
                case '|':
                    if (peekNext() == '|') {
                        advance();
                        tokens.add(new Token(Token.Type.OR, "||", line));
                        advance();
                    } else {
                        // 错误：单个|不是有效的操作符
                        SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
                        // 添加一个OR token作为替代，避免解析器陷入无限循环
                        tokens.add(new Token(Token.Type.OR, "||", line));
                        advance();
                    }
                    break;
                case ';':
                    tokens.add(new Token(Token.Type.SEMICN, ";", line));
                    advance();
                    break;
                case ',':
                    tokens.add(new Token(Token.Type.COMMA, ",", line));
                    advance();
                    break;
                case '(':
                    tokens.add(new Token(Token.Type.LPARENT, "(", line));
                    advance();
                    break;
                case ')':
                    tokens.add(new Token(Token.Type.RPARENT, ")", line));
                    advance();
                    break;
                case '[':
                    tokens.add(new Token(Token.Type.LBRACK, "[", line));
                    advance();
                    break;
                case ']':
                    tokens.add(new Token(Token.Type.RBRACK, "]", line));
                    advance();
                    break;
                case '{':
                    tokens.add(new Token(Token.Type.LBRACE, "{", line));
                    advance();
                    break;
                case '}':
                    tokens.add(new Token(Token.Type.RBRACE, "}", line));
                    advance();
                    break;
                default:
                    // 遇到不识别的字符
                    SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
                    advance();
                    break;
            }
        }
        
        return tokens;
    }
    
    /**
     * 向前移动一个字符
     */
    private void advance() {
        position++;
        if (position < source.length()) {
            currentChar = source.charAt(position);
        }
    }
    
    /**
     * 查看下一个字符，但不移动位置
     * @return 下一个字符，如果已到末尾则返回'\0'
     */
    private char peekNext() {
        if (position + 1 < source.length()) {
            return source.charAt(position + 1);
        }
        return '\0';
    }
    
    /**
     * 识别标识符或关键字
     * @return 标识符或关键字的词法单元
     */
    private Token identifier() {
        StringBuilder sb = new StringBuilder();
        
        // 收集标识符的所有字符
        while (position < source.length() && 
              (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append(currentChar);
            advance();
        }
        
        String lexeme = sb.toString();
        
        // 检查是否是关键字
        if (KEYWORDS.containsKey(lexeme)) {
            return new Token(KEYWORDS.get(lexeme), lexeme, line);
        }
        
        // 不是关键字，则是标识符
        return new Token(Token.Type.IDENFR, lexeme, line);
    }
    
    /**
     * 识别数字常量
     * @return 数字常量的词法单元
     */
    private Token number() {
        StringBuilder sb = new StringBuilder();
        
        // 收集数字的所有字符
        while (position < source.length() && Character.isDigit(currentChar)) {
            sb.append(currentChar);
            advance();
        }
        
        return new Token(Token.Type.INTCON, sb.toString(), line);
    }
    
    /**
     * 识别字符常量
     * @return 字符常量的词法单元
     */
    private Token character() {
        advance(); // 跳过开始的单引号
        
        // 检查字符是否有效
        if (position >= source.length() || currentChar == '\'' || currentChar == '\n') {
            // 字符常量为空或跨行
            SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
            return new Token(Token.Type.CHRCON, "", line);
        }
        
        StringBuilder value = new StringBuilder();
        
        // 处理转义字符
        if (currentChar == '\\') {
            value.append(currentChar);
            advance();
            
            // 确保还有下一个字符
            if (position < source.length() && currentChar != '\n') {
                value.append(currentChar);
                advance();
            } else {
                // 转义序列不完整
                SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
                return new Token(Token.Type.CHRCON, value.toString(), line);
            }
        } else {
            // 普通字符
            value.append(currentChar);
            advance();
        }
        
        // 字符常量必须以单引号结束
        if (currentChar != '\'') {
            // 缺少右单引号
            SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
            return new Token(Token.Type.CHRCON, value.toString(), line);
        }
        
        advance(); // 跳过结束的单引号
        return new Token(Token.Type.CHRCON, value.toString(), line);
    }
    
    /**
     * 识别字符串常量
     * @return 字符串常量的词法单元
     */
    private Token string() {
        advance(); // 跳过开始的双引号
        StringBuilder sb = new StringBuilder();
        
        // 收集字符串的所有字符
        while (position < source.length() && currentChar != '"' && currentChar != '\n') {
            sb.append(currentChar);
            advance();
        }
        
        // 字符串常量必须以双引号结束
        if (currentChar != '"') {
            // 缺少右双引号或字符串跨行
            SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
            return new Token(Token.Type.STRCON, sb.toString(), line);
        }
        
        advance(); // 跳过结束的双引号
        return new Token(Token.Type.STRCON, sb.toString(), line);
    }
    
    /**
     * 跳过行注释
     */
    private void skipLineComment() {
        // 跳过//
        advance();
        advance();
        
        // 跳过注释内容直到行尾
        while (position < source.length() && currentChar != '\n') {
            advance();
        }
    }
    
    /**
     * 跳过块注释
     */
    private void skipBlockComment() {
        // 跳过/*
        advance();
        advance();
        
        // 跳过注释内容直到*/
        while (position < source.length()) {
            if (currentChar == '*' && peekNext() == '/') {
                advance(); // 跳过*
                advance(); // 跳过/
                return;
            }
            
            if (currentChar == '\n') {
                line++;
            }
            
            advance();
        }
        
        // 如果到达这里，意味着块注释没有正确关闭
        SimpleErrorHandler.addError(line, "a"); // 错误类型a - 非法符号
    }
} 