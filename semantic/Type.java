package semantic;

public enum Type {
    // 基本类型 (用于变量和函数返回)
    INT("Int"),
    CHAR("Char"),
    VOID("Void"), // 用于函数返回类型

    // 符号的具体类型 (用于符号表条目)
    CONST_INT("ConstInt"),
    CONST_CHAR("ConstChar"),
    CONST_INT_ARRAY("ConstIntArray"),
    CONST_CHAR_ARRAY("ConstCharArray"),
    // INT, CHAR (已在上面定义，可复用)
    INT_ARRAY("IntArray"),
    CHAR_ARRAY("CharArray"),
    INT_FUNC("IntFunc"),
    CHAR_FUNC("CharFunc"),
    VOID_FUNC("VoidFunc"),

    UNKNOWN("Unknown"); // For error or uninitialized types

    private final String name;

    Type(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    // 辅助方法，从基本类型和是否常量/数组得到符号表中的具体类型
    public static Type getSymbolType(Type baseType, boolean isConst, boolean isArray) {
        if (isConst) {
            if (isArray) {
                if (baseType == INT) return CONST_INT_ARRAY;
                if (baseType == CHAR) return CONST_CHAR_ARRAY;
            } else {
                if (baseType == INT) return CONST_INT;
                if (baseType == CHAR) return CONST_CHAR;
            }
        } else {
            if (isArray) {
                if (baseType == INT) return INT_ARRAY;
                if (baseType == CHAR) return CHAR_ARRAY;
            } else {
                // 普通变量就是其基本类型
                return baseType; 
            }
        }
        return UNKNOWN; // 不应该发生
    }

    public static Type getFuncSymbolType(Type returnType) {
        if (returnType == INT) return INT_FUNC;
        if (returnType == CHAR) return CHAR_FUNC;
        if (returnType == VOID) return VOID_FUNC;
        return UNKNOWN; // 不应该发生
    }
} 