package symbols;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Wang keLong
 * @DateTime: 22:01 2021/5/6
 */
public class SymbolTable {
    private List<SymbolTableEntry> list; //保存符号表内容
    private int offset; //符号表宽度
    private SymbolTable previous; //指明他的上一个符号表

    //    SymbolTable next;
    public SymbolTable() {
        list = new ArrayList<>();
        offset = 0;
        previous = null;
    }

    public SymbolTable getPrevious() {
        return previous;
    }

    public void setPrevious(SymbolTable previous) {
        this.previous = previous;
    }

    /**
     * 向符号表中添加内容
     */
    public void addEntry(SymbolTableEntry ste) {
        list.add(ste);
        offset = offset + ste.getType().getWidth();
    }

    public List<SymbolTableEntry> getList() {
        return list;
    }

    public int getOffset() {
        return offset;
    }

    /**
     * 通过id寻找对应的符号表内容
     */
    public SymbolTableEntry getById(String id) {
        SymbolTableEntry ste = new SymbolTableEntry();
        ste.setId(id);
        return list.get(list.indexOf(ste));
    }

    public boolean contains(SymbolTableEntry ste) {
        return list.contains(ste);
    }

    public boolean contains(String s) {
        SymbolTableEntry ste = new SymbolTableEntry();
        ste.setId(s);
        return list.contains(s);
    }

    public SymbolTableEntry getUsedEntry(String id) {
        SymbolTable symbolTable = this;
        while (symbolTable != null) {
            if (symbolTable.contains(id)) {
                return getById(id);
            } else {
                symbolTable = symbolTable.getPrevious();
            }
        }
        return null;
    }

    public boolean isUseEntry(String s) {
        SymbolTable symbolTable = this;
        while (symbolTable != null) {
            if (symbolTable.contains(s)) {
                return true;
            } else {
                symbolTable = symbolTable.getPrevious();
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SymbolTableEntry entry : list) {
            sb.append(sb).append("\n");
        }
        return sb.toString();
    }
}
