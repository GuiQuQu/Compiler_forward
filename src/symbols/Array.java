package symbols;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 15:10 2021/5/8
 * array(4,int)
 * id  array(4,int),base
 */
public class Array extends Type {
    private int num;
    private Type type;


    //width
    //content
    public Array() {
    }

    public Array(int num, Type type) {
        super("array", type.getWidth() * num);
        this.num = num;
        this.type = type;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "array(" + num + "," + type + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Array array = (Array) o;
        return num == array.num &&
                type.equals(array.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), num, type);
    }
}
