package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import static edu.ufl.cise.plcsp23.ast.Type.*;

public class TypeChecker implements  ASTVisitor {

/*

    public static class Pair
    {
        NameDef namedef;
        int scope;
        Pair(NameDef _nameDef, int _scope)
        {
            namedef = _nameDef;
            scope = _scope;
        }

        public int getScope()
        {
            return scope;
        }

        public NameDef getPairName()
        {
            return namedef;
        }

    }
*/



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

    SymbolTable symbolTable;
    Vector<SymbolTable> ourTables = new Vector<SymbolTable>();

    int curr = 0;

    public void enterScope()
    {
        curr ++;
        ourTables.add(symbolTable);

        SymbolTable temp = new SymbolTable();
        symbolTable = temp;
    }

    public void leaveScope()
    {
        ourTables.remove(curr);
        curr--;
        symbolTable = ourTables.get(curr);
    }


    public NameDef lookup(String name)
    {
        for(int i = ourTables.size()-1; i>=0; i--)
        {
          if (ourTables.get(i).lookup(name) != null )
          {
              return ourTables.get(i).lookup(name);
          }
        }

        return null;
    }


    Program rootNode;






    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        zExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        randomExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        predeclaredVarExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        IToken.Kind op = binaryExpr.getOp();

        Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRight().visit(this, arg);


        Type resultType = null;
        switch(op) {
            case BITAND,BITOR -> // |, &
            {
                check(leftType == rightType, "incompatible types for comparison");
               if(leftType != PIXEL)
                   check(false,"not a pixel");

                resultType = PIXEL;
            }
            case AND,OR, LT, GT, LE, GE -> { // ||, &&,<,>,<=,>=
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else check(false,  "incompatible types for operator");
            }
            case EXP-> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == PIXEL && rightType == Type.INT) resultType = PIXEL;
                else check(false, "incompatible types for operator");
            }
            case EQ -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == PIXEL && rightType == PIXEL) resultType = Type.INT;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.INT;
                else if (leftType == Type.STRING && rightType == Type.STRING) resultType = Type.INT;
                else check(false, "incompatible types for operator");
            }
            case PLUS -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == PIXEL && rightType == PIXEL) resultType = PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else if (leftType == Type.STRING && rightType == Type.STRING) resultType = Type.STRING;
                else check(false, "incompatible types for operator");
            }
            case MINUS-> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == PIXEL && rightType == PIXEL) resultType = PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else check(false, "incompatible types for operator");
            }
            case DIV,MOD,TIMES-> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == PIXEL && rightType == PIXEL) resultType = PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else if (leftType == PIXEL && rightType == Type.INT) resultType = PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.INT) resultType = Type.IMAGE;
                else check(false, "incompatible types for operator");
            }

            default -> throw new TypeCheckException("compiler error");
        }
        binaryExpr.setType(resultType);
        return resultType;
    }

@Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        IToken.Kind kind = pixelFuncExpr.getFunction();
        Type exprType = (Type) pixelFuncExpr.getSelector().visit(this, arg);
        switch(kind)
        {
            case RES_x_cart,RES_y_cart,RES_a_polar,RES_r_polar ->
            {
                pixelFuncExpr.setType(INT);
                return INT;
            }
            default -> throw new TypeCheckException("compiler error");
        }

    }
    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        IToken.Kind op = unaryExpr.getOp();

        Type exprType = (Type) unaryExpr.getE().visit(this, arg);
        Type resultType = null;
        switch(op)
        {
            case BANG -> // |, &
            {
                if (exprType == Type.INT) resultType = INT;
                else if(exprType == PIXEL)  resultType = PIXEL;
                else check(false,  "Type error: visitUnaryExpr");
            }
            case MINUS,RES_cos,RES_sin,RES_atan ->
            { // ||, &&,<,>,<=,>=

                check(exprType == INT,"Type error: visitUnaryExpr");
                resultType = INT;

            }
            default -> throw new TypeCheckException("compiler error");
        }

        unaryExpr.setType(resultType);
        return resultType;
    }
    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {


        Type guardType = (Type) conditionalExpr.getGuard().visit(this, arg);
        Type trueType = (Type) conditionalExpr.getTrueCase().visit(this, arg);
        Type falseType = (Type) conditionalExpr.getFalseCase().visit(this, arg);
        Type resultType = null;

        if(guardType == Type.INT && trueType == falseType)
            resultType = trueType;
        else
            check(false,"Type Error: Visit ConditionalExpr");

        conditionalExpr.setType(resultType);
        return resultType;
    }


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        Type xType = (Type) pixelSelector.getX().visit(this, arg);
        Type yType = (Type) pixelSelector.getY().visit(this, arg);

        if(xType != Type.INT && yType != Type.INT)
            check(false,"Type Error: VisitPixelSelector");


        return null;

    }


///===================== FUNCTIONS I DEEM NOT THE ISSUE ======================////




    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        Type rType = (Type) expandedPixelExpr.getRedExpr().visit(this, arg);
        Type gType = (Type) expandedPixelExpr.getGrnExpr().visit(this, arg);
        Type bType = (Type) expandedPixelExpr.getBluExpr().visit(this, arg);



        if(rType == INT && gType == INT && bType == INT)
        {
            expandedPixelExpr.setType(PIXEL);
            return PIXEL;
        }
        else
            check(false,"Type Error: VisitPixelSelector");

      return null; // error
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        Type wType = (Type) dimension.getWidth().visit(this, arg);
        Type hType = (Type) dimension.getHeight().visit(this, arg);

        if(wType != Type.INT && hType != Type.INT)
            check(false,"Type Error: VisitPixelSelector");


        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        visitIdent(lValue.getIdent(),arg);
        NameDef name = lookup(lValue.getIdent().getName());
        Type identType = name.getType();
        PixelSelector ps =  lValue.getPixelSelector();
        ColorChannel cc = lValue.getColor();
        Type resultType = null;

        switch(identType)
        {
            case IMAGE ->
            {
                if(ps == null && cc == null) resultType = Type.IMAGE;
                     else if(ps != null && cc == null) resultType = PIXEL;
                          else if(ps == null && cc != null) resultType = Type.IMAGE;
                               else if(ps != null && cc != null) resultType = Type.INT;
                                    else check(false, "lvalue type error");

            }
            case PIXEL ->
            {
                if(ps == null && cc == null)resultType = PIXEL;
                     else if(ps == null && cc != null)resultType = Type.INT;
                         else check(false, "lvalue type error");
            }
            case STRING ->
            {
                if(ps == null && cc == null) resultType = Type.STRING;
                    else check(false, "lvalue type error");
            }
            case INT ->
            {
                if(ps == null && cc == null) resultType = Type.INT;
                    else check(false, "lvalue type error");
            }
            default -> throw new TypeCheckException("compiler error");
        }


        return resultType;
    }


    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        Type lvType = (Type)visitLValue(statementAssign.getLv(),arg);
        Type exprType = (Type)statementAssign.getE().visit(this, arg);

        switch(lvType)
        {
            case IMAGE ->
            {
                if(exprType == IMAGE || exprType == PIXEL || exprType == STRING)
                    return null;
                else
                    check(false,"Error, visit Assignment statement");
            }
            case PIXEL, INT ->
            {
                if(exprType == INT || exprType == PIXEL)
                    return null;
                else
                    check(false,"Error, visit Assignment statement");
            }
            case STRING ->
            {
            if(exprType == IMAGE || exprType == PIXEL || exprType == STRING || exprType == INT )
                return null;
            else
                check(false,"Error, visit Assignment statement");
            }
            default -> throw new TypeCheckException("compiler error");
        }

        return null;

    }


    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        Type priType = (Type) unaryExprPostfix.getPrimary().visit(this, arg);
        PixelSelector ps =  unaryExprPostfix.getPixel();
        if (ps != null)
        {
            Type psType = (Type) unaryExprPostfix.getPixel().visit(this,arg);
        }
        ColorChannel cc =  unaryExprPostfix.getColor();
        Type resultType = null;

        if (ps == null && cc == null)
        {

            check(false,"either ps or cc needs to be present");
        }

        switch(priType)
        {
            case IMAGE ->
            {
                if(ps == null && cc != null) resultType = IMAGE;
                else if(ps != null && cc == null) resultType = PIXEL;
                else if(ps != null && cc != null) resultType = INT;
                else check(false,"Error UnaryPostFix");

            }
            case PIXEL ->
            {

                if(ps == null && cc != null) resultType = INT;
                else check(false,"Error UnaryPostFix");
            }
            default -> throw new TypeCheckException("compiler error");
        }

        unaryExprPostfix.setType(resultType);
        return resultType;
    }



    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        String name = identExpr.getName();
        NameDef dec = lookup(name);
        check(dec != null, "undefined identifier " + name);
        Type type = dec.getType();
        identExpr.setType(type);
        return type;

    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        numLitExpr.setType(Type.INT);
        return INT;
    }



//==== Not finished below =====///

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        NameDef dec = lookup(ident.getName());
        check(dec != null, "undefined identifier " + ident.getName());

            return null;
    }




    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {



        List<Declaration> dec = block.getDecList();

        for (AST node : dec) {
            node.visit(this, arg);
        }
        List<Statement> Statements = block.getStatementList();

        for (AST node : Statements) {
            node.visit(this, arg);
        }



        return null;
    }



    public boolean assignmentCompatible(Type rhs,Type lhs){
    if(lhs == rhs)
    return true;

    switch(rhs)
        {
            case IMAGE->
            {
                if(lhs == PIXEL) return true;
                else if(lhs == STRING) return true;
                else return false;

            }
            case INT ->
            {

                if(lhs == PIXEL) return true;
                else return false;

            }
            case PIXEL ->
            {

                if(lhs == INT) return true;
                else return false;
            }
            case STRING ->
            {
                if(lhs == PIXEL) return true;
                else if(lhs == INT) return true;
                else if(lhs == IMAGE) return true;
                else return false;

            }
            default -> {
                return false;
            }
        }


    }
    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {

        Expr initializer = declaration.getInitializer(); // binary
        String nameDec = declaration.getNameDef().getIdent().getName();


        if (initializer != null) {

            Type initializerType = (Type) initializer.visit(this,arg);
            check(assignmentCompatible(declaration.getNameDef().getType(), initializerType),"type of expression and declared type do not match");

        }else if(declaration.getNameDef().getType() == IMAGE)
        {
            if(initializer != null)
            {
                Type initializerType = (Type) initializer.visit(this,arg);
                check(assignmentCompatible(declaration.getNameDef().getType(), initializerType),"type of expression and declared type do not match");
            }else
            {
            check(declaration.getNameDef().getDimension()!= null, "failled to pass null demsion");
            }

        }


        declaration.getNameDef().visit(this,arg);





        return null;
    }


    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName();
        boolean inserted = symbolTable.insert(name, nameDef);

        check(inserted, "variable " + name + " already declared");
       check(symbolTable.size() != 0,"empty");
        check(nameDef.getType() != VOID, "Error it was void NameDef");


        if(nameDef.getDimension() != null)
        {
            Type demType = (Type) nameDef.getDimension().visit(this,arg);
            check(nameDef.getType()==IMAGE, "Error namedef");

        }


        return null;
    }



    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {

        rootNode = program; // save the start for return statement to match.
       symbolTable = new SymbolTable();
        ourTables.add(symbolTable);


        List<NameDef> parametersCheck  = program.getParamList();

        for (NameDef node : parametersCheck) {
            node.visit(this,arg);
        }
        List<Declaration> dec = program.getBlock().getDecList();

        for (AST node : dec) {
            node.visit(this, arg);
        }

        List<Statement> Statements = program.getBlock().getStatementList();

        for (AST node : Statements) {
            node.visit(this, arg);
        }


        return program;
    }






    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        Type exprType = (Type) returnStatement.getE().visit(this,arg);
        check(assignmentCompatible(rootNode.getType(), exprType),"return types dont match ");
        return null;
    }


    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        Type exprType = (Type)statementWrite.getE().visit(this, arg);
        return null;
    }


    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        Type guardType = (Type)whileStatement.getGuard().visit(this, arg);
        check(guardType == INT,"Error is not INt: visitWhileStatement()");


        enterScope();
        Type blockType = (Type)whileStatement.getBlock().visit(this,arg);
        leaveScope();

        return null;
    }






    private void check(boolean condition, String message)throws TypeCheckException
    {
        if (! condition) { throw new TypeCheckException(message); }
    }


}

