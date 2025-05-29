package utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 简化版错误处理类，用于收集和输出编译错误
 */
public class SimpleErrorHandler {
    // 错误记录类
    public static class ErrorRecord {
        private int line;
        private String code;
        
        public ErrorRecord(int line, String code) {
            this.line = line;
            this.code = code;
        }
        
        public int getLine() {
            return line;
        }
        
        public String getCode() {
            return code;
        }
        
        @Override
        public String toString() {
            return line + " " + code;
        }
    }
    
    // 存储错误的列表
    private static final List<ErrorRecord> errors = new ArrayList<>();
    
    /**
     * 添加一个错误
     */
    public static void addError(int line, String code) {
        errors.add(new ErrorRecord(line, code));
    }
    
    /**
     * 获取所有错误（按行号排序）
     */
    public static List<ErrorRecord> getErrors() {
        errors.sort(Comparator.comparingInt(ErrorRecord::getLine));
        return errors;
    }
    
    /**
     * 清空错误列表
     */
    public static void clearErrors() {
        errors.clear();
    }
    
    /**
     * 检查是否有错误
     */
    public static boolean hasErrors() {
        return !errors.isEmpty();
    }
} 