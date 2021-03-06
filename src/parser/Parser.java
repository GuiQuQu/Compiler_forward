package parser;

import oldlexer.Token;
import oldlexer.TokenTable;
import parser.semantic.SemanticAction;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Author: Wang keLong
 * @DateTime: 20:22 2021/4/23
 * 按照LR文法来进行语法分析
 * 表示
 * 1. LR1项集使用 Set<LR1item>表示
 * 2. 文法符号使用RightNode表示
 * <p>
 * 待处理事项:
 * 1.生成语法分析树 √
 * 2.按照要求输出结果 √
 * 3.输出语法分析表
 * 4.构造合适的文法 √
 * 5.错误处理 √
 * <p>
 * 语义分析
 * 1. 构造翻译方案SDT
 * 2. 用代码实现翻译方案
 * 3. 可以根据名字调用指定的翻译动作
 * 4. 确定在LR(1)中何时执行相应的动作
 * 5. 建立符号表,以及与符号表相应的操作
 * <p/>
 */
public class Parser {
    //语法分析器的文法
    private Grammar grammar;
    //LR1分析表,key
//    private List<Set<LR1item>> lr1list = new ArrayList<>(); //LR1项目集族
    private List<LR1items> lr1list = new ArrayList<>();
    private Map<LR1TableKey, LR1TableValue> Action = new HashMap<>();
    private Map<LR1TableKey, LR1TableValue> SGoto = new HashMap<>();
    private boolean isError = false;
    static public Logger logger = Logger.getLogger("Logger");
    private Map<RightNode, Integer> priority = new HashMap<>();
    private  SemanticAction sA = new SemanticAction();
    static {
        logger.setLevel(Level.ALL);
//        ConsoleHandler consoleHandler = new ConsoleHandler();
//        consoleHandler.setLevel(Level.FINEST);
//        logger.addHandler(consoleHandler);
    }

    public Parser(String grammar_path) throws Exception {
        this.grammar = new Grammar(grammar_path);
        priority.put(new RightNode("else"), 1);  // else 悬空问题
        priority.put(new RightNode("M5"), 1); // M5 { 优先规约m5
        priority.put(new RightNode("M7"), 1); // M7 else 优先规约m7
        priority.put(new RightNode("M11"), 1); // M7 else 优先规约m11
        priority.put(new RightNode("M3"), 1); // M7 else 优先规约m3
        grammar.augmentGrammar();
        grammar.calFIRST();
        createLR1AnalysisTable();
        saveAnalysisTable();  //保存语法分析表
//        PrintLR1AnalysisTable();
    }

    public Parser(String grammar_path, String analysisTable, boolean debug) throws IOException {
        this.grammar = new Grammar(grammar_path);
        if (debug) {
            grammar.augmentGrammar();
            grammar.calFIRST();
            getLR1Items();
        }
        loadAnalysisTable(analysisTable);
    }

    public boolean isError() {
        return isError;
    }

    /**
     * 按照表格形式打印语法分析表
     */
    public void outputAnalysisTable() {

    }

    /**
     * 加载语法分析表
     */
    public void loadAnalysisTable(String path) throws IOException {
        File file = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        if (line.trim().equals("Action:")) {
            line = br.readLine();
            do {
//                4 d  reduce 2
                String[] strs = line.trim().split("\\s+");
                LR1TableKey key = new LR1TableKey(Integer.parseInt(strs[0]), new RightNode(strs[1]));
                LR1TableValue value = new LR1TableValue(strs[2], Integer.parseInt(strs[3]));
                Action.put(key, value);
                line = br.readLine();
            } while (!line.trim().equals("Goto:"));
            line = br.readLine();
            do {
//                3 C 7  3 C error -1
                String[] strs = line.trim().split("\\s+");
                LR1TableKey key = new LR1TableKey(Integer.parseInt(strs[0]), new RightNode(strs[1]));
                LR1TableValue value;
                if (strs[2].equals("error")) {
                    value = new LR1TableValue(strs[2], Integer.parseInt(strs[3]));
                } else {
                    value = new LR1TableValue("", Integer.parseInt(strs[2]));
                }
                SGoto.put(key, value);
                line = br.readLine();
            } while (line != null);
        }
    }

    /**
     * 保存语法分析表
     */
    public void saveAnalysisTable() throws IOException {
        System.out.println("正在保存分析表");
        File file = new File("analysisTable.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        OutputStream out = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write("Action:\n");
        for (LR1TableKey key : Action.keySet()) {
            bw.write(key.getStateNum() + " ");
            bw.write(key.getRn() + " ");
            bw.write(Action.get(key) + "\n");
        }
        bw.write("Goto:\n");
        for (LR1TableKey key : SGoto.keySet()) {
            bw.write(key.getStateNum() + " ");
            bw.write(key.getRn() + " ");
            bw.write(SGoto.get(key) + "\n");
        }
        bw.close();
    }

    public String errorHandle(int line, String info) {
        return "Syntax error at Line [" + line + "] " + "[" + info + "]";
    }

    /**
     * 按照先序遍历打印语法分析树
     */
    public String printAnalysisTree(Node root) {
        StringBuilder sb = new StringBuilder();
        Stack<Node> stack = new Stack<>();
        Set<Node> visited = new HashSet<>();
        stack.push(root);
        int i = 0; //当前层数
        while (!stack.isEmpty()) {
            Node node = stack.peek();
            if (!visited.contains(node)) {
                visited.add(node);//访问节点执行对应的语义动作...
                sb.append("  ".repeat(Math.max(0, i))).append(node.getRn().getValue());
                if (node.getChildren() == null) {
                    sb.append(":").append(node.getRn().getTokenDescription());
                }
                sb.append("(").append(node.getRn().getLineNum()).append(")").append("\n");
            }
            if (node.getChildren() != null) {
                boolean haveNotVisited = false;
                for (int j = 0; j < node.getChildren().size(); j++) {
                    if (!visited.contains(node.getChildren().get(j))) {
                        stack.push(node.getChildren().get(j));
                        haveNotVisited = true;
                        i++;
                        break;
                    }
                }
                if (!haveNotVisited) {
                    stack.pop();
                    i--;
                }
            } else {
                stack.pop();
                i--;
            }
        }
        return sb.toString();
    }

    /**
     * 将语法分析器的token表现形式转换为词法分析的保存形式
     */
    public List<RightNode> transFromOldLexer(List<Token> old_list) {
        TokenTable tt = new TokenTable();
        List<RightNode> rns = new ArrayList<>();
        for (Token token : old_list) {
            String value = token.getContent();
            String tokenDescription = tt.getDescription(token.getType());
            rns.add(new RightNode(tokenDescription, value, true, token.getLineNum()));
        }
        return rns;
    }

    public void PrintLR1AnalysisTable() {
        System.out.println("Action:");
        for (LR1TableKey key : Action.keySet()) {
            System.out.println(key.toString() + " -> " + Action.get(key).toString());
        }
        System.out.println("GoTo:");
        for (LR1TableKey key : SGoto.keySet()) {
            System.out.println(key.toString() + " -> " + SGoto.get(key).toString());
        }
    }

    public Node getAnalTree(List<LeftNode> actionList) {
        int i = actionList.size() - 1;
        Node root = new Node(actionList.get(i).getValue(), null); //根节点
        Stack<Node> stack = new Stack<>();
        stack.push(root);
        while (i >= 0 && stack.size() > 0) {
            Node rn = stack.pop();
            if (rn.getRn().isTerminator()) {
                continue;
            }
            LeftNode production = actionList.get(i);
            assert rn.getRn() == production.getValue();
            List<Node> list = new ArrayList<>();
            for (int j = 0; j < production.getRight().size(); j++) {
                Node newNode = new Node(production.getRight().get(j), null);
                list.add(newNode);
                stack.push(newNode);
            }
            rn.setChildren(list);
            i--;
        }
        return root;
    }

    public void printThreeAddressCode(){
        System.out.println(sA.printThreeAddressCode());
    }
    /**
     * 语法分析过程
     *
     * @param tokens tokens流,在使用前转换为parser中的表示形式
     * @return actionList 所有的规约操作
     */
    public List<LeftNode> grammarAnalysis(List<RightNode> tokens) throws Exception {
        List<LeftNode> actionList = new ArrayList<>();
        //增加结束标志
        tokens.add(RightNode.end);
        Stack<Integer> stateStack = new Stack<>(); //状态栈
        Stack<RightNode> symStack = new Stack<>(); //符号栈
//        SemanticAction sA = new SemanticAction();
        stateStack.push(0); //状态从0开始
        int i = 0;
        while (i < tokens.size()) { //输入流,只有输入流具有行号信息
            LR1TableValue ltv = Action.get(new LR1TableKey(stateStack.peek(), tokens.get(i))); //根据状态和当前输入终结符确定动作
            if (ltv == null) {
                System.out.println("error");
                isError = true;
                StringBuilder sb = new StringBuilder();
                int line = tokens.get(i).getLineNum();
                sb.append(tokens.get(i).getTokenDescription());
                System.out.println(errorHandle(line, sb.toString()));
                break;
            }
            if (ltv.getAction().equals("shift")) {
                symStack.push(tokens.get(i)); //将该符号移入
                stateStack.push(ltv.getUse()); //将状态转入指定状态
                System.out.println("移入" + tokens.get(i).getValue());
                i++;
            } else if (ltv.getAction().equals("reduce")) {
                LeftNode production = grammar.getProductions().get(ltv.getUse()); //规约使用的产生式
                List<RightNode> rights = new ArrayList<>();
                for (int j = 0; j < production.getRight().size(); j++) { //弹出右部文法符号
                    RightNode n = symStack.pop();
                    stateStack.pop();
                    rights.add(0, n);
                }
                int lineNum = 0;
                if (rights.size() > 0) {
                    lineNum = rights.get(0).getLineNum();
                }
                LeftNode treePro = new LeftNode(new RightNode(production.getValue().getValue(), "", false, lineNum));
                treePro.setRight(rights);
                // 添加语义动作
                treePro.setSemanticAction(production.getSemanticAction());
                //执行语义动作
                if (production.getSemanticAction().length() > 0) {
                    Method m = sA.getClass().getDeclaredMethod(production.getSemanticAction(), LeftNode.class);
                    m.invoke(sA, treePro);
                }
                symStack.push(treePro.getValue()); //压入新的非终结符
                LR1TableValue gotv = SGoto.get(new LR1TableKey(stateStack.peek(), production.getValue()));
                stateStack.push(gotv.getUse()); //转移到新的状态
                actionList.add(treePro); //记录规约操作，用于生成语法分析树
                System.out.println("使用" + production.toString() + "进行规约");
            } else if (ltv.getAction().equals("accept") && tokens.get(i) == RightNode.end) {
                System.out.println("接受");
                break;
            }
        }
        return actionList;
    }


    public void createLR1AnalysisTable() throws Exception {
        getLR1Items();
        System.out.println("正在计算语法分析表");
        for (int i = 0; i < lr1list.size(); i++) {
            LR1items state_i = lr1list.get(i); // 对应表的行，一行表示一个状态
            //Action表
            for (LR1item lr1item : state_i.getContent()) {
                LeftNode production = grammar.getProductions().get(lr1item.getGrammarNum());
                if (lr1item.getDotPoint() < production.getRight().size()) {
                    if (production.getRight().get(lr1item.getDotPoint()).isTerminator()) { //移进
                        RightNode a = production.getRight().get(lr1item.getDotPoint());
                        LR1items goto_i_a = Goto(state_i, a); // 状态i通过a转移到状态 goto_i_a
                        int j_index = lr1list.indexOf(goto_i_a);
                        if (j_index > 0) {
                            addAction(new LR1TableKey(i, a), new LR1TableValue("shift", j_index));
                        }
                    }
                } else { //规约或者接受
                    RightNode a = lr1item.getForward();
                    if (production.getValue().equals(grammar.getAugmentedStart())) {
                        addAction(new LR1TableKey(i, a), new LR1TableValue("accept", lr1item.getGrammarNum()));
                    } else {
                        addAction(new LR1TableKey(i, a), new LR1TableValue("reduce", lr1item.getGrammarNum()));
                    }
                }
            }
            //Goto表(state_i的goto表项)
            for (RightNode x : grammar.getNo_terminator()) {
                if (!x.equals(grammar.getAugmentedStart())) {
                    LR1items goto_i_x = Goto(state_i, x);
                    int j_index = lr1list.indexOf(goto_i_x);
                    if (j_index > 0)
                        addGoto(new LR1TableKey(i, x), new LR1TableValue("", j_index));
                }
            }
        }
    }

    /**
     * 计算LR1语法分析表
     */
    public void createLR1AnalysisTable1() throws Exception {
        getLR1Items();
//        PrintLR1List();
        System.out.println("正在计算语法分析表...");
        List<RightNode> allTerminator = new ArrayList<>(grammar.getTerminator());
        allTerminator.add(RightNode.end);  //为终结符集合中添加结束符号
        for (int i = 0; i < lr1list.size(); i++) {
            LR1items state_i = lr1list.get(i); // 对应表的行，一行表示一个状态
            //action表
            for (RightNode rightNode : allTerminator) {  // rightNode 对应action表头
                boolean isWrite = false;
                for (LR1item lr1item : state_i.getContent()) { // 对应状态i的一条产生式
                    LeftNode production = grammar.getProductions().get(lr1item.getGrammarNum());
                    if (lr1item.getDotPoint() < production.getRight().size()) { //只可能移进
                        if (production.getRight().get(lr1item.getDotPoint()).equals(rightNode)) {
                            LR1items goto_i_a = Goto(state_i, rightNode); // 状态i通过a转移到状态 goto_i_a
                            int j_index = lr1list.indexOf(goto_i_a);
                            if (j_index > 0) {
                                addAction(new LR1TableKey(i, rightNode), new LR1TableValue("shift", j_index));
                                isWrite = true;
                            }
                        }
                    } else { //点在产生式的最后，可能进行规约
                        if (lr1item.getForward().equals(rightNode)) {
                            isWrite = true;
                            if (lr1item.getGrammarNum() == grammar.getStartNum()) {
                                addAction(new LR1TableKey(i, rightNode), new LR1TableValue("accept", lr1item.getGrammarNum()));
                            } else {
                                addAction(new LR1TableKey(i, rightNode), new LR1TableValue("reduce", lr1item.getGrammarNum()));
                            }
                        }
                    }
                }
                if (!isWrite) {
                    addAction(new LR1TableKey(i, rightNode), new LR1TableValue("error", -1));
                }
            }
            //Goto表(state_i的goto表项)
            for (RightNode x : grammar.getNo_terminator()) {
                if (x.equals(grammar.getAugmentedStart())) {
                    continue;
                }
                LR1items goto_i_x = Goto(state_i, x);
                int j_index = lr1list.indexOf(goto_i_x);
                if (j_index > 0) {
                    addGoto(new LR1TableKey(i, x), new LR1TableValue("", j_index));
                } else {
                    addGoto(new LR1TableKey(i, x), new LR1TableValue("error", -1));
                }
            }
        }
    }

    /**
     * 计算LR1语法分析表的辅助函数
     */
    private int getPriority(RightNode rn) {
        if (priority.get(rn) == null) {
            return 0;
        } else {
            return priority.get(rn);
        }
    }

    private void addAction(LR1TableKey key, LR1TableValue value) throws Exception {
        if (Action.containsKey(key)) {
            //key (stateI,getRn) (13,{) -> (shift 22)/(reduce 38) 利用优先级解决了
            //shift 650 / reduce 56
            if (!Action.get(key).equals(value)) {  //可能会用不同的产生式均要求移入,但是操作不同一定发生了冲突
                RightNode rn1 = null;
                RightNode rn2 = null;
                if (Action.get(key).getAction().equals("shift")) {
                    rn1 = key.getRn();
                } else if (Action.get(key).getAction().equals("reduce")) {
                    rn1 = grammar.getProductions().get(Action.get(key).getUse()).getValue();
                }
                if (value.getAction().equals("shift")) {
                    rn2 = key.getRn();
                } else if (value.getAction().equals("reduce")) {
                    rn2 = grammar.getProductions().get(value.getUse()).getValue();
                }
                if (getPriority(rn2) > getPriority(rn1)) { // 二义性优先级要求 悬空else
                    // else 悬空问题 (优先移入else)
                    // M5 { (优先规约M5)
                    Action.put(key, value);
                    logger.info("二义性");
//                    System.out.println("填写Action表项" + key + "->" + value);
                } else if (getPriority(rn2) == getPriority(rn1)) { //发生冲突
                    logger.info(printStateI(key.getStateNum())); //记录冲突的状态
                    //记录冲突中规约使用的产生式
                    if (value.getAction().equals("reduce")) {
                        logger.info(value.toString() + ":" + grammar.getProductions().get(value.getUse()));
                    }
                    if (Action.get(key).getAction().equals("reduce")) {
                        logger.info(Action.get(key) + ":" + grammar.getProductions().get(Action.get(key).getUse()));
                    }
                    throw new Exception("Action出现冲突,表项为" + key.toString() + "冲突值为[" + Action.get(key) + "," + value + "]");
                }
            }
        } else { //不冲突
            Action.put(key, value);
//            System.out.println("填写Action表项" + key + "->" + value);
        }
    }

    /**
     * 计算LR1语法分析表的辅助函数
     */
    private void addGoto(LR1TableKey key, LR1TableValue value) throws Exception {
        if (SGoto.containsKey(key)) {
            if (!SGoto.get(key).equals(value)) {
                throw new Exception("GoTo出现冲突,表项为" + key.toString() + "冲突值为[" + SGoto.get(key) + "," + value + "]");
            }
        } else {
            SGoto.put(key, value);
//            System.out.println("填写GoTo表项" + key + "->" + value);
        }
    }

    /**
     * 得到一个文法的LR1项集族
     */
    public void getLR1Items() {
        System.out.println("正在计算文法的LR1项集族...");
        Set<LR1item> start = new HashSet<>();
        // 添加 argument -> .S,$
        start.add(new LR1item(grammar.getStartNum(), 0, RightNode.end));
        lr1list.add(closure(start));
        List<LR1items> list = new ArrayList<>(lr1list);
        List<LR1items> newList = new ArrayList<>();
        boolean notChange = true;
        do {
            notChange = true;
            newList = new ArrayList<>(); //一轮循环之后新加入的状态
            for (LR1items state : list) {  //对于一个状态i
                for (RightNode rightNode : grammar.getSymbol()) { //尝试用所有文法符号对其进行转移
                    LR1items gotoState = Goto(state, rightNode);
                    Set<LR1item> new_lr1Set = gotoState.getContent();
                    if (new_lr1Set.size() > 0 && !lr1list.contains(gotoState)) {
                        lr1list.add(gotoState);
                        newList.add(gotoState);
                        notChange = false;
                    }
                }
            }
            list = new ArrayList<>(newList);
            System.out.println("项集个数:" + lr1list.size());
        } while (!notChange);
    }

    /**
     * 计算一个LR1项集的闭包CLOSURE(I)
     */
    public LR1items closure(Set<LR1item> lr1items) {
        LR1items Closure = new LR1items();
        Closure.setCore(lr1items);
        Set<LR1item> result = new HashSet<>(lr1items); //添加核心项
        Set<LR1item> list = new HashSet<>(result);
        boolean notChange = true;
        do {
            notChange = true;
            Set<LR1item> new_list = new HashSet<>(); //新获取的LR1项
            for (LR1item item : list) { //对于I中的每一项 A-> alpha .B beta
                LeftNode production = grammar.getProductions().get(item.getGrammarNum()); // A-> alpha .B beta
                if (item.getDotPoint() < production.getRight().size()) {  //后面还有文法符号
                    RightNode B = production.getRight().get(item.getDotPoint());
                    for (int i = 0; i < grammar.getProductions().size(); i++) {  //对于G`中的每一个产生式 B->r
                        if (grammar.getProductions().get(i).getValue().equals(B)) {
                            Set<RightNode> firstSet = firstBetaA(item);
                            for (RightNode rightNode : firstSet) {
                                LR1item lr1i = new LR1item(i, 0, rightNode);
                                if (!result.contains(lr1i)) {
                                    result.add(lr1i);
                                    new_list.add(lr1i);
                                    notChange = false;
                                }
                            }
                        }
                    }
                }
            }
            list = new_list;
        } while (!notChange);
        Closure.setContent(result);
        return Closure;
    }

    /**
     * 添加Yi非空的展望符
     */
    private void addForwardWithEpsilon(Set<RightNode> forwords, RightNode Yi) {
        Set<RightNode> set = new HashSet<>(grammar.getFirsts().get(Yi));
        set.remove(RightNode.epsilon);
        forwords.addAll(set);
    }

    /**
     * closure的辅助函数,计算first (beta a)
     * beta=Y1Y2... 是B之后的所有产生式
     * A -> alpha .B beta ,a
     * A -> alpha .B ,a
     */
    private Set<RightNode> firstBetaA(LR1item item) {
        LeftNode p = grammar.getProductions().get(item.getGrammarNum());
        RightNode a = item.getForward(); //展望符a
        Set<RightNode> set = new HashSet<>();
        int i = item.getDotPoint() + 1;
        while (i < p.getRight().size()) {
            RightNode ri = p.getRight().get(i);
            //添加Yi非空的展望符
            addForwardWithEpsilon(set, ri);
            if (grammar.getFirsts().get(ri).contains(RightNode.epsilon)) {
                i++;
            } else {
                break;
            }
        }
        //说明beta是可以产生空串的
        if (i >= p.getRight().size()) {
            set.add(a);
        }
        return set;
    }

    /**
     * 计算GOTO(I,X)
     * <p>
     * 计算内核项
     */
    public LR1items Goto(LR1items i, RightNode x) {
        Set<LR1item> j = new HashSet<>();
        for (LR1item item : i.getContent()) {
            LeftNode production = grammar.getProductions().get(item.getGrammarNum());
            //可以向后移动一个位置并且点后面位置是X
            if (item.getDotPoint() < production.getRight().size() && production.getRight().get(item.getDotPoint()).equals(x)) {
                LR1item lr1i = new LR1item(item.getGrammarNum(), item.getDotPoint() + 1, item.getForward());
                j.add(lr1i);
            }
        }
        return closure(j);
    }

    /**
     * 打印LR(1)项集i
     */
    public String printStateI(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("I").append(i).append(':').append('\n');
        LR1items state = lr1list.get(i);
        for (LR1item item : state.getContent()) {
            if (state.isCore(item)) {
                sb.append("核心: ");
            }
            LeftNode production = grammar.getProductions().get(item.getGrammarNum());
            sb.append(production.getValue());
            sb.append(" -> ");
            boolean add = false;
            for (int j = 0; j < production.getRight().size(); j++) {
                if (item.getDotPoint() == j) {
                    sb.append(" /DOT/");
                    add = true;
                }
                sb.append(" ");
                sb.append(production.getRight().get(j));
            }
            if (!add) {
                sb.append(" /DOT/");
            }
            sb.append(" /FORWARD/ ").append(item.getForward()).append('\n');
            //.append(",").append(item.getDotPoint())
        }
        return sb.toString();
    }

    public void PrintLR1List() {
        for (int i = 0; i < lr1list.size(); i++) {
            System.out.println(printStateI(i));
        }
    }

    public String PrintLR1List(int begin, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = begin; i < end; i++) {
            sb.append(printStateI(i));
        }
        return sb.toString();
    }

    public int findLR1state(LR1item item) {
        for (int i = 0; i < lr1list.size(); i++) {
            if (lr1list.get(i).contains(item)) {
                return i;
            }
        }
        return -1;
    }
}
