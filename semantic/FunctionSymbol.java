package semantic;

import java.util.ArrayList;
import java.util.List;

public class FunctionSymbol extends Symbol {
    private Type returnType; // 函数的实际返回类型 (VOID, INT, CHAR)
    private List<VariableSymbol> parameters; // 形参列表

    public FunctionSymbol(String name, Type type, Type returnType, int scopeId, int lineNumber) {
        super(name, type, scopeId, lineNumber); // type 会是 VOID_FUNC, INT_FUNC, CHAR_FUNC
        this.returnType = returnType;
        this.parameters = new ArrayList<>();
        // 函数本身不是数组
        super.setArray(false); 
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<VariableSymbol> getParameters() {
        return parameters;
    }

    public void addParameter(VariableSymbol param) {
        this.parameters.add(param);
    }

    @Override
    public String toString() {
        return "FunctionSymbol{" +
                "name='" + name + '\'' +
                ", type=" + type + // This should be IntFunc, CharFunc, or VoidFunc
                ", returnType=" + returnType +
                ", scopeId=" + scopeId +
                ", lineNumber=" + lineNumber +
                ", parameters=" + parameters.size() + " params" +
                '}';
    }
} 