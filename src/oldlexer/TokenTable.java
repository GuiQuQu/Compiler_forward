package oldlexer;

import java.util.List;

/**
 * @Author: Wang keLong
 * @DateTime: 15:12 2021/3/25
 */
public class TokenTable {
    //种别码
    //关键字
    final public int INT = 0;
    final public int FLOAT = 1;
    final public int DOUBLE = 2;
    final public int LONG = 3;
    final public int SHORT = 4;
    final public int BYTE = 5;
    final public int BOOLEAN = 6;
    final public int CHAR = 7;
    final public int IF = 8;
    final public int ELSE = 9;
    final public int WHILE = 10;
    final public int DO = 11;
    final public int FOR = 12;
    final public int RETURN = 13;
    final public int TRUE = 14;
    final public int FALSE = 15;
    final public int STRUCT = 16; //结构体
    final public int BREAK = 60;
    final public int VOID =61;
    //标识符
    final public int ID = 17;
    //运算符
    final public int PLUS = 18; //+
    final public int PLUSEQUAL = 19;//+=
    final public int PLUSPLUS = 20;//++
    final public int SUBTRACT = 21; //-
    final public int SUBTRACTEQUAL = 22;//-=
    final public int SUBSUB = 23;//--
    final public int MULTI = 24; //*
    final public int INDEX = 59;// **
    final public int DIVIDE = 25;// /
    final public int REMAINDER = 26;// %
    //比较运算符
    final public int EQUAL = 27;// ==
    final public int GREAT = 28; // >
    final public int GE = 29; // >=
    final public int LESS = 30; // <
    final public int LE = 31; // <=
    final public int NE = 32; // !=
    //关系运算符
    final public int AND = 33; // &&
    final public int OR = 34; // ||
    final public int NOT = 35; // !
    //赋值运算符
    final public int ASSIGN = 36; // =
    //分界符
    final public int COMMA = 37; //,
    final public int SEMICOLON = 38; //;  语句结束
    final public int COLON = 39; //: case
    final public int POINT = 40; //.
    final public int LEFTS = 41; //(
    final public int LEFTM = 42; //[
    final public int LEFTL = 43; //{
    final public int RIGHTS = 44;//)
    final public int RIGHTM = 45;//]
    final public int RIGHTL = 46;//}
    //数字
    final public int INTEGER = 47; //十进制整数(可正可负)
    final public int OCTAL = 48;// 八进制无符号整数   以0开头
    final public int HEX = 49; // 十六进制无符号整数 以0x开头
    final public int REAL = 50; //小数
    //科学计数法记录的数字
    final public int SCINUM = 51;
    //字符串
    final public int STRING = 52;
    //单个字符
    final public int CHARACTER = 53;
    //应该跳过的词法单元
    final public int BLANK = 54; //空白
    final public int COMMENT = 55; //注释

    //遗漏补充
    final public int UNKNOWN = 56; //未知字符
    final public int STRINGKEY = 57; //关键字
    final public int ERROR = 58;

    public String getDescription(int type) {
        switch (type) {
            case INT:
                return "int";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case LONG:
                return "long";
            case SHORT:
                return "short";
            case BYTE:
                return "byte";
            case BOOLEAN:
                return "boolean";
            case CHAR:
                return "char";
            case FOR:
                return "for";
            case IF:
                return "if";
            case ELSE:
                return "else";
            case DO:
                return "do";
            case WHILE:
                return "while";
            case RETURN:
                return "return";
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case STRUCT:
                return "struct";
            case BREAK:
                return "break";
            case VOID:
                return "void";
            case ID:
                return "id";
            case PLUS:
                return "+";
            case SUBTRACT:
                return "-";
            case MULTI:
                return "*";
            case DIVIDE:
                return "/";
            case REMAINDER:
                return "%";
            case EQUAL:
                return "==";
            case GREAT:
                return ">";
            case GE:
                return ">=";
            case LESS:
                return "<";
            case LE:
                return "<=";
            case NE:
                return "!=";
            case AND:
                return "&&";
            case OR:
                return "||";
            case NOT:
                return "!";
            case ASSIGN:
                return "=";
            case COMMA:
                return ",";
            case SEMICOLON:
                return ";";
            case COLON:
                return ":";
            case LEFTS:
                return "(";
            case LEFTM:
                return "[";
            case LEFTL:
                return "{";
            case RIGHTS:
                return ")";
            case RIGHTM:
                return "]";
            case RIGHTL:
                return "}";
            case INTEGER:
                return "integer";
            case OCTAL:
                return "octal";
            case HEX:
                return "hex";
            case SCINUM:
                return "science number";
            case REAL:
                return "real";
            case STRING:
                return "string";
            case CHARACTER:
                return "character";
            case POINT:
                return ".";
            case UNKNOWN:
                return "unknown";
            case ERROR:
                return "error";
            case PLUSPLUS:
                return "++";
            case PLUSEQUAL:
                return "+=";
            case SUBSUB:
                return "--";
            case SUBTRACTEQUAL:
                return "-=";
            case INDEX:
                return "**";
        }
        return null;
    }
    //根据种别码获取对应对象的描述内容
    public String getDescription1(int type) {
        switch (type) {
            case INT:
                return "int";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case LONG:
                return "long";
            case SHORT:
                return "short";
            case BYTE:
                return "byte";
            case BOOLEAN:
                return "boolean";
            case CHAR:
                return "char";
            case FOR:
                return "for";
            case IF:
                return "if";
            case ELSE:
                return "else";
            case DO:
                return "do";
            case WHILE:
                return "while";
            case RETURN:
                return "return";
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case STRUCT:
                return "struct";
            case BREAK:
                return "break";
            case ID:
                return "id";
            case PLUS:
                return "PLUS";
            case SUBTRACT:
                return "SUBTRACT";
            case MULTI:
                return "MULTI";
            case DIVIDE:
                return "DIVIDE";
            case REMAINDER:
                return "REMAINDER";
            case EQUAL:
                return "EQUAL";
            case GREAT:
                return "GREAT";
            case GE:
                return "GE";
            case LESS:
                return "LESS";
            case LE:
                return "LE";
            case NE:
                return "NE";
            case AND:
                return "AND";
            case OR:
                return "OR";
            case NOT:
                return "NOT";
            case ASSIGN:
                return "ASSIGN";
            case COMMA:
                return "COMMA";
            case SEMICOLON:
                return "SEMICOLON";
            case COLON:
                return "COLON";
            case LEFTS:
                return "LEFTS";
            case LEFTM:
                return "LEFTM";
            case LEFTL:
                return "LEFTL";
            case RIGHTS:
                return "RIGHTS";
            case RIGHTM:
                return "RIGHTM";
            case RIGHTL:
                return "RIGHTL";
            case INTEGER:
                return "INTEGER";
            case OCTAL:
                return "OCTAL";
            case HEX:
                return "HEX";
            case SCINUM:
                return "Science Number";
            case REAL:
                return "REAL";
            case STRING:
                return "STRING";
            case CHARACTER:
                return "CHARACTER";
            case POINT:
                return "POINT";
            case UNKNOWN:
                return "UNKNOWN";
            case ERROR:
                return "ERROR";
            case PLUSPLUS:
                return "PLUSPLUS";
            case PLUSEQUAL:
                return "PLUSEQUAL";
            case SUBSUB:
                return "SUBSUB";
            case SUBTRACTEQUAL:
                return "SUBTRACTEQUAL";
            case INDEX:
                return "INDEX";
        }
        return null;
    }

    public void PrintResult(List<Token> result) {
        for (Token token : result) {
            String type = getDescription(token.getType());
            String content = "_";
            //确定需要描述的type
            if (token.getType() == ERROR || token.getType() == ID ||
                    token.getType() >= INTEGER && token.getType() <= CHARACTER ||
                    token.getType() == UNKNOWN) {
                content = token.getContent();
            }
            System.out.println(token.getContent() + "  " + "<" + type + "," + content + ">");
        }
    }

    //关键字查找可以利用trie树加快查找速度
    public int Install_Id(String token) {
        switch (token) {
            case "int":
                return INT;
            case "float":
                return FLOAT;
            case "long":
                return LONG;
            case "double":
                return DOUBLE;
            case "short":
                return SHORT;
            case "byte":
                return BYTE;
            case "boolean":
                return BOOLEAN;
            case "char":
                return CHAR;
            case "if":
                return IF;
            case "else":
                return ELSE;
            case "while":
                return WHILE;
            case "do":
                return DO;
            case "for":
                return FOR;
            case "true":
                return TRUE;
            case "false":
                return FALSE;
            case "struct":
                return STRUCT;
            case "return":
                return RETURN;
            case "break":
                return BREAK;
            case "void":
                return VOID;
            default:
                return ID;
        }
    }

    public int IsOperator(Token token) {
        int ret = -1;
        switch (token.getType()) {
            case PLUS:
            case PLUSPLUS:
            case SUBTRACT:
            case SUBSUB:
            case MULTI:
            case DIVIDE:
            case REMAINDER:
                ret = 1;
                break;
            case EQUAL:
            case GREAT:
            case GE:
            case LESS:
            case LE:
            case NE:
                ret = 2;
                break;
            case AND:
            case OR:
            case NOT:
                ret = 3;
                break;
            case ASSIGN:
            case PLUSEQUAL:
            case SUBTRACTEQUAL:
                ret = 4;
                break;
            default:
                ret = -1;
                break;
        }
        return ret;
    }
}
