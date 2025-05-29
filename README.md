# BUAA-Compiler2024
# 编译原理实验项目

## 项目简介

本项目实现了一个简单的编译器前端，包含词法分析、语法分析和语义分析三个阶段。该编译器可以处理一种类C语言，支持基本语法结构如变量声明、函数定义、条件语句和循环等。



### 词法分析

#### 单词类别码表

| 单词名称 | 类别码 | 单词名称 | 类别码 | 单词名称 | 类别码 | 单词名称 | 类别码 |
|---------|-------|---------|-------|---------|-------|---------|-------|
| Ident | IDENFR | else | ELSETK | void | VOIDTK | ; | SEMICN |
| IntConst | INTCON | ! | NOT | * | MULT | , | COMMA |
| StringConst | STRCON | && | AND | / | DIV | ( | LPARENT |
| CharConst | CHRCON | \|\| | OR | % | MOD | ) | RPARENT |
| main | MAINTK | for | FORTK | < | LSS | [ | LBRACK |
| const | CONSTTK | getint | GETINTTK | <= | LEQ | ] | RBRACK |
| int | INTTK | getchar | GETCHARTK | > | GRE | { | LBRACE |
| char | CHARTK | printf | PRINTFTK | >= | GEQ | } | RBRACE |
| break | BREAKTK | return | RETURNTK | == | EQL | | |
| continue | CONTINUETK | + | PLUS | != | NEQ | | |
| if | IFTK | - | MINU | = | ASSIGN | | |

#### 词法分析可能出现的错误

| 错误类型 | 错误码 | 描述 |
|---------|-------|------|
| 逻辑与表达式错误 | a | 逻辑与表达式 LAndExp → EqExp \| LAndExp '&&' EqExp |
| 逻辑或表达式错误 | a | 逻辑或表达式 LOrExp → LAndExp \| LOrExp '\|\|' LAndExp |

#### 词法分析样例

##### 正确源程序样例

**样例输入**：
```
const int array[2] = {1,2};

int main(){
    int c;
    c = getint();
    printf("output is %d",c);
    return c;
}
```

**样例输出**：
```
CONSTTK const
INTTK int
IDENFR array
LBRACK [
INTCON 2
RBRACK ]
ASSIGN =
LBRACE {
INTCON 1
COMMA ,
INTCON 2
RBRACE }
SEMICN ;
INTTK int
MAINTK main
LPARENT (
RPARENT )
LBRACE {
INTTK int
IDENFR c
SEMICN ;
IDENFR c
ASSIGN =
GETINTTK getint
LPARENT (
RPARENT )
SEMICN ;
PRINTFTK printf
LPARENT (
STRCON "output is %d"
COMMA ,
IDENFR c
RPARENT )
SEMICN ;
RETURNTK return
IDENFR c
SEMICN ;
RBRACE }
```

##### 错误源程序样例

**样例输入**：
```
int main(){
    if(1 & 2){
        printf("2024 Compiler\n");
    }
    return 0;
}
```

**样例输出**：
```
2 a
```

### 语法分析

语法分析器基于递归下降法，能够：
- 按照给定文法分析语法成分
- 构建语法树
- 输出识别的语法成分和词法单元
- 检测并报告语法错误

#### 语法分析的语法规则

```
编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef 
声明 Decl → ConstDecl | VarDecl 
常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' // i
基本类型 BType → 'int' | 'char' 
常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
变量声明 VarDecl → BType VarDef { ',' VarDef } ';' // i
变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal // k
变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst 
函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block // j
主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block // j
函数类型 FuncType → 'void' | 'int' | 'char'
函数形参表 FuncFParams → FuncFParam { ',' FuncFParam } 
函数形参 FuncFParam → BType Ident ['[' ']'] // k
语句块 Block → '{' { BlockItem } '}' 
语句块项 BlockItem → Decl | Stmt 
语句 Stmt → LVal '=' Exp ';' // i
| [Exp] ';' // i
| Block
| 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
| 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt 
| 'break' ';' | 'continue' ';' // i
| 'return' [Exp] ';' // i
| LVal '=' 'getint''('')'';' // i j
| LVal '=' 'getchar''('')'';' // i j
| 'printf''('StringConst {','Exp}')'';' // i j
语句 ForStmt → LVal '=' Exp 
表达式 Exp → AddExp 
条件表达式 Cond → LOrExp 
左值表达式 LVal → Ident ['[' Exp ']'] // k
基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number | Character// j
数值 Number → IntConst 
字符 Character → CharConst 
一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp // j
单目运算符 UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中 
函数实参表 FuncRParams → Exp { ',' Exp } 
乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp 
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp 
关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp 
相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp 
逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
常量表达式 ConstExp → AddExp 注：使用的 Ident 必须是常量
```

#### 语法分析可能出现的错误

| 错误类型 | 错误码 | 描述 |
|---------|-------|------|
| 缺少分号 | i | 常量声明、变量声明、语句等末尾缺少分号 |
| 函数定义错误 | j | 函数定义、主函数定义中的语法错误 |
| 数组定义错误 | k | 常量定义、变量定义、函数形参中数组相关的语法错误 |

#### 语法分析样例

##### 正确源程序样例

**样例输入**：
```
int main(){
    int c;
    c= getint();
    printf("%d",c);
    return c;
}
```

**样例输出**：
```
INTTK int
MAINTK main
LPARENT (
RPARENT )
LBRACE {
INTTK int
IDENFR c
<VarDef>
SEMICN ;
<VarDecl>
IDENFR c
<LVal>
ASSIGN =
GETINTTK getint
LPARENT (
RPARENT )
SEMICN ;
<Stmt>
PRINTFTK printf
LPARENT (
STRCON "%d"
COMMA ,
IDENFR c
RPARENT )
SEMICN ;
<Stmt>
RETURNTK return
IDENFR c
<LVal>
<PrimaryExp>
<UnaryExp>
<MulExp>
<AddExp>
<Exp>
SEMICN ;
<Stmt>
RBRACE }
<Block>
<MainFuncDef>
<CompUnit>
```

##### 错误源程序样例

**样例输入**：
```
int main(){
    char c;
    c = getchar()
    if(1 & 2){
        printf("output is %c\n", c);
    }
    return 0;
}
```

**样例输出**：
```
3 i
4 a
```

### 语义分析


#### 语义分析类型表

| 类型 | 类型名称 | 类型 | 类型名称 | 类型 | 类型名称 |
|-----|---------|-----|---------|-----|---------|
| char型常量 | ConstChar | char型变量 | Char | void型函数 | VoidFunc |
| int型常量 | ConstInt | int型变量 | Int | char型函数 | CharFunc |
| char型常量数组 | ConstCharArray | char型变量数组 | CharArray | int型函数 | IntFunc |
| int型常量数组 | ConstIntArray | int型变量数组 | IntArray | | |

#### 语义分析的语法规则

```
编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef 
声明 Decl → ConstDecl | VarDecl 
常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' 
基本类型 BType → 'int' | 'char' 
常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal // b
常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
变量声明 VarDecl → BType VarDef { ',' VarDef } ';' 
变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal // b
变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst 
函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block // b g
主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block // g
函数类型 FuncType → 'void' | 'int' | 'char'
函数形参表 FuncFParams → FuncFParam { ',' FuncFParam } 
函数形参 FuncFParam → BType Ident ['[' ']'] // b
语句块 Block → '{' { BlockItem } '}' 
语句块项 BlockItem → Decl | Stmt 
语句 Stmt → LVal '=' Exp ';' // h
| [Exp] ';' 
| Block
| 'if' '(' Cond ')' Stmt [ 'else' Stmt ] 
| 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt // h
| 'break' ';' | 'continue' ';' // m
| 'return' [Exp] ';' // f
| LVal '=' 'getint''('')'';' // h
| LVal '=' 'getchar''('')'';' // h
| 'printf''('StringConst {','Exp}')'';' // l
语句 ForStmt → LVal '=' Exp // h
表达式 Exp → AddExp 
条件表达式 Cond → LOrExp 
左值表达式 LVal → Ident ['[' Exp ']'] // c
基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number | Character
数值 Number → IntConst 
字符 Character → CharConst 
一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp // c d e
单目运算符 UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中 
函数实参表 FuncRParams → Exp { ',' Exp } 
乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp 
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp 
关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp 
相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp 
逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp 
逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp 
常量表达式 ConstExp → AddExp 注：使用的 Ident 必须是常量
```

#### 语义分析错误类型说明

| 错误类型 | 错误码 | 描述 |
|---------|-------|------|
| 名字重定义 | b | 常量定义、变量定义、函数定义、函数形参中的名字重定义 |
| 未定义的名字 | c | 左值表达式、一元表达式中使用未定义的名字 |
| 函数参数数量不匹配 | d | 一元表达式中函数调用的参数数量不匹配 |
| 函数参数类型不匹配 | e | 一元表达式中函数调用的参数类型不匹配 |
| 无返回值函数存在不匹配的return | f | return语句与函数声明的返回类型不匹配 |
| 有返回值函数缺少return语句 | g | 函数定义、主函数定义中缺少return语句 |
| 不能改变常量的值 | h | 赋值语句、for语句中试图改变常量的值 |
| 缺少printf参数 | l | printf语句中的参数数量与格式串不匹配 |
| 在非循环中使用break/continue | m | 在非循环语句中使用break或continue语句 |

#### 语义分析样例

##### 正确源程序样例

**样例输入**：
```
const int year = 2024, month = 9;
int day;

int getDay(){
    int day;
    day = getint();
    return day;
}

void putString(char s[], int length){
    int i = 0;
    for(;i < length; i = i + 1){
        printf("%c", s[i]);
    }
    printf("\n");
}

int strlen(char s[]){
    int i = 0;
    for(; s[i] != '\0'; i = i + 1) ;
    return i;
}

char charAt(char s[], int index){
    return s[index];
}

int main(){
    day = getDay();
    printf("Tody is %d-%d-%d\n", year, month, day);

    char s[12] = "hello world";
    {
        int length = strlen(s);
        if(length > 4){
            char tmp = charAt(s, 4);
        }
    }

    return 0;
}
```

**样例输出**：
```
1 year ConstInt
1 month ConstInt
1 day Int
1 getDay IntFunc
1 putString VoidFunc
1 strlen IntFunc
1 charAt CharFunc
2 day Int
3 s CharArray
3 length Int
3 i Int
5 s CharArray
5 i Int
6 s CharArray
6 index Int
7 s CharArray
8 length Int
9 tmp Char
```

##### 错误源程序样例

**样例输入**：
```
const int const1 = 1, const2 = -100;
int change1;
int gets1(int var1,int var2){
   const1 = 999;
   change1 = var1 + var2          return (change1);
}
int main(){
   change1 = 10;
   printf("Hello World$");
   return 0;
}
```

**样例输出**：
```
4 h
5 i
```

## 测试结果

### 词法分析
- 测试用例：15个
- 通过率：100%
### 语法分析
- 测试用例：20个
- 通过率：100%
### 语义分析
- 测试用例：26个
- 通过率：100%


## 个人收获
对了没错！全都是用cursor做的。
