package edu.ufl.cise.plcsp23;

public class StringLitToken implements IStringLitToken {

    final Kind kind;
    final int pos;
    final int row;
    final int length;
    final char[] source;

    //===== Constructor =====//
    public StringLitToken (Kind _kind, int _row, int _pos, int _length, char[] _source)
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
        StringBuilder tokenString = new StringBuilder();
        int j = pos;
        for( int i = 0; i<length ; i++)
        {
            tokenString.append(source[j]);
            j++;
        }

        return tokenString.toString();
    }

    @Override
    public String getValue() {
            return getTokenString();
    }

}