package symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public int size() {
        return list.size();
    }

    public SymbolTable getPrevious() {
        return previous;
    }

    public void setPrevious(SymbolTable previous) {
        this.previous = previous;
    }

    public SymbolTableEntry get(int index) {
        return list.get(index);
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
    public SymbolTableEntry getById(String id,SymbolTable st) {
        for (SymbolTableEntry entry : st.getList()) {
            if (entry.getId().equals(id)){
                return entry;
            }
        }
        return null;
    }
    public SymbolTableEntry getById(String id) {
        for (SymbolTableEntry entry : list) {
            if (entry.getId().equals(id)){
                return entry;
            }
        }
        return null;
    }
    public boolean contains(SymbolTableEntry ste) {
        return list.contains(ste);
    }

    public boolean contains(String s) {
        SymbolTableEntry ste = new SymbolTableEntry();
        ste.setId(s);
        for (SymbolTableEntry entry : list) {
            if (entry.getId().equals(s)) {
                return true;
            }
        }
        return false;
    }

    public SymbolTableEntry getUsedEntry(String id) {
        SymbolTable symbolTable = this;
        while (symbolTable != null) {
            if (symbolTable.contains(id)) {
                return getById(id,symbolTable);
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
            sb.append(entry).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolTable that = (SymbolTable) o;
        return list.equals(that.list) &&
                previous.equals(that.previous);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, previous);
    }
}
