package edu.ufl.cise.plcsp23;


import java.util.Arrays;
import java.util.HashMap;

import static edu.ufl.cise.plcsp23.IToken.Kind.*;

public class Scanner implements IScanner
{
    final String input;
    final char[] inputChars;

    int pos; // position of given Char
    char ch; // next char
    int tokenRow = 1;
    int col = 0;
    int col2 = 0;
    boolean closed = true;


    //constructor
    public Scanner(String _input)
    {
        this.input = _input;
        inputChars = Arrays.copyOf(_input.toCharArray(),_input.length()+1);
        pos =  0;
        ch = inputChars[pos];

    }



    @Override
    public IToken next() throws LexicalException
    {
        return scanToken();
    }

    private enum State {
        START,
        HAVE_EQ,
        IN_IDENT,
        IN_NUM_LIT,
        HAVE_LT,
        HAVE_GT,
        HAVE_AND,
        HAVE_OR,
        HAVE_EXP,
        IN_STRING_LIT,
        IN_COMMENT_LIT

    }

    private IToken scanToken() throws LexicalException
    {
        State state = State.START;
        int tokenStart = -1;
        //int tokenRow = 1;
        while(true) { //read chars, loop terminates when a Token is returned
            switch(state) {
                case  START -> {
                    tokenStart = pos;
                    col = col2;
                    switch (ch)
                    {
                        case 0 -> //EOF CASE
                        { //end of input
                            return new Token(EOF, tokenRow, col, tokenStart, 0, inputChars);
                        }
                        case ' ','\t','\f', '\r', '\n' -> {
                            if(ch == '\n') {
                            	tokenRow++;
                            	col = 0;
                            	col2 = -1;
                            } else {
                                col++;
                            }
                            nextChar(); // WHITE SPACE
                        }
                        case '+' -> { // single Char +
                            nextChar();
                            col++;
                            return new Token(PLUS, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '*' -> { // === returns '*' or '**' === //
                            state = State.HAVE_EXP;
                            col++;
                            nextChar();

                        }
                        case '0' -> { // === single Char 0 === //
                            nextChar();
                            col++;
                            return new NumLitToken(NUM_LIT, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '.' -> { // === single Char . === //
                            nextChar();
                            col++;
                            return new Token(DOT, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case ',' -> { // === single Char , === //
                            nextChar();
                            col++;
                            return new Token(COMMA, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '?' -> { // === single Char ? === //
                            nextChar();
                            col++;
                            return new Token(QUESTION, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case ':' -> { // === single Char : === //
                            nextChar();
                            col++;
                            return new Token(COLON, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '(' -> { // === single Char ( === //
                            nextChar();
                            col++;
                            return new Token(LPAREN, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case ')' -> { // === single Char ) === //
                            nextChar();
                            col++;
                            return new Token(RPAREN, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '<' -> { // === returns '='or '<=' or '<->' === //
                            state = State.HAVE_LT;
                            col++;
                            nextChar();

                        }
                        case '>' -> { // === returns '>' or '>=' token === //
                            state = State.HAVE_GT;
                            col++;
                            nextChar();
                        }
                        case '[' -> { // === single Char [ === //
                            nextChar();
                            col++;
                            return new Token(LSQUARE, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case ']' -> { // === single Char ] === //
                            nextChar();
                            col++;
                            return new Token(RSQUARE, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '{' -> { // === single Char { === //
                            nextChar();
                            col++;
                            return new Token(LCURLY, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '}' -> { // === single Char } === //
                            nextChar();
                            col++;
                            return new Token(RCURLY, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '=' -> { // === Returns = or == tokens  === //
                            state = State.HAVE_EQ;
                            nextChar();
                        }
                        case '!' -> { // === single Char ! === //
                            nextChar();
                            col++;
                            return new Token(BANG, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '&' -> { // === returns '&' or '&&' token === //
                            state = State.HAVE_AND;
                            col++;
                            nextChar();
                        }
                        case '|' -> { // === returns '|' or '||' token === //
                            state = State.HAVE_OR;
                            col++;
                            nextChar();
                        }
                        case '/' -> { // === single Char / === //
                            nextChar();
                            col++;
                            return new Token(DIV, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '-' -> { // === single Char - === //
                            nextChar();
                            col++;
                            return new Token(MINUS, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '%' -> { // === single Char % === //
                            nextChar();
                            col++;
                            return new Token(MOD, tokenRow, col, tokenStart, 1, inputChars);
                        }
                        case '1','2','3','4','5','6','7','8','9' -> {//char is nonzero digit
                            state = State.IN_NUM_LIT;
                            col++;
                            nextChar();
                        }
                        case '"' -> {
                            state = State.IN_STRING_LIT;
                            col++;
                            nextChar();
                        }
                        case '~' -> {
                            state = State.IN_COMMENT_LIT;
                            col++;
                            nextChar();
                        }
                        default -> {
                            if (isLetter(ch)) {
                                state = State.IN_IDENT;
                                col++;
                                nextChar();
                            }
                            else error("illegal char with ascii value: " + (int)ch);
                        }

                    }
                } // === END OF START CASE === //
                case HAVE_EQ -> // === Returns either a '=' token or '==' token === //
                {
                    if (ch == '=') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.EQ, tokenRow, col, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        return new Token(ASSIGN, tokenRow, col, tokenStart, 1, inputChars);
                    }

                } //=== END OF HAVE_EQ ===//
                case HAVE_GT ->
                {
                    if (ch == '=') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.GE, tokenRow, col, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;

                        return new Token(GT, tokenRow, col, tokenStart, 1, inputChars);
                    }

                }
                case HAVE_LT -> {

                    if (ch == '=') // === this case is '<=' === //
                    {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.LE, tokenRow, col, tokenStart, 2, inputChars);
                    }
                    else if (ch == '-')
                    {
                        nextChar();

                        if(ch == '>')// === this case is '<->' === //
                        {
                            state = state.START;
                            nextChar();
                            return new Token(EXCHANGE, tokenRow, col, tokenStart, 3, inputChars);
                        }
                        else { // === this case is '<' === //
                            // not sure if the pos++ in the array brackets permanently increases the pos variable or temporarily.
                            state = state.START;
                            pos--;
                            ch = inputChars[pos];
                            error("Not a valid Exchange token");
                            //return new Token(IToken.Kind.LT, tokenStart, 1, inputChars);
                        }
                    }
                    else { // === this case is '<' === //
                        // not sure if the pos++ in the array brackets permanently increases the pos variable or temporarily.
                        state = state.START;

                        return new Token(IToken.Kind.LT, tokenRow, col, tokenStart, 1, inputChars);
                    }
                }
                case HAVE_AND -> {
                    if (ch == '&') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.AND, tokenRow, col, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        return new Token(BITAND, tokenRow, col, tokenStart, 1, inputChars);
                    }

                }
                case HAVE_OR -> {
                    if (ch == '|') {
                        state = state.START;
                        nextChar();
                        return new Token(OR, tokenRow, col, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        return new Token(BITOR, tokenRow, col, tokenStart, 1, inputChars);
                    }
                }
                case HAVE_EXP -> {

                    if (ch == '*') {
                        state = state.START;
                        nextChar();
                        return new Token(EXP, tokenRow, col, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        return new Token(TIMES, tokenRow, col, tokenStart, 1, inputChars);
                    }

                }
                case IN_NUM_LIT -> {
                    if (isDigit(ch)) {//char is digit, continue in IN_NUM_LIT state
                        nextChar();
                    }
                    else {
                        try {
                            int length = pos - tokenStart;
                            state= state.START;
                            NumLitToken numLit = new NumLitToken(IToken.Kind.NUM_LIT, tokenRow, col, tokenStart, length, inputChars);
                            numLit.getValue();
                            return numLit;

                        }catch(NumberFormatException e)
                        {
                            error("Integer.parseInt failed");
                        }
                    }

                }
                case IN_IDENT -> { 	// === obtain from Neun02 branch === // RETURN IN_IDENT and Reserve TOKENS
                    if (isIdentStart(ch) || isDigit(ch)) {
                        nextChar();
                    }
                    else {//
                        //current char belongs to next token, so don't get next char
                        int length = pos-tokenStart;
                        //determine if this is a reserved word. If not, it is an ident.
                        String text = input.substring(tokenStart, tokenStart + length);
                        IToken.Kind kind = reservedWords.get(text);
                        if (kind == null)
                        {
                            kind = IDENT;
                        }
                        state = state.START;
                        return new Token(kind, tokenRow, col, tokenStart, length, inputChars);
                    }

                }
                case IN_STRING_LIT ->
                {
                    try {
                        if (isStringChar(ch))
                        {
                            nextChar();
                        }else if (ch == 92)//
                        {
                            if(isEscapeSeq(ch))
                            {
                                nextChar(); //"\\"
                                nextChar();

                            }else {
                                error(" Error just one -> bracket within string thats not escape sequence");
                            }
                        } else if (ch == '"')
                        {
                            nextChar();
                            int length = pos-tokenStart;
                            state = state.START;
                            return new StringLitToken(STRING_LIT, tokenRow, col, tokenStart,length,inputChars);
                        }else{
                            error("Not a valid string Token/Or does not satisfy token");
                        }
                    } catch (Exception e)
                    {
                        error("Not a valid string Token/Or does not satisfy token");
                    }
                }
                case IN_COMMENT_LIT -> {
                    if (isInput_Char(ch)) {
                        nextChar();
                    } else {
                    	state = state.START;
                    }
                }
                default -> {
                    throw new UnsupportedOperationException("Bug in Scanner");
                }

            }
        }
    }

    public void nextChar(){
    	col2++;
        pos++;
        ch = inputChars[pos];
    }

    private static HashMap<String, IToken.Kind> reservedWords; //=== Reserve Word Hash Map ===// used Neun02 branch code as reference //
    static {
        reservedWords = new HashMap<String, IToken.Kind>();
        reservedWords.put("image", RES_image);
        reservedWords.put("pixel", RES_pixel);
        reservedWords.put("int", RES_int);
        reservedWords.put("string", RES_string);
        reservedWords.put("void", RES_void);
        reservedWords.put("nil", RES_nil);
        reservedWords.put("load", RES_load);
        reservedWords.put("display", RES_display);
        reservedWords.put("write", RES_write);
        reservedWords.put("x", RES_x);
        reservedWords.put("y", RES_y);
        reservedWords.put("a", RES_a);
        reservedWords.put("r", RES_r);
        reservedWords.put("X", RES_X);
        reservedWords.put("Y", RES_Y);
        reservedWords.put("Z", RES_Z);
        reservedWords.put("x_cart", RES_x_cart);
        reservedWords.put("y_cart", RES_y_cart);
        reservedWords.put("a_polar", RES_a_polar);
        reservedWords.put("r_polar", RES_r_polar);
        reservedWords.put("rand", RES_rand);
        reservedWords.put("sin", RES_sin);
        reservedWords.put("cos", RES_cos);
        reservedWords.put("atan", RES_atan);
        reservedWords.put("if", RES_if);
        reservedWords.put("while", RES_while);

    }
    private boolean isDigit(int ch) {
        return '0' <= ch && ch <= '9';
    }
    private boolean isLetter(int ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }
    private boolean isIdentStart(int ch) {
        return isLetter(ch) || (ch == '$') || (ch == '_');
    }

    private boolean isEscapeSeq (int ch) throws LexicalException {
        if(ch == '\n') {
            tokenRow++;
            col = 1;
        }
        int tempPos = pos+1;
        int tempCh= inputChars[tempPos];

        switch(tempCh)
        {
            case 'b', 't', '"','n', 'r' ->
            {
                return true;
            }
            case 92 -> {
                int tempPos2 = pos+3;
                int tempCh2= inputChars[tempPos2];

                if(tempPos2 != '\n')
                {
                    nextChar();
                    return true;

                }else{
                    error( "single bracket inside ES function");
                    return false;
                }

            }
            default  -> {
                error( " not part of lexical structure");
                return false;
            }
        }

    }


    private boolean isInput_Char(int ch)
    {
        return  ( ch >= 0 && ch <= 127 && ch != 10 && ch != 14);
    }

    private boolean isStringChar(int ch)
    {

        return (isInput_Char(ch) && ch != '"' && ch != 92 && ch != '\n' && ch!='\r');
    }
    private void error(String message) throws LexicalException{
        throw new LexicalException("Error at pos " + pos + ": " + message);
    }








}