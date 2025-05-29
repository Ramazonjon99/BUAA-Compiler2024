package utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ErrorHandler {
    public static class Error {
        private final int line;
        private final String code;
        
        public Error(int line, String code) {
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
    
    private static final List<Error> errors = new ArrayList<>();
    
    public static void addError(int line, String code) {
        errors.add(new Error(line, code));
    }
    
    public static List<Error> getErrors() {
        errors.sort(Comparator.comparingInt(Error::getLine));
        return errors;
    }
    
    public static void clearErrors() {
        errors.clear();
    }
    
    public static boolean hasErrors() {
        return !errors.isEmpty();
    }
}
