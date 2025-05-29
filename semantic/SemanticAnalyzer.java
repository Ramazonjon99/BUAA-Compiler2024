package semantic;

import lexer.Token; // 假设Token类在lexer包下
import utils.SimpleErrorHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Collections;

public class SemanticAnalyzer {
    private List<Token> tokens; // 词法分析器产生的Token流
    private int currentTokenIndex; // 当前处理的Token的索引
    private Token currentToken; // 当前处理的Token

    private SymbolTable currentScope; // 当前作用域的符号表
    private int nextScopeId = 1;      // 用于分配新的作用域ID，全局为1
    private Stack<SymbolTable> scopeStack; // 作用域栈

    // 用于存储最终要输出到 symbol.txt 的符号信息，按作用域和声明顺序
    private List<SymbolTable> allScopes; // 保存所有作用域的符号表，用于最后统一输出

    // 添加循环跟踪变量
    private int loopDepth = 0; // 用于跟踪循环嵌套深度
    
    // 当前函数的返回类型，用于检查return语句
    private Type currentFunctionReturnType = Type.VOID;
    
    // 函数是否已经见过返回语句
    private boolean hasReturnStatement = false;
    
    // 为了处理语法错误情况，添加强制报告g错误的标记
    private boolean forceReportGError = false;
    
    // 静态变量，用于递归深度控制
    private static final int MAX_RECURSION_DEPTH = 100;
    private int recursionDepth = 0;

    // 当前函数名称
    private String currentFunctionName = "";
    
    // 是否在条件分支（如if语句）内部
    private boolean insideConditionalBranch = false;

    // 进入循环
    private void enterLoop() {
        loopDepth++;
    }
    
    // 退出循环
    private void exitLoop() {
        loopDepth--;
    }

    private static class PrintableSymbolInfo {
        int originalScopeId;
        String name;
        Type type;
        int lineNumber; // To preserve order within the original scope

        PrintableSymbolInfo(Symbol symbol) {
            this.originalScopeId = symbol.getScopeId();
            this.name = symbol.getName();
            this.type = symbol.getType();
            this.lineNumber = symbol.getLineNumber();
        }

        String getTypeString() {
            return type.toString();
        }
    }

    public SemanticAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        if (tokens != null && !tokens.isEmpty()) {
            this.currentToken = tokens.get(0);
        }
        this.scopeStack = new Stack<>();
        this.allScopes = new ArrayList<>();
        enterScope(); // 进入全局作用域
    }

    private void advance() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        } else {
            currentToken = null; // 表示Token流结束
        }
    }

    private Token peek(int offset) {
        int peekIndex = currentTokenIndex + offset;
        if (peekIndex >= 0 && peekIndex < tokens.size()) {
            return tokens.get(peekIndex);
        }
        return null;
    }

    private void enterScope() {
        SymbolTable newScope = new SymbolTable(currentScope, nextScopeId++);
        currentScope = newScope;
        scopeStack.push(newScope);
        allScopes.add(newScope); // 保存起来，为了最后的有序输出
        // System.out.println("Entering scope: " + currentScope.getScopeId());
    }

    private void exitScope() {
        if (!scopeStack.isEmpty()) {
            // System.out.println("Exiting scope: " + currentScope.getScopeId());
            scopeStack.pop();
            currentScope = scopeStack.isEmpty() ? null : scopeStack.peek();
        } else {
            System.err.println("Error: Attempted to exit global scope or empty scope stack.");
        }
    }

    // 主要的语义分析方法，将遍历Token流 (或者将来的AST)
    public void analyze() {
        // 这是一个非常简化的遍历，实际需要根据语法规则递归下降或遍历AST
        // 这里我们假设有一个方法来驱动整个分析过程，比如 parseCompUnit()
        parseCompUnit();
        
        // 分析完成后，写入符号表文件
        writeSymbolTableToFile("symbol.txt");
    }

    // ----------------------------------------------------------------------
    // 以下是根据文法结构模拟的解析方法 (需要根据Parser的结构来调整)
    // 这些方法需要被详细实现，以处理声明、定义并填充符号表
    // ----------------------------------------------------------------------

    private void parseCompUnit() {
        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        while (currentToken != null && isStartOfDecl()) {
            parseDecl();
        }
        while (currentToken != null && isStartOfFuncDef()) {
            parseFuncDef();
        }
        if (currentToken != null && currentToken.getType() == Token.Type.INTTK && 
            peek(1) != null && peek(1).getType() == Token.Type.MAINTK) {
            parseMainFuncDef();
            
            // 检查main函数之后是否还有额外的声明或定义
            // 根据文法，main函数应该是最后的元素
            // 但是如果有额外的声明，我们应该继续解析并检查语义错误
            while (currentToken != null) {
                if (isStartOfDecl()) {
                    parseDecl();
                } else if (isStartOfFuncDef() || 
                          (currentToken.getType() == Token.Type.INTTK && 
                           peek(1) != null && peek(1).getType() == Token.Type.MAINTK)) {
                    // 额外的函数定义或main函数定义，这是语法错误
                    // 但我们继续解析以检测语义错误
                    if (isStartOfFuncDef()) {
                        parseFuncDef();
                    } else {
                        parseMainFuncDef();
                    }
                } else {
                    // 未识别的元素，尝试跳过
                    advance();
                }
            }
        } else {
            // 语法错误或不完整的CompUnit (暂不处理)
        }
    }

    private boolean isStartOfDecl() {
        if (currentToken == null) return false;
        Token.Type type = currentToken.getType();
        // const BType ... | BType ... (not followed by main or Ident LPARENT for func)
        if (type == Token.Type.CONSTTK) return true;
        if (type == Token.Type.INTTK || type == Token.Type.CHARTK) {
            Token next = peek(1);
            Token nextNext = peek(2);
            if (next != null && next.getType() == Token.Type.MAINTK) return false; // MainFuncDef
            if (next != null && next.getType() == Token.Type.IDENFR && 
                nextNext != null && nextNext.getType() == Token.Type.LPARENT) return false; // FuncDef
            return true; // VarDecl
        }
        return false;
    }

    private void parseDecl() {
        // Decl -> ConstDecl | VarDecl
        if (currentToken.getType() == Token.Type.CONSTTK) {
            parseConstDecl();
        } else if (currentToken.getType() == Token.Type.INTTK || currentToken.getType() == Token.Type.CHARTK) {
            parseVarDecl();
        } else {
            // 语法错误 (暂不处理)
            // advance(); // 跳过未知Token以尝试恢复
        }
    }

    private void parseConstDecl() {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        // System.out.println("Parsing ConstDecl at line: " + (currentToken != null ? currentToken.getLineNumber() : "EOF"));
        assertAndAdvance(Token.Type.CONSTTK);
        Type btype = parseBType();
        boolean isFirstDef = true;
        do {
            if (!isFirstDef) {
                assertAndAdvance(Token.Type.COMMA);
            }
            parseConstDef(btype);
            isFirstDef = false;
        } while (currentToken != null && currentToken.getType() == Token.Type.COMMA);
        assertAndAdvance(Token.Type.SEMICN);
    }

    private void parseVarDecl() {
        // VarDecl -> BType VarDef { ',' VarDef } ';'
        // System.out.println("Parsing VarDecl at line: " + (currentToken != null ? currentToken.getLineNumber() : "EOF"));
        Type btype = parseBType();
        boolean isFirstDef = true;
        do {
            if (!isFirstDef) {
                assertAndAdvance(Token.Type.COMMA);
            }
            parseVarDef(btype);
            isFirstDef = false;
        } while (currentToken != null && currentToken.getType() == Token.Type.COMMA);
        assertAndAdvance(Token.Type.SEMICN);
    }

    private Type parseBType() {
        // BType -> 'int' | 'char'
        if (currentToken.getType() == Token.Type.INTTK) {
            advance(); // 'int'
            return Type.INT; // 基本类型是INT
        } else if (currentToken.getType() == Token.Type.CHARTK) {
            advance(); // 'char'
            return Type.CHAR; // 基本类型是CHAR
        } else {
            // 语法错误 (暂不处理)
            // advance();
            return Type.UNKNOWN; // 或抛出异常
        }
    }

    private void parseConstDef(Type baseBType) {
        // ConstDef -> Ident [ '[' ConstExp ']' ] '=' ConstInitVal
        Token identToken = currentToken;
        assertAndAdvance(Token.Type.IDENFR);
        String name = identToken.getValue();
        int lineNumber = identToken.getLineNumber();
        boolean isArray = false;
        // int arrayDim = 0; // 简单处理，只区分是否数组

        // 检查常量名是否在当前作用域中重定义 (b类型错误)
        Symbol existingSymbol = currentScope.lookupCurrentScope(name);
        if (existingSymbol != null) {
            // 错误b：常量名重定义
            SimpleErrorHandler.addError(lineNumber, "b");
        }

        if (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
            isArray = true;
            advance(); // '['
            parseConstExp(); // ConstExp (需要计算其值以确定维度，暂时跳过其内容)
            assertAndAdvance(Token.Type.RBRACK); // ']'
            // 如果需要多维，这里要循环处理
        }
        assertAndAdvance(Token.Type.ASSIGN); // '='
        parseConstInitVal(); // ConstInitVal (需要计算其值，并检查类型，暂时跳过)

        Type symbolType;
        if (isArray) {
            symbolType = (baseBType == Type.INT) ? Type.CONST_INT_ARRAY : Type.CONST_CHAR_ARRAY;
        } else {
            symbolType = (baseBType == Type.INT) ? Type.CONST_INT : Type.CONST_CHAR;
        }
        VariableSymbol symbol = new VariableSymbol(name, symbolType, currentScope.getScopeId(), lineNumber, true);
        currentScope.addSymbol(symbol);
        // System.out.println("Added const: " + symbol);
    }

    private void parseVarDef(Type baseBType) {
        // VarDef -> Ident [ '[' ConstExp ']' ] [ '=' InitVal ]
        Token identToken = currentToken;
        assertAndAdvance(Token.Type.IDENFR);
        String name = identToken.getValue();
        int lineNumber = identToken.getLineNumber();
        boolean isArray = false;

        // 检查变量名是否在当前作用域中重定义 (b类型错误)
        Symbol existingSymbol = currentScope.lookupCurrentScope(name);
        if (existingSymbol != null) {
            // 错误b：变量名重定义
            SimpleErrorHandler.addError(lineNumber, "b");
        }

        if (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
            isArray = true;
            advance(); // '['
            parseConstExp(); // Array size
            assertAndAdvance(Token.Type.RBRACK); // ']'
        }

        Type symbolType;
        if (isArray) {
            symbolType = (baseBType == Type.INT) ? Type.INT_ARRAY : Type.CHAR_ARRAY;
        } else {
            symbolType = (baseBType == Type.INT) ? Type.INT : Type.CHAR;
        }
        VariableSymbol symbol = new VariableSymbol(name, symbolType, currentScope.getScopeId(), lineNumber, false);
        currentScope.addSymbol(symbol);
        // System.out.println("Added var: " + symbol);

        if (currentToken != null && currentToken.getType() == Token.Type.ASSIGN) {
            advance(); // '='
            parseInitVal(); // Parse the initializer
        }
    }

    private void parseConstInitVal() {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        if (currentToken != null && currentToken.getType() == Token.Type.LBRACE) {
            advance(); // Consume '{'
            if (currentToken != null && currentToken.getType() == Token.Type.RBRACE) {
                // Empty initializer: {}
                advance(); // Consume '}'
                return;
            }
            
            // Non-empty initializer list for a constant
            parseConstInitVal(); // Parse the first ConstInitVal

            while (currentToken != null && currentToken.getType() == Token.Type.COMMA) {
                advance(); // Consume ','
                parseConstInitVal(); // Parse subsequent ConstInitVal
            }
            
            assertAndAdvance(Token.Type.RBRACE); // Consume '}'
        } else {
            // Single ConstExp initializer
            parseConstExp();
        }
    }
    
    private void parseInitVal() {
        // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        if (currentToken != null && currentToken.getType() == Token.Type.LBRACE) {
            advance(); // Consume '{'
            if (currentToken != null && currentToken.getType() == Token.Type.RBRACE) {
                // Empty initializer: {}
                advance(); // Consume '}'
                return;
            }
            
            // Non-empty initializer list for a variable
            parseInitVal(); // Parse the first InitVal

            while (currentToken != null && currentToken.getType() == Token.Type.COMMA) {
                advance(); // Consume ','
                parseInitVal(); // Parse subsequent InitVal
            }
            
            assertAndAdvance(Token.Type.RBRACE); // Consume '}'
        } else {
            // Single Exp initializer
            parseExp();
        }
    }

    private void parseConstExp() {
        // ConstExp -> AddExp
        parseAddExp(); 
        // 常量表达式不应该修改符号表，只是计算值
    }
    
    private void parseExp() {
        // Exp -> AddExp
        try {
            increaseRecursionDepth("parseExp");
            
            // 保存当前位置，以检测parseAddExp是否有进展
            int startPos = currentTokenIndex;
            
        parseAddExp();
            
            // 如果没有前进，说明可能陷入了循环
            if (currentTokenIndex == startPos && currentToken != null) {
                System.err.println("Warning: No progress in parseAddExp. Skipping to avoid infinite loop.");
                advance(); // 至少前进一个token
            }
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Maximum recursion depth exceeded")) {
                System.err.println("Recovering from excessive recursion in parseExp");
                // 尝试跳过到表达式边界
                while (currentToken != null && 
                      currentToken.getType() != Token.Type.SEMICN && 
                      currentToken.getType() != Token.Type.RPARENT &&
                      currentToken.getType() != Token.Type.RBRACK &&
                      currentToken.getType() != Token.Type.COMMA) {
                    advance();
                }
            } else {
                throw e; // 重新抛出其他异常
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
    
    private void parseAddExp() {
        // AddExp -> MulExp { ('+' | '−') MulExp }
        try {
            increaseRecursionDepth("parseAddExp");
            
            // 保存当前位置，以检测parseMulExp是否有进展
            int startPos = currentTokenIndex;
            
        parseMulExp();
            
            // 如果没有前进，说明可能陷入了循环
            if (currentTokenIndex == startPos && currentToken != null) {
                System.err.println("Warning: No progress in parseMulExp. Skipping to avoid infinite loop.");
                advance(); // 至少前进一个token
                return; // 提前返回，不处理后续的加减操作
            }
            
            // 防止无限循环：添加最大操作符次数限制
            int maxOperations = 50;
            int opCount = 0;
            
        while (currentToken != null && 
               (currentToken.getType() == Token.Type.PLUS || 
                    currentToken.getType() == Token.Type.MINU) &&
                   opCount < maxOperations) {
                opCount++;
            advance(); // Skip '+' or '-'
                
                // 保存当前位置以检测后续MulExp是否有进展
                startPos = currentTokenIndex;
                
            parseMulExp();
                
                // 检查是否有进展
                if (currentTokenIndex == startPos && currentToken != null) {
                    System.err.println("Warning: No progress in subsequent parseMulExp. Skipping to avoid infinite loop.");
                    advance(); // 至少前进一个token
                    break; // 停止处理后续的加减操作
                }
            }
            
            // 如果达到操作符最大次数限制，尝试恢复
            if (opCount >= maxOperations) {
                System.err.println("Warning: Exceeded maximum operations in parseAddExp. Possible infinite loop.");
            }
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Maximum recursion depth exceeded")) {
                System.err.println("Recovering from excessive recursion in parseAddExp");
                // 尝试跳过到表达式边界
                while (currentToken != null && 
                      currentToken.getType() != Token.Type.SEMICN && 
                      currentToken.getType() != Token.Type.RPARENT &&
                      currentToken.getType() != Token.Type.RBRACK &&
                      currentToken.getType() != Token.Type.COMMA) {
                    advance();
                }
            } else {
                throw e; // 重新抛出其他异常
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
    
    private void parseMulExp() {
        // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
        try {
            increaseRecursionDepth("parseMulExp");
            
            // 保存当前位置，以检测parseUnaryExp是否有进展
            int startPos = currentTokenIndex;
            
        parseUnaryExp();
            
            // 如果没有前进，说明可能陷入了循环
            if (currentTokenIndex == startPos && currentToken != null) {
                System.err.println("Warning: No progress in parseUnaryExp. Skipping to avoid infinite loop.");
                advance(); // 至少前进一个token
                return; // 提前返回，不处理后续的乘除操作
            }
            
            // 防止无限循环：添加最大操作符次数限制
            int maxOperations = 50;
            int opCount = 0;
            
        while (currentToken != null && 
               (currentToken.getType() == Token.Type.MULT || 
                currentToken.getType() == Token.Type.DIV ||
                    currentToken.getType() == Token.Type.MOD) &&
                   opCount < maxOperations) {
                opCount++;
            advance(); // Skip '*', '/' or '%'
                
                // 保存当前位置以检测后续UnaryExp是否有进展
                startPos = currentTokenIndex;
                
            parseUnaryExp();
                
                // 检查是否有进展
                if (currentTokenIndex == startPos && currentToken != null) {
                    System.err.println("Warning: No progress in subsequent parseUnaryExp. Skipping to avoid infinite loop.");
                    advance(); // 至少前进一个token
                    break; // 停止处理后续的乘除操作
                }
            }
            
            // 如果达到操作符最大次数限制，尝试恢复
            if (opCount >= maxOperations) {
                System.err.println("Warning: Exceeded maximum operations in parseMulExp. Possible infinite loop.");
            }
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Maximum recursion depth exceeded")) {
                System.err.println("Recovering from excessive recursion in parseMulExp");
                // 尝试跳过到表达式边界
                while (currentToken != null && 
                      currentToken.getType() != Token.Type.SEMICN && 
                      currentToken.getType() != Token.Type.RPARENT &&
                      currentToken.getType() != Token.Type.RBRACK &&
                      currentToken.getType() != Token.Type.COMMA) {
                    advance();
                }
            } else {
                throw e; // 重新抛出其他异常
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
    
    private void parseUnaryExp() {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        try {
            increaseRecursionDepth("parseUnaryExp");
            
        if (currentToken != null) {
            if (currentToken.getType() == Token.Type.IDENFR) {
                Token identToken = currentToken;
                    String identName = identToken.getValue();
                    int identLine = identToken.getLineNumber();
                advance(); // Move past identifier
                
                if (currentToken != null && currentToken.getType() == Token.Type.LPARENT) {
                    // Function call
                    advance(); // Skip '('
                        
                        // 保存当前括号位置，用于后续判断右括号是否缺失
                        int parenPosition = currentTokenIndex - 1;
                        boolean rightParenFound = false;
                        
                        // 向前查找是否有匹配的右括号
                        // 如果没有找到匹配的右括号，说明函数调用语法不完整，是j类型错误
                        // 此时应该跳过参数检查以避免额外的d错误
                        for (int i = currentTokenIndex; i < tokens.size(); i++) {
                            Token token = tokens.get(i);
                            if (token.getType() == Token.Type.RPARENT) {
                                rightParenFound = true;
                                break;
                            } else if (token.getType() == Token.Type.SEMICN || 
                                      token.getType() == Token.Type.RBRACE) {
                                // 提前遇到分号或右花括号，说明缺少右括号
                                break;
                            }
                        }
                        
                        // 只有找到了匹配的右括号才进行参数和函数检查
                        if (rightParenFound) {
                            // 查找函数符号
                            Symbol symbol = currentScope.lookup(identName);
                            
                            // 错误c: 未定义的名字 - 函数未定义
                            if (symbol == null) {
                                SimpleErrorHandler.addError(identLine, "c");
                            } else if (!(symbol instanceof FunctionSymbol)) {
                                // 标识符存在但不是函数
                                SimpleErrorHandler.addError(identLine, "c");
                            } else {
                                // 函数已定义，检查参数
                                FunctionSymbol funcSymbol = (FunctionSymbol)symbol;
                                List<VariableSymbol> expectedParams = funcSymbol.getParameters();
                                List<Type> actualParamTypes = new ArrayList<>();
                                
                                // 收集实际参数类型 - 添加深度限制和错误恢复
                    if (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
                                    try {
                                        collectFuncRParamTypes(actualParamTypes);
                                    } catch (Exception e) {
                                        System.err.println("Error collecting function parameters: " + e.getMessage());
                                        // 尝试恢复到右括号
                                        while (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
                                            advance();
                                        }
                                    }
                                }
                                
                                // 错误d: 函数参数个数不匹配
                                if (expectedParams.size() != actualParamTypes.size()) {
                                    SimpleErrorHandler.addError(identLine, "d");
                                } else {
                                    // 错误e: 函数参数类型不匹配
                                    for (int i = 0; i < expectedParams.size(); i++) {
                                        VariableSymbol expectedParam = expectedParams.get(i);
                                        Type actualType = actualParamTypes.get(i);
                                        
                                        // 类型不匹配的情况：
                                        // 1. 传递数组给变量
                                        // 2. 传递变量给数组
                                        // 3. 传递char型数组给int型数组
                                        // 4. 传递int型数组给char型数组
                                        boolean typeError = false;
                                        
                                        if (expectedParam.isArray() && !isArrayType(actualType)) {
                                            // 传递变量给数组
                                            typeError = true;
                                        } else if (!expectedParam.isArray() && isArrayType(actualType)) {
                                            // 传递数组给变量
                                            typeError = true;
                                        } else if (expectedParam.isArray() && isArrayType(actualType)) {
                                            // 数组类型检查
                                            if ((expectedParam.getType() == Type.INT_ARRAY && actualType == Type.CHAR_ARRAY) ||
                                                (expectedParam.getType() == Type.CHAR_ARRAY && actualType == Type.INT_ARRAY)) {
                                                // 数组类型不匹配
                                                typeError = true;
                                            }
                                        } else {
                                            // 基本类型检查
                                            if ((expectedParam.getType() == Type.INT && actualType == Type.CHAR) ||
                                                (expectedParam.getType() == Type.CHAR && actualType == Type.INT)) {
                                                // 基本类型不匹配
                                                typeError = true;
                                            }
                                        }
                                        
                                        if (typeError) {
                                            SimpleErrorHandler.addError(identLine, "e");
                                            break; // 一旦发现类型不匹配，就不再检查其他参数
                                        }
                                    }
                                }
                            }
                        } else {
                            // 如果没有找到匹配的右括号，跳过所有的参数检查，尝试跳到分号
                            // System.err.println("Missing right parenthesis in function call");
                            // 参数解析阶段跳过，直接继续到函数结束
                            while (currentToken != null && 
                                  currentToken.getType() != Token.Type.SEMICN && 
                                  currentToken.getType() != Token.Type.RBRACE) {
                                advance();
                            }
                            // 返回，不继续进行函数参数解析
                            return;
                        }
                        
                        // 继续原来的代码逻辑 - 解析函数参数
                        if (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
                            try {
                        parseFuncRParams();
                            } catch (Exception e) {
                                System.err.println("Error parsing function parameters: " + e.getMessage());
                                // 尝试恢复到右括号
                                while (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
                                    advance();
                                }
                            }
                        }
                        
                        // 检查是否匹配到了右括号，如果没有匹配到，不尝试跳过（由调用者处理）
                        if (currentToken != null && currentToken.getType() == Token.Type.RPARENT) {
                            advance(); // Skip ')'
                        }
                } else {
                    // It's a variable, not a function call
                        // 错误c: 未定义的名字 - 检查变量是否定义
                        Symbol symbol = currentScope.lookup(identName);
                        if (symbol == null) {
                            SimpleErrorHandler.addError(identLine, "c");
                        }
                        
                        // 如果有后续的数组访问表达式，处理它们
                        while (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
                            try {
                                // 保存当前位置以便检测parseExp是否有进展
                                int startPos = currentTokenIndex;
                                
                                advance(); // Skip '['
                                parseExp(); // Parse the array index
                                
                                // 确保parseExp有进展
                                if (currentTokenIndex == startPos) {
                                    System.err.println("Warning: No progress in array index expression. Skipping to avoid infinite loop.");
                                    advance(); // 至少前进一个token
                                }
                                
                                assertAndAdvance(Token.Type.RBRACK); // Skip ']'
                            } catch (Exception e) {
                                System.err.println("Error parsing array access: " + e.getMessage());
                                // 尝试恢复到右中括号或表达式边界
                                while (currentToken != null && 
                                      currentToken.getType() != Token.Type.RBRACK &&
                                      currentToken.getType() != Token.Type.SEMICN &&
                                      currentToken.getType() != Token.Type.RPARENT) {
                                    advance();
                                }
                                if (currentToken != null && currentToken.getType() == Token.Type.RBRACK) {
                                    advance(); // Skip ']'
                                }
                                break; // 停止处理后续的数组访问
                            }
                        }
                }
            } else if (currentToken.getType() == Token.Type.PLUS || 
                      currentToken.getType() == Token.Type.MINU || 
                      currentToken.getType() == Token.Type.NOT) {
                // UnaryOp UnaryExp
                advance(); // Skip unary operator
                    
                    // 保存当前位置以便检测后续parseUnaryExp是否有进展
                    int startPos = currentTokenIndex;
                    
                    try {
                parseUnaryExp();
                        
                        // 确保parseUnaryExp有进展
                        if (currentTokenIndex == startPos && currentToken != null) {
                            System.err.println("Warning: No progress in unary expression. Skipping to avoid infinite loop.");
                            advance(); // 至少前进一个token
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing unary expression: " + e.getMessage());
                        // 尝试恢复到表达式边界
                        while (currentToken != null && 
                              currentToken.getType() != Token.Type.SEMICN && 
                              currentToken.getType() != Token.Type.RPARENT &&
                              currentToken.getType() != Token.Type.RBRACK &&
                              currentToken.getType() != Token.Type.COMMA) {
                            advance();
                        }
                    }
            } else {
                // PrimaryExp
                    try {
                        int startPos = currentTokenIndex;
                        
                parsePrimaryExp();
                        
                        // 确保parsePrimaryExp有进展
                        if (currentTokenIndex == startPos && currentToken != null) {
//                            System.err.println("Warning: No progress in primary expression. Skipping to avoid infinite loop.");
                            advance(); // 至少前进一个token
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing primary expression: " + e.getMessage());
                        // 尝试恢复到表达式边界
                        while (currentToken != null && 
                              currentToken.getType() != Token.Type.SEMICN && 
                              currentToken.getType() != Token.Type.RPARENT &&
                              currentToken.getType() != Token.Type.RBRACK &&
                              currentToken.getType() != Token.Type.COMMA) {
                            advance();
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().equals("Maximum recursion depth exceeded")) {
                System.err.println("Recovering from excessive recursion in parseUnaryExp");
                // 尝试跳过到表达式边界
                while (currentToken != null && 
                      currentToken.getType() != Token.Type.SEMICN && 
                      currentToken.getType() != Token.Type.RPARENT &&
                      currentToken.getType() != Token.Type.RBRACK &&
                      currentToken.getType() != Token.Type.COMMA) {
                    advance();
                }
            } else {
                throw e; // 重新抛出其他异常
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
    
    // 辅助方法：收集函数调用中的实际参数类型
    private void collectFuncRParamTypes(List<Type> paramTypes) {
        try {
            increaseRecursionDepth("collectFuncRParamTypes");
            
            // 收集第一个参数的类型
            int startPos = currentTokenIndex;
            Type paramType = parseExpType();
            
            // 检查是否有进展
            if (currentTokenIndex == startPos && currentToken != null) {
                System.err.println("Warning: No progress in parsing expression type. Skipping to avoid infinite loop.");
                advance(); // 至少前进一个token
                paramType = Type.UNKNOWN; // 无法确定类型，设为未知
            }
            
            paramTypes.add(paramType);
            
            // 收集后续参数的类型
            int paramCount = 0;
            final int MAX_PARAMS = 50; // 防止无限循环
            
            while (currentToken != null && currentToken.getType() == Token.Type.COMMA && paramCount < MAX_PARAMS) {
                paramCount++;
                advance(); // Skip ','
                
                startPos = currentTokenIndex;
                paramType = parseExpType();
                
                // 检查是否有进展
                if (currentTokenIndex == startPos && currentToken != null) {
                    System.err.println("Warning: No progress in parsing subsequent expression type. Skipping to avoid infinite loop.");
                    advance(); // 至少前进一个token
                    paramType = Type.UNKNOWN; // 无法确定类型，设为未知
                }
                
                paramTypes.add(paramType);
            }
            
            if (paramCount >= MAX_PARAMS) {
                System.err.println("Warning: Maximum parameter count exceeded. Possible infinite loop.");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().equals("Maximum recursion depth exceeded")) {
                System.err.println("Recovering from excessive recursion in collectFuncRParamTypes");
                // 尝试跳过至右括号或逗号
                while (currentToken != null && 
                      currentToken.getType() != Token.Type.RPARENT && 
                      currentToken.getType() != Token.Type.COMMA) {
                    advance();
                }
            } else {
                throw e; // 重新抛出其他异常
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
    
    // 辅助方法：解析表达式并返回其类型
    private Type parseExpType() {
        // 标记当前位置以便后面回溯
        int savedPosition = currentTokenIndex;
        Token savedToken = currentToken;
        
        // 这里需要记录当前表达式是否是数组访问
        boolean isArray = false;
        Type baseType = Type.UNKNOWN;
        
        // 添加最大递归深度检测，防止无限递归
        int maxDepth = 100;
        int currentDepth = 0;
        
        try {
            // 如果是标识符开头，可能是变量引用或函数调用
            if (currentToken != null && currentToken.getType() == Token.Type.IDENFR) {
                String identName = currentToken.getValue();
                Symbol symbol = currentScope.lookup(identName);
                
                if (symbol != null) {
                    advance(); // 跳过标识符
                    
                    // 检查是否是函数调用
                    if (currentToken != null && currentToken.getType() == Token.Type.LPARENT) {
                        // 这是函数调用
                        if (symbol instanceof FunctionSymbol) {
                            FunctionSymbol funcSymbol = (FunctionSymbol)symbol;
                            Type returnType = funcSymbol.getReturnType();
                            
                            // 函数返回类型
                            baseType = returnType;
                            isArray = false; // 函数调用结果不是数组
                        }
                        
                        // 跳过函数调用的其余部分
                        int parenCount = 1;
                        advance(); // 跳过左括号
                        while (currentToken != null && parenCount > 0 && currentDepth++ < maxDepth) {
                            if (currentToken.getType() == Token.Type.LPARENT) parenCount++;
                            else if (currentToken.getType() == Token.Type.RPARENT) parenCount--;
                            advance();
                            
                            // 防止无限循环
                            if (currentDepth >= maxDepth) {
                                System.err.println("Warning: Max depth exceeded in parseExpType. Possible syntax error.");
                                break;
                            }
                        }
                    } else {
                        // 这是变量引用
                        
                        // 获取基本类型
                        if (symbol.getType() == Type.INT || symbol.getType() == Type.INT_ARRAY) {
                            baseType = Type.INT;
                        } else if (symbol.getType() == Type.CHAR || symbol.getType() == Type.CHAR_ARRAY) {
                            baseType = Type.CHAR;
                        }
                        
                        // 检查是否是数组访问
                        if (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
                            // 数组访问，意味着实际传递的是数组元素，不是整个数组
                            isArray = false;
                            
                            // 跳过数组访问部分
                            advance(); // 跳过左中括号
                            int bracketCount = 1;
                            currentDepth = 0;
                            while (currentToken != null && bracketCount > 0 && currentDepth++ < maxDepth) {
                                if (currentToken.getType() == Token.Type.LBRACK) bracketCount++;
                                else if (currentToken.getType() == Token.Type.RBRACK) bracketCount--;
                                advance();
                                
                                // 防止无限循环
                                if (currentDepth >= maxDepth) {
                                    System.err.println("Warning: Max depth exceeded in array access. Possible syntax error.");
                                    break;
                                }
                            }
                        } else {
                            // 不是数组访问，检查符号本身是否为数组
                            isArray = symbol.isArray();
                        }
                    }
                } else {
                    // 符号未定义，跳过
                    advance();
                }
            } else if (currentToken != null && currentToken.getType() == Token.Type.INTCON) {
                // 整数字面量
                baseType = Type.INT;
                isArray = false;
                advance();
            } else if (currentToken != null && currentToken.getType() == Token.Type.CHRCON) {
                // 字符字面量
                baseType = Type.CHAR;
                isArray = false;
                advance();
            } else {
                // 其他表达式类型，例如括号表达式、一元表达式等
                // 简单处理，假设所有复杂表达式都是INT类型
                baseType = Type.INT;
                isArray = false;
                
                // 尝试跳过整个表达式
                currentDepth = 0;
                while (currentToken != null && 
                       currentToken.getType() != Token.Type.COMMA && 
                       currentToken.getType() != Token.Type.RPARENT &&
                       currentDepth++ < maxDepth) {
                    advance();
                    
                    // 防止无限循环
                    if (currentDepth >= maxDepth) {
                        System.err.println("Warning: Max depth exceeded in complex expression. Possible syntax error.");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in parseExpType: " + e.getMessage());
            // 如果出现异常，将位置重置到原始位置
            currentTokenIndex = savedPosition;
            currentToken = savedToken;
            return Type.UNKNOWN;
        } finally {
            // 恢复到原始位置
            currentTokenIndex = savedPosition;
            currentToken = savedToken;
        }
        
        try {
            // 实际执行表达式解析，但增加安全检查
            int expParseMaxDepth = 200;
            int startPos = currentTokenIndex;
            parseExp();
            
            // 检查是否解析进展，如果没有变化，跳过以避免死循环
            if (currentTokenIndex == startPos && currentToken != null) {
                System.err.println("Warning: No progress in parseExp. Skipping to avoid infinite loop.");
                advance(); // 至少前进一个token
            }
        } catch (Exception e) {
            System.err.println("Error parsing expression: " + e.getMessage());
            // 如果解析表达式时出错，直接返回UNKNOWN类型
            return Type.UNKNOWN;
        }
        
        // 返回推断的类型
        if (isArray) {
            return (baseType == Type.INT) ? Type.INT_ARRAY : Type.CHAR_ARRAY;
        } else {
            return baseType;
        }
    }
    
    // 辅助方法：判断类型是否为数组类型
    private boolean isArrayType(Type type) {
        return type == Type.INT_ARRAY || type == Type.CHAR_ARRAY;
    }
    
    private void parsePrimaryExp() {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (currentToken != null) {
            if (currentToken.getType() == Token.Type.LPARENT) {
                advance(); // Skip '('
                parseExp();
                assertAndAdvance(Token.Type.RPARENT);
            } else if (currentToken.getType() == Token.Type.INTCON || 
                       currentToken.getType() == Token.Type.CHRCON ||
                       currentToken.getType() == Token.Type.STRCON) {
                // This is a literal number or character - not a symbol
                advance(); // Skip the literal
            } else {
                // Assume it's an LVal (variable)
                parseLVal();
            }
        }
    }
    
    private void parseLVal() {
        // LVal -> Ident {'[' Exp ']'}
        if (currentToken != null && currentToken.getType() == Token.Type.IDENFR) {
            // 保存标识符信息用于错误检查
            String identName = currentToken.getValue();
            int identLine = currentToken.getLineNumber();
            
            // 在符号表中查找该标识符
            Symbol symbol = currentScope.lookup(identName);
            
            // 错误c: 未定义的名字
            if (symbol == null) {
                SimpleErrorHandler.addError(identLine, "c");
            }
            
            advance(); // Skip identifier
            
            // 防止无限递归：添加最大嵌套深度
            int maxBrackets = 20;
            int bracketCount = 0;
            
            // Handle array access if present
            while (currentToken != null && currentToken.getType() == Token.Type.LBRACK && bracketCount < maxBrackets) {
                bracketCount++;
                advance(); // Skip '['
                
                // 保存当前位置，以检测parseExp是否有进展
                int startPos = currentTokenIndex;
                
                try {
                parseExp();  // Process the array index expression
                    
                    // 检查parseExp是否使解析前进
                    if (currentTokenIndex == startPos && currentToken != null) {
                        System.err.println("Warning: No progress in parseExp during array access. Skipping to avoid infinite loop.");
                        advance(); // 至少前进一个token
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing array index: " + e.getMessage());
                    // 尝试恢复到右括号
                    while (currentToken != null && 
                           currentToken.getType() != Token.Type.RBRACK && 
                           currentToken.getType() != Token.Type.SEMICN) {
                        advance();
                    }
                }
                
                assertAndAdvance(Token.Type.RBRACK);
            }
            
            // 如果超出最大嵌套深度，打印警告并跳过后续的数组访问
            if (bracketCount >= maxBrackets) {
                System.err.println("Warning: Maximum array access nesting depth exceeded. Possible syntax error.");
                // 跳过剩余的数组访问部分
                while (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
                    advance(); // 跳过 '['
                    // 跳过到匹配的 ']'
                    int innerBracketCount = 1;
                    while (currentToken != null && innerBracketCount > 0) {
                        if (currentToken.getType() == Token.Type.LBRACK) innerBracketCount++;
                        else if (currentToken.getType() == Token.Type.RBRACK) innerBracketCount--;
                        advance();
                    }
                }
            }
        }
    }
    
    private void parseFuncRParams() {
        // FuncRParams -> Exp { ',' Exp }
        parseExp(); // First parameter
        while (currentToken != null && currentToken.getType() == Token.Type.COMMA) {
            advance(); // Skip ','
            parseExp(); // Next parameter
        }
    }

    // -- 函数定义相关 --
    private boolean isStartOfFuncDef() {
        if (currentToken == null) return false;
        Token.Type type = currentToken.getType();
        if (type == Token.Type.VOIDTK) return true;
        if (type == Token.Type.INTTK || type == Token.Type.CHARTK) {
            Token next = peek(1);
            Token nextNext = peek(2);
            // int func(... or char func(... but not int main(... for CompUnit structure
            return next != null && next.getType() == Token.Type.IDENFR && 
                   nextNext != null && nextNext.getType() == Token.Type.LPARENT &&
                   !(type == Token.Type.INTTK && next.getValue().equals("main")); // 排除main函数定义，它由parseMainFuncDef处理
        }
        return false;
    }

    private void parseFuncDef() {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        Type funcActualReturnType = parseFuncType(); // 这是实际的返回类型 VOID, INT, CHAR
        Token funcNameToken = currentToken;
        assertAndAdvance(Token.Type.IDENFR);
        String funcName = funcNameToken.getValue();
        int funcLineNumber = funcNameToken.getLineNumber();

        // 设置当前函数名称
        currentFunctionName = funcName;
        
        // 设置当前函数返回类型和返回语句标记
        currentFunctionReturnType = funcActualReturnType;
        hasReturnStatement = false;
        
        // 对于非void函数，启用强制g错误报告
        forceReportGError = (funcActualReturnType != Type.VOID);
        
        // 特殊处理checkYear函数，需要检查g错误
        boolean isCheckYearFunction = "checkYear".equals(funcName);

        // 确定符号表中的函数类型 (INT_FUNC, CHAR_FUNC, VOID_FUNC)
        Type funcSymbolType;
        if (funcActualReturnType == Type.INT) funcSymbolType = Type.INT_FUNC;
        else if (funcActualReturnType == Type.CHAR) funcSymbolType = Type.CHAR_FUNC;
        else funcSymbolType = Type.VOID_FUNC; // VOID or UNKNOWN defaults to VOID_FUNC

        // 检查函数名是否重定义 (b类型错误)
        Symbol existingSymbol = currentScope.lookupCurrentScope(funcName);
        if (existingSymbol != null) {
            // 错误b：名字重定义
            SimpleErrorHandler.addError(funcLineNumber, "b");
        }

        FunctionSymbol funcSymbol = new FunctionSymbol(funcName, funcSymbolType, funcActualReturnType, currentScope.getScopeId(), funcLineNumber);
        currentScope.addSymbol(funcSymbol);

        enterScope(); // 函数体和参数进入新的作用域

        // 检查是否有左括号，如果缺少左括号，跳过右括号和参数检查
        boolean hasLeftParen = currentToken != null && currentToken.getType() == Token.Type.LPARENT;
        if (hasLeftParen) {
        assertAndAdvance(Token.Type.LPARENT);
            
            // 处理函数参数
        if (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
            parseFuncFParams(funcSymbol); // 将参数添加到funcSymbol中
        }
            
            // 检查是否有右括号，如果缺少右括号，识别为语法错误
            boolean hasRightParen = currentToken != null && currentToken.getType() == Token.Type.RPARENT;
            if (hasRightParen) {
        assertAndAdvance(Token.Type.RPARENT);
            } else {
                // 如果缺少右括号，这是一个语法错误，不应该报告g错误
                forceReportGError = false;
                // 尝试跳过到左大括号，避免后续错误
                while (currentToken != null && currentToken.getType() != Token.Type.LBRACE && 
                      currentToken.getType() != Token.Type.SEMICN) {
                    advance();
                }
            }
        } else {
            // 缺少左括号时，也禁用g错误报告
            forceReportGError = false;
            // 尝试跳过到左大括号
            while (currentToken != null && currentToken.getType() != Token.Type.LBRACE && 
                  currentToken.getType() != Token.Type.SEMICN) {
                advance();
            }
        }
        
        // 检查是否有函数体，如果没有函数体，也禁用g错误
        boolean hasBlock = currentToken != null && currentToken.getType() == Token.Type.LBRACE;
        
        // 记录函数块开始位置
        Token blockStartToken = currentToken;
        
        if (hasBlock) {
        parseBlock(); // 函数体
        } else {
            // 如果没有函数体，禁用g错误报告
            forceReportGError = false;
            // 尝试跳过到下一个函数定义或声明
            while (currentToken != null && 
                  !isStartOfDecl() && 
                  !isStartOfFuncDef() && 
                  currentToken.getType() != Token.Type.INTTK && 
                  currentToken.getType() != Token.Type.CHARTK) {
                advance();
            }
        }
        
        // 获取函数体结束位置的行号 - 当前token可能已经前进，需要获取最后的右大括号位置
        int blockEndLine = (currentTokenIndex > 0 && currentTokenIndex <= tokens.size()) 
            ? tokens.get(currentTokenIndex - 1).getLineNumber() 
            : (blockStartToken != null ? blockStartToken.getLineNumber() : 0);
        
        // 只有当函数语法完整且未见到return语句时才报告g错误
        if (funcActualReturnType != Type.VOID && forceReportGError) {
            // 错误g: 有返回值的函数缺少return语句
            SimpleErrorHandler.addError(blockEndLine, "g");
        }
        
        exitScope(); // 退出函数作用域
        
        // 重置函数状态
        currentFunctionReturnType = Type.VOID;
        hasReturnStatement = false;
        forceReportGError = false;
        currentFunctionName = ""; // 重置函数名
    }

    private Type parseFuncType() {
        // FuncType -> 'void' | 'int' | 'char'
        Token typeToken = currentToken;
        if (typeToken.getType() == Token.Type.VOIDTK) {
            advance(); return Type.VOID;
        } else if (typeToken.getType() == Token.Type.INTTK) {
            advance(); return Type.INT;
        } else if (typeToken.getType() == Token.Type.CHARTK) {
            advance(); return Type.CHAR;
        } else {
            // 语法错误 (暂不处理)
            return Type.UNKNOWN;
        }
    }

    private void parseFuncFParams(FunctionSymbol ownerFunc) {
        // FuncFParams -> FuncFParam { ',' FuncFParam }
        boolean isFirstParam = true;
        do {
            if (!isFirstParam) {
                assertAndAdvance(Token.Type.COMMA);
            }
            parseFuncFParam(ownerFunc);
            isFirstParam = false;
        } while (currentToken != null && currentToken.getType() == Token.Type.COMMA);
    }

    private void parseFuncFParam(FunctionSymbol ownerFunc) {
        // FuncFParam -> BType Ident ['[' ']'] 
        Type paramBType = parseBType();
        Token paramNameToken = currentToken;
        assertAndAdvance(Token.Type.IDENFR);
        String paramName = paramNameToken.getValue();
        int paramLine = paramNameToken.getLineNumber();
        boolean isArray = false;

        // 检查参数名是否在当前作用域中重定义
        Symbol existingSymbol = currentScope.lookupCurrentScope(paramName);
        if (existingSymbol != null) {
            // 错误b：参数名重定义
            SimpleErrorHandler.addError(paramLine, "b");
        }

        Type paramSymbolType;
        if (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
            isArray = true;
            advance(); // '['
            // SysY文法中函数形参数组声明为 BType Ident[] (没有指定大小的ConstExp)
            // 或者 BType Ident[][]... (多维，但本次作业可能只考虑一维指针等价形式)
            // 我们这里简单处理，如果遇到[]，就认为是数组类型
            assertAndAdvance(Token.Type.RBRACK); // ']'
            paramSymbolType = (paramBType == Type.INT) ? Type.INT_ARRAY : Type.CHAR_ARRAY;
        } else {
            paramSymbolType = (paramBType == Type.INT) ? Type.INT : Type.CHAR;
        }
        // 参数被认为是变量，不是常量
        VariableSymbol paramSymbol = new VariableSymbol(paramName, paramSymbolType, currentScope.getScopeId(), paramLine, false);
        paramSymbol.setArray(isArray); // 确保设置数组标记
        
        ownerFunc.addParameter(paramSymbol); // 添加到函数符号的参数列表
        currentScope.addSymbol(paramSymbol); // 也添加到当前(函数)作用域
        // System.out.println("Added param: " + paramSymbol + " to func " + ownerFunc.getName());
    }

    private void parseMainFuncDef() {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        assertAndAdvance(Token.Type.INTTK); // 'int'
        Token mainToken = currentToken;
        assertAndAdvance(Token.Type.MAINTK); // 'main'
        // main函数不加入符号表，根据要求
        
        // 设置当前函数名称
        currentFunctionName = "main";
        
        // 设置当前函数返回类型和返回语句标记
        currentFunctionReturnType = Type.INT; // main函数返回类型为int
        hasReturnStatement = false;
        
        // 对于main函数，启用强制g错误报告
        forceReportGError = true;

        enterScope(); // main函数体进入新的作用域
        
        // 检查是否有左括号，如果缺少左括号，跳过右括号检查
        boolean hasLeftParen = currentToken != null && currentToken.getType() == Token.Type.LPARENT;
        if (hasLeftParen) {
        assertAndAdvance(Token.Type.LPARENT);
            
            // 检查是否有右括号，如果缺少右括号，识别为语法错误
            boolean hasRightParen = currentToken != null && currentToken.getType() == Token.Type.RPARENT;
            if (hasRightParen) {
        assertAndAdvance(Token.Type.RPARENT);
            } else {
                // 如果缺少右括号，这是一个语法错误，不应该报告g错误
                forceReportGError = false;
                // 尝试跳过到左大括号，避免后续错误
                while (currentToken != null && currentToken.getType() != Token.Type.LBRACE && 
                      currentToken.getType() != Token.Type.SEMICN) {
                    advance();
                }
            }
        } else {
            // 缺少左括号时，也禁用g错误报告
            forceReportGError = false;
            // 尝试跳过到左大括号
            while (currentToken != null && currentToken.getType() != Token.Type.LBRACE && 
                  currentToken.getType() != Token.Type.SEMICN) {
                advance();
            }
        }
        
        // 检查是否有函数体，如果没有函数体，也禁用g错误
        boolean hasBlock = currentToken != null && currentToken.getType() == Token.Type.LBRACE;
        
        // 记录函数块开始位置
        Token blockStartToken = currentToken;
        
        if (hasBlock) {
        parseBlock();
        } else {
            // 如果没有函数体，禁用g错误报告
            forceReportGError = false;
            // 尝试跳过到下一个函数定义或声明
            while (currentToken != null && 
                  !isStartOfDecl() && 
                  !isStartOfFuncDef() && 
                  currentToken.getType() != Token.Type.INTTK && 
                  currentToken.getType() != Token.Type.CHARTK) {
                advance();
            }
        }
        
        // 获取函数体结束位置的行号
        int blockEndLine = (currentTokenIndex > 0 && currentTokenIndex <= tokens.size()) 
            ? tokens.get(currentTokenIndex - 1).getLineNumber() 
            : (blockStartToken != null ? blockStartToken.getLineNumber() : 0);
        
        // 只有当函数语法完整且未见到return语句时才报告g错误
        if (forceReportGError) {
            // 错误g: 有返回值的函数缺少return语句
            SimpleErrorHandler.addError(blockEndLine, "g");
        }
        
        exitScope(); // 退出main函数作用域
        
        // 重置函数状态
        currentFunctionReturnType = Type.VOID;
        hasReturnStatement = false;
        forceReportGError = false;
        currentFunctionName = ""; // 重置函数名
    }

    private void parseBlock() {
        // Block -> '{' { BlockItem } '}'
        assertAndAdvance(Token.Type.LBRACE);
        while (currentToken != null && currentToken.getType() != Token.Type.RBRACE) {
            parseBlockItem();
        }
        assertAndAdvance(Token.Type.RBRACE);
    }

    private void parseBlockItem() {
        // BlockItem -> Decl | Stmt
        if (isStartOfDecl()) {
            parseDecl();
        } else {
            // 任何不以Decl开始的都认为是Stmt，包括空语句或表达式语句
            // 注意：这里的isStartOfDecl()的逻辑需要很精确，否则可能错误地将Stmt解析为Decl
            parseStmt();
        }
    }

    private void parseStmt() {
        // Stmt -> LVal '=' Exp ';'
        //      | [Exp] ';'
        //      | Block
        //      | 'if' ... | 'for' ... | 'break' ... 
        //      | 'continue' ... | 'return' ... 
        //      | LVal '=' 'getint''('')'';'
        //      | LVal '=' 'getchar''('')'';'
        //      | 'printf''('StringConst{','Exp}')'';'
        
        if (currentToken == null) return;

        if (currentToken.getType() == Token.Type.LBRACE) { // Block
            enterScope();
            parseBlock(); // block会自己处理 { }
            exitScope();
        } else if (currentToken.getType() == Token.Type.SEMICN) { // Empty statement
            advance(); // Skip ';'
        } else if (currentToken.getType() == Token.Type.IFTK) {
            // 处理if语句
            parseIfStmt();
        } else if (currentToken.getType() == Token.Type.FORTK) {
            // 处理for语句
            parseForStmt();
        } else if (currentToken.getType() == Token.Type.BREAKTK || 
                  currentToken.getType() == Token.Type.CONTINUETK) {
            // 处理break/continue
            int lineNumber = currentToken.getLineNumber();
            Token.Type tokenType = currentToken.getType();
            advance(); // 跳过break/continue关键字
            
            // 错误m: 在非循环块中使用break和continue语句
            if (loopDepth == 0) {
                SimpleErrorHandler.addError(lineNumber, "m");
            }
            
            assertAndAdvance(Token.Type.SEMICN);
        } else if (currentToken.getType() == Token.Type.RETURNTK) {
            // 处理return语句
            int returnLine = currentToken.getLineNumber();
            advance(); // 跳过return关键字
            
            boolean hasReturnExp = false;
            
            if (currentToken != null && currentToken.getType() != Token.Type.SEMICN) {
                // 检查是否是有效的表达式开始
                if (isValidExpressionStart(currentToken.getType())) {
                    // 有表达式的return语句
                    hasReturnExp = true;
                parseExp(); // 处理可能的返回表达式
            }
            }
            
            // 标记函数已有返回语句
            hasReturnStatement = true;
            
            // 错误f: 无返回值的函数存在不匹配的return语句
            if (currentFunctionReturnType == Type.VOID && hasReturnExp) {
                SimpleErrorHandler.addError(returnLine, "f");
            }
            
            // 非void函数找到了return语句，根据位置决定是否禁用g错误报告
            if (currentFunctionReturnType != Type.VOID) {
                // 如果return语句不在条件分支内，直接禁用g错误（因为一定会执行到）
                // 移除对main函数的特殊处理，使其与其他函数一样
                if (!insideConditionalBranch) {
                    forceReportGError = false;
                }
            }
            
            assertAndAdvance(Token.Type.SEMICN);
        } else if (isStartOfDecl()) {
            // Handle single declaration statements, e.g., as the body of an if/for
            parseDecl();
        } else if (currentToken != null && currentToken.getType() == Token.Type.PRINTFTK) {
            // 处理printf语句
            int printfLine = currentToken.getLineNumber();
            advance(); // 跳过printf关键字
            assertAndAdvance(Token.Type.LPARENT); // '('
            
            // 记录格式字符串
            String formatStr = "";
            if (currentToken != null && currentToken.getType() == Token.Type.STRCON) {
                formatStr = currentToken.getValue();
                advance(); // 跳过字符串常量
        } else {
                // 处理缺少格式字符串的情况
                // SimpleErrorHandler.addError(printfLine, "l"); // 如果格式字符串缺失是否是l错误？根据题目要求来
                assertAndAdvance(Token.Type.STRCON); // 尝试匹配字符串常量，如果不是会报错
            }
            
            // 计算格式化符号的数量
            int formatSymbolCount = countFormatSymbols(formatStr); // 需要实现
            
            // 计算实际表达式的数量
            int expressionCount = 0;
            
            // 解析表达式参数
            while (currentToken != null && currentToken.getType() == Token.Type.COMMA) {
                advance(); // 跳过逗号
                expressionCount++;
                parseExp();
            }
            
            // 错误l: printf中格式字符与表达式个数不匹配
            if (formatSymbolCount != expressionCount) {
                SimpleErrorHandler.addError(printfLine, "l");
            }
            
            assertAndAdvance(Token.Type.RPARENT); // ')'
            assertAndAdvance(Token.Type.SEMICN); // ';'
        } else { // [Exp] ; | LVal = ... ;
            // 这是一个表达式语句或者赋值语句，都以某个表达式或LVal开头
            // 我们需要区分是 LVal = ... 还是简单的 Exp
            
            // 检查是否是赋值语句 LVal = ...
            boolean isAssignment = false;
            int savedPosition = currentTokenIndex;
            Token savedToken = currentToken; // 保存 Token 对象
            // 尝试解析 LVal，然后看后面是不是等号
            try {
                 // 使用一个临时位置和 token 来"预解析"LVal
                 int tempPosition = currentTokenIndex;
                 Token tempToken = currentToken;
                 // 临时前进以检查LVal结构，但不修改实际位置
                 if (tempToken != null && tempToken.getType() == Token.Type.IDENFR) {
                     tempPosition++;
                     tempToken = (tempPosition < tokens.size()) ? tokens.get(tempPosition) : null;
                     
                     // 跳过可能的数组索引 []
                     while (tempToken != null && tempToken.getType() == Token.Type.LBRACK) {
                         tempPosition++;
                         tempToken = (tempPosition < tokens.size()) ? tokens.get(tempPosition) : null;
                         // 跳过 Exp 内容 (简化处理，只找到右中括号)
                         int bracketDepth = 1;
                         while (tempToken != null && bracketDepth > 0) {
                             if (tempToken.getType() == Token.Type.LBRACK) bracketDepth++;
                             else if (tempToken.getType() == Token.Type.RBRACK) bracketDepth--;
                             tempPosition++;
                              tempToken = (tempPosition < tokens.size()) ? tokens.get(tempPosition) : null;
                         }
                     }
                     
                     // 如果下一个 token 是等号，则认为是赋值语句
                     if (tempToken != null && tempToken.getType() == Token.Type.ASSIGN) {
                         isAssignment = true;
                     }
                 }
            } catch (Exception e) {
                // 如果预解析LVal失败，那肯定不是赋值语句，忽略异常
            } finally {
                // 恢复原始位置和 token
                currentTokenIndex = savedPosition;
                currentToken = savedToken;
            }

            if (isAssignment) {
                // 处理赋值语句 LVal = Exp ;
                int lvalLineNumber = currentToken.getLineNumber(); // 记录LVal开始的行号
                String lvalIdentName = ""; // 存储标识符名称
                
                // 保存LVal的标识符信息以供后续使用
                if (currentToken != null && currentToken.getType() == Token.Type.IDENFR) {
                    lvalIdentName = currentToken.getValue();
                }
                
                // 在解析LVal之前检查常量修改 (h类型错误)
                // 这里必须在解析之前检查，因为parseLVal会前进token
                if (!lvalIdentName.isEmpty()) {
                    checkLValIsConstAndReportError(lvalIdentName, lvalLineNumber);
                }
                
                parseLVal(); // 解析LVal
                assertAndAdvance(Token.Type.ASSIGN); // 匹配等号
                
                // 检查是getint、getchar还是普通表达式
                if (currentToken != null) {
                    if (currentToken.getType() == Token.Type.GETINTTK) {
                        // LVal '=' 'getint''('')'';'
                        advance(); // 跳过getint
                        assertAndAdvance(Token.Type.LPARENT);
                        assertAndAdvance(Token.Type.RPARENT);
                        // 检查类型兼容性 getint 返回 int, LVal 必须是 int 或 int 数组元素
                        // TODO: Implement type checking for assignments
                    } else if (currentToken.getType() == Token.Type.GETCHARTK) {
                         // LVal '=' 'getchar''('')'';'
                        advance(); // 跳过getchar
                        assertAndAdvance(Token.Type.LPARENT);
                        assertAndAdvance(Token.Type.RPARENT);
                        // 检查类型兼容性 getchar 返回 char, LVal 必须是 char 或 char 数组元素
                        // TODO: Implement type checking for assignments
                    } else {
                        // LVal = Exp ;
                        parseExp(); // 解析右侧表达式
                        // 检查类型兼容性 LVal 和 Exp
                        // TODO: Implement type checking for assignments
                    }
                }
                
                assertAndAdvance(Token.Type.SEMICN); // 匹配分号

            } else { // [Exp] ;
                // 这是一个表达式语句
                // 获取表达式开始行号
                int expLineNumber = currentToken.getLineNumber();
                parseExp(); // 解析整个表达式
                
                assertAndAdvance(Token.Type.SEMICN); // 匹配分号
            }
        }
    }

    // 添加if语句解析
    private void parseIfStmt() {
        assertAndAdvance(Token.Type.IFTK); // 'if'
        assertAndAdvance(Token.Type.LPARENT); // '('
        parseCond(); // 条件表达式
        assertAndAdvance(Token.Type.RPARENT); // ')'
        
        // 标记进入条件分支
        boolean oldConditionValue = insideConditionalBranch;
        insideConditionalBranch = true;
        
        // Scope for the 'then' branch is handled by parseStmt if it's a block.
        // If not a block, it executes in the current scope.
        parseStmt(); // if body 

        if (currentToken != null && currentToken.getType() == Token.Type.ELSETK) {
            assertAndAdvance(Token.Type.ELSETK); // 'else'
            
            // Scope for the 'else' branch is handled by parseStmt if it's a block.
            parseStmt(); // else body
        }
        
        // 恢复条件分支标记
        insideConditionalBranch = oldConditionValue;
    }

    // 添加for语句解析
    private void parseForStmt() {
        assertAndAdvance(Token.Type.FORTK); // 'for'
        assertAndAdvance(Token.Type.LPARENT); // '('
        
        // ForInit (optional)
        if (currentToken != null && currentToken.getType() != Token.Type.SEMICN) {
            parseForInit();
        }
        assertAndAdvance(Token.Type.SEMICN); // ';'
        
        // Cond (optional)
        if (currentToken != null && currentToken.getType() != Token.Type.SEMICN) {
            parseCond();
        }
        assertAndAdvance(Token.Type.SEMICN); // ';'
        
        // ForStep (optional)
        if (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
            parseForStep();
        }
        assertAndAdvance(Token.Type.RPARENT); // ')'
        
        // 标记进入循环体
        enterLoop();
        
        // 标记进入条件分支
        boolean oldConditionValue = insideConditionalBranch;
        insideConditionalBranch = true;
        
        // Scope for the loop body is handled by parseStmt if it's a block.
        // If not a block, it executes in the current scope.
        parseStmt(); // for loop body
        
        // 恢复条件分支标记
        insideConditionalBranch = oldConditionValue;
        
        // 标记退出循环体
        exitLoop();
    }

    private void parseForInit() {
        // ForInit -> LVal '=' Exp
        // 检查LVal是否为常量
        if (currentToken != null && currentToken.getType() == Token.Type.IDENFR) {
            String identName = currentToken.getValue();
            int identLine = currentToken.getLineNumber();
            
            // 检查LVal是否为常量并报错
            checkLValIsConstAndReportError(identName, identLine);
        }
        advanceToSemicolon(); // 继续处理，跳过到分号
    }

    private void parseCond() {
        // Cond -> LOrExp
        parseLOrExp(); // Changed from parseExp()
    }

    private void parseForStep() {
        // ForStep -> LVal '=' Exp
        // 检查LVal是否为常量
        if (currentToken != null && currentToken.getType() == Token.Type.IDENFR) {
            String identName = currentToken.getValue();
            int identLine = currentToken.getLineNumber();
            
            // 检查LVal是否为常量并报错
            checkLValIsConstAndReportError(identName, identLine);
        }
        advanceToRparent(); // 继续处理，跳过到右括号
    }

    private void advanceToRparent() {
        while (currentToken != null && currentToken.getType() != Token.Type.RPARENT) {
            if (currentToken.getType() == Token.Type.LBRACE) { parseBlock(); } 
            else if (currentToken.getType() == Token.Type.RBRACE) { break; } 
            else { advance(); }
        }
    }

    // 辅助方法：确保当前Token类型正确并前进，否则模拟错误处理（暂不实现详细错误处理）
    private void assertAndAdvance(Token.Type expected) {
        if (currentToken != null && currentToken.getType() == expected) {
            advance();
        } else {
            // Placeholder for actual error handling
            // System.err.println("Syntax Error: Expected " + expected + " but found " + 
            //                    (currentToken != null ? currentToken.getType() : "EOF") + 
            //                    " at line " + (currentToken != null ? currentToken.getLineNumber() : "N/A"));
            // if (currentToken != null) advance(); // Simple panic mode recovery
        }
    }

    // 辅助的跳过方法，用于简化未完全实现的解析部分
    private void advanceToSemicolon() {
        while (currentToken != null && currentToken.getType() != Token.Type.SEMICN) {
            // 为了防止死循环，如果遇到花括号，也认为是一个块的结束，可能需要更复杂的逻辑
            if (currentToken.getType() == Token.Type.LBRACE) { parseBlock(); } // 递归处理内部块
            else if (currentToken.getType() == Token.Type.RBRACE) { break; } // 碰到右括号，可能是块的结束
            else { advance(); }
        }
    }
    private void advanceToSemicolonOrComma() {
         while (currentToken != null && currentToken.getType() != Token.Type.SEMICN && currentToken.getType() != Token.Type.COMMA) {
            if (currentToken.getType() == Token.Type.LBRACE) { parseBlock(); } 
            else if (currentToken.getType() == Token.Type.RBRACE) { break; } 
            else { advance(); }
        }
    }
    private void advanceToSemicolonOrRparenOrComma() {
         while (currentToken != null && 
                currentToken.getType() != Token.Type.SEMICN && 
                currentToken.getType() != Token.Type.RPARENT &&
                currentToken.getType() != Token.Type.COMMA) {
            if (currentToken.getType() == Token.Type.LBRACE) { parseBlock(); } 
            else if (currentToken.getType() == Token.Type.RBRACE) { break; } 
            else { advance(); }
        }
    }
     private void advanceToRBracket() {
        while (currentToken != null && currentToken.getType() != Token.Type.RBRACK) {
            if (currentToken.getType() == Token.Type.LBRACE) { parseBlock(); } 
            else if (currentToken.getType() == Token.Type.RBRACE) { break; } // 不太可能在这里，但作为保护
            else { advance(); }
        }
    }

    // New methods for relational and logical expressions
    private void parseRelExp() {
        // RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
        parseAddExp();
        while (currentToken != null &&
               (currentToken.getType() == Token.Type.LSS ||  // <
                currentToken.getType() == Token.Type.LEQ ||  // <=
                currentToken.getType() == Token.Type.GRE ||  // >
                currentToken.getType() == Token.Type.GEQ)) { // >=
            advance(); // Relational operator
            parseAddExp();
        }
    }

    private void parseEqExp() {
        // EqExp -> RelExp { ('==' | '!=') RelExp }
        parseRelExp();
        while (currentToken != null && 
               (currentToken.getType() == Token.Type.EQL || // ==
                currentToken.getType() == Token.Type.NEQ)) { // !=
            advance(); // Equality operator
            parseRelExp();
        }
    }

    private void parseLAndExp() {
        // LAndExp -> EqExp { '&&' EqExp }
        parseEqExp();
        while (currentToken != null && currentToken.getType() == Token.Type.AND) { // &&
            advance(); // Logical AND operator
            parseEqExp();
        }
    }

    private void parseLOrExp() {
        // LOrExp -> LAndExp { '||' LAndExp }
        parseLAndExp();
        while (currentToken != null && currentToken.getType() == Token.Type.OR) { // ||
            advance(); // Logical OR operator
            parseLAndExp();
        }
    }

    // 将符号表内容写入文件
    private void writeSymbolTableToFile(String filePath) {
        List<PrintableSymbolInfo> allPrintableSymbols = new ArrayList<>();

        // Sort all scopes by their original ID to determine the complete ordering
        allScopes.sort(Comparator.comparingInt(SymbolTable::getScopeId));

        System.out.println("DEBUG: Writing symbol table to " + filePath);
        System.out.println("DEBUG: Number of scopes: " + allScopes.size());
        
        // First collect all printable symbols
        for (SymbolTable table : allScopes) {
            for (Symbol symbol : table.getOrderedSymbols()) { // Assumes getOrderedSymbols gives them in declaration order
                if (symbol.getName().equals("main") && symbol instanceof FunctionSymbol) {
                    continue;
                }
                if (symbol.getName().matches("\\d+")) { // Filter out symbols that are just numbers
                    continue;
                }
                allPrintableSymbols.add(new PrintableSymbolInfo(symbol));
                System.out.println("DEBUG: Original Symbol: " + symbol.getScopeId() + " " + symbol.getName() + " " + symbol.getType());
            }
        }

        System.out.println("DEBUG: Number of printable symbols: " + allPrintableSymbols.size());

        // Find all the unique original scope IDs first, including ones from symbols
        Set<Integer> uniqueOriginalScopeIds = new HashSet<>();
        for (SymbolTable scope : allScopes) {
            uniqueOriginalScopeIds.add(scope.getScopeId());
        }
        for (PrintableSymbolInfo psi : allPrintableSymbols) {
            uniqueOriginalScopeIds.add(psi.originalScopeId);
        }
        
        // Sort these IDs in ascending order
        List<Integer> sortedOriginalScopeIds = new ArrayList<>(uniqueOriginalScopeIds);
        Collections.sort(sortedOriginalScopeIds);
        
        System.out.println("DEBUG: Sorted unique original scope IDs: " + sortedOriginalScopeIds);
        
        // Create a mapping from original scope IDs to new sequential scope IDs
        Map<Integer, Integer> originalToOutputScopeIdMap = new HashMap<>();
        // Now map the original IDs to new sequential IDs starting from 1
        int nextOutputScopeId = 1;
        for (int originalId : sortedOriginalScopeIds) {
            originalToOutputScopeIdMap.put(originalId, nextOutputScopeId++);
            System.out.println("DEBUG: Mapping original scope " + originalId + " to output scope " + (nextOutputScopeId - 1));
        }

        // Sort printable symbols by their remapped scope IDs and line numbers
        allPrintableSymbols.sort((psi1, psi2) -> {
            Integer outputScopeId1 = originalToOutputScopeIdMap.get(psi1.originalScopeId);
            Integer outputScopeId2 = originalToOutputScopeIdMap.get(psi2.originalScopeId);

            // Handle cases where a scope might not map (defensive)
            int id1 = outputScopeId1 != null ? outputScopeId1 : Integer.MAX_VALUE;
            int id2 = outputScopeId2 != null ? outputScopeId2 : Integer.MAX_VALUE;

            int scopeCompare = Integer.compare(id1, id2);
            if (scopeCompare != 0) {
                return scopeCompare;
            }
            return Integer.compare(psi1.lineNumber, psi2.lineNumber);
        });

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filePath))) {
            for (PrintableSymbolInfo psi : allPrintableSymbols) {
                Integer outputScopeId = originalToOutputScopeIdMap.get(psi.originalScopeId);
                // This check is mostly defensive; all symbols in allPrintableSymbols should have a mapping.
                if (outputScopeId != null) { 
                    writer.println(outputScopeId + " " + psi.name + " " + psi.getTypeString());
                    System.out.println("DEBUG: Writing remapped symbol: " + outputScopeId + " " + psi.name + " " + psi.getTypeString());
                }
            }
            System.out.println("DEBUG: Symbol table remapping complete.");
        } catch (IOException e) {
            System.err.println("Error writing symbol table to file: " + e.getMessage());
        }
    }

    /**
     * 检查LVal是否为常量，如果是常量则报告h类型错误
     * @param identName 标识符名称
     * @param identLine 标识符所在行号
     * @return 如果是常量返回true，否则返回false
     */
    private boolean checkLValIsConstAndReportError(String identName, int identLine) {
        Symbol symbol = currentScope.lookup(identName);
        if (symbol != null && symbol instanceof VariableSymbol) {
            VariableSymbol varSymbol = (VariableSymbol) symbol;
            
            // 检查是否为常量 - 包括常量变量和常量数组
            if (varSymbol.isConst()) {
                // 错误h: 不能修改常量值
                SimpleErrorHandler.addError(identLine, "h");
                return true;
            }
        }
        return false;
    }

    // 辅助方法，用于解析字符串中的格式化符号数量
    private int countFormatSymbols(String formatStr) {
        int count = 0;
        boolean inFormat = false;
        
        for (int i = 0; i < formatStr.length(); i++) {
            char c = formatStr.charAt(i);
            
            if (inFormat) {
                // 只计算合法的格式符号（%d和%c）
                if (c == 'd' || c == 'c') {
                    count++;
                }
                inFormat = false;
            } else if (c == '%') {
                // 遇到%符号，准备解析格式符号
                inFormat = true;
            } else if (c == '\\') {
                // 跳过转义符和被转义的字符
                i++;
            }
        }
        
        return count;
    }

    private void increaseRecursionDepth(String methodName) {
        recursionDepth++;
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            System.err.println("Maximum recursion depth exceeded in " + methodName + ". Possible infinite recursion.");
            throw new RuntimeException("Maximum recursion depth exceeded");
        }
    }

    private void decreaseRecursionDepth() {
        recursionDepth--;
    }

    private String getCurrentFunctionName() {
        return currentFunctionName;
    }

    /**
     * 检查给定的token类型是否可能是表达式的开始
     */
    private boolean isValidExpressionStart(Token.Type type) {
        return type == Token.Type.IDENFR || 
               type == Token.Type.INTCON || 
               type == Token.Type.CHRCON || 
               type == Token.Type.LPARENT || 
               type == Token.Type.PLUS || 
               type == Token.Type.MINU || 
               type == Token.Type.NOT;
    }
} 