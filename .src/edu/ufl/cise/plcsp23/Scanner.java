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

    //constructor
    public Scanner(String _input)
    {
        this.input = _input;
        inputChars = Arrays.copyOf(_input.toCharArray(),_input.length()+1);
        pos =  0;
        ch = inputChars[pos];

    }



    @Override
    public Token next() throws LexicalException
    {
    	
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

    private Token scanToken() throws LexicalException
    {
        State state = State.START;
        int tokenStart = -1;
        while(true) { //read chars, loop terminates when a Token is returned
            switch(state) {
                case  START -> {
                    tokenStart = pos;
                    switch (ch)
                    {
                        case 0 -> //EOF CASE
                        { //end of input
                            return new Token(EOF, tokenStart, 0, inputChars);
                        }
                        case ' ','\n','\r','\t','\f' -> nextChar(); // WHITE SPACE
                        case '+' -> { // single Char +
                            nextChar();
                            return new Token(PLUS, tokenStart, 1, inputChars);
                        }
                        case '*' -> { // === returns '*' or '**' === //
                            state = State.HAVE_EXP;
                            nextChar();

                        }
                        case '0' -> { // === single Char 0 === //
                            nextChar();
                            return new Token(NUM_LIT, tokenStart, 1, inputChars);
                        }
                        case '.' -> { // === single Char . === //
                            nextChar();
                            return new Token(DOT, tokenStart, 1, inputChars);
                        }
                        case ',' -> { // === single Char , === //
                            nextChar();
                            return new Token(COMMA, tokenStart, 1, inputChars);
                        }
                        case '?' -> { // === single Char ? === //
                            nextChar();
                            return new Token(QUESTION, tokenStart, 1, inputChars);
                        }
                        case ':' -> { // === single Char : === //
                            nextChar();
                            return new Token(COLON, tokenStart, 1, inputChars);
                        }
                        case '(' -> { // === single Char ( === //
                            nextChar();
                            return new Token(LPAREN, tokenStart, 1, inputChars);
                        }
                        case ')' -> { // === single Char ) === //
                            nextChar();
                            return new Token(RPAREN, tokenStart, 1, inputChars);
                        }
                        case '<' -> { // === returns '='or '<=' or '<->' === //
                            state = State.HAVE_LT;
                            nextChar();

                        }
                        case '>' -> { // === returns '>' or '>=' token === //
                            state = State.HAVE_GT;
                            nextChar();
                        }
                        case '[' -> { // === single Char [ === //
                            nextChar();
                            return new Token(LSQUARE, tokenStart, 1, inputChars);
                        }
                        case ']' -> { // === single Char ] === //
                            nextChar();
                            return new Token(RSQUARE, tokenStart, 1, inputChars);
                        }
                        case '{' -> { // === single Char { === //
                            nextChar();
                            return new Token(LCURLY, tokenStart, 1, inputChars);
                        }
                        case '}' -> { // === single Char } === //
                            nextChar();
                            return new Token(RCURLY, tokenStart, 1, inputChars);
                        }
                        case '=' -> { // === Returns = or == tokens  === //
                            state = State.HAVE_EQ;
                            nextChar();
                        }
                        case '!' -> { // === single Char ! === //
                            nextChar();
                            return new Token(BANG, tokenStart, 1, inputChars);
                        }
                        case '&' -> { // === returns '&' or '&&' token === //
                            state = State.HAVE_AND;
                            nextChar();
                        }
                        case '|' -> { // === returns '|' or '||' token === //
                            state = State.HAVE_OR;
                            nextChar();
                        }
                        case '/' -> { // === single Char / === //
                            nextChar();
                            return new Token(DIV, tokenStart, 1, inputChars);
                        }
                        case '-' -> { // === single Char - === //
                            nextChar();
                            return new Token(MINUS, tokenStart, 1, inputChars);
                        }
                        case '%' -> { // === single Char % === //
                            nextChar();
                            return new Token(MOD, tokenStart, 1, inputChars);
                        }
                        case '1','2','3','4','5','6','7','8','9' -> {//char is nonzero digit
                            state = State.IN_NUM_LIT;
                            nextChar();
                        }
                        case '"' -> {
                            state = State.IN_STRING_LIT;
                            nextChar();


                        }
                        case '~' -> {
                        	state = State.IN_COMMENT_LIT;
                        	nextChar();
                        }
                        default -> {
                            if (isLetter(ch)) {
                                state = State.IN_IDENT;
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
                        return new Token(IToken.Kind.EQ, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        nextChar();
                        return new Token(ASSIGN, tokenStart, 1, inputChars);
                    }

                } //=== END OF HAVE_EQ ===//
                case HAVE_GT ->
                {
                    if (ch == '=') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.GE, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        nextChar();
                        return new Token(GT, tokenStart, 1, inputChars);
                    }

                }
                case HAVE_LT -> {

                    if (ch == '=') // === this case is '<=' === //
                    {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.LE, tokenStart, 2, inputChars);
                    }
                    else if (ch == '-')
                    {
                        char temp = inputChars[pos++];

                        if(temp == '>')// === this case is '<->' === //
                        {
                            state = state.START;
                            nextChar();
                            return new Token(EXCHANGE, tokenStart, 3, inputChars);
                        }
                        else { // === this case is '<' === //
                            pos--;// not sure if the pos++ in the array brackets permanently increases the pos variable or temporarily.
                            state = state.START;
                            nextChar();
                            return new Token(IToken.Kind.LT, tokenStart, 1, inputChars);
                        }
                    }
                }
                case HAVE_AND -> {
                    if (ch == '&') {
                        state = state.START;
                        nextChar();
                        return new Token(IToken.Kind.AND, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        nextChar();
                        return new Token(BITAND, tokenStart, 1, inputChars);
                    }

                }
                case HAVE_OR -> {
                    if (ch == '|') {
                        state = state.START;
                        nextChar();
                        return new Token(OR, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        nextChar();
                        return new Token(BITOR, tokenStart, 1, inputChars);
                    }
                }
                case HAVE_EXP -> {

                    if (ch == '*') {
                        state = state.START;
                        nextChar();
                        return new Token(EXP, tokenStart, 2, inputChars);
                    }
                    else {
                        state = state.START;
                        nextChar();
                        return new Token(TIMES, tokenStart, 1, inputChars);
                    }

                }
                case IN_NUM_LIT -> {
                    if (isDigit(ch)) {//char is digit, continue in IN_NUM_LIT state
                        nextChar();
                    }
                    else {
                        //current char belongs to next token, so don't get next char
                        int length = pos-tokenStart;
                        return new Token(IToken.Kind.NUM_LIT, tokenStart, length, inputChars);
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
                        return new Token(kind, tokenStart, length, inputChars);
                    }

                }
                case IN_STRING_LIT ->
                {
                    if (isStringChar(ch))
                    {
                        nextChar();
                    }
                    else if (ch == '"')
                    {
                        int length = pos-tokenStart;
                        return new Token(STRING_LIT,tokenStart,length,inputChars);

                    }
                    else{

                        error("Not a valid string Token/Or does not satisfy token");
                    }

                }
                case IN_COMMENT_LIT -> {
                	if (isInput_Char(ch)) {
                		nextChar();
                	}
                	else {
                		error("Not a valid string Token/Or does not satisfy token");
                	}
                }
                default -> {
                    throw new UnsupportedOperationException("Bug in Scanner");
                }

            }
        }
    }

    public void nextChar(){
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

    private boolean isEscapeSeq(int ch)
    {
        return (ch == '\b') || (ch == '\t') || (ch == '\n') || (ch == '\"') || (ch == '\\');
    }

    private boolean isInput_Char(int ch)
    {
        return  ( ch >= 0 && ch <= 127 && ch != 10 && ch != 14);
    }

    private boolean isStringChar(int ch)
    {

        return (isInput_Char(ch) && ch !='"' && ch != '\'' ) || (isEscapeSeq(ch));
    }
    private void error(String message) throws LexicalException{
        throw new LexicalException("Error at pos " + pos + ": " + message);
    }








}