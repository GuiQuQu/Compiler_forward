package symbols;

import java.util.Objects;

/**
 * @Author: Wang keLong
 * @DateTime: 15:16 2021/5/8
 */
public class Struct extends Type {
    private SymbolTable st = new SymbolTable();
    private String name; //结构体的名字

    // content
    // width
    public Struct() {
        super("struct", 0);
    }

    public Struct(SymbolTable st, String name) {
        super("struct", st.getOffset());
        this.st = st;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 增加结构体中的内容
     */
    public void addEntry(SymbolTableEntry ste) {
        st.addEntry(ste);
    }

    public int getWidth() {
        return st.getOffset();
    }

    /**
     * 获取结构体中的一个属性
     */
    public SymbolTableEntry getContentById(String id) {
        return st.getById(id);
    }


    @Override
    public String toString() {
        return getContent() + "(" + getWidth() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Struct struct = (Struct) o;
        return st.equals(struct.st) &&
                name.equals(struct.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), st, name);
    }
}
