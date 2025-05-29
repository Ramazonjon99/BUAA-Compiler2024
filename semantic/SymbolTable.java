package semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class SymbolTable {
    private final Map<String, Symbol> symbols; // 当前作用域的符号
    private final SymbolTable parentScope;     // 父作用域
    private final int scopeId;                 // 当前作用域的ID
    private final List<Symbol> orderedSymbols; // 按声明顺序存储符号，用于输出

    public SymbolTable(SymbolTable parentScope, int scopeId) {
        this.symbols = new HashMap<>();
        this.parentScope = parentScope;
        this.scopeId = scopeId;
        this.orderedSymbols = new ArrayList<>();
    }

    /**
     * 向当前作用域添加符号。
     * @param symbol 要添加的符号
     * @return 如果成功添加（没有重定义）返回true，否则false
     */
    public boolean addSymbol(Symbol symbol) {
        if (symbols.containsKey(symbol.getName())) {
            // 错误b：名字重定义 (暂时不处理错误，但返回false表示失败)
            // ErrorHandler.addError(symbol.getLineNumber(), 'b');
            return false; 
        }
        symbols.put(symbol.getName(), symbol);
        orderedSymbols.add(symbol); // 保持插入顺序
        return true;
    }

    /**
     * 在当前作用域查找符号 (不查找父作用域)。
     * @param name 符号名称
     * @return 如果找到则返回符号，否则返回null
     */
    public Symbol lookupCurrentScope(String name) {
        return symbols.get(name);
    }

    /**
     * 查找符号，会从当前作用域开始，递归向上查找父作用域。
     * @param name 符号名称
     * @return 如果找到则返回符号，否则返回null
     */
    public Symbol lookup(String name) {
        Symbol symbol = lookupCurrentScope(name);
        if (symbol != null) {
            return symbol;
        }
        if (parentScope != null) {
            return parentScope.lookup(name);
        }
        return null; // 在所有作用域都未找到
    }

    public SymbolTable getParentScope() {
        return parentScope;
    }

    public int getScopeId() {
        return scopeId;
    }

    /**
     * 获取当前作用域内按声明顺序排列的符号列表。
     */
    public List<Symbol> getOrderedSymbols() {
        return orderedSymbols;
    }

    @Override
    public String toString() {
        return "SymbolTable{scopeId=" + scopeId + ", symbols=" + symbols.size() + "}";
    }
} 