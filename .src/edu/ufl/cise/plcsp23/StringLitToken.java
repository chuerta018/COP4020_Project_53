package edu.ufl.cise.plcsp23;

public class StringLitToken implements IStringLitToken {

    final Kind kind;
    final int pos;
    final int length;
    final char[] source;
    final int row;
    final int col;

    //===== Constructor =====//
    public StringLitToken (Kind _kind, int _row, int _col, int _pos, int _length, char[] _source)
    {
        super(); // idk what this does tbh // ima watch lecture
        kind = _kind;
        row = _row;
        col = _col;
        pos = _pos;
        length = _length;
        source = _source;

    }

    public SourceLocation getSourceLocation()
    {
        //wrong I need to figure out how to get line, pos is the colomun
        return new SourceLocation(row,col);

    }

    public Kind getKind()
    {
        return kind;
    }

    public String getTokenString()
    {
        StringBuilder tokenString = new StringBuilder();
        int j = pos;
        for( int i = 0; i <length; i++)
        {
            tokenString.append(source[j]);
            j++;
        }

        return tokenString.toString();
    }

    @Override
    public String getValue() {
    	StringBuilder tokenString = new StringBuilder();
        int j = pos;
        for( int i = 0; i<length; i++)
        {
            if (source[j] != '"') // includes all escape squences just not "
            {
            	//tokenString.append('L');
                if(source[j] == 92)
                {
                	//tokenString.append('L');
               
                   if (source[j+1] == 92) {
                    	if (source[j+2] == 'b') {
                    		j+=2;
                            i+=2;
                    		tokenString.append('\b');
                    	} else if (source[j+2] == 't') {
                    		j+=2;
                            i+=2;
                    		tokenString.append('\t');
                    	} else if (source[j+2] == '"') {
                    		j+=2;
                            i+=2;
                            tokenString.append('P');
                    		tokenString.append('\"');
                    	} else if (source[j+2] == 'n') {
                    		j+=2;
                            i+=2;
                    		tokenString.append('\n');
                    	} else if (source[j+2] == 'r') {
                    		j+=2;
                            i+=2;
                    		tokenString.append('\r');
                    	} else {
                    		tokenString.append('Q');
                    	}
                 } else if (source[j+1] == 'b') {
                	 j++;
                     i++;
                	 tokenString.append('\b');
                 } else if (source[j+1] == 't') {
                	 j++;
                     i++;
                     //tokenString.append('F');
                	 tokenString.append('\t');
                 } else if (source[j+1] == '"') {
                	 j++;
                     i++;
                	 tokenString.append('"');
                 } else {
                	 tokenString.append('W');
                 }
            } else
            {
            	//tokenString.append('H');
                tokenString.append(source[j]);
                
            }
                
            }
            j++;
        }
        return tokenString.toString();
    }

}