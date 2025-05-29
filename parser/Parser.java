package parser;

import lexer.Token;
import utils.SimpleErrorHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 语法分析器，使用递归下降分析法实现
 */
public class Parser {
    private List<Token> tokens;          // 词法分析产生的Token序列
    private int position;                // 当前分析的位置
    private Token currentToken;          // 当前Token
    private List<String> output;         // 输出结果
    private int recursionDepth = 0;
    private static final int MAX_RECURSION_DEPTH = 1000; // 设置合理的最大递归深度

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.output = new ArrayList<>();
        if (!tokens.isEmpty()) {
            this.currentToken = tokens.get(0);
        }
    }

    /**
     * 获取语法分析的输出结果
     */
    public List<String> getOutput() {
        return output;
    }

    /**
     * 开始语法分析
     */
    public void parse() {
        try {
            recursionDepth = 0; // 重置递归深度
            // 开始递归下降分析
            compUnit();
        } catch (Exception e) {
            System.err.println("Error during parsing: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试恢复到分析结束
            System.err.println("Attempting to recover from error...");
            position = tokens.size() - 1; // 设置到最后一个token
            if (position >= 0) {
                currentToken = tokens.get(position);
            }
        }
    }

    /**
     * 向前移动一个Token
     */
    private void advance() {
        position++;
        if (position < tokens.size()) {
            currentToken = tokens.get(position);
        }
    }

    /**
     * 检查并增加递归深度，如果超过最大深度则抛出异常
     */
    private void checkRecursionDepth(String methodName) {
        recursionDepth++;
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            recursionDepth--; // 减少计数以防止进一步递归
            System.err.println("Maximum recursion depth exceeded in " + methodName + ". Possible syntax error or infinite recursion.");
            skipToExpressionBoundary(); // 尝试恢复到表达式边界
            throw new RuntimeException("Maximum recursion depth exceeded"); // 抛出异常以中断当前递归链
        }
    }

    /**
     * 减少递归深度计数
     */
    private void decreaseRecursionDepth() {
        recursionDepth--;
    }

    /**
     * 匹配当前Token并前进，如果不匹配则报告错误并尝试恢复
     * @param type 期望的Token类型
     * @return 如果匹配成功返回true，否则返回false
     */
    private boolean match(Token.Type type) {
        if (currentToken.getType() == type) {
            // 先输出Token信息
            output.add(currentToken.toString());
            advance();
            return true;
        } else {
            // 不匹配时仍然前进以避免卡住
            advance();
            return false;
        }
    }

    /**
     * 添加语法成分
     * @param syntaxComponent 语法成分名称
     */
    private void addSyntaxComponent(String syntaxComponent) {
        // 检查递归深度
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            System.err.println("Maximum recursion depth exceeded in addSyntaxComponent: " + syntaxComponent);
            return; // 不添加，直接返回以防止栈溢出
        }
        
        // 检查当前Token是否为null
        if (currentToken == null && position >= tokens.size()) {
            // 已经到达Token流末尾，仍然添加语法成分，但记录警告
            System.err.println("Warning: Adding syntax component with no more tokens: " + syntaxComponent);
        }
        
        output.add("<" + syntaxComponent + ">");
    }

    /**
     * 判断当前Token类型是否匹配
     */
    private boolean check(Token.Type type) {
        return currentToken.getType() == type;
    }

    /**
     * 判断当前Token是否属于给定的类型集合
     */
    private boolean checkAny(Token.Type... types) {
        for (Token.Type type : types) {
            if (check(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * CompUnit -> {Decl} {FuncDef} MainFuncDef
     */
    private void compUnit() {
        // 解析声明序列
        while (isDecl()) {
            decl();
        }

        // 解析函数定义序列
        while (isFuncDef()) {
            parseFuncDef();
        }

        // 解析主函数定义
        mainFuncDef();

        // 添加编译单元语法成分
        addSyntaxComponent("CompUnit");
    }

    /**
     * 判断当前是否是声明
     */
    private boolean isDecl() {
        if (check(Token.Type.CONSTTK)) {
            return true;
        }
        if (check(Token.Type.INTTK) || check(Token.Type.CHARTK)) {
            // 向前看一个Token，排除函数定义的情况
            if (position + 1 < tokens.size()) {
                Token nextToken = tokens.get(position + 1);
                if (nextToken.getType() == Token.Type.MAINTK) {
                    return false; // 主函数定义
                }
                if (position + 2 < tokens.size() && nextToken.getType() == Token.Type.IDENFR) {
                    Token nextNextToken = tokens.get(position + 2);
                    if (nextNextToken.getType() == Token.Type.LPARENT) {
                        return false; // 函数定义
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 判断当前是否是函数定义
     */
    private boolean isFuncDef() {
        if (check(Token.Type.VOIDTK)) {
            return true;
        }
        if ((check(Token.Type.INTTK) || check(Token.Type.CHARTK)) && position + 1 < tokens.size()) {
            Token nextToken = tokens.get(position + 1);
            if (nextToken.getType() == Token.Type.IDENFR && 
                position + 2 < tokens.size() && 
                tokens.get(position + 2).getType() == Token.Type.LPARENT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decl -> ConstDecl | VarDecl
     */
    private void decl() {
        if (check(Token.Type.CONSTTK)) {
            constDecl();
        } else {
            varDecl();
        }
    }

    /**
     * ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
     */
    private void constDecl() {
        // 保存const关键字的行号，用于错误报告
        int constLineNumber = currentToken.getLineNumber();
        
        match(Token.Type.CONSTTK);
        bType();
        constDef();
        
        // 记录最后一个ConstDef的行号(声明所在行)
        int declLineNumber = currentToken.getLineNumber() - 1;

        while (check(Token.Type.COMMA)) {
            match(Token.Type.COMMA);
            constDef();
            // 更新行号
            declLineNumber = currentToken.getLineNumber() - 1;
        }
        
        // 检查分号
        if (currentToken.getType() != Token.Type.SEMICN) {
            // 错误：缺少分号，错误类型i
            // 对于第一个常量定义使用const关键字行号，其他情况使用declLineNumber
            SimpleErrorHandler.addError(constLineNumber, "i");
        } else {
            match(Token.Type.SEMICN);
        }
        addSyntaxComponent("ConstDecl");
    }

    /**
     * BType -> 'int' | 'char'
     */
    private void bType() {
        if (check(Token.Type.INTTK)) {
            match(Token.Type.INTTK);
        } else if (check(Token.Type.CHARTK)) {
            match(Token.Type.CHARTK);
        }
    }

    /**
     * ConstDef -> Ident [ '[' ConstExp ']' ] '=' ConstInitVal
     */
    private void constDef() {
        try {
            checkRecursionDepth("constDef");
            
            match(Token.Type.IDENFR);

            if (check(Token.Type.LBRACK)) {
                match(Token.Type.LBRACK);
                int constExpLineNumber = currentToken.getLineNumber();
                
                // 检查括号是否配对 - 如果下一个token是等号，说明缺少右中括号
                if (check(Token.Type.ASSIGN)) {
                    // 错误：缺少右中括号，错误类型k
                    SimpleErrorHandler.addError(constExpLineNumber, "k");
                } else {
                    try {
                        constExp();
                    } catch (RuntimeException e) {
                        // 捕获可能的递归深度异常，继续处理
                        System.err.println("Error in constExp: " + e.getMessage());
                        // 跳过直到遇到右中括号或等号
                        while (position < tokens.size() && 
                              !check(Token.Type.RBRACK) && 
                              !check(Token.Type.ASSIGN) &&
                              !check(Token.Type.SEMICN)) {
                            advance();
                        }
                    }
                    
                    // 检查右中括号
                    if (!check(Token.Type.RBRACK)) {
                        // 错误：缺少右中括号，错误类型k
                        SimpleErrorHandler.addError(constExpLineNumber, "k");
                    } else {
                        match(Token.Type.RBRACK);
                    }
                }
            }

            // 检查等号
            if (!check(Token.Type.ASSIGN)) {
                // 常量定义必须有初始值，如果没有等号就是错误
                // 但是继续分析，假设等号存在
                System.err.println("Error: Missing assignment in constant definition");
            } else {
                match(Token.Type.ASSIGN);
            }

            try {
                constInitVal();
            } catch (RuntimeException e) {
                // 捕获可能的递归深度异常，继续处理
                System.err.println("Error in constInitVal: " + e.getMessage());
                // 跳过直到分号
                while (position < tokens.size() && !check(Token.Type.SEMICN)) {
                    advance();
                }
            }
            
            addSyntaxComponent("ConstDef");
        } finally {
            decreaseRecursionDepth();
        }
    }

    /**
     * ConstInitVal -> ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
     */
    private void constInitVal() {
        if (check(Token.Type.LBRACE)) {
            match(Token.Type.LBRACE);

            if (!check(Token.Type.RBRACE)) {
                constExp();

                while (check(Token.Type.COMMA)) {
                    match(Token.Type.COMMA);
                    constExp();
                }
            }

            match(Token.Type.RBRACE);
        } else if (check(Token.Type.STRCON)) {
            match(Token.Type.STRCON);
        } else {
            constExp();
        }
        addSyntaxComponent("ConstInitVal");
    }

    /**
     * VarDecl -> BType VarDef { ',' VarDef } ';'
     */
    private void varDecl() {
        // 保存类型关键字的行号
        int typeLineNumber = currentToken.getLineNumber();
        
        bType();
        varDef();

        // 记录最后一个VarDef的行号(声明所在行)
        int declLineNumber = currentToken.getLineNumber() - 1;

        while (check(Token.Type.COMMA)) {
            match(Token.Type.COMMA);
            varDef();
            // 更新行号
            declLineNumber = currentToken.getLineNumber() - 1;
        }
        
        // 检查分号
        if (currentToken.getType() != Token.Type.SEMICN) {
            // 错误：缺少分号，错误类型i
            // 对变量声明使用类型关键字行号
            SimpleErrorHandler.addError(typeLineNumber, "i");
        } else {
            match(Token.Type.SEMICN);
        }
        addSyntaxComponent("VarDecl");
    }

    /**
     * VarDef -> Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
     */
    private void varDef() {
        try {
            checkRecursionDepth("varDef");
            
            match(Token.Type.IDENFR);

            if (check(Token.Type.LBRACK)) {
                match(Token.Type.LBRACK);
                int constExpLineNumber = currentToken.getLineNumber();
                
                // 检查括号是否配对 - 如果下一个token是等号，说明缺少右中括号
                if (check(Token.Type.ASSIGN)) {
                    // 错误：缺少右中括号，错误类型k
                    SimpleErrorHandler.addError(constExpLineNumber, "k");
                } else {
                    try {
                        constExp();
                    } catch (RuntimeException e) {
                        // 捕获可能的递归深度异常，继续处理
                        System.err.println("Error in constExp within varDef: " + e.getMessage());
                        // 跳过直到遇到右中括号或等号
                        while (position < tokens.size() && 
                              !check(Token.Type.RBRACK) && 
                              !check(Token.Type.ASSIGN) &&
                              !check(Token.Type.SEMICN)) {
                            advance();
                        }
                    }
                    
                    // 检查右中括号
                    if (!check(Token.Type.RBRACK)) {
                        // 错误：缺少右中括号，错误类型k
                        SimpleErrorHandler.addError(constExpLineNumber, "k");
                    } else {
                        match(Token.Type.RBRACK);
                    }
                }
            }

            if (check(Token.Type.ASSIGN)) {
                match(Token.Type.ASSIGN);
                try {
                    initVal();
                } catch (RuntimeException e) {
                    // 捕获可能的递归深度异常，继续处理
                    System.err.println("Error in initVal: " + e.getMessage());
                    // 跳过直到分号
                    while (position < tokens.size() && !check(Token.Type.SEMICN)) {
                        advance();
                    }
                }
            }
            
            addSyntaxComponent("VarDef");
        } finally {
            decreaseRecursionDepth();
        }
    }

    /**
     * InitVal -> Exp | '{' [ Exp { ',' Exp } ] '}'
     */
    private void initVal() {
        if (check(Token.Type.LBRACE)) {
            match(Token.Type.LBRACE);

            if (!check(Token.Type.RBRACE)) {
                exp();

                while (check(Token.Type.COMMA)) {
                    match(Token.Type.COMMA);
                    exp();
                }
            }

            match(Token.Type.RBRACE);
        } else if (check(Token.Type.STRCON)) {
            match(Token.Type.STRCON);
        } else {
            exp();
        }
        addSyntaxComponent("InitVal");
    }

    /**
     * FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
     */
    private void parseFuncDef() {
        funcType();
        match(Token.Type.IDENFR);
        
        // 处理左括号，并记录行号用于错误检测
        int leftParenLine = currentToken != null ? currentToken.getLineNumber() : 0;
        boolean hasLeftParen = match(Token.Type.LPARENT);
        
        // 只有当遇到左括号时才解析参数
        if (hasLeftParen) {
            // 检查下一个token是否可能是函数参数的开始
            if (currentToken != null && 
                (currentToken.getType() == Token.Type.INTTK || 
                currentToken.getType() == Token.Type.CHARTK)) {
                parseFuncFParams();
            }
            
            // 检查右括号
            if (currentToken == null || currentToken.getType() != Token.Type.RPARENT) {
                // 错误：缺少右括号，错误类型j
                SimpleErrorHandler.addError(leftParenLine, "j");
            } else {
                match(Token.Type.RPARENT);
            }
        }
        
        // 即使有错误也尝试解析函数体
        if (currentToken != null && currentToken.getType() == Token.Type.LBRACE) {
            block();
        } else {
            // 如果没有函数体，尝试跳过到下一个合理的位置
            skipToNextFunctionOrDeclaration();
        }
        
        addSyntaxComponent("FuncDef");
    }

    /**
     * 尝试解析函数参数列表，如果遇到无效的语法则进行错误恢复
     */
    private void parseFuncFParams() {
        // 记录起始位置和深度，用于检测无限循环
        int startPos = position;
        int maxIterations = 100; // 防止无限循环
        int iterations = 0;
        
        try {
            // FuncFParams -> FuncFParam { ',' FuncFParam }
            parseFuncFParam();
            
            while (currentToken != null && 
                   currentToken.getType() == Token.Type.COMMA && 
                   iterations++ < maxIterations) {
                match(Token.Type.COMMA);
                
                // 检查下一个Token是否可能是参数的开始
                if (currentToken != null && 
                    (currentToken.getType() == Token.Type.INTTK || 
                     currentToken.getType() == Token.Type.CHARTK)) {
                    parseFuncFParam();
                } else {
                    // 遇到非参数开始的Token，跳到右括号或错误边界
                    skipToRightParenOrSemicolon();
                    break;
                }
            }
            
            // 检查是否可能陷入无限循环
            if (iterations >= maxIterations) {
                System.err.println("Warning: Too many iterations in parseFuncFParams, possible syntax error");
                skipToRightParenOrSemicolon();
            }
            
            // 在所有参数解析完毕后添加FuncFParams标签
            addSyntaxComponent("FuncFParams");
        } catch (Exception e) {
            System.err.println("Error in parseFuncFParams: " + e.getMessage());
            skipToRightParenOrSemicolon();
        }
    }

    /**
     * 跳过到右括号或分号，用于错误恢复
     */
    private void skipToRightParenOrSemicolon() {
        while (currentToken != null && 
               currentToken.getType() != Token.Type.RPARENT &&
               currentToken.getType() != Token.Type.SEMICN && 
               currentToken.getType() != Token.Type.LBRACE) {
            advance();
        }
    }

    /**
     * 跳过到下一个函数定义或声明的开始
     */
    private void skipToNextFunctionOrDeclaration() {
        while (currentToken != null) {
            Token.Type type = currentToken.getType();
            if (type == Token.Type.VOIDTK || 
                type == Token.Type.INTTK || 
                type == Token.Type.CHARTK || 
                type == Token.Type.CONSTTK) {
                // 找到可能的下一个函数或声明的开始
                break;
            }
            advance();
        }
    }

    /**
     * MainFuncDef -> 'int' 'main' '(' ')' Block
     */
    private void mainFuncDef() {
        match(Token.Type.INTTK);
        match(Token.Type.MAINTK);
        
        // 处理左括号并记录位置用于错误处理
        int lineNumber = currentToken != null ? currentToken.getLineNumber() : 0;
        boolean hasLeftParen = match(Token.Type.LPARENT);
        
        // 只有成功匹配左括号才检查右括号
        if (hasLeftParen) {
            // 检查右括号，如果找不到右括号则报告错误
            if (currentToken == null || currentToken.getType() != Token.Type.RPARENT) {
                // 错误：缺少右括号，错误类型j
                SimpleErrorHandler.addError(lineNumber, "j");
            } else {
                match(Token.Type.RPARENT);
            }
        }
        
        // 即使有错误也尝试解析函数体
        if (currentToken != null && currentToken.getType() == Token.Type.LBRACE) {
            block();
        } else {
            // 如果没有函数体，尝试跳过到下一个合理的位置
            skipToNextFunctionOrDeclaration();
        }
        
        addSyntaxComponent("MainFuncDef");
    }

    /**
     * FuncType -> 'void' | 'int' | 'char'
     */
    private void funcType() {
        if (check(Token.Type.VOIDTK)) {
            match(Token.Type.VOIDTK);
        } else if (check(Token.Type.INTTK)) {
            match(Token.Type.INTTK);
        } else if (check(Token.Type.CHARTK)) {
            match(Token.Type.CHARTK);
        }
        addSyntaxComponent("FuncType");
    }

    /**
     * 解析函数参数
     * FuncFParam -> BType Ident ['[' ']']
     */
    private void parseFuncFParam() {
        // 检查是否有有效的参数类型
        if (currentToken != null && 
            (currentToken.getType() == Token.Type.INTTK || 
             currentToken.getType() == Token.Type.CHARTK)) {
            bType(); // 解析参数类型
            match(Token.Type.IDENFR); // 解析参数名
            
            // 处理数组参数 ['[' ']']
            if (currentToken != null && currentToken.getType() == Token.Type.LBRACK) {
                match(Token.Type.LBRACK);
                int lineNumber = currentToken != null ? currentToken.getLineNumber() : 0;
                
                // 检查右中括号
                if (currentToken == null || currentToken.getType() != Token.Type.RBRACK) {
                    // 错误：缺少右中括号，错误类型k
                    SimpleErrorHandler.addError(lineNumber, "k");
                } else {
                    match(Token.Type.RBRACK);
                }
            }
            
            addSyntaxComponent("FuncFParam");
        } else {
            // 没有有效的参数类型，尝试跳过到下一个合理的位置
            skipToRightParenOrComma();
        }
    }

    /**
     * 跳过到右括号或逗号，用于参数解析中的错误恢复
     */
    private void skipToRightParenOrComma() {
        while (currentToken != null && 
               currentToken.getType() != Token.Type.RPARENT &&
               currentToken.getType() != Token.Type.COMMA) {
            advance();
        }
    }

    /**
     * Block -> '{' { BlockItem } '}'
     */
    private void block() {
        match(Token.Type.LBRACE);

        while (!check(Token.Type.RBRACE)) {
            blockItem();
        }

        match(Token.Type.RBRACE);
        addSyntaxComponent("Block");
    }

    /**
     * BlockItem -> Decl | Stmt
     */
    private void blockItem() {
        if (isDecl()) {
            decl();
        } else {
            stmt();
        }
    }

    /**
     * 判断当前是否是LVal的开始
     * 需要考虑数组元素的情况
     */
    private boolean isLValStart() {
        return check(Token.Type.IDENFR);
    }

    /**
     * 判断当前位置开始的token序列是否是一个数组访问
     * 用于在赋值语句中提前判断，不产生回溯
     */
    private boolean isArrayAccess() {
        // 已经确认当前是标识符，检查后面是否是 '['
        return position + 1 < tokens.size() && tokens.get(position + 1).getType() == Token.Type.LBRACK;
    }

    /**
     * 判断当前位置开始的token序列是否形成一个赋值语句
     */
    private boolean isAssignmentStatement() {
        if (!isLValStart()) {
            return false;
        }
        
        // 保存当前位置，用于回溯
        int startPosition = position;
        
        try {
            // 跳过标识符
            int pos = position + 1;
            
            // 如果是数组访问，跳过 [ Exp ]
            if (pos < tokens.size() && tokens.get(pos).getType() == Token.Type.LBRACK) {
                pos++; // 跳过 '['
                
                // 跳过表达式，直到找到 ']' 或 '=' 或 ';'
                // 注意：这里处理了缺少右中括号的情况
                while (pos < tokens.size()) {
                    Token.Type tokenType = tokens.get(pos).getType();
                    if (tokenType == Token.Type.RBRACK) {
                        pos++; // 找到了右中括号，前进一位
                        break;
                    } else if (tokenType == Token.Type.ASSIGN || tokenType == Token.Type.SEMICN) {
                        // 遇到赋值符号或分号，说明缺少右中括号，但仍可能是赋值语句
                        break;
                    }
                    pos++;
                }
            }
            
            // 检查下一个token是否是赋值符号
            return pos < tokens.size() && tokens.get(pos).getType() == Token.Type.ASSIGN;
        } finally {
            // 确保不修改原始位置
            position = startPosition;
        }
    }

    /**
     * Stmt -> LVal '=' Exp ';'
     *      | [Exp] ';'
     *      | Block
     *      | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     *      | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     *      | 'break' ';' | 'continue' ';'
     *      | 'return' [Exp] ';'
     *      | LVal '=' 'getint''('')'';'
     *      | LVal '=' 'getchar''('')'';'
     *      | 'printf''('StringConst{','Exp}')'';'
     */
    private void stmt() {
        if (check(Token.Type.LBRACE)) {
            // Block
            block();
        } else if (check(Token.Type.IFTK)) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            match(Token.Type.IFTK);
            match(Token.Type.LPARENT);
            
            int condLineNumber = currentToken.getLineNumber(); // 保存条件开始行号
            cond();
            
            // 检查右括号
            if (currentToken.getType() != Token.Type.RPARENT) {
                // 错误：缺少右括号，错误类型j
                SimpleErrorHandler.addError(condLineNumber, "j");
            } else {
                match(Token.Type.RPARENT);
            }
            
            stmt();

            if (check(Token.Type.ELSETK)) {
                match(Token.Type.ELSETK);
                stmt();
            }
        } else if (check(Token.Type.FORTK)) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            match(Token.Type.FORTK);
            match(Token.Type.LPARENT);

            if (!check(Token.Type.SEMICN)) {
                forStmt();
            }
            match(Token.Type.SEMICN);

            if (!check(Token.Type.SEMICN)) {
                cond();
            }
            match(Token.Type.SEMICN);

            if (!check(Token.Type.RPARENT)) {
                forStmt();
            }
            match(Token.Type.RPARENT);
            stmt();
        } else if (check(Token.Type.BREAKTK)) {
            // 'break' ';'
            // 获取break关键字行号
            int breakLineNumber = currentToken.getLineNumber();
            match(Token.Type.BREAKTK);
            
            // 检查分号
            if (currentToken.getType() != Token.Type.SEMICN) {
                // 错误：缺少分号，错误类型i
                SimpleErrorHandler.addError(breakLineNumber, "i");
            } else {
                match(Token.Type.SEMICN);
            }
        } else if (check(Token.Type.CONTINUETK)) {
            // 'continue' ';'
            // 获取continue关键字行号
            int continueLineNumber = currentToken.getLineNumber();
            match(Token.Type.CONTINUETK);
            
            // 检查分号
            if (currentToken.getType() != Token.Type.SEMICN) {
                // 错误：缺少分号，错误类型i
                SimpleErrorHandler.addError(continueLineNumber, "i");
            } else {
                match(Token.Type.SEMICN);
            }
        } else if (check(Token.Type.RETURNTK)) {
            // 'return' [Exp] ';'
            // 获取return关键字行号
            int returnLineNumber = currentToken.getLineNumber();
            match(Token.Type.RETURNTK);
            
            if (!check(Token.Type.SEMICN)) {
                exp();
            }
            
            // 检查分号
            if (currentToken.getType() != Token.Type.SEMICN) {
                // 错误：缺少分号，错误类型i
                SimpleErrorHandler.addError(returnLineNumber, "i");
            } else {
                match(Token.Type.SEMICN);
            }
        } else if (check(Token.Type.PRINTFTK)) {
            // 'printf''('StringConst{','Exp}')'';'
            // 获取printf关键字行号
            int printfLineNumber = currentToken.getLineNumber();
            match(Token.Type.PRINTFTK);
            match(Token.Type.LPARENT);
            match(Token.Type.STRCON);
            
            while (check(Token.Type.COMMA)) {
                match(Token.Type.COMMA);
                exp();
            }
            
            // 检查右括号
            if (currentToken.getType() != Token.Type.RPARENT) {
                // 错误：缺少右括号，错误类型j
                SimpleErrorHandler.addError(printfLineNumber, "j");
            } else {
                match(Token.Type.RPARENT);
            }
            
            // 处理分号 ';'
            if (currentToken.getType() != Token.Type.SEMICN) {
                // 错误：缺少分号，错误类型i
                SimpleErrorHandler.addError(printfLineNumber, "i");
            } else {
                match(Token.Type.SEMICN);
            }
        } else if (check(Token.Type.SEMICN)) {
            // 空语句，只有一个分号
            match(Token.Type.SEMICN);
        } else if (isAssignmentStatement()) {
            // 赋值语句: LVal '=' ...
            // 记录LVal开始的行号
            int lvalLineNumber = currentToken.getLineNumber();
            lVal();
            match(Token.Type.ASSIGN);
            
            if (check(Token.Type.GETINTTK)) {
                // LVal '=' 'getint''('')'';'
                match(Token.Type.GETINTTK);
                match(Token.Type.LPARENT);
                
                // 检查右括号
                if (currentToken.getType() != Token.Type.RPARENT) {
                    // 错误：缺少右括号，错误类型j
                    SimpleErrorHandler.addError(lvalLineNumber, "j");
                } else {
                    match(Token.Type.RPARENT);
                }
                
                // 检查分号
                if (currentToken.getType() != Token.Type.SEMICN) {
                    // 错误：缺少分号，错误类型i
                    SimpleErrorHandler.addError(lvalLineNumber, "i");
                } else {
                    match(Token.Type.SEMICN);
                }
            } else if (check(Token.Type.GETCHARTK)) {
                // LVal '=' 'getchar''('')'';'
                match(Token.Type.GETCHARTK);
                match(Token.Type.LPARENT);
                
                // 检查右括号
                if (currentToken.getType() != Token.Type.RPARENT) {
                    // 错误：缺少右括号，错误类型j
                    SimpleErrorHandler.addError(lvalLineNumber, "j");
                } else {
                    match(Token.Type.RPARENT);
                }
                
                // 检查分号
                if (currentToken.getType() != Token.Type.SEMICN) {
                    // 错误：缺少分号，错误类型i
                    SimpleErrorHandler.addError(lvalLineNumber, "i");
                } else {
                    match(Token.Type.SEMICN);
                }
            } else {
                // LVal '=' Exp ';'
                exp();
                
                // 检查分号
                if (currentToken.getType() != Token.Type.SEMICN) {
                    // 错误：缺少分号，错误类型i
                    SimpleErrorHandler.addError(lvalLineNumber, "i");
                } else {
                    match(Token.Type.SEMICN);
                }
            }
        } else {
            // 表达式语句 [Exp] ';'
            if (!check(Token.Type.SEMICN)) {
                // 获取表达式开始行号
                int expLineNumber = currentToken.getLineNumber();
                exp();
                
                // 检查分号
                if (currentToken.getType() != Token.Type.SEMICN) {
                    // 错误：缺少分号，错误类型i
                    SimpleErrorHandler.addError(expLineNumber, "i");
                } else {
                    match(Token.Type.SEMICN);
                }
            } else {
                // 空语句，只有一个分号
                match(Token.Type.SEMICN);
            }
        }
        addSyntaxComponent("Stmt");
    }

    /**
     * ForStmt -> LVal '=' Exp
     */
    private void forStmt() {
        lVal();
        match(Token.Type.ASSIGN);
        exp();
        addSyntaxComponent("ForStmt");
    }

    /**
     * Exp -> AddExp
     */
    private void exp() {
        try {
            checkRecursionDepth("exp");
            addExp();
            addSyntaxComponent("Exp");
        } catch (RuntimeException e) {
            System.err.println("Error processing expression: " + e.getMessage());
            // 尝试跳到表达式边界恢复
            skipToExpressionBoundary();
            addSyntaxComponent("Exp"); // 即使有错误也添加表达式组件
            throw e; // 重新抛出异常以便上层方法处理
        } finally {
            decreaseRecursionDepth();
        }
    }

    /**
     * Cond -> LOrExp
     */
    private void cond() {
        lOrExp();
        addSyntaxComponent("Cond");
    }

    /**
     * 判断当前是否是LVal
     */
    private boolean isLVal() {
        // Check if it's an identifier that's not followed by a left parenthesis (to exclude function calls)
        return check(Token.Type.IDENFR) && 
               (position + 1 >= tokens.size() || 
                tokens.get(position + 1).getType() != Token.Type.LPARENT);
    }

    /**
     * LVal -> Ident ['[' Exp ']']
     */
    private void lVal() {
        try {
            checkRecursionDepth("lVal");
            
            match(Token.Type.IDENFR);
            if (check(Token.Type.LBRACK)) {
                match(Token.Type.LBRACK);
                int expLineNumber = currentToken.getLineNumber();
                
                // 检查赋值情况 - 如果直接遇到赋值符号，说明缺少右中括号和表达式
                if (check(Token.Type.ASSIGN)) {
                    // 直接报告错误，跳过表达式解析
                    SimpleErrorHandler.addError(expLineNumber, "k");
                } else if (isValidExpStart()) {
                    // 正常解析表达式
                    try {
                        exp();
                    } catch (Exception e) {
                        // 防止递归解析异常导致崩溃
                        System.err.println("Error parsing expression in lVal: " + e.getMessage());
                        // 跳过直到找到右中括号或分号或赋值符号
                        while (position < tokens.size() && 
                              !check(Token.Type.RBRACK) && 
                              !check(Token.Type.SEMICN) && 
                              !check(Token.Type.ASSIGN) &&
                              !check(Token.Type.RBRACE)) {
                            advance();
                        }
                    }
                } else {
                    // 表达式无效，可能是错误的语法，尝试恢复
                    System.err.println("Invalid expression start in array access");
                    // 跳过直到找到右中括号或分号或赋值符号
                    while (position < tokens.size() && 
                          !check(Token.Type.RBRACK) && 
                          !check(Token.Type.SEMICN) && 
                          !check(Token.Type.ASSIGN) &&
                          !check(Token.Type.RBRACE)) {
                        advance();
                    }
                }
                
                // 检查右中括号
                if (!check(Token.Type.RBRACK)) {
                    // 错误：缺少右中括号，错误类型k
                    SimpleErrorHandler.addError(expLineNumber, "k");
                    // 不尝试匹配缺失的右中括号，继续解析
                } else {
                    match(Token.Type.RBRACK);
                }
            }
            addSyntaxComponent("LVal");
        } finally {
            decreaseRecursionDepth();
        }
    }

    /**
     * 检查当前是否是有效的表达式开始
     */
    private boolean isValidExpStart() {
        if (position >= tokens.size()) return false;
        
        Token.Type type = currentToken.getType();
        return type == Token.Type.PLUS || type == Token.Type.MINU || 
               type == Token.Type.NOT || type == Token.Type.IDENFR || 
               type == Token.Type.LPARENT || type == Token.Type.INTCON || 
               type == Token.Type.CHRCON;
    }

    /**
     * PrimaryExp -> '(' Exp ')' | LVal | Number | Character
     */
    private void primaryExp() {
        if (check(Token.Type.LPARENT)) {
            match(Token.Type.LPARENT);
            int expLineNumber = currentToken.getLineNumber();
            exp();
            
            // 检查右括号
            if (currentToken.getType() != Token.Type.RPARENT) {
                // 错误：缺少右括号，错误类型j
                SimpleErrorHandler.addError(expLineNumber, "j");
            } else {
                match(Token.Type.RPARENT);
            }
        } else if (isLVal()) {
            // 因为在lVal方法中已经输出了<LVal>，所以这里直接调用即可
            lVal();
        } else if (check(Token.Type.INTCON)) {
            number();
        } else if (check(Token.Type.CHRCON)) {
            character();
        }
        addSyntaxComponent("PrimaryExp");
    }

    /**
     * Number -> IntConst
     */
    private void number() {
        match(Token.Type.INTCON);
        addSyntaxComponent("Number");
    }

    /**
     * Character -> CharConst
     */
    private void character() {
        match(Token.Type.CHRCON);
        addSyntaxComponent("Character");
    }

    /**
     * UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     */
    private void unaryExp() {
        if (check(Token.Type.IDENFR) && position + 1 < tokens.size() && 
            tokens.get(position + 1).getType() == Token.Type.LPARENT) {
            // This is a function call - do not create an LVal
            match(Token.Type.IDENFR);
            match(Token.Type.LPARENT);
            
            int paramsLineNumber = currentToken.getLineNumber();
            if (!check(Token.Type.RPARENT)) {
                funcRParams();
            }
            
            // 检查右括号
            if (currentToken.getType() != Token.Type.RPARENT) {
                // 错误：缺少右括号，错误类型j
                SimpleErrorHandler.addError(paramsLineNumber, "j");
            } else {
                match(Token.Type.RPARENT);
            }
        } else if (check(Token.Type.PLUS) || check(Token.Type.MINU) || check(Token.Type.NOT)) {
            unaryOp();
            unaryExp();
        } else {
            primaryExp();
        }
        addSyntaxComponent("UnaryExp");
    }

    /**
     * UnaryOp -> '+' | '−' | '!'
     */
    private void unaryOp() {
        if (check(Token.Type.PLUS)) {
            match(Token.Type.PLUS);
        } else if (check(Token.Type.MINU)) {
            match(Token.Type.MINU);
        } else if (check(Token.Type.NOT)) {
            match(Token.Type.NOT);
        }
        addSyntaxComponent("UnaryOp");
    }

    /**
     * FuncRParams -> Exp { ',' Exp }
     */
    private void funcRParams() {
        exp();
        
        while (check(Token.Type.COMMA)) {
            match(Token.Type.COMMA);
            exp();
        }
        addSyntaxComponent("FuncRParams");
    }

    /**
     * MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     * 改写成右递归：
     * MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
     */
    private void mulExp() {
        try {
            checkRecursionDepth("mulExp");
            
            try {
                unaryExp();
                
                if (check(Token.Type.MULT) || check(Token.Type.DIV) || check(Token.Type.MOD)) {
                    addSyntaxComponent("MulExp");
                    
                    int loopCount = 0;
                    final int MAX_LOOP_COUNT = 50; // 防止无限循环
                    
                    while (check(Token.Type.MULT) || check(Token.Type.DIV) || check(Token.Type.MOD)) {
                        if (++loopCount > MAX_LOOP_COUNT) {
                            System.err.println("Warning: Excessive loop detected in mulExp, likely a syntax error");
                            break;
                        }
                        
                        if (check(Token.Type.MULT)) {
                            match(Token.Type.MULT);
                        } else if (check(Token.Type.DIV)) {
                            match(Token.Type.DIV);
                        } else {
                            match(Token.Type.MOD);
                        }
                        
                        try {
                            unaryExp();
                        } catch (Exception e) {
                            System.err.println("Error in unaryExp within mulExp: " + e.getMessage());
                            skipToExpressionBoundary();
                            break; // 停止循环，避免进一步尝试解析
                        }
                        
                        addSyntaxComponent("MulExp");
                    }
                } else {
                    addSyntaxComponent("MulExp");
                }
            } catch (Exception e) {
                System.err.println("Error in mulExp: " + e.getMessage());
                skipToExpressionBoundary();
                addSyntaxComponent("MulExp"); // 即使有错误也添加组件
            }
        } finally {
            decreaseRecursionDepth();
        }
    }

    /**
     * AddExp -> MulExp { ('+' | '−') MulExp }
     */
    private void addExp() {
        try {
            checkRecursionDepth("addExp");
            
            try {
                mulExp();
                addSyntaxComponent("AddExp");
                
                int loopCount = 0;
                final int MAX_LOOP_COUNT = 50; // 防止无限循环
                
                while (check(Token.Type.PLUS) || check(Token.Type.MINU)) {
                    if (++loopCount > MAX_LOOP_COUNT) {
                        System.err.println("Warning: Excessive loop detected in addExp, likely a syntax error");
                        break;
                    }
                    
                    if (check(Token.Type.PLUS)) {
                        match(Token.Type.PLUS);
                    } else {
                        match(Token.Type.MINU);
                    }
                    
                    try {
                        mulExp();
                    } catch (Exception e) {
                        System.err.println("Error in mulExp within addExp: " + e.getMessage());
                        skipToExpressionBoundary();
                        break; // 停止循环，避免进一步尝试解析
                    }
                    
                    addSyntaxComponent("AddExp");
                }
            } catch (Exception e) {
                System.err.println("Error in addExp: " + e.getMessage());
                // 尝试跳到表达式边界恢复
                skipToExpressionBoundary();
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
    
    /**
     * 跳过tokens直到找到表达式边界（分号、右括号、右中括号等）
     */
    private void skipToExpressionBoundary() {
        System.err.println("Attempting to recover from error at token: " + 
                          (currentToken != null ? currentToken.toString() : "null"));
        
        int startPosition = position;
        while (position < tokens.size() && 
               !check(Token.Type.SEMICN) && 
               !check(Token.Type.RPARENT) && 
               !check(Token.Type.RBRACK) && 
               !check(Token.Type.COMMA) &&
               !check(Token.Type.ASSIGN) &&
               !check(Token.Type.RBRACE)) {
            advance();
        }
        
        if (position > startPosition) {
            System.err.println("Skipped to token: " + 
                              (currentToken != null ? currentToken.toString() : "end of input"));
        }
    }

    /**
     * RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     * 改写成右递归：
     * RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
     */
    private void relExp() {
        addExp();
        addSyntaxComponent("RelExp");
        
        while (check(Token.Type.LSS) || check(Token.Type.GRE) || 
               check(Token.Type.LEQ) || check(Token.Type.GEQ)) {
            if (check(Token.Type.LSS)) {
                match(Token.Type.LSS);
            } else if (check(Token.Type.GRE)) {
                match(Token.Type.GRE);
            } else if (check(Token.Type.LEQ)) {
                match(Token.Type.LEQ);
            } else {
                match(Token.Type.GEQ);
            }
            addExp();
            addSyntaxComponent("RelExp");
        }
    }

    /**
     * EqExp -> RelExp | EqExp ('==' | '!=') RelExp
     * 改写成右递归：
     * EqExp -> RelExp { ('==' | '!=') RelExp }
     */
    private void eqExp() {
        relExp();
        addSyntaxComponent("EqExp");
        
        while (check(Token.Type.EQL) || check(Token.Type.NEQ)) {
            if (check(Token.Type.EQL)) {
                match(Token.Type.EQL);
            } else {
                match(Token.Type.NEQ);
            }
            relExp();
            addSyntaxComponent("EqExp");
        }
    }

    /**
     * LAndExp -> EqExp | LAndExp '&&' EqExp
     * 改写成右递归：
     * LAndExp -> EqExp { '&&' EqExp }
     */
    private void lAndExp() {
        eqExp();
        addSyntaxComponent("LAndExp");
        
        while (check(Token.Type.AND)) {
            match(Token.Type.AND);
            eqExp();
            addSyntaxComponent("LAndExp");
        }
    }

    /**
     * LOrExp -> LAndExp | LOrExp '||' LAndExp
     * 改写成右递归：
     * LOrExp -> LAndExp { '||' LAndExp }
     */
    private void lOrExp() {
        lAndExp();
        addSyntaxComponent("LOrExp");
        
        while (check(Token.Type.OR)) {
            match(Token.Type.OR);
            lAndExp();
            addSyntaxComponent("LOrExp");
        }
    }

    /**
     * ConstExp -> AddExp
     */
    private void constExp() {
        try {
            checkRecursionDepth("constExp");
            
            try {
                addExp();
                addSyntaxComponent("ConstExp");
            } catch (Exception e) {
                System.err.println("Error in constExp: " + e.getMessage());
                skipToExpressionBoundary();
                addSyntaxComponent("ConstExp"); // 即使有错误也添加组件
            }
        } finally {
            decreaseRecursionDepth();
        }
    }
} 