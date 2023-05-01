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
        boolean type = true;
       StringBuilder temp = new StringBuilder();
        fromAssign = true;

       switch(name.getType())
       {
           case PIXEL ->
           {

                   temp.append(statementAssign.getLv().visit(this,arg));
                   temp.append(" = ");
                   if(statementAssign.getE().getType() != name.getType()) {
                       if (appendType(name.getType()).equals("String")) {
                           temp.append("String.valueOf( ");
                           type = false;
                       } else
                           temp.append("(" + appendType(name.getType()) + ") ");

                   }else
                   temp.append(statementAssign.getE().visit(this,arg));



           }
           case IMAGE ->
           {
               if(statementAssign.getLv().getPixelSelector() == null && statementAssign.getLv().getColor() == null )
               {

                   switch(statementAssign.getE().getType())
                   {
                       case STRING ->
                       {


                               temp.append("ImageOps.copyInto(FileURLIO.readImage(" + statementAssign.getE().visit(this,arg) +"),");
                               temp.append(name.getIdent().getName() +")");


                       }
                       case IMAGE ->
                       {


                               temp.append("ImageOps.copyInto(" + statementAssign.getE().visit(this,arg) +",");
                               temp.append(name.getIdent().getName() +")");

                       }
                       case PIXEL ->
                       {

                               temp.append("ImageOps.setAllPixels("+name.getIdent().getName()+",");
                               temp.append(statementAssign.getE().visit(this,arg) +")");

                       }
                   }

               }else if(statementAssign.getLv().getPixelSelector() != null && statementAssign.getLv().getColor() == null )
               {
                   temp.append("\nfor(int y = 0; y != "+statementAssign.getLv().getIdent().visit(this,arg)+".getHeight();y++){\n");
                   temp.append("for(int x = 0; x != "+statementAssign.getLv().getIdent().visit(this,arg)+".getWidth();x++){\n");
                   temp.append("ImageOps.setRGB(" + name.getIdent().getName()+"," +statementAssign.getLv().getPixelSelector().visit(this,arg)+",");
                   temp.append(statementAssign.getE().visit(this,arg));
                   temp.append(");\n}\n}");

               }else if(statementAssign.getLv().getPixelSelector() != null && statementAssign.getLv().getColor() != null )
               {
                   temp.append("\nfor(int y = 0; y != "+statementAssign.getLv().getIdent().visit(this,arg)+".getHeight();y++){\n");
                   temp.append("for(int x = 0; x != "+statementAssign.getLv().getIdent().visit(this,arg)+".getWidth();x++){\n");
                   temp.append("ImageOps.setRGB(" + name.getIdent().getName()+"," +statementAssign.getLv().getPixelSelector().visit(this,arg)+",");
                   temp.append("PixelOps.set"+ appendColor(statementAssign.getLv().getColor()) +"(");
                   temp.append("ImageOps.getRGB("+name.getIdent().getName()+","+statementAssign.getLv().getPixelSelector().visit(this,arg)+"),");
                   temp.append(statementAssign.getE().visit(this,arg) +"));\n}\n}");
               }
           }
           default ->
           {
               temp.append(statementAssign.getLv().visit(this,arg));
               temp.append(" = ");
               if(statementAssign.getE().getType() != name.getType())
               {
                   if (appendType(name.getType()).equals("String"))
                   {
                       temp.append("String.valueOf( ");
                       type = false;
                   }
                   else
                       temp.append("(" + appendType(name.getType()) + ") ");

               }
               temp.append(statementAssign.getE().visit(this, arg));

           }
       }


        //if(statementAssign.getE() != null)
       // temp.append(statementAssign.getE().visit(this, arg));

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



        switch (left.getType())
        {
            case IMAGE ->
            {
                if(right.getType() == Type.IMAGE && validOP(binaryExpr.getOp()))
                {
                    temp.append("(");

                    temp.append("ImageOps.binaryImageImageOp(ImageOps.OP." + binaryExpr.getOp() + ",");
                    temp.append(binaryExpr.getLeft().visit(this,arg) +",");
                    temp.append(binaryExpr.getRight().visit(this,arg) +")");

                    temp.append(")");
                } else if (binaryExpr.getRight().getType() == Type.INT && validOP(binaryExpr.getOp()))
                {
                    temp.append("(");

                    temp.append("ImageOps.binaryImageScalarOp(ImageOps.OP." + binaryExpr.getOp() + ",");
                    temp.append(binaryExpr.getLeft().visit(this,arg) +",");
                    temp.append(binaryExpr.getRight().visit(this,arg) +")");

                    temp.append(")");

                }

            }
            case PIXEL ->
            {
                if(binaryExpr.getRight().getType() == Type.PIXEL && validOP(binaryExpr.getOp()))
                {
                    temp.append("(");

                    temp.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP." + binaryExpr.getOp() + ",");
                    temp.append(binaryExpr.getLeft().visit(this,arg) +",");
                    temp.append(binaryExpr.getRight().visit(this,arg) +")");

                    temp.append(")");
                }else if(binaryExpr.getRight().getType() == Type.INT && validOP(binaryExpr.getOp()))
                {

                    temp.append("(");

                    temp.append("ImageOps.binaryPackedPixelIntOp(ImageOps.OP." + binaryExpr.getOp() + ",");
                    temp.append(binaryExpr.getLeft().visit(this,arg) +",");
                    temp.append(binaryExpr.getRight().visit(this,arg) +")");

                    temp.append(")");
                }

            }
            default ->
            {
                temp.append("(");

                switch (op) {
                    case EXP -> {

                        addVector("import java.lang.Math;");
                        temp.append("(int)Math.pow(" + (String) binaryExpr.getLeft().visit(this, arg) + ", " + (String) binaryExpr.getRight().visit(this, arg) + ")");

                        if (fromCond)
                            temp.append(" != 0");
                    }
                    case GT, GE, LT, LE, EQ -> {
                        if ((fromDec || fromAssign || fromReturn) && !fromCond) {


                            temp.append("(");
                            temp.append((String) binaryExpr.getLeft().visit(this, arg));
                            temp.append(appendKind(op));
                            temp.append((String) binaryExpr.getRight().visit(this, arg));
                            temp.append(") ? 1 : 0");

                        } else {
                            temp.append((String) binaryExpr.getLeft().visit(this, arg));
                            temp.append(appendKind(op));
                            temp.append((String) binaryExpr.getRight().visit(this, arg));

                        }

                    }
                    case OR, AND -> {
                        if (((fromDec || fromAssign || fromReturn) && !fromCond)) {

                            temp.append("((");
                            temp.append((binaryExpr.getLeft().visit(this, arg)));
                            temp.append(" != 0) " + appendKind(op));
                            temp.append("(" + binaryExpr.getRight().visit(this, arg) + " != 0)");
                            temp.append(") ? 1 : 0");


                        } else if (fromReturn && fromCond) {

                            temp.append("((");
                            temp.append((binaryExpr.getLeft().visit(this, arg)));
                            temp.append(" != 0) " + appendKind(op));
                            temp.append("(" + binaryExpr.getRight().visit(this, arg) + " != 0)");
                            temp.append(")");

                        } else {
                            temp.append((String) binaryExpr.getLeft().visit(this, arg));
                            temp.append(appendKind(op));
                            temp.append((String) binaryExpr.getRight().visit(this, arg));
                        }

                    }
                    default -> {
                        temp.append((String) binaryExpr.getLeft().visit(this, arg));
                        temp.append(appendKind(op));
                        temp.append((String) binaryExpr.getRight().visit(this, arg));

                    }
                }
                temp.append(")");
            }

        }


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


        if(declaration.getNameDef().getType() == Type.IMAGE)
        {
            temp.append(" = ");

            if(declaration.getNameDef().getDimension() == null)
            {
                switch(declaration.getInitializer().getType())
                {
                    case STRING ->
                    {

                        temp.append("FileURLIO.readImage(");
                        temp.append(declaration.getInitializer().visit(this,arg));
                        temp.append(")");
                    }
                    case IMAGE ->
                    {

                        temp.append("ImageOps.cloneImage(");
                        temp.append(declaration.getInitializer().visit(this,arg));
                        temp.append(")");
                    }
                    default -> throw new PLCException("Code generation declaration error");
                }
            }else // if there is deminsion
            {
                if(declaration.getInitializer() == null)
                {

                    temp.append("ImageOps.makeImage(");
                    temp.append(declaration.getNameDef().getDimension().visit(this,arg));
                    temp.append(")");

                }else//if there is initilizer
                {
                    switch(declaration.getInitializer().getType())
                    {
                        case STRING ->
                        {

                            temp.append("FileURLIO.readImage(");
                            temp.append(declaration.getInitializer().visit(this,arg) +",");
                            temp.append(declaration.getNameDef().getDimension().visit(this,arg));
                            temp.append(")");
                        }
                        case IMAGE ->
                        {

                            temp.append("ImageOps.copyAndResize(");
                            temp.append(declaration.getInitializer().visit(this,arg) +",");
                            temp.append(declaration.getNameDef().getDimension().visit(this,arg));
                            temp.append(")");
                        }
                        case PIXEL ->
                        {

                                temp.append("ImageOps.makeImage("+declaration.getNameDef().getDimension().visit(this,arg)+ ");\n");
                                temp.append("ImageOps.setAllPixels("+declaration.getNameDef().getIdent().getName() +",");
                                temp.append(declaration.getInitializer().visit(this,arg) +")");


                        }
                        default -> throw new PLCException("Code generation declaration error");
                    }

                }


            }

        }else if(declaration.getInitializer() != null )
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
        StringBuilder temp = new StringBuilder();
        temp.append(dimension.getWidth().visit(this,arg) +","+ dimension.getHeight().visit(this,arg));


        return temp.toString();
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();


        temp.append("PixelOps.pack(" + expandedPixelExpr.getRedExpr().visit(this,arg)+ ",");
        temp.append(expandedPixelExpr.getGrnExpr().visit(this,arg) + ",");
        temp.append(expandedPixelExpr.getBluExpr().visit(this,arg));
        temp.append(")");

        return temp.toString();
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
//remember reutnr statement adjustment of the else
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

       //=== PA6 said not to implement ===//
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();
        temp.append(pixelSelector.getX().visit(this,arg)+","+pixelSelector.getY().visit(this,arg) );

        return temp.toString();
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
       StringBuilder temp = new StringBuilder();
        IToken.Kind kind = predeclaredVarExpr.getKind();
       switch (kind)
       {
           case RES_y ->
           {
               temp.append("y");
           }
           case RES_x ->
           {
               temp.append("x");
           }
       }

        return temp.toString();
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
                temp.append(returnStatement. getE().visit(this,arg));

            } else
                temp.append("(" + appendType(rootNode.getType()) + ") " + returnStatement.getE().visit(this, arg));
        }else
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

        StringBuilder temp = new StringBuilder();

        if(unaryExpr.getE().getType()==Type.INT) {
            temp.append("(");
            switch (unaryExpr.getOp()) {
                case BANG ->
                {
                    temp.append(unaryExpr.getE().visit(this,arg));
                    temp.append(" == 0 ? 1 :0");


                }
                case MINUS ->
                {
                    temp.append("-");
                    temp.append(unaryExpr.getE().visit(this,arg));
                }
                default -> throw new PLCException("Error in unary:omitted sin cos ata aka dont use");

            }
            temp.append(")");
        }


        return temp.toString();
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        StringBuilder temp = new StringBuilder();

        switch(unaryExprPostfix.getPrimary().getType())
        {
            case IMAGE ->
            {
                if(unaryExprPostfix.getPixel()!= null && unaryExprPostfix.getColor()== null)
                {
                    temp.append("ImageOps.getRGB(");
                    temp.append(unaryExprPostfix.getPrimary().visit(this,arg) +",");
                    temp.append(unaryExprPostfix.getPixel().visit(this,arg));
                    temp.append(")");

                }else if(unaryExprPostfix.getPixel()!= null && unaryExprPostfix.getColor()!= null)
                {
                    temp.append("PixelOps." + appendColor(unaryExprPostfix.getColor()).toLowerCase() + "(ImageOps.getRGB(");
                    temp.append(unaryExprPostfix.getPrimary().visit(this,arg) +",");
                    temp.append(unaryExprPostfix.getPixel().visit(this,arg));
                    temp.append("))");

                }else if(unaryExprPostfix.getPixel()== null && unaryExprPostfix.getColor()!= null)
                {
                    temp.append("ImageOps.extract" + appendColor(unaryExprPostfix.getColor()) + "(");
                    temp.append(unaryExprPostfix.getPrimary().visit(this,arg));
                    temp.append(")");

                }

            }
            case PIXEL ->
            {
                if(unaryExprPostfix.getPixel()== null && unaryExprPostfix.getColor()!= null)
                {
                    temp.append("PixelOps." +appendColor(unaryExprPostfix.getColor()).toLowerCase()+"(");
                    temp.append(unaryExprPostfix.getPrimary().visit(this,arg));
                    temp.append(")");
                }


            }
            default ->
            {
                temp.append(unaryExprPostfix.getPrimary().visit(this,arg));
            }
        }


        return temp.toString();
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
       if(statementWrite.getE().getType() == Type.PIXEL)
           temp.append("ConsoleIO.writePixel("+statementWrite.getE().visit(this,arg)+")");
       else
       temp.append("ConsoleIO.write(" +statementWrite.getE().visit(this,arg)+")");

        return temp.toString();
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {

        return "255";
    }


    public String appendType(Type type)
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
                toJava.append("BufferedImage");
                addVector("import java.awt.image.BufferedImage;");
                addVector("import edu.ufl.cise.plcsp23.runtime.ImageOps;");
                addVector("import edu.ufl.cise.plcsp23.runtime.PixelOps;");
                addVector("import edu.ufl.cise.plcsp23.runtime.FileURLIO;");
            }
            case PIXEL ->
            {
                toJava.append("int");
                addVector("import java.awt.image.BufferedImage;");
                addVector("import edu.ufl.cise.plcsp23.runtime.ImageOps;");
                addVector("import edu.ufl.cise.plcsp23.runtime.PixelOps;");
                addVector("import edu.ufl.cise.plcsp23.runtime.FileURLIO;");
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


    public static Boolean validOP(IToken.Kind op)
    {
        switch (op)
        {
            case MOD,MINUS,PLUS, DIV,TIMES ->
            {
                return true;
            }
            default ->
            {
                return false;
            }
        }
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

    public static String appendColor(ColorChannel color)  {

        StringBuilder temp = new StringBuilder();
        switch (color)
        {
            case blu ->
            {
             temp.append("Blu");
            }
            case grn ->
            {
                temp.append("Grn");
            }
            case red ->
            {
                temp.append("Red");
            }
            default -> temp.append("");
        }

    return temp.toString();

    }


    void addVector(String  dupe)
    {
        if(!imported.contains(dupe))
        {
            imported.add(dupe);
        }
    }
}

