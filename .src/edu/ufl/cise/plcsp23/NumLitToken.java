package edu.ufl.cise.plcsp23;

public class NumLitToken implements INumLitToken{

    final Kind kind;
    final int pos;
    final int length;
    final char[] source;
    final int row;
    final int col;

    //===== Constructor =====//
    public NumLitToken (Kind _kind, int _row, int _col, int _pos, int _length, char[] _source)
    {
        super();
        kind = _kind;
        pos = _pos;
        row = _row;
        col = _col;
        length = _length;
        source = _source;

    }

    public SourceLocation getSourceLocation()
    {
        return new SourceLocation(row,col);
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