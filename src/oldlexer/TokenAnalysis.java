package oldlexer;

import java.io.*;
import java.util.*;


/**
 * @Author: Wang keLong
 * @DateTime: 19:32 2021/3/23
 */
public class TokenAnalysis {
    private File file;  //文件
    private InputStream input = null;  //输入流，用来读取文件内容
    private List<Token> result= new ArrayList<>();
    private List<Token> error =new ArrayList<>();

    private TokenTable tokenTable = new TokenTable(); //符号表

    public TokenAnalysis(String FilePath) {
        this.file = new File(FilePath);
    }

    //    对一个txt文件进行词法分析，并输出词法分析的结果

    public List<Token> getResult() {
        return result;
    }

    public List<Token> getError() {
        return error;
    }

    /**
     * forward 当前位置(0~2*buffer_size-1)
     */
    public void tokenAnalysis() throws IOException {
        int buffer_size = 1024;
        int[] real_size = {0, 0};
        byte[] buffer1 = new byte[buffer_size];
        byte[] buffer2 = new byte[buffer_size];

        try { //获取文件读入流
            input = new FileInputStream(this.file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int begin = 0;  //指明识别的token的位置和内容,范围在0~2*buffer_size之间变化
        int forward = 0;
        int cur_line = 1;
        real_size[0] = input.read(buffer1);
        char cur;
        try { //读取文件中1024个字节
            while (true) {
                //进行token识别
                forward = begin;
                //退出条件 1.有一个real_size=-1说明正好读完了 2.有一个buffer没有读满，说明已经到头了
                if (real_size[0] == -1 || real_size[1] == -1) {
                    break;
                }
                cur = getChar(buffer1, buffer2, real_size, forward, input);
                if (cur == '\u0000') {
                    break;
                }
                if (isAlpha(cur) || isUnderLine(cur)) {
                    forward = PointerPlus(forward, buffer_size);
                    while (true) {
                        cur = getChar(buffer1, buffer2, real_size, forward, input);
                        if (isAlNum(cur) || isUnderLine(cur)) {
                            forward = PointerPlus(forward, buffer_size);
                        } else if (isWordEnd(cur)) {
                            forward = PointerSubtract(forward, buffer_size);
                            // 判断结果
                            String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                            Token token = new Token(Install_Id(tokenContent), tokenContent,cur_line);
                            result.add(token);
                            break;
                        } else {
                            //报错
                            String curContent = getSubString(buffer1, buffer2, begin, forward);
                            error = errorHandle(cur_line, error, "wrong identifier in [ " + curContent + " ]");
                            break;
                        }
                    }
//                    begin = (forward + 1) % (buffer_size * 2);  //更改begin到一个新的位置
                } else { // 数字或者是其他token
                    if (isDigit(cur)) { //以数字开头的token(无符号10进制/无符号8进制/无符号16进制)
                        if (cur == '0') { //可能是八进制或者十六进制
                            forward = PointerPlus(forward, buffer_size);
                            cur = getChar(buffer1, buffer2, real_size, forward, input);
                            if (cur == 'x') { //16进制
                                do {
                                    forward = PointerPlus(forward, buffer_size);
                                    cur = getChar(buffer1, buffer2, real_size, forward, input);
                                } while (isHex(cur));
                                //判断结果处理
                                if (isNumEnd(cur)) {
                                    forward = PointerSubtract(forward, buffer_size);
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    Token token = new Token(tokenTable.HEX, tokenContent,cur_line);
                                    result.add(token);
                                } else {
                                    //错误处理
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    error = DigitErrorHandle(cur_line, error, tokenContent);
//                                    return result;
                                }
                            } else if (isOCTAL(cur)) {  //八进制
                                do {
                                    forward = PointerPlus(forward, buffer_size);
                                    cur = getChar(buffer1, buffer2, real_size, forward, input);
                                } while (isOCTAL(cur));
                                //判断结果处理
                                if (isNumEnd(cur)) {
                                    forward = PointerSubtract(forward, buffer_size);
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    Token token = new Token(tokenTable.OCTAL, tokenContent,cur_line);
                                    result.add(token);
                                } else {
                                    //错误处理
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    error = DigitErrorHandle(cur_line, error, tokenContent);
//                                    return result;
                                }
                            } else { //单纯的一个0
                                //判断结果处理
                                if (isNumEnd(cur)) {
                                    forward = PointerSubtract(forward, buffer_size);
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    Token token = new Token(tokenTable.INTEGER, tokenContent,cur_line);
                                    result.add(token);
                                } else if (cur == '.' || cur == 'e' || cur == 'E') {
                                    forward = PointerSubtract(forward, buffer_size);
                                    cur = getChar(buffer1, buffer2, real_size, forward, input);
                                } else {
                                    //错误处理
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    error = DigitErrorHandle(cur_line, error, tokenContent);
//                                    return result;
                                }
                            }
                        }
                        if (isDigit(cur)) { // 第一次字符是数字，十进制数字
                            do {
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                            } while (isDigit(cur));
                            if (isNumEnd(cur)) {
                                forward = PointerSubtract(forward, buffer_size);
                                String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                Token token = new Token(tokenTable.INTEGER, tokenContent,cur_line);
                                result.add(token);
                            }
                            if (cur == '.') {
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (isDigit(cur)) {
                                    do {
                                        forward = PointerPlus(forward, buffer_size);
                                        cur = getChar(buffer1, buffer2, real_size, forward, input);
                                    } while (isDigit(cur));
                                    if (cur == 'e' || cur == 'E') {

                                    } else if (isNumEnd(cur)) {
                                        forward = PointerSubtract(forward, buffer_size);
                                        String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                        Token token = new Token(tokenTable.REAL, tokenContent,cur_line);
                                        result.add(token);
                                    } else {
                                        //错误处理
                                        String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                        error = DigitErrorHandle(cur_line, error, tokenContent);
//                                    return result;
                                    }
                                } else {
                                    //错误处理
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    error = DigitErrorHandle(cur_line, error, tokenContent);
//                                    return result;
                                }
                            }
                            //判断科学计数法
                            if (cur == 'e' || cur == 'E') {
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '+' || cur == '-') {
                                    forward = PointerPlus(forward, buffer_size);
                                    cur = getChar(buffer1, buffer2, real_size, forward, input);
                                }
                                if (isDigit(cur)) {
                                    do {
                                        forward = PointerPlus(forward, buffer_size);
                                        cur = getChar(buffer1, buffer2, real_size, forward, input);
                                    } while (isDigit(cur));
                                    if (isNumEnd(cur)) {
                                        forward = PointerSubtract(forward, buffer_size);
                                        String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                        Token token = new Token(tokenTable.SCINUM, tokenContent,cur_line);
                                        result.add(token);
                                    } else {
                                        //错误处理
                                        String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                        error = DigitErrorHandle(cur_line, error, tokenContent);
//                                              return result;
                                    }
                                } else {
                                    //错误处理
                                    String tokenContent = getSubString(buffer1, buffer2, begin, forward);
                                    error = DigitErrorHandle(cur_line, error, tokenContent);
//                                          return result;
                                }
                            }

//                            // 更改begin到一个新的位置
//                            begin = (forward + 1) % (buffer_size * 2);
                        }
                    } else {  //其他符号
                        String tokens = null;
                        Token token = null;
                        switch (cur) {
                            case '+':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '=') {  //+=
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.PLUSEQUAL, tokens,cur_line);
                                    result.add(token);
                                } else if (cur == '+') {//++
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.PLUSPLUS, tokens,cur_line);
                                    result.add(token);
                                } else {//+
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.PLUS, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '-':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '=') {  //-=
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.SUBTRACTEQUAL, tokens,cur_line);
                                    result.add(token);
                                } else if (cur == '-') {//--
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.SUBSUB, tokens,cur_line);
                                    result.add(token);
                                } else {//-
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.SUBTRACT, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '*':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '*') { //**
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.INDEX, tokens,cur_line);
                                    result.add(token);
                                } else {
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.MULTI, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '/':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '*') { //注释
                                    char next_cur = getChar(buffer1, buffer2, real_size, forward + 1, input);
                                    do {
                                        cur = next_cur;
                                        forward = PointerPlus(forward, buffer_size);
                                        next_cur = getChar(buffer1, buffer2, real_size, forward + 1, input);
                                        if (cur == '\n') {
                                            cur_line++;
                                        }
                                    } while (!((cur == '*' && next_cur == '/') || cur == '\u0000'));  //或者文件结束的时候
                                    forward = PointerPlus(forward, buffer_size);
                                } else {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.DIVIDE, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '%':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.REMAINDER, tokens,cur_line);
                                result.add(token);
                                break;
                            case '=':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '=') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.EQUAL, tokens,cur_line);
                                    result.add(token);
                                } else {
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.ASSIGN, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '>':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '=') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.GE, tokens,cur_line);
                                    result.add(token);
                                } else {
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.GREAT, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '<':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '=') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.LE, tokens,cur_line);
                                    result.add(token);
                                } else {
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.LESS, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '!':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '=') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.NE, tokens,cur_line);
                                    result.add(token);
                                } else {
                                    forward = PointerSubtract(forward, buffer_size);
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.NOT, tokens,cur_line);
                                    result.add(token);
                                }
                                break;
                            case '&':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '&') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.AND, tokens,cur_line);
                                    result.add(token);
                                } else { //只单独出现了一个&
                                    //可能的出错处理
                                    forward = PointerSubtract(forward, buffer_size);
                                    String content = "Lexical error at line [" + cur_line + "]:" + "不允许单独出现一个&";
                                    error.add(errorHandle(content,cur_line));
                                }
                                break;
                            case '|':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '|') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.OR, tokens,cur_line);
                                    result.add(token);
                                } else { //只单独出现了一个|
                                    //可能的出错处理
                                    forward = PointerSubtract(forward, buffer_size);
                                    String content = "Lexical error at line [" + cur_line + "]:" + "不允许单独出现一个|";
                                    error.add(errorHandle(content,cur_line));
                                }
                                break;
                            case ',':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.COMMA, tokens,cur_line);
                                result.add(token);
                                break;
                            case ';':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.SEMICOLON, tokens,cur_line);
                                result.add(token);
                                break;
                            case ':':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.COLON, tokens,cur_line);
                                result.add(token);
                                break;
//                            case '.':
//                                tokens = getSubString(buffer1, buffer2, begin, forward);
//                                token = new oldlexer.Token(tokenTable.POINT, tokens);
//                                result.add(token);
//                                break;
                            case '(':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.LEFTS, tokens,cur_line);
                                result.add(token);
                                break;
                            case ')':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.RIGHTS, tokens,cur_line);
                                result.add(token);
                                break;
                            case '[':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.LEFTM, tokens,cur_line);
                                result.add(token);
                                break;
                            case ']':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.RIGHTM, tokens,cur_line);
                                result.add(token);
                                break;
                            case '{':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.LEFTL, tokens,cur_line);
                                result.add(token);
                                break;
                            case '}':
                                tokens = getSubString(buffer1, buffer2, begin, forward);
                                token = new Token(tokenTable.RIGHTL, tokens,cur_line);
                                result.add(token);
                                break;
                            case '\'':
                                forward = PointerPlus(forward, buffer_size);
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '\\') { //第二个字符
                                    do {
                                        forward = PointerPlus(forward, buffer_size);
                                        cur = getChar(buffer1, buffer2, real_size, forward, input);
                                    }
                                    while (cur != '\'');
//                                    forward = PointerPlus(forward, buffer_size);
                                } else {
                                    forward = PointerPlus(forward, buffer_size);
                                }
                                cur = getChar(buffer1, buffer2, real_size, forward, input);
                                if (cur == '\'') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.CHARACTER, tokens,cur_line);
                                    result.add(token);
                                } else { //一对单引号内容过多或者只出现一个单引号
                                    //错误处理
                                    result = errorHandle(cur_line, result, "wrong char format");
                                }
                                break;
                            case '"':
                                do {
                                    forward = PointerPlus(forward, buffer_size);
                                    cur = getChar(buffer1, buffer2, real_size, forward, input);
                                    if (cur == '\n') {
                                        cur_line++;
                                    }
                                } while (!(cur == '"' || cur == '\u0000'));
                                if (cur == '"') {
                                    tokens = getSubString(buffer1, buffer2, begin, forward);
                                    token = new Token(tokenTable.STRING, tokens,cur_line);
                                    result.add(token);
                                } else {
                                    result = errorHandle(cur_line, result, "wrong string format");
                                }
                                break;
                            case '\n':  //得到行号用于错误信息处理
                                cur_line++;
                                break;
                            case '\t':  //其他空格符直接跳过
                            case '\r':
                            case ' ':
                                break;
                            default: //未知符号
                                tokens = "Lexical error at line [" + cur_line + "]:" + "[ " + getSubString(buffer1, buffer2, begin, forward) + " ] is unknown.";
                                token = new Token(tokenTable.UNKNOWN, tokens,cur_line);
                                error.add(token);
                                break;
                        }

                    }
                }
                // 更改begin到一个新的位置
                begin = (forward + 1) % (buffer_size * 2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 有/无符号整数.无符号整数整数 = 小数
     * 有/无整数e有/无整数 =sciNum
     * 数组识别 []
     */
    public List<Token> tokenComposite(List<Token> oldResult) {
        int begin = 0;
        int forward = 0;
        Token cur;
        List<Token> result = new ArrayList<>();
        while (begin < oldResult.size()) {
            forward = begin;
            cur = oldResult.get(forward);
            if (cur.getType() == tokenTable.INTEGER) {

            }
        }

        return null;
    }

    public List<Token> DigitErrorHandle(int cur_line, List<Token> error, String curContent) {
        //错误处理
        String content = "Lexical error at line [" + cur_line + "]:" + "Wrong number pattern:" + curContent;
        error.add(errorHandle(content,cur_line));
        return error;
    }

    public List<Token> errorHandle(int cur_line, List<Token> result, String errorMes) {
        String content = "Lexical error at line [" + cur_line + "]:" + errorMes;
        result.add(errorHandle(content,cur_line));
        return result;
    }

    public Token errorHandle(String errorMes,int line) {
        return new Token(tokenTable.ERROR, errorMes,line);
    }

    public void PrintResult(List<Token> result) {
        tokenTable.PrintResult(result);
    }

    //    begin 代表这个字符串的第一个字符，forward代表这个字符串的最后一个字符
    public String getSubString(byte[] buffer1, byte[] buffer2, int begin, int forward) {
        String buffer1S = new String(buffer1);
        String buffer2S = new String(buffer2);
        int bf1Length = buffer1S.length();
        if (begin < buffer1S.length() && forward < buffer1S.length()) {
            return buffer1S.substring(begin, forward + 1);
        } else if (begin < buffer1S.length() && forward >= buffer1S.length()) {
            return buffer1S.substring(begin) + buffer2S.substring(0, forward - bf1Length + 1);
        } else if (begin >= buffer1S.length() && forward >= buffer1S.length()) {
            return buffer2S.substring(begin - bf1Length, forward - bf1Length + 1);
        } else {
            return buffer2S.substring(begin - bf1Length) + buffer1S.substring(0, forward + 1);
        }
    }

    // 完成读取index位置的字符的同时，追加文件读取功能,在Java中，数组是引用类型，可以通过传递数组改变数组的内容
    public char getChar(byte[] buffer1, byte[] buffer2, int[] real_size, int index, InputStream in) throws IOException {
        if (index == 0 && real_size[1] > 0) {
            real_size[0] = in.read(buffer1);
        }
        if (index == buffer1.length) {
            real_size[1] = in.read(buffer2);
        }
        String buffer1S = new String(buffer1);
        String buffer2S = new String(buffer2);
        if (index < buffer1.length) {
            return buffer1S.charAt(index);
        } else {
            return buffer2S.charAt(index - buffer1S.length());
        }
    }
    //判断文件是否结束
    /**
     * @param forward 当前位置
     * @param real_size 实际大小[buffer1_size,buffer2_size]
     * @param buffer_size
     * */
    /**
     * 1. real_size[0] =-1
     * 2. forward <real_size[0]-1
     * 当forward 恰好等于 real_size[0]时
     */
    public boolean FileIsFinish(int[] real_size, int forward, int buffer_size) {
        if (forward < buffer_size) {  //块1
            if (real_size[0] < buffer_size) { //恰好等于块长时不一定结束
                if (real_size[0] == -1) {
                    return true;
                } else if (forward < real_size[0] - 1) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            if (real_size[1] < buffer_size) { //恰好等于块长时不一定结束
                if (real_size[1] == -1) {
                    return true;
                } else if (forward - buffer_size < real_size[1] - 1) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    //    内部操作函数
    private boolean isAlpha(char i) {
        return (i >= 'A' && i <= 'Z') || (i >= 'a' && i <= 'z');
    }

    private boolean isNumEnd(char i) {
        return isBlank(i) || isOperator(i) || isDivide(i) || i == ')' || i == ']';
    }

    private boolean isWordEnd(char i) {
        return isNumEnd(i) || i == '[' || i == '{' || i == '(';
    }

    private boolean isOperator(char i) {
        return isCalOperator(i) || isCompareOperator(i) || isLogicOperator(i);
    }

    private boolean isCalOperator(char i) {
        return i == '/' || i == '+' || i == '-' || i == '*' || i == '%';
    }

    private boolean isCompareOperator(char i) {
        return i == '=' || i == '<' || i == '>';
    }

    private boolean isDivide(char i) {
        return i == ':' || i == ';' || i == ',';
    }

    private boolean isLogicOperator(char i) {
        return i == '&' || i == '|' || i == '!';
    }

    private boolean isBlank(char i) {
        return i == '\t' || i == ' ' || i == '\r' || i == '\n';
    }

    private boolean isUnderLine(char i) {
        return i == '_';
    }

    private boolean isDigit(char i) {
        return i >= '0' && i <= '9';
    }

    private boolean isAlNum(char i) {
        return isAlpha(i) || isDigit(i);
    }

    private boolean isOCTAL(char i) {
        return i >= '0' && i < '8';
    }

    private boolean isHex(char i) {
        return isDigit(i) || (i >= 'a' && i <= 'f') || i >= 'A' && i <= 'F';
    }

    private int PointerPlus(int now, int size) {
        return ++now % (size * 2);
    }

    private int PointerSubtract(int now, int size) {
        return --now % (size * 2);
    }

    // 识别一个数字字符串是标识符还是关键字,并返回对应的种别码
    private int Install_Id(String token) {
        return tokenTable.Install_Id(token);
    }

    private int Install_Digit(String token) {
        if (token.contains(".")) {
            return tokenTable.REAL;
        } else if (token.toLowerCase().contains("e")) {
            return tokenTable.SCINUM;
        } else {
            return tokenTable.INTEGER;
        }
    }
}
