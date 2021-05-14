package Utils;

/**
 * @Author: Wang keLong
 * @DateTime: 15:52 2021/4/22
 */
public class Char {
    public static boolean isAlpha(char i) {
        return (i >= 'A' && i <= 'Z') || (i >= 'a' && i <= 'z');
    }

    public static boolean isNumEnd(char i) {
        return isBlank(i) || isOperator(i) || isDivide(i) || i == ')' || i == ']';
    }

    public static boolean isWordEnd(char i) {
        return isNumEnd(i) || i == '[' || i == '{' || i == '(';
    }

    public static boolean isOperator(char i) {
        return isCalOperator(i) || isCompareOperator(i) || isLogicOperator(i);
    }

    public static boolean isCalOperator(char i) {
        return i == '/' || i == '+' || i == '-' || i == '*' || i == '%';
    }

    public static boolean isCompareOperator(char i) {
        return i == '=' || i == '<' || i == '>';
    }

    public static boolean isDivide(char i) {
        return i == ':' || i == ';' || i == ',';
    }

    public static boolean isLogicOperator(char i) {
        return i == '&' || i == '|' || i == '!';
    }

    public static boolean isBlank(char i) {
        return i == '\t' || i == ' ' || i == '\r' || i == '\n';
    }

    public static boolean isUnderLine(char i) {
        return i == '_';
    }

    public static boolean isDigit(char i) {
        return i >= '0' && i <= '9';
    }

    public static boolean isAlNum(char i) {
        return isAlpha(i) || isDigit(i);
    }

    public static boolean isOCTAL(char i) {
        return i >= '0' && i < '8';
    }

    public static boolean isHex(char i) {
        return isDigit(i) || (i >= 'a' && i <= 'f') || i >= 'A' && i <= 'F';
    }
}
