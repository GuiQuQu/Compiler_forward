package symbols;

import Utils.Char;
import lexer.tokenUnit.Tag;
import lexer.tokenUnit.Token;
import lexer.tokenUnit.Word;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 15:43 2021/4/22
 * int , 4
 *
 */
public class Type {
    private int width = 0;
    private String content;

    public Type() {
    }

    public Type(String content, int width) {
        this.width = width;
        this.content = content;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static final Type
            Int = new Type("int", 4),
            Float = new Type("float", 8),
            Char = new Type("char", 1),
            Boolean = new Type("boolean", 1),
            Void =new Type("void",0);

    @Override
    public String toString() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return content.equals(type.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
