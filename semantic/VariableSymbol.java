package semantic;

public class VariableSymbol extends Symbol {
    private boolean isConst; // 是否是常量
    // 如果需要，可以添加更多变量特有的属性，比如维度信息等
    // private int dimension; // 0 for non-array, 1 for 1D array, etc.

    public VariableSymbol(String name, Type type, int scopeId, int lineNumber, boolean isConst) {
        super(name, type, scopeId, lineNumber);
        this.isConst = isConst;
        // 根据 type 更新 isArray 状态
        if (type == Type.CONST_CHAR_ARRAY || type == Type.CONST_INT_ARRAY || 
            type == Type.CHAR_ARRAY || type == Type.INT_ARRAY) {
            super.setArray(true);
        }
    }

    public boolean isConst() {
        return isConst;
    }

    // 如果添加了 dimension 属性，需要对应的 getter/setter
    // public int getDimension() {
    //     return dimension;
    // }

    // public void setDimension(int dimension) {
    //     this.dimension = dimension;
    // }

    @Override
    public String toString() {
        return "VariableSymbol{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", scopeId=" + scopeId +
                ", lineNumber=" + lineNumber +
                ", isConst=" + isConst +
                ", isArray=" + isArray +
                '}';
    }
} 