package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.AST;
import edu.ufl.cise.plcsp23.ast.ConditionalExpr;

import static edu.ufl.cise.plcsp23.IToken.Kind.*;

public class Parse implements IParser {

    IToken t; // Holds current token
    Scanner myScanner; // scans tokens from input


    @Override
    public AST parse() throws PLCException {

        return null;
    }

    //=======  Constructor  =======//
    Parse(Scanner scanner) throws LexicalException {
         myScanner = scanner;
         t = myScanner.next();
    }

    //======= Concrete Syntax Methods =======//

    public void PrimaryExpr() throws PLCException// <primary_expr> ::= STRING_LIT | NUM_LIT | IDENT | (<expr)| Z | rand
    {
        switch(t.getKind())
        {
            case STRING_LIT -> consume();
            case NUM_LIT -> consume();
            case IDENT -> consume();
            case LPAREN ->
            {
               consume();
                Expr();
                match(RPAREN);
            }
            case RES_Z -> consume();
            case RES_rand -> consume();
            default -> PLCerror("expected a Predict set of PrimaryExpress IN PRIMARY: string| num | indent | ( | rand |z");
        }

        return;

    }
    public void UnaryExpr() throws PLCException {
        switch(t.getKind())
        {
            case BANG, MINUS,RES_sin,RES_cos,RES_atan ->
            {
                consume();
                UnaryExpr();
            }
            default ->
            {
                    PLCerror("expected a Predict set of UNARY: ! | - | sin | cos | atan");
            }
        }
        PrimaryExpr();
    }
    public void MultiplicativeExpr() throws PLCException {
        UnaryExpr();
        while(t.getKind() == TIMES || t.getKind() == DIV || t.getKind() == MOD)
        {
            switch(t.getKind())
            {
                case TIMES, DIV,MOD->
                {
                  consume();
                  UnaryExpr();
                }
                default ->
                {
                        PLCerror("expected a Predict set of MULTIPLICATIVE : * | / | % ");
                }
            }
        }

    }



    public void AdditiveExpr() throws PLCException
    {
        MultiplicativeExpr();
        while(t.getKind() == PLUS || t.getKind() == MINUS)
        {
            switch(t.getKind())
            {
                case PLUS,MINUS ->
                {
                    consume();
                    MultiplicativeExpr();
                }
                default ->
                {
                    PLCerror("expected a Predict set of ADDITIVE: + | - ");
                }
            }
        }


    }

    public void PowerExpr() throws PLCException
    {
        AdditiveExpr();
        switch (t.getKind())
        {
            case EXP ->
            {
                consume();
                AdditiveExpr();
            }
            default ->
            {
                return;
            }

        }

    }

    public void CompareExpr() throws PLCException
    {
        PowerExpr();
        while(t.getKind() == LT || t.getKind() == GT || t.getKind() == LE || t.getKind() == GE || t.getKind() == EQ)
        {
            switch(t.getKind())
            {
                case LT,LE,GT,GE,EQ ->
                {
                    consume();
                    PowerExpr();
                }
                default ->
                {
                    PLCerror("expected a Predict set of COMPARE");
                }
            }
        }


    }
    public void ANDExpr() throws PLCException
    {
        CompareExpr();
        while(t.getKind() == AND || t.getKind() == BITAND)
        {
            switch(t.getKind())
            {
                case AND,BITAND ->
                {
                    consume();
                   CompareExpr();
                }
                default ->
                {
                    PLCerror("expected a Predict set of AND");
                }
            }

        }

    }
    public void ORExpr() throws PLCException
    {
        ANDExpr();
        while(t.getKind() == OR || t.getKind() == BITOR)
        {
            switch(t.getKind())
            {
                case OR,BITOR->
                {
                    consume();
                    ANDExpr();
                }
                default ->
                {
                    PLCerror("expected a Predict set of OR");
                }
            }

        }

    }
    public void ConditionalExpr() throws PLCException
    {
        switch(t.getKind())
        {
            case RES_if ->
            {
                consume();
                Expr();
                match(QUESTION);
                Expr();
                match(QUESTION);
                Expr();
            }
            default ->
            {
                PLCerror("expected a Predict set of CONDITIONAL: if ");
            }
        }

    }
    public void Expr() throws PLCException
    {

        switch(t.getKind())
        {
            case RES_if ->
            {
                ConditionalExpr();
            }
            default ->
            {
               ORExpr();
            }
        }

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
        throw new LexicalException("Parsing error at : " + t.getSourceLocation() + ": type->" + message);
    }


}
