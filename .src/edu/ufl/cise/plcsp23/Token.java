package edu.ufl.cise.plcsp23;

import javax.xml.transform.Source;

public class Token implements IToken
{
    //Create fundamentals variables of a token definition
    final Kind kind;
    final int pos;
    final int length;
    final char[] source;
    final int row;
    final int col;

    //===== Contructor =====//
    public Token (Kind _kind,int _row, int _col, int _pos, int _length, char[] _source)
    {
        super(); // idk what this does tbh // ima watch lecture
        kind = _kind;
        pos = _pos;
        length = _length;
        source = _source;
        row = _row;
        col = _col;

    }

    public SourceLocation getSourceLocation()
    {
        //wrong I need to figure out how to get line, pos is the colomun
    SourceLocation sourceLocation = new SourceLocation(row,col);
    return sourceLocation;

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


}