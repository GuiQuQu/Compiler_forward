package parser;

import oldlexer.Token;
import oldlexer.TokenTable;

import java.io.*;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @Author: Wang keLong
 * @DateTime: 20:22 2021/4/23
 * 按照LR文法来进行语法分析
 * 表示
 * 1. LR1项集使用 List<LR1item>表示
 * 2. 文法符号使用RightNode表示
 * <p>
 * 待处理事项:
 * 1.生成语法分析树 √
 * 2.按照要求输出结果 √
 * 3.输出语法分析表
 * 4.构造合适的文法
 * 5.错误处理 √
 */
public class Parser {
    //语法分析器的文法
    private Grammar grammar;
    //LR1分析表,key
    private List<List<LR1item>> lr1list = new ArrayList<>();
    private Map<LR1TableKey, LR1TableValue> Action = new HashMap<>();
    private Map<LR1TableKey, LR1TableValue> SGoto = new HashMap<>();
    private boolean isError = false;
    static public Logger logger = Logger.getLogger("Logger");
    private Map<RightNode, Integer> priority = new HashMap<>();

    static {
        logger.setLevel(Level.ALL);
//        ConsoleHandler consoleHandler = new ConsoleHandler();
//        consoleHandler.setLevel(Level.FINEST);
//        logger.addHandler(consoleHandler);
    }

    public Parser(String grammar_path) throws Exception {
        this.grammar = new Grammar(grammar_path);
        priority.put(new RightNode("else"), 1);  // else 悬空问题
        grammar.augmentGrammar();
        grammar.calFIRST();
        createLR1AnalysisTable();
        saveAnalysisTable();  //保存语法分析表
//        PrintLR1AnalysisTable();
    }

    public Parser(String grammar_path, String analysisTable) throws IOException {
        this.grammar = new Grammar(grammar_path);
        grammar.augmentGrammar();
        grammar.calFIRST();
        getLR1Items();
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
                visited.add(node);
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
     * 将语法分析器的token表现形式转换为词法分析的
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

    //层序遍历生成语法分析树
    public Node getAnalTree(List<LeftNode> actionList) {
        int i = actionList.size() - 1;
        Node root = new Node(actionList.get(i).getValue(), null);
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        while (i >= 0 && queue.size() > 0) {
            Node rn = queue.poll();
            if (rn.getRn().isTerminator()) { //防止队列中的非终结符占用产生式进行扩展
                continue;
            }
            LeftNode production = actionList.get(i);
            if (rn.getRn().equals(production.getValue())) {
                List<Node> list = new ArrayList<>();
                for (int j = production.getRight().size() - 1; j >= 0; j--) {
                    Node newNode = new Node(production.getRight().get(j), null);
                    list.add(0, newNode);
                    queue.offer(newNode);
                }
                rn.setChildren(list);
            }
            i--;
        }
        return root;
    }

    /**
     * 语法分析过程
     *
     * @param tokens tokens流,在使用前转换为parser中的表示形式
     * @return actionList 所有的规约操作
     */
    public List<LeftNode> grammarAnalysis(List<RightNode> tokens) {
        List<LeftNode> actionList = new ArrayList<>();
        //增加结束标志
        tokens.add(RightNode.end);
        Stack<Integer> stateStack = new Stack<>(); //状态栈
        Stack<RightNode> symStack = new Stack<>(); //符号栈
        stateStack.push(0); //状态从0开始
        int i = 0;
        while (i < tokens.size()) { //输入流,只有输入流具有行号信息
            LR1TableValue ltv = Action.get(new LR1TableKey(stateStack.peek(), tokens.get(i))); //根据状态和当前输入终结符确定动作
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
                int linNum = rights.get(0).getLineNum();
                LeftNode treePro = new LeftNode(new RightNode(production.getValue().getValue(), "", false, linNum));
                treePro.setRight(rights);
                symStack.push(treePro.getValue()); //压入新的非终结符
                LR1TableValue gotv = SGoto.get(new LR1TableKey(stateStack.peek(), production.getValue()));
                stateStack.push(gotv.getUse()); //转移到新的状态
                actionList.add(treePro); //记录规约操作，用于生成语法分析树
                System.out.println("使用" + production.toString() + "进行规约");
            } else if (ltv.getAction().equals("accept") && tokens.get(i) == RightNode.end) {
                System.out.println("接受");
                break;
            } else {
                System.out.println("error");
                isError = true;
                StringBuilder sb = new StringBuilder();
                int line = symStack.peek().getLineNum();
                sb.append(tokens.get(i).getTokenDescription());
                System.out.println(errorHandle(line, sb.toString()));
                break;
            }
        }
        return actionList;
    }

    /**
     * 计算LR1语法分析表
     */
    public void createLR1AnalysisTable() throws Exception {
        getLR1Items();
//        PrintLR1List();
        System.out.println("正在计算语法分析表...");
        for (int i = 0; i < lr1list.size(); i++) {
            List<LR1item> state_i = lr1list.get(i); // 对应表的行，一行表示一个状态
            List<RightNode> allTerminator = new ArrayList<>(grammar.getTerminator());
            allTerminator.add(RightNode.end);  //为终结符集合中添加结束符号
            //action表
            for (RightNode rightNode : allTerminator) {  // rightNode 对应action表头
                boolean isWrite = false;
                for (LR1item lr1item : state_i) { // 对应状态i的一条产生式
                    LeftNode production = grammar.getProductions().get(lr1item.getGrammarNum());
                    if (lr1item.getDotPoint() < production.getRight().size()) { //只可能移进
                        if (production.getRight().get(lr1item.getDotPoint()).equals(rightNode)) {
                            List<LR1item> goto_i_a = Goto(state_i, rightNode); // 状态i通过a转移到状态 goto_i_a
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
                List<LR1item> goto_i_x = Goto(state_i, x);
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
    private void addAction(LR1TableKey key, LR1TableValue value) throws Exception {
        if (Action.containsKey(key)) {
            if (!Action.get(key).equals(value)) {  //可能会用不同的产生式均要求移入,但是操作不同一定发生了冲突
                if (priority.get(key.getRn()) != null && priority.get(key.getRn()) > 0) { // 二义性优先级要求 悬空else
                    Action.put(key, value);
                    logger.info("else悬空问题");
                    System.out.println("填写Action表项" + key + "->" + value);
                } else {
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
            System.out.println("填写Action表项" + key + "->" + value);
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
            System.out.println("填写GoTo表项" + key + "->" + value);
        }
    }

    /**
     * 得到一个文法的LR1项集族
     */
    public void getLR1Items() {
        System.out.println("正在计算文法的LR1项集族...");
        List<LR1item> start = new ArrayList<>();
        start.add(new LR1item(grammar.getStartNum(), 0, RightNode.end));
        lr1list.add(closure(start));
        List<List<LR1item>> list = new ArrayList<>(lr1list);
        boolean notChange = true;
        do {
            notChange = true;
            for (List<LR1item> lr1itemList : list) {  //对于一个状态i
                for (RightNode rightNode : grammar.getSymbol()) {
                    List<LR1item> new_lr1List = Goto(lr1itemList, rightNode);
                    if (new_lr1List.size() > 0 && !lr1list.contains(new_lr1List)) {
                        lr1list.add(new_lr1List);
                        notChange = false;
                    }
                }
            }
            list = new ArrayList<>(lr1list);
            System.out.println("项集个数:" + lr1list.size());
        } while (!notChange);
    }

    /**
     * 计算一个LR1项集的闭包CLOSURE(I)
     */
    public List<LR1item> closure(List<LR1item> lr1items) {
        List<LR1item> result = new ArrayList<>(lr1items); //添加核心项
        List<LR1item> list = new ArrayList<>(result);
        boolean notChange = true;
        do {
            notChange = true;
            List<LR1item> new_list = new ArrayList<>(); //新获取的LR1项
            for (LR1item item : list) { //对于I中的每一项 A-> alpha .B beta
                LeftNode production = grammar.getProductions().get(item.getGrammarNum()); // A-> alpha .B beta
                if (item.getDotPoint() < production.getRight().size()) {  //后面还有文法符号
                    RightNode B = production.getRight().get(item.getDotPoint());
                    RightNode beta = RightNode.epsilon; // 闭包新增的是First(beta a)
                    if (item.getDotPoint() + 1 < production.getRight().size()) {
                        beta = production.getRight().get(item.getDotPoint() + 1);
                    }
                    for (int i = 0; i < grammar.getProductions().size(); i++) {  //对于G`中的每一个产生式 B->r
                        if (grammar.getProductions().get(i).getValue().equals(B)) {
                            Set<RightNode> firstSet = first(beta, item.getForward());
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
        return result;
    }

    /**
     * closure的辅助函数
     */
    private Set<RightNode> first(RightNode beta, RightNode a) {
        Set<RightNode> set = new HashSet<>();
        if (RightNode.epsilon.equals(beta)) {
            set.add(a);
            return set;
        }
        if (grammar.getFirsts().get(beta).contains(RightNode.epsilon)) {
            set.add(a);
            set.addAll(grammar.getFirsts().get(beta));
            set.remove(RightNode.epsilon);
        } else {
            set.addAll(grammar.getFirsts().get(beta));
        }
        return set;
    }

    /**
     * 计算GOTO(I,X)
     * @return 返回内核项
     */
    public List<LR1item> Goto(List<LR1item> i, RightNode x) {
        List<LR1item> j = new ArrayList<>();
        for (LR1item item : i) {
            LeftNode production = grammar.getProductions().get(item.getGrammarNum());
            //可以向后移动一个位置
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
        List<LR1item> state = lr1list.get(i);
        for (LR1item item : state) {
            LeftNode production = grammar.getProductions().get(item.getGrammarNum());
            sb.append(production.getValue());
            sb.append(" -> ");
            boolean add =false;
            for (int j = 0; j < production.getRight().size(); j++) {
                if (item.getDotPoint()==j){
                    sb.append(" /DOT/");
                    add=true;
                }
                sb.append(" ");
                sb.append(production.getRight().get(j));
            }
            if (!add){
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
}
