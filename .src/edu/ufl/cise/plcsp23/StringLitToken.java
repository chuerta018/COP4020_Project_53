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
        for( int i = 0; i<length; i++)
        {
            tokenString.append(source[j]);
            j++;
        }

        return tokenString.toString();
    }

    @Override
    public String getValue() {
    	StringBuilder tokenString = new StringBuilder();
        int j = pos+1;
        int k = 0;
        tokenString.append(source[j]);
        j++;
        for( int i = 1; i< length - 2; i++)
        { 
            if(tokenString.charAt(k) != 92) {
            	k++;
              	tokenString.append(source[j]);
            } else {
            	if (source[j] == 92) {

            	} else if (source[j] == 'b') {
            		tokenString.deleteCharAt(k);
            		tokenString.append('\b');          		
            	} else if (source[j] == 't') {
            		tokenString.deleteCharAt(k);
            		tokenString.append('\t');   		
            	} else if (source[j] == 'n') {
            		tokenString.deleteCharAt(k);
            		tokenString.append('\n');
            	} else if (source[j] == 'r') {
            		tokenString.deleteCharAt(k);
            		tokenString.append('\r');
            	} else if (source[j] == '"') {
            		tokenString.deleteCharAt(k);
            		tokenString.append('\"');
            		tokenString.deleteCharAt(k);
            	} else {
            		tokenString.append('F');
            	}
            	//char bSlash = 92;
            	//tokenString.append(bSlash);
            	//j++;
            	//tokenString.append('"');
            }
            j++;
        }

        return tokenString.toString();
    }

}