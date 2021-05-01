package oldlexer;

/**
 * @Author: Wang keLong
 * @DateTime: 19:32 2021/3/23
 */
public class Token {
    private int type;  //种别码
    private String content; //词素
    private int lineNum; //当前行数
//    public Token(int type, String content) {
//        this.type = type;
//        this.content = content;
//    }

    public Token(int type, String content, int lineNum) {
        this.type = type;
        this.content = content;
        this.lineNum = lineNum;
    }

    public int getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getLineNum() {
        return lineNum;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", content='" + content + '\'' +
                ", lineNum=" + lineNum +
                '}';
    }
}
