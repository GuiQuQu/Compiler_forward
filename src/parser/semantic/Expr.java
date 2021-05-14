package parser.semantic;

import symbols.Type;

/**
 * @Author: Wang keLong
 * @DateTime: 11:12 2021/5/13
 */
public class Expr {
    private int description; //表示该内容是变量地址还是常量
    private String value;
    private Type type;
    private String base =null; //帮助结构体和数组确定值
    static public int Constant = 0;
    static public int Variable = 1;

    public Expr() {
    }

    public Expr(int description, String value) {
        this.description = description;
        this.value = value;
    }

    public Expr(int description, String value, Type type) {
        this.description = description;
        this.value = value;
        this.type = type;
    }

    public Expr(int description, String value, Type type, String base) {
        this.description = description;
        this.value = value;
        this.type = type;
        this.base = base;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isConstant() {
        if (getDescription() > 0) {
            return false;
        } else {
            return true;
        }
    }

    public int getDescription() {
        return description;
    }

    public void setDescription(int description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
