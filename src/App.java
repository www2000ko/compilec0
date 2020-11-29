import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//import analyser.Analyser;
import error.CompileError;
import instruction.Instruction;
import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;


public class App {
    public static void main(String[] args) throws CompileError, FileNotFoundException {
        File input = new File(args[0]);
        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);
        var tokens = new ArrayList<Token>();
        try {
            while (true) {
                var token = tokenizer.nextToken();
                if (token.getTokenType().equals(TokenType.EOF)) {
                    break;
                }
                tokens.add(token);
            }
        } catch (Exception e) {
            // 遇到错误不输出，直接退出
            System.err.println(e);
            System.exit(0);
            return;
        }
        for (Token token : tokens) {
            System.out.println(token.toString());
        }
    }
    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
