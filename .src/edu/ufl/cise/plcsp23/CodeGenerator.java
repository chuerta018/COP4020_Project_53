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


    Stack<Integer> scope_stack= new Stack<Integer>();
    int curr = 0;
    int num = 1; //next serial number to assign
    void enterScope()
    { curr= num++;
        scope_stack.push(curr);
    }
    void closeScope()
    { curr = scope_stack.pop();
    }
    Vector<String> imported = new Vector<String>();
    CodeGenerator(String _packageName)
    {
        packageName = _packageName;
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        NameDef name = symbolTable.lookup(statementAssign.getLv().getIdent().getName());
        boolean type = true;
       StringBuilder temp = new StringBuilder();
       temp.append(statementAssign.getLv().visit(this,arg));
       temp.append(" = ");
        if(statementAssign.getE().getType() != name.getType()) {
            if (appendType(name.getType()).equals("String"))
            {
                temp.append("String.valueOf( ");
                type = false;
            }

            else
                temp.append("(" + appendType(name.getType()) + ") ");

        }

        if(statementAssign.getE() instanceof  BinaryExpr)
        {
            switch( ((BinaryExpr) statementAssign.getE()).getOp())
            {
                case GT,GE,LT,LE,EQ  ->
                {
                    temp.append(statementAssign.getE().visit(this, arg) + " ? 1 : 0");
                }
                case OR,AND->
                {
                    temp.append("(");
                    temp.append(statementAssign.getE().visit(this,arg) + " != 0) " + " ? 1 : 0");
                }
                default ->
                {
                    temp.append(statementAssign.getE().visit(this, arg));
                }
            }


        }else {
            temp.append(statementAssign.getE() .visit(this, arg));
        }
    if(type == false)
    {
        temp.append(")");
    }

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
                temp.append("(int)Math.pow(" + (String)binaryExpr.getLeft().visit(this,arg) + ", " + (String)binaryExpr.getRight().visit(this,arg) + "))");
            }
            default ->
            {
                temp.append((String) binaryExpr.getLeft().visit(this, arg));
                temp.append(appendKind(op));
                temp.append((String) binaryExpr.getRight().visit(this, arg));
                temp.append(")");
            }
        }


        //does not include boolean thing

        return temp.toString();
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
       StringBuilder temp = new StringBuilder();
       enterScope();

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
        closeScope();
        return temp.toString();
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {

        StringBuilder temp = new StringBuilder();
        temp.append("(");

        if(conditionalExpr.getGuard() instanceof BinaryExpr) {

            temp.append("(" + conditionalExpr.getGuard().visit(this,arg)+")");

        }else {
            temp.append("(" + conditionalExpr.getGuard().visit(this,arg)+"!= 0)");
        }

        temp.append(" ? " + conditionalExpr.getTrueCase().visit(this,arg) +" : " + conditionalExpr.getFalseCase().visit(this,arg));
        temp.append(")");
// does not do the boolean thingy
        return temp.toString();
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        temp.append(visitNameDef(declaration.getNameDef(),arg));
        boolean type = true;


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

            if(declaration.getInitializer() instanceof  BinaryExpr)
            {
                switch(((BinaryExpr) declaration.getInitializer()).getOp())
                {
                    case GT,GE,LT,LE,EQ,OR,AND ->
                    {
                        temp.append(declaration.getInitializer().visit(this, arg) + " ? 1 : 0");
                    }
                    default ->
                    {
                        temp.append(declaration.getInitializer().visit(this, arg));
                    }
                }


            }else {
                temp.append(declaration.getInitializer().visit(this, arg));
            }

        }

        if(type == false)
            temp.append(")");

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
        return ident.getName();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {

        return (String)identExpr.getName();
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
            temp.append(appendType(nameDef.getType())+" "+ nameDef.getIdent().getName() +"_" + curr);
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
        temp.append(returnStatement. getE().visit(this,arg));

        if(type == false)
            temp.append(")");

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

        temp.append(whileStatement.getBlock().visit(this,arg)+"\n}");

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
            case IDENT -> {}
            case TIMES ->
            {
                toJava.append(" * ");
            }
            case LSQUARE ->{}
            case COMMA -> {}
            case RES_x ->{}
            case LT ->
            {

                toJava.append(" < ");
            }
            case RES_y ->{}
            case AND ->
            {
                toJava.append(" && ");
            }
            case BANG ->{}
            case BITOR ->
            {
                toJava.append(" | ");
            }
            case COLON ->{}
            case BITAND ->
            {
                toJava.append(" & ");
            }
            case RES_int ->{}
            case RES_void ->{}
            case RES_image ->{}
            case RES_pixel ->{}
            case RES_string ->{}
            case GE ->
            {
                toJava.append(" >= ");
            }
            case RES_a_polar ->{}
            case RES_r_polar ->{}
            case LE ->
            {
                toJava.append(" <= ");
            }
            case RES_while ->{}
            case OR ->
            {
                toJava.append(" || ");
            }
            case RES_blu ->{}
            case RES_write ->{}
            case RES_cos ->{}
            case RES_grn ->{}
            case RES_red -> {}
            case RES_sin ->{}
            case RSQUARE ->{}
            case RES_atan ->{}
            case RES_x_cart ->{}
            case RES_y_cart -> {}
            case DOT ->{}
            case RES_a ->{}
            case RES_r ->{}
            case ASSIGN ->{}
            case LPAREN -> {}
            case RES_if ->{}
            case RPAREN ->{}
            case NUM_LIT ->{}
            case QUESTION ->{}
            case EOF ->{}
            case RES_Z ->{}
            case LCURLY ->{}
            case RCURLY ->{}
            case RES_rand ->{}
            case STRING_LIT ->{}
            case RES_X ->{}
            case RES_Y -> {}
            case RES_nil ->{}
            case EXCHANGE ->{}
            case RES_load ->{}
            case RES_display ->{}
            case ERROR ->{}
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

