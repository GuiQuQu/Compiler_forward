package symbols;

/**
 * @Author: Wang keLong
 * @DateTime: 16:00 2021/5/8
 */
public class Function extends Type {
    //content 函数名
    private Type returnType; // 返回值类型
    private SymbolTable funcBody = new SymbolTable();// 函数体

    public Function(Type returnType) {
        super("function", 0);
        this.returnType = returnType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public SymbolTable getFuncBody() {
        return funcBody;
    }

    public void setFuncBody(SymbolTable funcBody) {
        this.funcBody = funcBody;
    }


}
