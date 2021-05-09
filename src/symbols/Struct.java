package symbols;

/**
 * @Author: Wang keLong
 * @DateTime: 15:16 2021/5/8
 */
public class Struct extends Type {
    private SymbolTable st = new SymbolTable();

    // content
    // width
    public Struct() {
        super("struct", 0);
    }

    /**
     * 增加结构体中的内容
     */
    public void addEntry(SymbolTableEntry ste) {
        st.addEntry(ste);
        setWidth(st.getOffset());
    }

    @Override
    public String toString() {
        return getContent() + "(" + getWidth() + ")";
    }
}
