package utils;

import lexer.Token;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * 文件处理工具类，用于源文件的读取和结果的写入
 */
public class FileHandler {
    
    /**
     * 读取源文件内容
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException 如果文件读取错误
     */
    public static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * 将词法分析结果写入文件
     * @param tokens 词法单元列表
     * @param filePath 输出文件路径
     * @throws IOException 如果文件写入错误
     */
    public static void writeTokensToFile(List<Token> tokens, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Token token : tokens) {
                writer.write(token.toString());
                writer.newLine();
            }
        }
    }
    
    /**
     * 将文本列表写入文件
     * @param lines 要写入的文本列表
     * @param filePath 输出文件路径
     * @throws IOException 如果文件写入错误
     */
    public static void writeToFile(List<String> lines, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
    
    /**
     * 将错误信息写入文件
     * @param filePath 输出文件路径
     * @throws IOException 如果文件写入错误
     */
    public static void writeErrorsToFile(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            List<SimpleErrorHandler.ErrorRecord> errors = SimpleErrorHandler.getErrors();
            for (SimpleErrorHandler.ErrorRecord error : errors) {
                writer.write(error.toString());
                writer.newLine();
            }
        }
    }
} 