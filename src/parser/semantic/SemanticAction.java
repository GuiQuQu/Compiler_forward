package parser.semantic;

import parser.LeftNode;
import symbols.*;

import java.util.*;

/**
 * @Author: Wang keLong
 * @DateTime: 20:11 2021/5/8
 * 变量名是由字符串表示的
 * 语义栈中的内容各种各样，表示各个非终结符的属性
 */
public class SemanticAction {
    //语义栈
    private Stack<Attribute> stack = new Stack<>();
    private int tempNum = 0;
    private Stack<SymbolTable> symTabStack = new Stack<>();
    private List<Quadruple> threeAddressCode = new ArrayList<>();
    private List<Struct> structs = new ArrayList<>();

    public SemanticAction() {
        stack = new Stack<>();
        tempNum = 0;
        symTabStack = new Stack<>();
        threeAddressCode = new ArrayList<>();
        //建立第一个符号表
        SymbolTable st = new SymbolTable();
        st.setPrevious(null);
        symTabStack.push(st);
    }

    public String printThreeAddressCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < threeAddressCode.size(); i++) {
            sb.append(i).append(":").append(threeAddressCode.get(i).transToThreeAddressCode()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 得到一个临时变量
     */
    public String getNewTemp() {
        String s = "t" + tempNum;
        tempNum++;
        return s;
    }

    public void push(String name, Object value) {
        stack.push(new Attribute(name, value));
    }

    public void gen(Quadruple q) {
        threeAddressCode.add(q);
    }

    public List<Quadruple> getThreeAddressCode() {
        return threeAddressCode;
    }

    /**
     * 获取下一条指令的标号
     */
    private int getNextInstr() {
        return threeAddressCode.size();
    }

    /**
     * 为某一条指令拉链
     */
    private List<Integer> makeList(int nextInstr) {
        List<Integer> list = new ArrayList<>();
        list.add(nextInstr);
        return list;
    }

    /**
     * 用一个标号回填
     */
    private void backPatch(List<Integer> list, int instr) {
        for (Integer i : list) {
            Quadruple q = threeAddressCode.get(i);
            if (q.getResult().equals("_")) {
                q.setResult(Integer.toString(instr));
            }
        }
    }

    public String errorHandle(String info, int line) {
        String sb = "Semantic error at Line[" +
                line +
                "]:[" +
                info +
                "]";
        return sb;

    }

    /**
     * 合并两个拉链的列表
     */
    private List<Integer> merge(List<Integer> list1, List<Integer> list2) {
        List<Integer> result = new ArrayList<>(list1);
        result.addAll(list2);
        return result;
    }

    /**
     * primary_expression -> id
     * primary_expression.addr类
     */
    public void primaryId(LeftNode production) throws Exception {
        SymbolTable st = symTabStack.peek();
        String lexeme = production.getRight().get(0).getTokenDescription();
        if (st.isUseEntry(lexeme)) {
            Expr addr = new Expr(Expr.Variable, lexeme, st.getUsedEntry(lexeme).getType());
            stack.add(new Attribute("id", addr));
        } else {
            throw new Exception(errorHandle("未知变量名:" + lexeme, production.getValue().getLineNum()));
        }
    }

    /**
     * primary_expression -> real
     */
    public void primaryReal(LeftNode production) {
        Expr addr = new Expr(Expr.Constant, production.getRight().get(0).getTokenDescription(), Type.Float);
        stack.push(new Attribute("real", addr));
    }

    /**
     * primary_expression -> integer
     */
    public void primaryInteger(LeftNode production) {
        Expr addr = new Expr(Expr.Constant, production.getRight().get(0).getTokenDescription(), Type.Int);
        stack.push(new Attribute("integer", addr));
    }

    /**
     * primary_expression -> character
     */
    public void primaryCharacter(LeftNode production) {
        Expr addr = new Expr(Expr.Constant, production.getRight().get(0).getTokenDescription(), Type.Char);
        stack.push(new Attribute("char", addr));
    }

    /**
     * 当计算布尔值时,通过'control'指明是值还是控制流
     * primary_expression -> true
     * primary_expression.addr
     */
    public void primaryTrue(LeftNode production) {
        Attribute a;
        if (!stack.isEmpty()) {
            a = stack.peek();
            if (a.getName().equals("control")) {
                stack.pop();
                List<Integer> trueList = makeList(getNextInstr());
                List<Integer> falseList = new ArrayList<>();
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("falseList", falseList));
                stack.push(new Attribute("trueList", trueList));
                return;
            }
        }
        Expr addr = new Expr(Expr.Constant, "1", Type.Boolean);
        stack.push(new Attribute("true", addr));
    }

    /**
     * primary_expression -> false
     * primary_expression.value
     * primary_expression.type
     */
    public void primaryFalse(LeftNode production) {
        Attribute a;
        if (!stack.isEmpty()) {
            a = stack.peek();
            if (a.getName().equals("control")) {
                stack.pop();
                List<Integer> falseList = makeList(getNextInstr());
                List<Integer> trueList = new ArrayList<>();
                gen(new Quadruple("goto", "", "", "_"));
                stack.push(new Attribute("trueList", trueList));
                stack.push(new Attribute("falseList", falseList));
                return;
            }
        }
        Expr addr = new Expr(Expr.Constant, "0", Type.Boolean);
        stack.push(new Attribute("false", addr));
    }

    /**
     * postfix_expression -> postfix_expression [ expression ] 综合属性往上算
     * condition：
     * expression.addr (表达式数值,INT)
     * postfix_expression.addr (offset Variable Array)  postfix_expression.addr (id Variable Array base)
     * -> (offset Variable Array,base)
     */
    public void postfix2(LeftNode p) throws Exception {
        Expr expr = (Expr) stack.pop().getValue();
        Type typeE = expr.getType();
        Expr post = (Expr) stack.pop().getValue();
        if (!(post.getType() instanceof Array)) {
            throw new Exception(errorHandle(post.getValue() + "非数组变量", p.getValue().getLineNum()));
        }
        if (!typeE.equals(Type.Int)) {
            throw new Exception(errorHandle("请使用整数表示数组下标", p.getValue().getLineNum()));
        }
        Array typeP = (Array) post.getType();
        SymbolTable st = symTabStack.peek();
        String t2 = getNewTemp(); //保存偏移值
        Expr addr = new Expr();
        if (st.isUseEntry(post.getValue())) { // 是声明的id
            String t1 = getNewTemp();
            addr.setBase(t1);
            gen(new Quadruple("=", "base(" + post.getValue() + ")", "", t1));
            gen(new Quadruple("*", expr.getValue(), restoreType(typeP).getWidth() + "", t2));
        } else { //是偏移值
            String t3 = getNewTemp();
            addr.setBase(post.getBase());
            gen(new Quadruple("*", expr.getValue(), restoreType(typeP).getWidth() + "", t3));
            gen(new Quadruple("+", post.getValue(), t3, t2));
        }
        addr.setDescription(Expr.Variable);
        addr.setValue(t2);
        addr.setType(typeP);
        stack.push(new Attribute("addr", addr));
    }

    public void postfix(LeftNode production) throws Exception {
        Attribute addr = stack.pop(); //值或者id
        Type type = (Type) stack.pop().getValue();
        Attribute posAddr = stack.pop(); // id 或者是已经计算的数组值
        if (type.equals(Type.Int)) {
            throw new Exception("数组下标不是整数");
        }
        if (posAddr.getName().equals("id")) {
            //取出数组的基地址
            Array arrayType = (Array) stack.pop().getValue();
            String a = getNewTemp();
            String c = getNewTemp();
            String b = getNewTemp();
            //数组的基地址设定为a
//            t1=C(Y)
//            t2 = width * e.addr
//            t3 =t1[t2]
            gen(new Quadruple("=", posAddr.getValue().toString(), "", a));
            gen(new Quadruple("*", addr.getValue().toString(), "" + arrayType.getType().getWidth(), c));
            gen(new Quadruple("=[]", a, addr.getValue().toString(), b));
            stack.push(new Attribute("type", arrayType.getType()));
            stack.push(new Attribute("addr", b));
        } else {
            //t1=C(Y)
            //t2 = width * e1.addr
            //t3 = width * e2.addr
            //t4 = t2+t3
            //t5 =t1[t4]  t5 使用的变量还是原来那个变量
            Attribute typeA = stack.pop();
            Type arrayType = (Type) typeA.getValue();
            Quadruple q = threeAddressCode.get(threeAddressCode.size() - 1);
            threeAddressCode.remove(threeAddressCode.size() - 1);
            String t2 = q.getArg2();
            String t3 = getNewTemp();
            String t4 = getNewTemp();
            gen(new Quadruple("*", addr.getValue().toString(), "" + arrayType.getWidth(), t3));
            gen(new Quadruple("+", t2, t3, t4));
            q.setArg2(t4);
            gen(q);
            stack.push(typeA);
            stack.push(posAddr);
        }
    }

    /**
     * postfix_expression -> id ( )
     * <p>
     * postfix_expression_1.type(Function)
     */
    public void postfix3(LeftNode production) throws Exception {
        String funcName = production.getRight().get(0).getTokenDescription();
        SymbolTable st = symTabStack.peek();
        if (!st.isUseEntry(funcName) || !(st.getUsedEntry(funcName).getType() instanceof Function)) {
            throw new Exception(errorHandle("未知函数名" + funcName, production.getValue().getLineNum()));
        }
        Function function = (Function) st.getUsedEntry(funcName).getType();
        if (function.getArgs().size() > 0) {
            throw new Exception(errorHandle(funcName + "函数参数数量不正确", production.getValue().getLineNum()));
        }
        if (function.getReturnType().equals(Type.Void)) {
            gen(new Quadruple("call", funcName, "", ""));
        } else {
            String t1 = getNewTemp();
            gen(new Quadruple("call", funcName, "", t1));
            Expr addr = new Expr(Expr.Constant, t1, function.getReturnType()); //函数调用只能成为右值
            stack.push(new Attribute("addr", addr));
        }
    }

    /**
     * argument_expression_list -> assignment_expression
     * assignment_expression.addr (Addr)
     * func(1,2)
     */
    public void argument_expression1(LeftNode production) {
        Expr addr = (Expr) stack.pop().getValue();
//        Type type = (Type) stack.pop().getValue();
        Type type = addr.getType();
        List<Type> param_list = new ArrayList<>();
        param_list.add(type);
        gen(new Quadruple("param", addr.getValue(), "", ""));
        stack.push(new Attribute("param_list", param_list));
    }

    /**
     * argument_expression_list -> argument_expression_list , assignment_expression
     * assignment_expression.addr
     * argument_expression_list_1.param_list
     */
    public void argument_expression2(LeftNode production) {
        Expr addr = (Expr) stack.pop().getValue();
//        Type type = (Type) stack.pop().getValue();
        Type type = addr.getType();
        List<Type> param_list = (List<Type>) stack.pop().getValue();
        param_list.add(type);
        gen(new Quadruple("param", addr.getValue(), "", ""));
        stack.push(new Attribute("param_list", param_list));
    }

    /**
     * postfix_expression -> id ( argument_expression_list )
     * argument_expression_list.param_list
     */
    public void postfix4(LeftNode production) throws Exception {
        String funcName = production.getRight().get(0).getTokenDescription();
        SymbolTable st = symTabStack.peek();
        List<Type> param_list = (List<Type>) stack.pop().getValue();
        if (!st.isUseEntry(funcName) || !(st.getUsedEntry(funcName).getType() instanceof Function)) {
            throw new Exception(errorHandle("未知函数名" + funcName, production.getValue().getLineNum()));
        }
        Function function = (Function) st.getUsedEntry(funcName).getType();
        if (!function.paramCompare(param_list)) {
            throw new Exception(errorHandle(funcName + "函数参数不正确", production.getValue().getLineNum()));
        }
        if (function.getReturnType().equals(Type.Void)) {
            gen(new Quadruple("call", funcName, param_list.size() + "", ""));
        } else {
            String t1 = getNewTemp();
            gen(new Quadruple("call", funcName, param_list.size() + "", t1));
            Expr addr = new Expr(Expr.Constant, t1, function.getReturnType()); //函数调用只能成为右值
            stack.push(new Attribute("addr", addr));
        }
    }

    /**
     * postfix_expression -> postfix_expression_1 ++
     */
    public void postfix5(LeftNode production) throws Exception {
    }

    /**
     * postfix_expression -> postfix_expression_1 --
     */
    public void postfix6(LeftNode production) throws Exception {
    }

    /**
     * 通过expr取出对应结构体/数组/普通变量中的值
     */
    public Expr getRightAddrValueOne(Expr expr) {
        Expr addr = expr;
        Type type = addr.getType();
        if (type instanceof Array && addr.getBase() != null) {
            Array array = (Array) type;
            String offset = addr.getValue();
            String base = addr.getBase();
            String t1 = getNewTemp();
            gen(new Quadruple("=[]", base + "[" + offset + "]", "", t1));
            Type addrType = array.getType();
            while (addrType instanceof Array) {
                addrType = ((Array) addrType).getType();
            }
            addr = new Expr(addr.getDescription(), t1, addrType);
        } else if (type instanceof Struct && addr.getBase() != null) {
            Struct struct = (Struct) type;
            String t1 = getNewTemp();
            gen(new Quadruple("=.", addr.getBase() + "." + addr.getValue(), "", t1));
            addr = new Expr(addr.getDescription(), t1, struct.getContentById(addr.getValue()).getType());
        }
        return addr;
    }

    /**
     * postfix_expression -> postfix_expression . id
     * condition:
     * postfix_expression.addr (结构体标识符,Variable,STRUCT)
     * result:
     * postfix_expression.addr (对应id内容,Variable,STRUCT,base)
     */
    public void postfix7(LeftNode production) throws Exception {
        Expr addrs = (Expr) stack.pop().getValue();
        Expr addr = getRightAddrValueOne(addrs);
        if (addr.getType() instanceof Struct && addr.getBase() == null) {
            Struct struct = (Struct) addr.getType();
            String id = production.getRight().get(2).getTokenDescription();
            if (struct.getContentById(id) == null) {
                throw new Exception(errorHandle(addr.getValue() + "不存在" + id + "这个属性", production.getValue().getLineNum()));
            }
            Expr expr = new Expr(Expr.Variable, id, addr.getType(), addr.getValue());
            stack.push(new Attribute("addr", expr));
        } else {
            throw new Exception(errorHandle(addr.getValue() + "非结构体变量", production.getValue().getLineNum()));
        }
    }

    /**
     * unary_expression -> ++ postfix_expression
     */
    public void unary2(LeftNode production) throws Exception {

    }

    /**
     * unary_expression -> -- postfix_expression
     */
    public void unary3(LeftNode production) throws Exception {
    }

    /**
     * unary_expression -> unary_operator unary_expression
     * (普通表达式)
     * unary_expression.addr
     * unary_operator.op
     * 控制流语句
     * unary_expression.trueList
     * unary_expression.falseList
     * unary_operator.op=!
     */
    public void unary4(LeftNode production) {
        Attribute expr = stack.pop(); //true
        Attribute op = stack.pop(); //false
        if (!op.getName().equals("op")) {
            Attribute realOp = stack.pop();
            if (realOp.getValue().toString().equals("!")) {
                //交换trueList和falseList
                stack.push(expr);
                stack.push(op);
            }
        } else {
            String opS = (String) op.getValue();
            String a = getNewTemp();
            Expr addr = (Expr) expr.getValue(); // 数字 或者是 id
            switch (opS) {
                case "+":
                    break;
                case "-":
                    gen(new Quadruple("minus", addr.getValue().toString(), "", a));
                    break;
                case "!":
                    int instr1 = getNextInstr() + 3;
                    gen(new Quadruple("==", addr.getValue().toString(), "0", instr1 + ""));
                    gen(new Quadruple("=", "0", "", a));
                    int instr2 = getNextInstr() + 2;
                    gen(new Quadruple("goto", "", "", instr2 + ""));
                    gen(new Quadruple("=", "1", "", a));
            }
        }
    }

    /**
     * unary_operator -> +
     */
    public void unaryOp1(LeftNode production) {
        stack.push(new Attribute("op", "+"));
    }

    /**
     * unary_operator -> -
     */
    public void unaryOp2(LeftNode production) {
        stack.push(new Attribute("op", "-"));
    }

    /**
     * unary_operator -> !
     */
    public void unaryOp3(LeftNode production) {
        stack.push(new Attribute("op", "!"));
    }

    /**
     * 类型转换(只有INT和FLOAT)
     */
    public Type mergeType(Type typeA, Type typeB) {
        Type type = Type.Int;
        if (typeA.equals(Type.Float) || typeB.equals(Type.Float)) {
            type = Type.Float;
        }
        return type;
    }

    private int MakeExprDes(int des1, int des2) {
        int des = Expr.Variable;
        if (des1 == Expr.Constant && des2 == Expr.Constant) {
            des = Expr.Constant;
        }
        return des;
    }

    public void ExprCalculateHandle(String op, Expr expr1, Expr expr2, int lineNum) throws Exception {
        Expr expr1T = getRightAddrValueOne(expr1);
        Expr expr2T = getRightAddrValueOne(expr2);
        Type type1 = expr1T.getType();
        Type type2 = expr2T.getType();
        if (!type1.equals(type2)) {
            throw new Exception(errorHandle("表达式类型不匹配", lineNum));
        }
        if (type1.equals(Type.Char) || type2.equals(Type.Char)) {
            throw new Exception(errorHandle("Char型变量无法使用符号" + op, lineNum));
        }
        if (op.equals("%") && (!type1.equals(Type.Int) || !type2.equals(Type.Int))) {
            throw new Exception(errorHandle("非Int类型无法使用" + op, lineNum));
        }
        String t1 = getNewTemp();
        gen(new Quadruple(op, expr1T.getValue(), expr2T.getValue(), t1));
        Expr expr = new Expr(MakeExprDes(expr1T.getDescription(), expr2T.getDescription()), t1, type1);
        stack.push(new Attribute("addr", expr));
    }

    /**
     * multiplicative_expression -> multiplicative_expression * unary_expression
     * unary_expression.addr
     * multiplicative_expression.addr
     */
    public void multiplicative2(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        ExprCalculateHandle("*", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * multiplicative_expression -> multiplicative_expression / unary_expression
     * unary_expression.addr
     * multiplicative_expression.addr
     */
    public void multiplicative3(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        ExprCalculateHandle("/", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * multiplicative_expression -> multiplicative_expression % unary_expression
     * unary_expression.addr
     * unary_expression.type
     * multiplicative_expression.addr
     * multiplicative_expression.type
     */
    public void multiplicative4(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        ExprCalculateHandle("%", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * additive_expression -> additive_expression + multiplicative_expression
     * multiplicative_expression.addr
     * multiplicative_expression.type
     * additive_expression.addr
     * additive_expression.type
     */
    public void additive2(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        ExprCalculateHandle("+", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * additive_expression -> additive_expression - multiplicative_expression
     * multiplicative_expression.addr
     * multiplicative_expression.type
     * additive_expression.addr
     * additive_expression.type
     */
    public void additive3(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        ExprCalculateHandle("-", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * 处理各种不等式
     */
    public void InequalityHandle(String op, Expr expr1, Expr expr2, int lineNum) throws Exception {
        Expr expr1T = getRightAddrValueOne(expr1);
        Expr expr2T = getRightAddrValueOne(expr2);
        String control = "boolean";
        if (!stack.isEmpty() && stack.peek().getName().equals("control")) {
            stack.pop();
            control = "control";
        }
        if (expr1T.getType().equals(Type.Char) || expr2T.getType().equals(Type.Char)) {
            throw new Exception(errorHandle("Char型变量无法使用符号" + op, lineNum));
        }
        if (control.equals("control")) {
            List<Integer> BTrueList = makeList(getNextInstr());
            List<Integer> BFalseList = makeList(getNextInstr() + 1);
            gen(new Quadruple(op, expr1T.getValue(), expr2T.getValue(), "_"));
            gen(new Quadruple("goto", "", "", "_"));
            stack.push(new Attribute("falseList", BFalseList));
            stack.push(new Attribute("trueList", BTrueList));
        } else {
            //算术运算
            String t1 = getNewTemp();
            int instr1 = getNextInstr() + 3;
            gen(new Quadruple(op, expr1T.getValue(), expr2T.getValue(), "" + instr1));
            gen(new Quadruple("=", "0", "", t1)); //false
            int instr2 = getNextInstr() + 2;
            gen(new Quadruple("goto", "", "", "" + instr2));
            gen(new Quadruple("=", "1", "", "a")); //true
            Expr expr = new Expr(MakeExprDes(expr1T.getDescription(), expr2T.getDescription()), t1, Type.Boolean);
            stack.push(new Attribute("addr", expr));
        }
    }

    /**
     * relational_expression -> relational_expression > additive_expression
     * additive_expression.addr
     * relational_expression.addr
     * control?
     */
    public void relation2(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        InequalityHandle(">", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * relational_expression -> relational_expression < additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     * if t1<t2 goto true
     * t3=0
     * goto next
     * t3=1 (true)
     * (next)
     */
    public void relation3(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        InequalityHandle("<", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * relational_expression -> relational_expression >= additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     */
    public void relation4(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        InequalityHandle(">=", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * relational_expression -> relational_expression <= additive_expression
     * additive_expression.addr
     * additive_expression.type
     * relational_expression.addr
     * relational_expression.type
     * control?
     */
    public void relation5(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        InequalityHandle("<=", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * equality_expression -> equality_expression == relational_expression
     * relational_expression.addr
     * relational_expression.type
     * equality_expression.addr
     * equality_expression.type
     * control?getName()
     */
    public void equality2(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        InequalityHandle("==", expr1, expr2, production.getValue().getLineNum());
    }

    /**
     * equality_expression -> equality_expression != relational_expression
     * relational_expression.addr
     * relational_expression.type
     * equality_expression.addr
     * equality_expression.type
     * control?getName()
     */
    public void equality3(LeftNode production) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        InequalityHandle("!=", expr1, expr2, production.getValue().getLineNum());
    }

    public void m100(LeftNode p) {
        Attribute a = stack.pop();
        Attribute b = stack.pop();
    }

    /**
     * M8 -> epsilon
     * 在逻辑运算中向后传递该运算是控制流还是普通布尔表达式计算
     * 获取下一行的行号,使得trueList和falseList可以回填
     */
    public void m8(LeftNode p) {
        Attribute a = stack.peek();
        stack.push(new Attribute("instr", getNextInstr()));
        assert a != null;
        if (!(a.getValue() instanceof Expr)) {
            stack.push(new Attribute("control", null));
        }
    }

    /**
     * logical_and_expression -> logical_and_expression_1 && M8 equality_expression
     * M8
     * equality_expression.trueList
     * equality_expression.falseList
     * M.nextInstr
     * logical_and_expression_1.trueList
     * logical_and_expression_1.falseList
     * or
     * equality_expression.addr(t1)
     * M.nextInstr
     * logical_and_expression_1.addr(t2)
     * if t1==0 goto false
     * if t2==0 goto false
     * t3=1
     * goto next
     * t3=0 (false)
     */
    public void logical_and2(LeftNode p) {
        Attribute a1 = stack.peek();
        assert a1 != null;
        if (a1.getValue() instanceof Expr) {
            Expr expr2 = (Expr) stack.pop().getValue();
            stack.pop();
            Expr expr1 = (Expr) stack.pop().getValue();
            String t3 = getNewTemp();
            String t1 = expr1.getValue();
            String t2 = expr2.getValue();
            int False = getNextInstr() + 4;
            gen(new Quadruple("==", t1, "0", "" + False));
            gen(new Quadruple("==", t2, "0", "" + False));
            gen(new Quadruple("=", "1", "", t3));
            int instr = getNextInstr() + 2;
            gen(new Quadruple("goto", "", "", "" + instr + ""));
            gen(new Quadruple("=", "0", "", t3));
            Expr expr = new Expr(MakeExprDes(expr1.getDescription(), expr2.getDescription()), t3, Type.Boolean);
            stack.push(new Attribute("addr", expr));
        } else {
            List<Integer> B2trueList = (List<Integer>) stack.pop().getValue();
            List<Integer> B2falseList = (List<Integer>) stack.pop().getValue();
            Integer instr = (Integer) stack.pop().getValue();
            List<Integer> B1trueList = (List<Integer>) stack.pop().getValue();
            List<Integer> B1falseList = (List<Integer>) stack.pop().getValue();
            backPatch(B1trueList, instr);
            List<Integer> BFalseList = merge(B1falseList, B2falseList);
            stack.push(new Attribute("falseList", BFalseList));
            stack.push(new Attribute("trueList", B2trueList));
        }
    }

    /**
     * logical_or_expression -> logical_or_expression || M8 logical_and_expression
     * M8
     * equality_expression.trueList
     * equality_expression.falseList
     * M.nextInstr
     * logical_and_expression_1.trueList
     * logical_and_expression_1.falseList
     * or
     * equality_expression.addr (t1)
     * equality_expression.type =boolean
     * M.nextInstr
     * logical_and_expression_1.addr (t2)
     * logical_and_expression_1.boolean
     * if t1==1 goto true
     * if t2==1 goto true
     * t3=0
     * goto next
     * t3=1 (true)
     */
    public void logical_or2(LeftNode p) {
        Attribute a1 = stack.peek();
        assert a1 != null;
        if (a1.getValue() instanceof Expr) {
            Expr expr2 = (Expr) stack.pop().getValue();
            stack.pop();
            Expr expr1 = (Expr) stack.pop().getValue();
            String t3 = getNewTemp();
            String t1 = expr1.getValue();
            String t2 = expr2.getValue();
            int True = getNextInstr() + 4;
            gen(new Quadruple("==", t1, "1", "" + True));
            gen(new Quadruple("==", t2, "1", "" + True));
            gen(new Quadruple("=", "0", "", t3));
            int instr = getNextInstr() + 2;
            gen(new Quadruple("goto", "", "", "" + instr + ""));
            gen(new Quadruple("=", "1", "", t3));
            Expr expr = new Expr(MakeExprDes(expr1.getDescription(), expr2.getDescription()), t3, Type.Boolean);
            stack.push(new Attribute("addr", expr));
        } else {
            List<Integer> B2trueList = (List<Integer>) stack.pop().getValue();
            List<Integer> B2falseList = (List<Integer>) stack.pop().getValue();
            Integer instr = (Integer) stack.pop().getValue();
            List<Integer> B1trueList = (List<Integer>) stack.pop().getValue();
            List<Integer> B1falseList = (List<Integer>) stack.pop().getValue();
            List<Integer> BTrueList = merge(B1trueList, B2trueList);
            backPatch(B1falseList, instr);
            stack.push(new Attribute("falseList", B2falseList));
            stack.push(new Attribute("trueList", BTrueList));
        }
    }

    public Type calculateLeftType(Expr expr) {
        if (expr.getType() instanceof Array) {
            return restoreType(expr.getType());
        } else if (expr.getType() instanceof Struct) {
            Struct struct = (Struct) expr.getType();
            return struct.getContentById(expr.getValue()).getType();
        } else {
            return expr.getType();
        }
    }

    /**
     * assignment_expression -> unary_expression = assignment_expression
     * assignment_expression.addr
     * unary_expression.addr
     */
    public void assignment2(LeftNode p) throws Exception {
        Expr expr2 = (Expr) stack.pop().getValue();
        Expr expr1 = (Expr) stack.pop().getValue();
        Expr exprRight = getRightAddrValueOne(expr2);
        if (expr1.getDescription() == Expr.Constant) {
            throw new Exception(errorHandle(expr1.getValue() + "不是一个左值", p.getValue().getLineNum()));
        }
        if (!calculateLeftType(expr1).equals(exprRight.getType())) {
            throw new Exception(errorHandle("赋值语句类型不匹配", p.getValue().getLineNum()));
        }
        if (expr1.getType() instanceof Array) {
            String offset = expr1.getValue();
            String base = expr1.getBase();
            gen(new Quadruple("[]=", exprRight.getValue(), "", base + "[" + offset + "]"));
        } else if (expr1.getType() instanceof Struct) {
            String id = expr1.getValue();
            String base = expr1.getBase();
            gen(new Quadruple(".=", exprRight.getValue(), "", base + "." + id));
        } else {
            gen(new Quadruple("=", exprRight.getValue(), "", expr1.getValue()));
        }
    }

    /**
     * expression_statement -> expression ;
     */
    public void expressionS2(LeftNode p) {
        List<Integer> list = new ArrayList<>();
        stack.push(new Attribute("ExprNextList", list));
    }

    //从头开始

    ///////////////////////声明语句/////////////////////////

    /**
     * M2 -> epsilon
     * 没有使用
     */
    public void m2(LeftNode production) {
        SymbolTable st = new SymbolTable();
        st.setPrevious(null);
        symTabStack.push(st);
    }

    /**
     * declaration_list -> declaration
     */
    public void declaration_list1(LeftNode p) {
        List<Integer> list = new ArrayList<>();
        stack.push(new Attribute("DecNextList", list));
    }

    /**
     * declaration_list -> declaration_list declaration
     */
    public void declaration_list2(LeftNode p) {

    }

    /**
     * declaration -> type_specifier ;
     * int;也合法
     */
    public void declaration1(LeftNode p) {
        stack.pop(); //弹出没有使用的type
    }

    /**
     * declaration -> type_specifier init_declarator_list ;
     * 增加nextList属性只是为了和其他语句保持一致型
     */
    public void declaration2(LeftNode p) {
        stack.pop(); //弹出保留的type
    }

    /**
     * type_specifier -> int
     */
    public void type_specifier1(LeftNode p) {
        stack.push(new Attribute("type", Type.Int));
    }

    /**
     * type_specifier -> float
     */
    public void type_specifier2(LeftNode p) {
        stack.push(new Attribute("type", Type.Float));
    }

    /**
     * type_specifier -> boolean
     */
    public void type_specifier3(LeftNode p) {
        stack.push(new Attribute("type", Type.Boolean));
    }

    /**
     * type_specifier -> char
     */
    public void type_specifier4(LeftNode p) {
        stack.push(new Attribute("type", Type.Char));
    }

    /**
     * type_specifier -> void
     */
    public void type_specifier5(LeftNode p) {
        stack.push(new Attribute("type", Type.Void));
    }

    //结构体跳过
    ///////////////////////结构体/////////////////////////

    /**
     * type_specifier -> struct_specifier
     * 交给具体的结构体标识符来push一个type
     */
    public void type_specifier6(LeftNode p) {

    }

    /**
     * struct_specifier -> struct id { struct_declaration_list }
     * # id 是这个结构体的名字,可以使用这个名字来引用结构体
     * struct_declaration_list.structArgs (SymbolTable)
     */
    public void struct_specifier1(LeftNode p) throws Exception {
        SymbolTable st = (SymbolTable) stack.pop().getValue();
        String id = p.getRight().get(1).getTokenDescription();
        Struct struct = new Struct(st, id);
        if (findStructById(id) == null) {
            structs.add(struct);
            stack.push(new Attribute("struct", struct));
        } else {
            throw new Exception(errorHandle("结构体明明重复出现", p.getValue().getLineNum()));
        }
    }

    /**
     * struct_specifier -> struct { struct_declaration_list }
     * # 匿名结构体,不知道名字
     */
    public void struct_specifier2(LeftNode p) {
        SymbolTable st = (SymbolTable) stack.pop().getValue();
        Struct struct = new Struct(st, "temp");
        stack.push(new Attribute("struct", struct));
    }

    private Struct findStructById(String id) {
        for (Struct struct : structs) {
            if (struct.getName().equals(id)) {
                return struct;
            }
        }
        return null;
    }

    /**
     * struct_specifier -> struct id
     */
    public void struct_specifier3(LeftNode p) {
        String id = p.getRight().get(1).getTokenDescription();
        Struct struct = findStructById(id);
        if (struct != null) {
            stack.push(new Attribute("struct", struct));
        }
    }

    /**
     * struct_declaration_list -> struct_declaration
     * struct_declaration_list.structArgs
     */
    public void struct_declaration_list1(LeftNode p) {
        SymbolTable structArgs = new SymbolTable();
        SymbolTableEntry ste = (SymbolTableEntry) stack.pop().getValue();
        structArgs.addEntry(ste);
        stack.push(new Attribute("structArgs_list", structArgs));
    }

    /**
     * struct_declaration_list -> struct_declaration_list struct_declaration
     * struct_declaration.ste
     * struct_declaration_list.structArgs
     */
    public void struct_declaration_list2(LeftNode p) {
        SymbolTableEntry ste = (SymbolTableEntry) stack.pop().getValue();
        assert stack.peek() != null;
        SymbolTable structArgs = (SymbolTable) stack.peek().getValue();
        structArgs.addEntry(ste);
    }

    /**
     * struct_declaration -> type_specifier var_declarator ;
     */
    public void struct_declaration1(LeftNode p) {
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        SymbolTableEntry ste = new SymbolTableEntry(id.getValue().toString(), (Type) type.getValue());
        stack.push(new Attribute("structArg", ste));
    }

    /**
     * var_declarator -> id
     * # 使用临时的名字来引用结构体
     */
    public void var_declarator1(LeftNode p) {
        String lexeme = p.getRight().get(0).getTokenDescription();
        stack.push(new Attribute("id", lexeme));
    }

    /**
     * var_declarator -> var_declarator [ integer ]
     * var_declarator.id
     * var_declarator.type
     */
    public void var_declarator2(LeftNode p) throws Exception {
        Attribute id = stack.pop();
        Type type = (Type) stack.pop().getValue();
        int integer = Integer.parseInt(p.getRight().get(2).getTokenDescription());
        Array array = new Array(integer, type);
        stack.push(new Attribute("type", array));
        stack.push(id);
    }

    /**
     * init_declarator -> var_declarator
     * var_declarator.id
     * var_declarator.type
     * 将一个id加入符号表条目中
     */
    public void init_declarator1(LeftNode p) throws Exception {
        Attribute id = stack.pop();
        Type type = (Type) stack.pop().getValue();
        SymbolTable st = symTabStack.peek();
        SymbolTableEntry ste = new SymbolTableEntry(id.getValue().toString(), type);
        if (st.contains(ste)) {
            throw new Exception(errorHandle(id.getValue().toString() + "重复声明", p.getValue().getLineNum()));
        } else {
            st.addEntry(ste);
            stack.push(new Attribute("type", restoreType(type)));
        }

    }

    /**
     * init_declarator -> var_declarator = conditional_expression
     * initializer.addr
     * declarator.id
     * type_specifier.type
     */
    public void init_declarator2(LeftNode p) throws Exception {
        Expr expr = (Expr) stack.pop().getValue();
        Expr exprS = getRightAddrValueOne(expr);
        Attribute id = stack.pop();
        Type typeD = (Type) stack.pop().getValue();
        if (typeD.equals(expr.getType())) {
            SymbolTable st = symTabStack.peek();
            SymbolTableEntry ste = new SymbolTableEntry(id.getValue().toString(), typeD);
            if (st.contains(ste)) {
                throw new Exception(errorHandle(id.getValue().toString() + "重复声明", p.getValue().getLineNum()));
            } else {
                gen(new Quadruple("=", exprS.getValue(), "", id.getValue().toString()));
                st.addEntry(new SymbolTableEntry(id.getValue().toString(), typeD));
                stack.push(new Attribute("type", restoreType(typeD)));
            }
        } else {
            throw new Exception(errorHandle("初始化类型不匹配", p.getValue().getLineNum()));
        }
    }

    public Type restoreType(Type type) {
        Type ret = type;
        while (ret instanceof Array) {
            Array array = (Array) ret;
            ret = array.getType();
        }
        return ret;
    }

    ////////////////////函数定义////////////////////////////////

    /**
     * func_declarator -> id ( parameter_list )
     */
    public void func_declarator1(LeftNode p) {
        assert stack.peek() != null;
        SymbolTable args = (SymbolTable) stack.pop().getValue();
        String funcName = p.getRight().get(0).getTokenDescription();
        Type returnType = (Type) stack.pop().getValue();
        Function function = new Function(funcName, returnType);
        function.setArgs(args);
        SymbolTable st = symTabStack.peek();
        st.addEntry(new SymbolTableEntry(funcName, function));
        symTabStack.push(args);
        gen(new Quadruple("comment", "", "", funcName));
        stack.push(new Attribute("funcName", funcName));
    }

    /**
     * func_declarator -> id ( )
     * type:returnType
     */
    public void func_declarator2(LeftNode p) {
        String name = p.getRight().get(0).getTokenDescription();
        Type returnType = (Type) stack.pop().getValue();
        Function function = new Function(name, returnType);
        //当作函数开始的注释
        gen(new Quadruple("comment", "", "", name));
        SymbolTable st = symTabStack.peek();
        st.addEntry(new SymbolTableEntry(name, function));
        stack.push(new Attribute("funcName", name));
    }

    /**
     * parameter_list -> parameter_declaration
     * 函数的参数
     */
    public void parameter_list1(LeftNode p) {
        SymbolTable args = new SymbolTable();
        args.setPrevious(symTabStack.peek());
        SymbolTableEntry ste = (SymbolTableEntry) stack.pop().getValue();
        args.addEntry(ste);
        stack.push(new Attribute("args_list", args));
    }

    /**
     * parameter_list -> parameter_list , parameter_declaration
     */
    public void parameter_list2(LeftNode p) {
        SymbolTableEntry ste = (SymbolTableEntry) stack.pop().getValue();
        assert stack.peek() != null;
        SymbolTable args = (SymbolTable) stack.peek().getValue();
        args.addEntry(ste);
    }

    /**
     * parameter_declaration -> type_specifier var_declarator
     * var_declarator.id
     * var_declarator.type
     */
    public void parameter_declaration1(LeftNode p) {
        Attribute id = stack.pop();
        Attribute type = stack.pop();
        SymbolTableEntry ste = new SymbolTableEntry(id.getValue().toString(), (Type) type.getValue());
        stack.push(new Attribute("arg", ste));
    }

    /**
     * function_definition -> type_specifier func_declarator compound_statement
     * 结束一个函数定义,弹出设定参数的语句块
     * compound_statement.nextList有return在最后的话是没有语句需要回填的
     * 如果有return,则函数不需要回填
     * 如果没有return
     */
    public void function_definition1(LeftNode p) {
//        stack.pop(); //nextList
        symTabStack.pop();
    }


    //////////////////////程序语句/////////////////////////////////
    //////////////////////////////////////////////////////////////

    /**
     * M5 -> epsilon 新建符号表
     * 遇见程序块,新建一个符号表
     */
    public void m5(LeftNode p) {
        SymbolTable oldSt = symTabStack.peek();
        SymbolTable st = new SymbolTable();
        st.setPrevious(oldSt);
        symTabStack.push(st);
    }

    /**
     * compound_statement -> M5 { statement_list }
     * compound_statement -> M5 { declaration_list }
     * 结束程序块,返回上一个符号表
     * 继承nextList
     */
    public void compound2(LeftNode p) {
        symTabStack.pop();
    }

    /**
     * compound_statement -> M5 { declaration_list statement_list }
     * statement_list.nextList
     * declaration_list.nextList
     */
    public void compound4(LeftNode p) {
        symTabStack.pop();
        Attribute nextList1 = stack.pop();
        stack.pop();
        stack.push(nextList1);
    }

    /**
     * statement_list -> statement_list M6 statement
     * statement.nextList
     * M6.instr
     * statement.nextList
     */
    public void statement_list2(LeftNode p) {
        List<Integer> s2List = (List<Integer>) stack.pop().getValue();
        Integer instr = (Integer) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        backPatch(s1List, instr);
        stack.push(new Attribute("nextList", s2List));
    }

    /**
     * jump_statement -> return expression ;
     * expression.addr(Des,value,Type),这里必须返回一个右值
     * 在声明函数时，向栈中加入<函数名>作为其一个属性(),
     * 在遇到return语句时将其弹出，并且可以检查返回的类型是否正确
     * 如果将一个函数定义规约完成了函数名还没有弹出,
     * 说明没有return,进行报错
     */
    /**
     * 找不到该属性时返回空
     */
    public Attribute getAttributeByName(String name) {
        Deque<Attribute> temp = new LinkedList<>();
        Attribute result = null;
        while (!stack.isEmpty()) {
            Attribute a = stack.pop();
            if (!a.getName().equals(name)) {
                temp.push(a);
            } else {
                result = a;
                break;
            }
        }
        while (!temp.isEmpty()) {
            stack.push(temp.pop());
        }
        return result;
    }

    /**
     * jump_statement -> return expression ;
     */
    public void jump1(LeftNode p) throws Exception {
        String funcName = getAttributeByName("funcName").getValue().toString();
        Expr addr = (Expr) stack.pop().getValue();
        Expr addrS = getRightAddrValueOne(addr);
        SymbolTable st = symTabStack.peek();
        SymbolTableEntry ste = st.getUsedEntry(funcName);
        assert ste.getType() instanceof Function;
        Function function = (Function) ste.getType();
        if (function.getReturnType().equals(addrS.getType())) {
            gen(new Quadruple("return", getRightAddrValueOne(addr).getValue(), "", ""));
            List<Integer> nextList = new ArrayList<>();
            stack.push(new Attribute("jumpNextList", nextList));
        } else {
            throw new Exception(errorHandle(funcName + "返回值类型错误", p.getValue().getLineNum()));
        }

    }

    /**
     * jump_statement -> return ;
     */
    public void jump2(LeftNode p) throws Exception {
        String funcName = getAttributeByName("funcName").getValue().toString();
        SymbolTable st = symTabStack.peek();
        SymbolTableEntry ste = st.getUsedEntry(funcName);
        assert ste.getType() instanceof Function;
        Function function = (Function) ste.getType();
        if (function.getReturnType().equals(Type.Void)) {
            gen(new Quadruple("return", "", "", ""));
            List<Integer> nextList = new ArrayList<>();
            stack.push(new Attribute("jumpNextList", nextList));
        } else {
            throw new Exception(errorHandle(funcName + "返回值类型错误", p.getValue().getLineNum()));
        }

    }

    /**
     * M6 -> epsilon
     * 获取下一行语句的标号
     */
    public void m6(LeftNode p) {
        stack.push(new Attribute("instr", getNextInstr()));
    }

    /**
     * M9 -> epsilon
     * 为表达式添加控制流信息
     */
    public void m9(LeftNode p) {
        stack.push(new Attribute("control", null));
    }

    /**
     * selection_statement -> if M9 ( expression ) M6 statement
     */
    public void selection1(LeftNode p) {
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer ifInstr = (Integer) stack.pop().getValue();
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        List<Integer> sList = merge(s1List, falseList);
        backPatch(trueList, ifInstr);
        stack.push(new Attribute("SelectNextList", sList));
    }

    /**
     * M7 -> epsilon
     * 增加一条未填goto语句，并获取其标号拉链,待填
     */
    public void m7(LeftNode p) {
        List<Integer> li = makeList(getNextInstr());
        stack.push(new Attribute("nextList", li));
        gen(new Quadruple("goto", "", "", "_"));
    }

    /**
     * selection_statement -> if M9 ( expression ) M6 statement M7 else M6 statement
     * statement.nextList
     * M6.elseInstr
     * M7.nextList
     * statement.nextList
     * M6.ifInstr
     * expression.trueList
     * expression.falseList
     */
    public void selection2(LeftNode p) {
        List<Integer> s2List = (List<Integer>) stack.pop().getValue();
        Integer elseInstr = (Integer) stack.pop().getValue();
        List<Integer> m7List = (List<Integer>) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer ifInstr = (Integer) stack.pop().getValue();
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        backPatch(trueList, ifInstr);
        backPatch(falseList, elseInstr);
        List<Integer> temp = merge(s1List, m7List);
        List<Integer> sList = merge(temp, s2List);
        stack.push(new Attribute("SelectNextList", sList));
    }

    /**
     * M10 ->
     */
    public void m10(LeftNode p) {
        m6(p);
        m9(p);
    }

    /**
     * iteration_statement -> while M10 ( expression ) M6 statement
     * statement.nextList
     * M6.instr2
     * expression.trueList
     * expression.falseList
     * M6.instr1
     */
    public void iteration1(LeftNode p) {
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer instr2 = (Integer) stack.pop().getValue();
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        Integer begin = (Integer) stack.pop().getValue();
        backPatch(trueList, instr2);
        backPatch(s1List, begin);
        gen(new Quadruple("goto", "", "", "" + begin));
        stack.push(new Attribute("iterationNextList", falseList));
    }

    /**
     * iteration_statement -> do M6 statement while M9 ( expression ) ;
     * expression.trueList
     * expression.falseList
     * statement.nextList
     * M.instr
     */
    public void iteration2(LeftNode p) {
        List<Integer> trueList = (List<Integer>) stack.pop().getValue();
        List<Integer> falseList = (List<Integer>) stack.pop().getValue();
        List<Integer> s1List = (List<Integer>) stack.pop().getValue();
        Integer begin = (Integer) stack.pop().getValue();
        backPatch(trueList, begin);
        stack.push(new Attribute("iterationNextList", falseList));
    }

}
