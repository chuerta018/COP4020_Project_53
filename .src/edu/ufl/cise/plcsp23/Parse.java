package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.List;

import static edu.ufl.cise.plcsp23.IToken.Kind.*;

public class Parse implements IParser {

    IToken t; // Holds current token
    Scanner myScanner; // scans tokens from input


    @Override
    public AST parse() throws PLCException {

        return program();
    }

    //=======  Constructor  =======//
    Parse(Scanner scanner) throws LexicalException {
         myScanner = scanner;
         t = myScanner.next();

    }

    //======= Methods of PA2 AND PA3 =======//
    // ======= PA3 function Manipulation and New function  ========//
    public Expr expandedPixel() throws PLCException
    {
        IToken first = t;
        Expr one = null;
        Expr two = null;
        Expr three = null;

        switch(t.getKind())
        {
            case LSQUARE ->
            {
                consume();
                one = Expr();
                match(COMMA);
                two = Expr();
                match(COMMA);
                three = Expr();
                match(RSQUARE);
                one = new ExpandedPixelExpr(first,one,two,three);
            }
            default ->
            {
            PLCerror("ExpandedPixel() Predict Set wrong, expected [ but got : " + t.getKind());
            }
        }
            return one;
    }

    public Expr PixelFuntionExpr() throws PLCException
    {
        IToken first = t;
        PixelSelector ps = null;
        switch(t.getKind())
        {
            case RES_x_cart,RES_y_cart,RES_a_polar,RES_r_polar ->
            {
              IToken tokenKind = t;
              consume();
              ps = PixelSelector();

              return new PixelFuncExpr(first, tokenKind.getKind(), ps);
            }
            default ->
            {
                PLCerror("PixelFunctionExpr() Predict Set wrong, expected x cart | y cart |  a polar | r polar  but got : " + t.getKind());
            }
        }
        return null;
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
            case RES_x,RES_a,RES_y,RES_r ->
            {
                e = new PredeclaredVarExpr(t);
                consume();
            }
            case RES_x_cart,RES_y_cart,RES_a_polar,RES_r_polar->
            {
                e = PixelFuntionExpr();
            }
            case LSQUARE ->
            {
               e = expandedPixel();
            }
            default -> PLCerror("expected a Predict set of PrimaryExpress IN PRIMARY: string| num | indent | ( | rand |z");
        }
        return e;
    }


    public Expr unaryExprPostFix() throws PLCException
    {
        IToken first = t;
        Expr prime = null;
        PixelSelector ps = null;
        ColorChannel cc = null;

        prime = PrimaryExpr();

        if(t.getKind() == LSQUARE)
        {
            ps = PixelSelector();
        }

        if(t.getKind() == COLON)
        {
            consume();

            if(t.getKind() != RES_grn && t.getKind() != RES_red && t.getKind() != RES_blu )
                PLCerror("ChannelSelector not valid");

            cc = cc.getColor(t);
            consume();
        }

        if(ps == null && cc == null)
            return prime;

        return new UnaryExprPostfix(first,prime,ps,cc);
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
                    e = unaryExprPostFix(); //CHANGE THIS PRIMARY TO UNARYPOST Fix after AST IMPLEMENT
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

    public Dimension dimension() throws PLCException
    {

        IToken first = t;
        Expr w = null;
        Expr h = null;
        switch(t.getKind())
        {
            case LSQUARE->{
                consume();
                w = Expr();
                match(COMMA);
                h= Expr();
                match(RSQUARE);
                return new Dimension(first,w,h);
            }
            default -> {
                return null;
            }
        }
    }


    public NameDef nameDef() throws PLCException
    {
        IToken first = t;
        Type type;
        Ident ident;
        Dimension dimension = null;
        type = Type.getType(first);

        consume();
        switch(t.getKind())
        {
            case IDENT->
            {
                ident = new Ident(t);
                consume();
                return new NameDef(first,type,dimension,ident);
            }
            default ->
            {

                dimension = dimension();
                if(t.getKind() == IDENT)
                {
                 ident = new Ident(t);
                 consume();
                 return new NameDef(first,type,dimension,ident);
                 }else
                 {
                 PLCerror("not an Ident in function call nameDEF");
                 }
             }

        }
        return null;
        }

    public List<NameDef> ParamList(List<NameDef> ndList) throws PLCException  // likely to cause errors tbh I wanna say it works
    {
        NameDef nd = null;

        switch(t.getKind())
        {
            case RES_image, RES_pixel, RES_int, RES_string, RES_void ->
            {

                nd = nameDef();
                ndList.add(nd);
                while (t.getKind() == COMMA)
                {
                    consume();
                    nd = nameDef();
                    ndList.add(nd);
                }
            }
        }
        return ndList;
    }

    public Declaration declaration() throws PLCException
    {
        IToken first = t;
        NameDef nd;
        Expr e;
        nd = nameDef();
        switch(t.getKind())
        {
            case ASSIGN->{
                consume();
                e= Expr();
                return new Declaration(first,nd,e);
            }
            default -> {
               return new Declaration(first,nd,null);
            }
        }
    }

    public PixelSelector PixelSelector() throws PLCException
    {
        IToken first  = t;
        Expr x = null;
        Expr y = null;
        PixelSelector ps = null;

        switch(t.getKind())
        {
            case LSQUARE->{
                consume();
                x = Expr();
                match(COMMA);
                y = Expr();
                match(RSQUARE);
               ps = new PixelSelector(t,x,y);
            }
            default -> {
                PLCerror("PixelSelector() expected Predict set of : LParen instead got kind: " +t.getKind());
            }
        }
        return ps;
    }


    public LValue lValue() throws PLCException
    {
        IToken first = t;
        LValue emptyCase = null;
        PixelSelector ps = null;
        ColorChannel cc = null;
        Ident ident = null;

        switch(t.getKind())
        {
            case IDENT->
            {
                ident = new Ident(t);
                consume();

                if(t.getKind() == LSQUARE)
                {
                    ps = PixelSelector();
                }

                if(t.getKind() == COLON)
                {
                    consume();

                    if(t.getKind() != RES_grn && t.getKind() != RES_red && t.getKind() != RES_blu )
                        PLCerror("ChannelSelector not valid");

                    cc = cc.getColor(t);
                    consume();
                }
                return new LValue(first,ident,ps,cc);
            }
            default ->
            {
              PLCerror("Lvalue() predict set expected IDENT ,Instead got Kind: " + t.getKind());
            }
        }
        return emptyCase;
    }
    public Statement statement() throws PLCException
    {
        IToken first = t;
        Expr e = null;
        Statement s = null;
        LValue lv = null;
        Block b = null;

        switch(t.getKind())
        {
            case IDENT->
            {
                lv = lValue();
                match(ASSIGN);
                e = Expr();
                return new AssignmentStatement(first,lv,e);
            }
            case RES_write ->
            {
                consume();
                e = Expr();
                return new WriteStatement(first,e);
            }
            case RES_while ->
            {
                consume();
                e = Expr();
                b = block();
                return new WhileStatement(first,e,b);
            }
            default ->
            {
               return s;
            }
        }
    }

    public List<Statement> StatementList( List<Statement> sList) throws PLCException
    {
        while(t.getKind() == IDENT || t.getKind() == RES_write || t.getKind() == RES_while )
        {
            sList.add(statement());
            match(DOT);
        }

        return sList;
    }

    public List<Declaration> DecList(List<Declaration> dList) throws PLCException
    {

        while(t.getKind() == RES_image|| t.getKind() == RES_pixel || t.getKind() == RES_int || t.getKind() == RES_string || t.getKind() == RES_void )
        {
           dList.add(declaration());
            match(DOT);
        }
        return dList;

        //WHEN implementing ast tree return AST in while loop or NULL outside the loop

    }

    public  Block block() throws PLCException
    {
        IToken first = t;
        List<Declaration> dList = new ArrayList<Declaration>();
        List<Statement> sList = new ArrayList<Statement>();

        switch(t.getKind())
        {
            case LCURLY->
            {
                consume();
                DecList(dList);
                StatementList(sList);
                match(RCURLY);

                return new Block(first,dList,sList);
            }
            default ->
            {
                PLCerror("Block() predict set was not either  { . Instead got kind: " + t.getKind());
            }
        }
        return null;
    }

    public Program program() throws PLCException
    {
        IToken first = t;
        Type type;
        Ident ident;
        List<NameDef> ndList = new ArrayList<NameDef>();
        Block b;

        switch(t.getKind())
        {
            case RES_image, RES_pixel, RES_int, RES_string, RES_void ->
            {
                type = Type.getType(t);
                consume();

                if(t.getKind() == IDENT)
                {
                    ident = new Ident(t);
                    consume();
                    match(LPAREN);
                    ParamList(ndList);
                    match(RPAREN);
                    b = block();

                    if (t.getKind() == EOF)
                    return new Program(first,type,ident,ndList,b);
                    else
                        PLCerror("Program error: type throws an runtime error... this maybe doesnt ever get reached");

                }else
                {
                    PLCerror("Program error: type throws an runtime error... this maybe doesnt ever get reached");
                }
            }
            default->
            {

                PLCerror("Type is not valid type enum in Program");
            }
        }
        return null;
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
        throw new SyntaxException("Parsing error at : " + t.getSourceLocation() + ": type->" + message + ":ITOKEN here -> " + t.getKind());
    }


}

