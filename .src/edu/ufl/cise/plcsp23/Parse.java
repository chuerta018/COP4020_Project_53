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

    //======= Methods of PA2 AND PA3 =======//
    // ======= PA3 function Manipulation and New function  ========//
    public void ExpandedPixel() throws PLCException
    {
        switch(t.getKind())
        {
            case LSQUARE ->
            {
                consume();
                Expr();
                match(COMMA);
                Expr();
                match(RSQUARE);
            }
            default ->
            {
            PLCerror("ExpandedPixel() Predict Set wrong, expected [ but got : " + t.getKind());
            }
        }

    }

    public void PixelFuntionExpr() throws PLCException
    {
        switch(t.getKind())
        {
            case RES_x_cart,RES_y_cart,RES_a_polar,RES_r_polar ->
            {
                consume();
                PixelSelector();
            }
            default ->
            {
                PLCerror("PixelFunctionExpr() Predict Set wrong, expected x cart | y cart |  a polar | r polar  but got : " + t.getKind());
            }
        }

    }



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
            case RES_x_cart,RES_y_cart,RES_a_polar,RES_r_polar->
            {
                PixelFuntionExpr();
            }
            case RSQUARE ->
            {
                ExpandedPixel();
            }
            default -> PLCerror("expected a Predict set of PrimaryExpress IN PRIMARY: string| num | indent | ( | rand |z");
        }
        return e;
    }


    public void unaryExprPostFix() throws PLCException
    {

    PrimaryExpr();
        switch(t.getKind())
        {
                case RPAREN ->
                {
                    PixelSelector();

                }
                case COLON ->
                {
                    ChannelSelector();
                }
                default ->
                {
                    //return a null ast tree here to notify its empty
                }
        }
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
                    e = PrimaryExpr(); //CHANGE THIS PRIMARY TO UNARYPOST Fix after AST IMPLEMENT
            }
        }
        return e;
    }

    // ======= end of PA3 function Manipulation and New function  ========//
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
                right = PowerExpr();
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

    //========== PA3 Grammar Functions JUST PARSE NO ==========//
    public void Type() throws PLCException
    {
        switch(t.getKind())
        {
            case RES_image->{
                consume();
            }
            case RES_pixel -> {
                consume();
            }
            case RES_int ->{
                consume();
            }
            case RES_string->
            {
                consume();
            }
            case RES_void -> {
                consume();
            }
            default -> PLCerror("expected a Predict set of PrimaryExpress IN PRIMARY: string| num | indent | ( | rand |z");
        }
    }


    public void Dimension() throws PLCException
    {

        switch(t.getKind())
        {
            case LSQUARE->{
                consume();
                Expr();
                match(COMMA);
                Expr();
                match(RSQUARE);
            }
            default -> {
                PLCerror("Error in Dimension: There is no [ but yet this was called?");
            }
        }
    }


    public void NameDef() throws PLCException
    {
        Type();
        switch(t.getKind())
        {
            case IDENT->{
                consume();
            }
            default -> {
                Dimension();
                match(IDENT);
            }
        }
    }
    public void ParamList() throws PLCException  // likely to cause errors tbh I wanna say it works
    {
        try {
            NameDef();
            while (t.getKind() == COMMA)
            {
                consume();
                NameDef();
            }

        } catch (PLCException e) {
            // return a null AST tree corresponding to it's abstract syntax.

        }

    }

    public void Declaration() throws PLCException
    {
        NameDef();
        switch(t.getKind())
        {
            case ASSIGN->{
                consume();
                Expr();
            }
            default -> {
                //RETURN THE nameDef(); at the beginning when implementing ast
            }
        }
    }

    public void ChannelSelector() throws PLCException
    {
        switch(t.getKind())
        {
            case COLON->{

                switch(t.getKind())
                {
                    case RES_red,RES_grn,RES_blu ->
                    {
                        consume();
                    }
                    default ->
                    {
                        PLCerror("ChannelSelector got the right predict Set but following token was not the reserve words:  red | grn | blu");
                    }
                }
            }
            default -> {
                PLCerror("ChannelSelector() expected Predict set of : COLON instead got kind: " +t.getKind());
            }
        }
    }

    public void PixelSelector() throws PLCException
    {
        switch(t.getKind())
        {
            case LSQUARE->{
                consume();
                Expr();
                match(COMMA);
                Expr();
                match(RPAREN);
            }
            default -> {
                PLCerror("PixelSelector() expected Predict set of : LParen instead got kind: " +t.getKind());
            }
        }
    }


    public void LValue() throws PLCException
    {

        switch(t.getKind())
        {
            case IDENT->
            {
                switch(t.getKind())
                {
                    case RPAREN ->
                    {
                        PixelSelector();

                    }
                    case COLON ->
                    {
                        ChannelSelector();
                    }
                    default ->
                    {
                    //return a null ast tree here to notify its empty
                    }
                }
            }
            default ->
            {
              PLCerror("Lvalue() predict set expected IDENT ,Instead got Kind: " + t.getKind());
            }
        }
    }
    public void Statement() throws PLCException
    {

        switch(t.getKind())
        {
            case IDENT->
            {
                LValue();
                match(ASSIGN);
                Expr();
            }
            case RES_write ->
            {
                Expr();
            }
            case RES_while ->
            {
                Expr();
                Block();
            }
            default ->
            {
                PLCerror("Statement predict set was not either  IDENT, write, while. Instead got kind: " + t.getKind());
            }
        }
    }

    public void StatementList() throws PLCException
    {

        while(t.getKind() == IDENT || t.getKind() == RES_write || t.getKind() == RES_while )
        {
            Statement();
            match(DOT);
        }

        //WHEN implementing ast tree return AST in while loop or NULL outside the loop

    }

    public void DecList() throws PLCException
    {

        while(t.getKind() == RES_image|| t.getKind() == RES_pixel || t.getKind() == RES_int || t.getKind() == RES_string || t.getKind() == RES_void )
        {
           Declaration();
            match(DOT);
        }

        //WHEN implementing ast tree return AST in while loop or NULL outside the loop

    }

    public void Block() throws PLCException
    {
        switch(t.getKind())
        {
            case LCURLY->
            {
                consume();
                DecList();
                StatementList();
                match(RPAREN);
            }
            default ->
            {
                PLCerror("Block() predict set was not either  { . Instead got kind: " + t.getKind());
            }
        }
    }

    public void Program() throws PLCException
    {
        Type();
        match(IDENT);
        match(LPAREN);
        ParamList();
        match(RPAREN);
        Block();
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

