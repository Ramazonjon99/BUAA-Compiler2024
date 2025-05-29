import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import semantic.SemanticAnalyzer;
import utils.FileHandler;
import utils.SimpleErrorHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    public static void main(String[] args) {
        // 从命令行获取输入文件名，默认为 testfile.txt
        String sourceFile = "testfile.txt"; 
        if (args.length > 0) {
            sourceFile = args[0];
        }

        String outputFileLexer = "lexer.txt";
        String outputFileParser = "parser.txt";
        String outputFileSymbol = "symbol.txt";
        String outputFileError = "error.txt"; // 统一的错误输出文件

        // 清空之前的错误记录 (如果SimpleErrorHandler是静态累积的)
        SimpleErrorHandler.clearErrors();

        List<Token> tokens = null;
        try {
            // 1. 词法分析
            System.out.println("Starting Lexical Analysis...");
            String sourceCode = FileHandler.readFile(sourceFile);
            Lexer lexer = new Lexer(sourceCode);
            tokens = lexer.tokenize();
            System.out.println("Lexical Analysis Completed. Tokens: " + (tokens != null ? tokens.size() : 0));

            // 只有在没有词法错误时才输出lexer.txt (或根据你的评测要求)
            if (!SimpleErrorHandler.hasErrors()) {
                 FileHandler.writeTokensToFile(tokens, outputFileLexer);
                 System.out.println("Lexer output written to " + outputFileLexer);
            }

            // 2. 语法分析
            System.out.println("Starting Syntax Analysis...");
            // 创建Token列表的副本，以防Parser修改原始列表
            List<Token> tokensForParser = (tokens != null) ? new ArrayList<>(tokens) : new ArrayList<>();
            Parser parser = new Parser(tokensForParser);
            parser.parse();
            System.out.println("Syntax Analysis Completed.");

            // 只有在没有累积错误时才输出parser.txt (或根据你的评测要求)
            if (!SimpleErrorHandler.hasErrors() && parser.getOutput() != null) {
                 FileHandler.writeToFile(parser.getOutput(), outputFileParser);
                 System.out.println("Parser output written to " + outputFileParser);
            }

//            // 3. 语义分析
            System.out.println("Starting Semantic Analysis...");
            // 创建Token列表的副本给SemanticAnalyzer
            List<Token> tokensForSemantic = (tokens != null) ? new ArrayList<>(tokens) : new ArrayList<>();
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(tokensForSemantic);
            semanticAnalyzer.analyze();
            System.out.println("Semantic Analysis Completed.");
            // symbol.txt 的写入由SemanticAnalyzer内部完成

            // 4. 统一错误处理
            // 在所有阶段完成后，检查是否有错误，并写入error.txt
            if (SimpleErrorHandler.hasErrors()) {
                System.out.println("Errors found during compilation. Check " + outputFileError);
                FileHandler.writeErrorsToFile(outputFileError);
            } else {
                System.out.println("Compilation completed successfully. No errors found.");
                // 如果所有阶段都成功，并且没有错误，根据需要可以删除空的error.txt
                // 或者评测系统会自动处理
            }

        } catch (IOException e) {
            System.err.println("File I/O Error: " + e.getMessage());
            e.printStackTrace();
            // 发生IO错误时，也尝试写入已收集的错误（如果有）
            try {
                if (SimpleErrorHandler.hasErrors()) {
                    FileHandler.writeErrorsToFile(outputFileError);
                }
            } catch (IOException ex) {
                System.err.println("Could not write to error file after IO exception: " + ex.getMessage());
            }
        } catch (Exception e) {
            // 捕获其他可能的运行时异常
            System.err.println("An unexpected error occurred during compilation: " + e.getMessage());
            e.printStackTrace();
            try {
                if (SimpleErrorHandler.hasErrors()) {
                    SimpleErrorHandler.addError(0, "CRASH"); // 添加一个通用崩溃错误
                    FileHandler.writeErrorsToFile(outputFileError);
                } else {
                     // 如果没有其他错误，但程序崩溃了，至少要生成一个error.txt
                    SimpleErrorHandler.addError(0, "CRASH"); 
                    FileHandler.writeErrorsToFile(outputFileError);
                }
            } catch (IOException ex) {
                System.err.println("Could not write to error file after runtime exception: " + ex.getMessage());
            }
        }
    }
} 