package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import static edu.ufl.cise.plcsp23.IToken.Kind.*;

public class Parse implements IParser {

    IToken t; // Holds current token
    Scanner myScanner; // scans tokens from input


    @Override
    public AST parse() throws PLCException {

        return Expr();
    }

    //=======  Constructor  =======//
    Parse(Scanner scanner) throws LexicalException {
         myScanner = scanner;
         t = myScanner.next();

    }

    //======= Concrete Syntax Methods =======//

    public Expr PrimaryExpr() throws PLCException// <primary_expr> ::= STRING_LIT | NUM_LIT | IDENT | (<expr)| Z | rand
    {
        Expr e = null;
        switch(t.getKind())
        {
            case STRING_LIT ->{
                e  = new StringLitExpr(t);
                consume();
            }
            case NUM_LIT -> {
                e = new NumLitExpr(t);
                consume();
            }
            case IDENT ->{
                e = new IdentExpr(t);
                consume();
            }
            case LPAREN ->
            {
               consume();
                e = Expr();
                match(RPAREN);
            }
            case RES_Z -> {
                e =  new ZExpr(t);
                consume();
            }
            case RES_rand ->{
                e = new RandomExpr(t);
                consume();
            }
            default -> PLCerror("expected a Predict set of PrimaryExpress IN PRIMARY: string| num | indent | ( | rand |z");
        }
        return e;
    }
    public Expr unaryExpr() throws PLCException {
        IToken first = t;
        Expr e = null;
        switch(t.getKind())
        {
            case BANG, MINUS,RES_sin,RES_cos,RES_atan ->
            {
                IToken op = t;
                consume();
                e  = new UnaryExpr(first,op.getKind(),unaryExpr());
            }
            default ->
            {
                    e = PrimaryExpr();
            }
        }
        return e;
    }
    public Expr MultiplicativeExpr() throws PLCException {
        IToken first = t;
        Expr left = null;
        left = unaryExpr();
        Expr right = null;

        while(t.getKind() == TIMES || t.getKind() == DIV || t.getKind() == MOD)
        {
            switch(t.getKind())
            {
                case TIMES, DIV,MOD->
                {
                    IToken op = t;
                    consume();
                    right = unaryExpr();
                    left = new BinaryExpr(first,left,op.getKind(),right);
                }
                default ->
                {
                        PLCerror("expected a Predict set of MULTIPLICATIVE : * | / | % ");
                }
            }
        }
        return left;
    }



    public Expr AdditiveExpr() throws PLCException
    {
        IToken first = t;
        Expr left = null;
        left = MultiplicativeExpr();
        Expr right = null;
        while(t.getKind() == PLUS || t.getKind() == MINUS)
        {
            switch(t.getKind())
            {
                case PLUS,MINUS ->
                {
                    IToken op = t;
                    consume();
                    right = MultiplicativeExpr();
                    left = new BinaryExpr(first,left,op.getKind(),right);
                }
                default ->
                {
                    PLCerror("expected a Predict set of ADDITIVE: + | - ");
                }
            }
        }
        return left;
    }

    public Expr PowerExpr() throws PLCException
    {
        IToken first = t;
        Expr left = null;
        left = AdditiveExpr();
        Expr right = null;
        switch (t.getKind())
        {
            case EXP ->
            {
                IToken op = t;
                consume();
                right = AdditiveExpr();
                left = new BinaryExpr(first,left,op.getKind(),right);
            }
            default ->
            {
                return left;
            }
        }
        return left;
    }

    public Expr CompareExpr() throws PLCException
    {
        IToken first = t;
        Expr left = null;
        left =  PowerExpr();
        Expr right = null;
        while(t.getKind() == LT || t.getKind() == GT || t.getKind() == LE || t.getKind() == GE || t.getKind() == EQ)
        {
            switch(t.getKind())
            {
                case LT,LE,GT,GE,EQ ->
                {
                    IToken op = t;
                    consume();
                    right = PowerExpr();
                    left = new BinaryExpr(first,left,op.getKind(),right);
                }
                default ->
                {
                    PLCerror("expected a Predict set of COMPARE");
                }
            }
        }
        return left;
    }
    public Expr ANDExpr() throws PLCException
    {
        IToken first = t;
        Expr left = null;
        left = CompareExpr();
        Expr right = null;
        while(t.getKind() == AND || t.getKind() == BITAND)
        {
            switch(t.getKind())
            {
                case AND,BITAND ->
                {
                    IToken op = t;
                    consume();
                   right = CompareExpr();
                   left = new BinaryExpr(first,left,op.getKind(),right);
                }
                default ->
                {
                    PLCerror("expected a Predict set of AND");
                }
            }

        }
    return left;
    }
    public Expr ORExpr() throws PLCException
    {
        IToken first = t;
        Expr left = null;
        left = ANDExpr();
        Expr right = null;
        while(t.getKind() == OR || t.getKind() == BITOR)
        {
            switch(t.getKind())
            {
                case OR,BITOR->
                {
                    IToken op = t;
                    consume();
                    right = ANDExpr();
                    left = new BinaryExpr(first,left,op.getKind(),right);
                }
                default ->
                {
                    PLCerror("expected a Predict set of OR");
                }
            }

        }
        return left;
    }
    public Expr ConditionalExpr() throws PLCException
    {
        IToken first = t;
        Expr guard = null;
        Expr TRUE = null;
        Expr FALSE = null;

        switch(t.getKind())
        {
            case RES_if ->
            {
                consume();
                guard = Expr();
                match(QUESTION);
                TRUE = Expr();
                match(QUESTION);
                FALSE = Expr();
                guard = new ConditionalExpr(first,guard,TRUE,FALSE);
            }
            default ->
            {
                PLCerror("expected a Predict set of CONDITIONAL: if ");
            }
        }
 return guard;
    }
    public Expr Expr() throws PLCException
    {
        IToken first = t;
        Expr parse;
        parse = null;

        switch(t.getKind())
        {
            case RES_if ->
            {
               parse = ConditionalExpr();
            }
            default ->
            {
              parse = ORExpr();
            }
        }
            return parse;
    }


    //======= Helper functions =======//

    void match(IToken.Kind c) throws PLCException {
        if(t.getKind() == c )
        {
            t = myScanner.next();
        }
        else
        {
           PLCerror("match error");
        }

    }

    void consume() throws PLCException{

        t = myScanner.next();

    }
    private void PLCerror(String message) throws PLCException{
        throw new SyntaxException("Parsing error at : " + t.getSourceLocation() + ": type->" + message);
    }


}
