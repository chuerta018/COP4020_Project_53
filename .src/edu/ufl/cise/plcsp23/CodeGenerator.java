package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.util.*;


public class CodeGenerator implements ASTVisitor {
    public static class SymbolTable
    {

        HashMap<String, NameDef> entries = new HashMap<String,NameDef>();


        //returns true if name successfully inserted in symbol table, false if already present
        public boolean insert(String name, NameDef namedef) {

            return (entries.putIfAbsent(name,namedef) == null);
        }
        //returns Declaration if present, or null if name not declared.
        public NameDef lookup(String name) {
            return entries.get(name);
        }
        public int size()
        {
            return entries.size();

        }


    }

    String packageName;
    SymbolTable symbolTable = new SymbolTable();// to retain dupes
    StringBuilder JavaClass = new StringBuilder();
    Program rootNode;

    static boolean fromDec = false;
    static boolean fromAssign =  false;
    static boolean fromCond = false;
    static boolean fromReturn = false;



    Stack<Integer> scope_stack= new Stack<Integer>();
    int curr = 0;
    int num = 1; //next serial number to assign
    void enterScope()
    {
        scope_stack.push(curr);
        curr= num;
        num++;


    }
    void closeScope()
    { curr = (int)scope_stack.pop();
    }


    NameDef loopUpScope (String name)
    {

        int scope = curr;

    if( curr != 0)
    {

        for(int i = 0 ; i < scope_stack.size(); i++)
        {
            if(symbolTable.lookup(name+"_" +scope) != null)
                return symbolTable.lookup(name+"_" +scope);

                scope = scope_stack.elementAt(i);

        }

    }

    return symbolTable.lookup(name);
    }
    Vector<String> imported = new Vector<String>();
    CodeGenerator(String _packageName)
    {
        packageName = _packageName;
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
       NameDef name = loopUpScope(statementAssign.getLv().getIdent().getName());
       /* if(curr == 0)
        name = symbolTable.lookup(statementAssign.getLv().getIdent().getName());
        else name = symbolTable.lookup(statementAssign.getLv().getIdent().getName() +"_" +curr);
*/
        boolean type = true;
       StringBuilder temp = new StringBuilder();
       temp.append(statementAssign.getLv().visit(this,arg));
       temp.append(" = ");
       fromAssign = true;
        if(statementAssign.getE().getType() != name.getType()) {
            if (appendType(name.getType()).equals("String"))
            {
                temp.append("String.valueOf( ");
                type = false;
            }

            else
                temp.append("(" + appendType(name.getType()) + ") ");

        }
        if(statementAssign.getE() != null)
        temp.append(statementAssign.getE().visit(this, arg));

    if(type == false)
    {
        temp.append(")");
    }
fromAssign = false;
        return temp.toString();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        IToken.Kind op = binaryExpr.getOp();
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();
        temp.append("(");



        switch(op)
        {
            case EXP ->
            {

                addVector("import java.lang.Math;");
                temp.append("(int)Math.pow(" + (String)binaryExpr.getLeft().visit(this,arg) + ", " + (String)binaryExpr.getRight().visit(this,arg) + ")");

                if(fromCond)
                    temp.append(" != 0");
            }
            case GT,GE,LT,LE,EQ ->
            {
                if((fromDec || fromAssign || fromReturn) && !fromCond)
                {


                    temp.append("(");
                    temp.append((String) binaryExpr.getLeft().visit(this, arg));
                    temp.append(appendKind(op));
                    temp.append((String) binaryExpr.getRight().visit(this, arg));
                    temp.append(") ? 1 : 0");

                }
                else
                {
                    temp.append((String) binaryExpr.getLeft().visit(this, arg));
                    temp.append(appendKind(op));
                    temp.append((String) binaryExpr.getRight().visit(this, arg));

                }

            }
            case OR,AND -> {
                if (((fromDec || fromAssign || fromReturn ) && !fromCond))
                {

                    temp.append("((");
                    temp.append((binaryExpr.getLeft().visit(this, arg)));
                    temp.append(" != 0) " + appendKind(op));
                    temp.append("("+ binaryExpr.getRight().visit(this,arg) +" != 0)");
                    temp.append(") ? 1 : 0");


                }else if(fromReturn && fromCond)
                {

                    temp.append("((");
                    temp.append((binaryExpr.getLeft().visit(this, arg)));
                    temp.append(" != 0) " + appendKind(op));
                    temp.append("("+ binaryExpr.getRight().visit(this,arg) +" != 0)");
                    temp.append(")");

                }
                else
                {
                    temp.append((String) binaryExpr.getLeft().visit(this, arg));
                    temp.append(appendKind(op));
                    temp.append((String) binaryExpr.getRight().visit(this, arg));
                }

            }
            default ->
            {
                temp.append((String) binaryExpr.getLeft().visit(this, arg));
                temp.append(appendKind(op));
                temp.append((String) binaryExpr.getRight().visit(this, arg));

            }
        }



        temp.append(")");
        //does not include boolean thing

        return temp.toString();
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
       StringBuilder temp = new StringBuilder();


        List<Declaration> dec = block.getDecList();

        for (AST node : dec) {
           temp.append(node.visit(this, arg));
           temp.append(";\n");
        }

        List<Statement> Statements = block.getStatementList();

        for (AST node : Statements) {
            temp.append(node.visit(this, arg));
            temp.append(";\n");
        }

        return temp.toString();
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {

        StringBuilder temp = new StringBuilder();
        fromCond = true;
        temp.append("(");

        if(conditionalExpr.getGuard() instanceof BinaryExpr) {

            temp.append("(" + conditionalExpr.getGuard().visit(this,arg)+")");

        }else {
            temp.append("(" + conditionalExpr.getGuard().visit(this,arg)+"!= 0)");
        }

        temp.append(" ? " + conditionalExpr.getTrueCase().visit(this,arg) +" : " + conditionalExpr.getFalseCase().visit(this,arg));
        temp.append(")");
        fromCond = false;
// does not do the boolean thingy
        return temp.toString();
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        temp.append(visitNameDef(declaration.getNameDef(),arg));
        boolean type = true;
        fromDec = true;



        if(declaration.getInitializer() != null)
        {
            temp.append(" = ");
            if(declaration.getInitializer().getType() != declaration.getNameDef().getType())
            {
                if(appendType(declaration.getNameDef().getType()).equals("String"))
                {
                    temp.append("String.valueOf( ");
                    type = false;

                }else
                temp.append("("+ appendType(declaration.getNameDef().getType())+ ") ");
            }

            temp.append(declaration.getInitializer().visit(this, arg));

        }



        if(type == false)
            temp.append(")");

        fromDec = false;
        return temp.toString();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();

        if(curr != 0)
        {
            if(symbolTable.lookup(ident.getName() +"_"+curr)!=null)
            temp.append(ident.getName() +"_"+curr);
            else
                temp.append(ident.getName());


        }else
        {
            temp.append(ident.getName());
        }

        return temp.toString();

    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {

        StringBuilder temp = new StringBuilder();

        if(curr != 0)
        {
            if(symbolTable.lookup(identExpr.getName() +"_"+curr)!=null)
                temp.append(identExpr.getName() +"_"+curr);
            else
                temp.append(identExpr.getName());

        }else
        {
            temp.append(identExpr.getName());
        }
        return temp.toString();
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {


        return lValue.getIdent().visit(this,arg);
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName();
        boolean inserted = symbolTable.insert(name, nameDef);
        StringBuilder temp = new StringBuilder();

        if(!inserted)
        {
            symbolTable.insert((name+"_"+curr),nameDef);
            temp.append(appendType(nameDef.getType())+" "+ nameDef.getIdent().getName() +"_" + curr);
        }
          else
            temp.append(appendType(nameDef.getType())+" "+ nameDef.getIdent().getName());

        return temp.toString();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        temp.append(Integer.toString(numLitExpr.getValue()));
        return temp.toString();
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        rootNode = program;
        addVector(packageName);

        JavaClass.append("public class " + program.getIdent().getName() + " {\n\t" + " public static " + appendType(program.getType()) + " apply(");

        List<NameDef> parametersCheck  = program.getParamList();
        ListIterator<NameDef> nameDefIter = parametersCheck.listIterator();
        while (nameDefIter.hasNext())
        {
            JavaClass.append((String) nameDefIter.next().visit(this, arg));
            if(nameDefIter.hasNext() == true)
            JavaClass.append(", ");
        }

        JavaClass.append("){\n");

        List<Declaration> dec = program.getBlock().getDecList();

        for (AST node : dec) {
            JavaClass.append( node.visit(this, arg));
            JavaClass.append(";\n");
        }

        List<Statement> Statements = program.getBlock().getStatementList();

        for (AST node : Statements) {
            JavaClass.append(node.visit(this, arg));
            JavaClass.append(";\n");
        }

        JavaClass.append("\t}\n}");

        if(!imported.isEmpty())
        {
            StringBuilder temp = new StringBuilder();
            for(int i = 0; i< imported.size();i++)
            {
                temp.append(imported.get(i) +"\n");
            }
            temp.append(JavaClass.toString());
            return temp.toString();
        }

        return JavaClass.toString();
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        temp.append("(int)Math.floor(Math.random()*256)");
        addVector("import java.lang.Math;");

        return temp.toString();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        boolean type = true;
        fromReturn =  true;
        temp.append("return ");

        if(rootNode.getType() != returnStatement.getE().getType())
        {


            if (appendType(rootNode.getType()).equals("String"))
            {
                type = false;
                temp.append("String.valueOf(" );

            } else
                temp.append("(" + appendType(rootNode.getType()) + ") " + returnStatement.getE().visit(this, arg));
        }
/*
        if(returnStatement.getE() instanceof BinaryExpr)
        {
            switch(((BinaryExpr) returnStatement.getE()).getOp())
            {
                case GT,GE,LT,LE,EQ,AND,OR ->
                {
                    temp.append(returnStatement.getE().visit(this, arg) + " ? 1 : 0");
                }
                default -> temp.append(returnStatement. getE().visit(this,arg));
            }
        }else
        */

            temp.append(returnStatement. getE().visit(this,arg));

        if(type == false)
            temp.append(")");

        fromReturn =  false;
        return temp.toString();
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
      StringBuilder temp = new StringBuilder();
      temp.append("\"" + stringLitExpr.getValue() + "\"");
        return temp.toString();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
       StringBuilder temp = new StringBuilder();


        if(whileStatement.getGuard() instanceof BinaryExpr){
            switch(((BinaryExpr) whileStatement.getGuard()).getOp())
            {
                case PLUS,MINUS,DIV,EXP,MOD,TIMES ->
                {
                    temp.append("while (" + whileStatement.getGuard().visit(this,arg) + " != 0){\n");
                }
                default ->
                {
                    temp.append("while (" + whileStatement.getGuard().visit(this,arg) + "){\n");
                }
            }
        }
        else // for ran
        {
            temp.append("while (" + whileStatement.getGuard().visit(this,arg) + " != 0){\n");
        }
        enterScope();
        temp.append(whileStatement.getBlock().visit(this,arg)+"\n}");
        closeScope();

// does not do boolean thing and redeclare variables?!
        return temp.toString();
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
       StringBuilder temp = new StringBuilder();
       addVector("import edu.ufl.cise.plcsp23.runtime.ConsoleIO;");
       temp.append("ConsoleIO.write(" +statementWrite.getE().visit(this,arg)+")");

        return temp.toString();
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {

        return "255";
    }


    public static String appendType(Type type)
    {
        StringBuilder toJava = new StringBuilder();

        switch(type)
        {
            case INT->
            {
                toJava.append("int");
            }
            case IMAGE->
            {
                return null;
            }
            case PIXEL ->
            {
                return null;
            }
            case STRING ->
            {
                toJava.append("String");
            }
            case VOID ->
            {
                toJava.append("void");
            }
        }


        return toJava.toString();
    }


    public static String appendKind(IToken.Kind kind)
    {
        StringBuilder toJava = new StringBuilder();
        switch(kind)
        {
            case MINUS ->
            {
                toJava.append(" - ");
            }
            case PLUS ->
            {
                toJava.append(" + ");
            }
            case DIV ->
            {
                toJava.append(" / ");
            }
            case EQ ->
            {
                if(fromDec)
                {

                }
                toJava.append(" == ");
            }
            case GT ->
            {
                toJava.append(" > ");
            }
            case MOD ->
            {
                toJava.append(" % ");
            }
            case TIMES ->
            {
                toJava.append(" * ");
            }
            case LT ->
            {

                toJava.append(" < ");
            }

            case AND ->
            {
                toJava.append(" && ");
            }

            case BITOR ->
            {
                toJava.append(" | ");
            }
            case BITAND ->
            {
                toJava.append(" & ");
            }
            case GE ->
            {
                toJava.append(" >= ");
            }
            case LE ->
            {
                toJava.append(" <= ");
            }
            case OR ->
            {
                toJava.append(" || ");
            }

            default ->
            {
                throw new RuntimeException(" some issue tbh");
            }
        }
        return toJava.toString();
    }


    void addVector(String  dupe)
    {
        if(!imported.contains(dupe))
        {
            imported.add(dupe);
        }
    }
}

