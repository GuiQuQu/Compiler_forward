package symbols;

/**
 * @Author: Wang keLong
 * @DateTime: 15:10 2021/5/8
 */
public class Array extends Type {
    private int num;
    private Type type;

    //width
    //content
    public Array() {
    }

    public Array( int num, Type type) {
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
}
