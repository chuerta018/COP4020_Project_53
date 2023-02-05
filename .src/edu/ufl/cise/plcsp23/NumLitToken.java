package edu.ufl.cise.plcsp23;

public class NumLitToken implements INumLitToken{

    final Kind kind;
    final int pos;
    final int row;
    final int length;
    final char[] source;

    //===== Constructor =====//
    public NumLitToken (Kind _kind, int _row, int _pos, int _length, char[] _source)
    {
        super(); // idk what this does tbh // ima watch lecture
        kind = _kind;
        pos = _pos;
        row = _row;
        length = _length;
        source = _source;

    }

    public SourceLocation getSourceLocation()
    {
        //wrong I need to figure out how to get line, pos is the colomun
        return new SourceLocation(row,pos);

    }

    public Kind getKind()
    {
        return kind;
    }

    public String getTokenString()
    {
        String tokenString = "";
        int j = pos;
        for( int i = 0; i<length ; i++)
        {
            tokenString += source[j];
            j++;
        }

        return tokenString;
    }

    @Override
    public int getValue(){
        String tokenString = getTokenString();
        return Integer.parseInt(tokenString);
    }


}