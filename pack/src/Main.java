import oldlexer.Token;
import oldlexer.TokenAnalysis;
import parser.LeftNode;
import parser.Node;
import parser.Parser;
import parser.RightNode;

import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Wang keLong
 * @DateTime: 19:53 2021/3/23
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String FilePath = "test2.txt";
        TokenAnalysis ta = new TokenAnalysis(FilePath);
        ta.tokenAnalysis();
        ta.PrintResult(ta.getResult());
        boolean a = false;
        Parser parser;
        if (a) {
            parser = new Parser("grammar.txt");
        } else {
            parser = new Parser(
                    "grammar.txt",
                    "analysisTable.txt",
                    false);
        }
        if (ta.getError().size() > 0) {
            ta.PrintResult(ta.getError());
            throw new Exception("请先处理词法错误!");
        } else {
            List<RightNode> lexerResult = parser.transFromOldLexer(ta.getResult());
            List<LeftNode> nodes = parser.grammarAnalysis(lexerResult);
            if (!parser.isError()) {
                Node root = parser.getAnalTree(nodes);
                System.out.println(parser.printAnalysisTree(root));
            }
        }
    }
}


