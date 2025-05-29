package semantic;

public abstract class Symbol {
    protected String name;         // 符号名称
    protected Type type;           // 符号的语义类型 (ConstInt, IntFunc, etc.)
    protected int scopeId;         // 作用域序号
    protected int lineNumber;      // 定义所在的行号 (用于错误处理和调试)
    protected boolean isArray;       // 是否是数组
    // 可以添加其他通用属性，例如维度信息等
    // protected List<Integer> dimensions; 

    public Symbol(String name, Type type, int scopeId, int lineNumber) {
        this.name = name;
        this.type = type;
        this.scopeId = scopeId;
        this.lineNumber = lineNumber;
        this.isArray = false; // 默认为非数组
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getScopeId() {
        return scopeId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    // 用于输出到 symbol.txt 的方法
    public String toSymbolTableEntry() {
        return scopeId + " " + name + " " + type.toString();
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", scopeId=" + scopeId +
                ", lineNumber=" + lineNumber +
                ", isArray=" + isArray +
                '}';
    }
}