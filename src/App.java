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
import analyser.Analyser;
import error.CompileError;
import instruction.Instruction;
import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;


public class App {
    public static void main(String[] args) throws CompileError, FileNotFoundException {
        File input = new File(args[1]);
        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);
        var tokens = new ArrayList<Token>();
        if(args[0].equals("--tokenize")){
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                    System.out.println(token.toString());
                }
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                System.err.println(e);
                System.exit(0);
                return;
            }
        }
        else if(args[0].equals("--analyse")){
            var analyzer = new Analyser(tokenizer);
            List<Instruction> instructions;
            analyzer.analyse();
        }

    }
    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
