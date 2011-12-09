//
// Copyright (c) 2009 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.json;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;

//import spiralcraft.log.ClassLog;
import spiralcraft.text.LookaheadParserContext;
import spiralcraft.text.ParseException;
import spiralcraft.util.string.StringUtil;

public class Parser
{
//  private static final ClassLog log
//    =ClassLog.getInstance(Parser.class);
  
  private static final int TT_WORD=-1;
  private static final int TT_EOF=-2;

  
  private static final Reader safeReader(InputStream in)
  {
    try
    { return new InputStreamReader(in,"UTF-8");
    }
    catch (UnsupportedEncodingException x)
    { throw new RuntimeException("UTF-8",x);
    }
  }
  
  private final LookaheadParserContext context;
  private final ContentHandler handler;
  
  private String stringToken;
  private int charToken;
  
//  private StringBuilder ws=new StringBuilder();  
  
  public Parser(InputStream in,ContentHandler handler)
    throws ParseException
  { this(safeReader(in),handler);
  }
  
  public Parser(Reader in,ContentHandler handler)
    throws ParseException
  { 
    this.context=new LookaheadParserContext(in);
    this.handler=handler;
    handler.setLocator(context.getPosition());
  }
  
  public void parse()
    throws ParseException
  {
    nextToken();    
    parseJSONtext(null);
    if (charToken!=TT_EOF)
    { throwUnexpected("End of input expected");
    }
  }
  
  /**
   * JSON-text = object / array
   */
  private void parseJSONtext(String name)
    throws ParseException
  {

    switch (charToken)
    {
    case '{':
      parseObject(name);
      break;
    case '[':
      parseArray(name);
      break;
    default:
      throwUnexpected();
    }
    
  }
  
  /**
   *  object = begin-object [ member *( value-separator member ) ] end-object
   */
  private void parseObject(String name)
    throws ParseException
  {
    nextToken();
    handler.openObject(name);
    if (context.getCurrentChar()!='}')
    {
      while (true)
      { 
        // log.fine("["+context.getCurrentChar()+"]:["+charToken+"]:["+stringToken+"]");
        parseMember();

        if (charToken!=',')
        { break;
        }
        else
        { nextToken();
        }
      }
    }
    expect('}');
    
    handler.closeObject(name);
  }
  
  
  /**
   * array = begin-array [ value *( value-separator value ) ] end-array
   * 
   * @param name
   * @throws ParseException
   */
  private void parseArray(String name)
    throws ParseException
  {
    nextToken();
    handler.openArray(name);
    
    if (charToken!=']')
    {
      while (true)
      { 
        parseValue(null);
        if (charToken!=',')
        { break;
        }
        else
        { nextToken();
        }
      }
    }
    expect(']');
    handler.closeArray(name);
  }
  
  /**
   * value = false / null / true / object / array / number / string
   * 
   * @param name
   */
  private void parseValue(String name)
    throws ParseException
  {
    switch (charToken)
    {
    case '{':
      parseObject(name);
      break;
    case '[':
      parseArray(name);
      break;
    case TT_WORD:
      parseLiteral(name);
      break;
    case '"':
      handler.handleString(name,stringToken);
      nextToken();
      break;
    default:
      throwUnexpected();
    }    
  }
  
  
  private void parseLiteral(String name)
    throws ParseException
  {
    
    if (stringToken.equals("true"))
    { 
      handler.handleBoolean(name,true);
      nextToken();
    }
    else if (stringToken.equals("false"))
    { 
      handler.handleBoolean(name,false);
      nextToken();
    }
    else if (stringToken.equals("null"))
    { 
      handler.handleNull(name);
      nextToken();
    }
    else if (stringToken.startsWith("-") 
            || Character.isDigit(stringToken.charAt(0))
            )
    { 
      parseNumber(name);
    }
    else
    { throwUnexpected();
    }
    
  }
  
  private void parseNumber(String name)
    throws ParseException
  {
    
//    boolean negative=false;
    int pos=0;
    if (stringToken.startsWith("-"))
    {
//      negative=true;
      pos=1;
    }
    
    pos=skipDigits(pos);
    boolean decimal=false;
    if (pos<stringToken.length() && stringToken.charAt(pos)=='.')
    {
      decimal=true;
      pos++;
      skipDigits(pos);
    }
    
    boolean exponent=false;
    if (pos<stringToken.length() && stringToken.charAt(pos)=='E')
    {
      pos++;
      exponent=true;
      
//      boolean negativeExponent=false;
      if (pos<stringToken.length() 
          && 
            (stringToken.charAt(pos)=='-'
              || stringToken.charAt(pos)=='+'
            )
         )
      { 
//        negativeExponent=true;
        pos++;
      }
      skipDigits(pos);
      
    }
    
    if (decimal || exponent)
    { 
      handler.handleNumber
        (name,new BigDecimal(stringToken));
    }
    else
    { 
      handler.handleNumber
        (name,new BigInteger(stringToken));
    }
    
    nextToken();
  }
  
  private int skipDigits(int pos)
    throws ParseException
  {
    if (!Character.isDigit(stringToken.charAt(pos)))
    { throwUnexpected("Expected a digit");
    }
    
    while (pos<stringToken.length() 
            && Character.isDigit(stringToken.charAt(pos))
           )
    { pos++;
    }
    return pos;
  }
  
//  private String readDigits(int tokenPos)
//  {
//    
//    StringBuilder digits=new StringBuilder();
//    while (tokenPos<stringToken.length() 
//           && Character.isDigit(stringToken.charAt(tokenPos))
//           )
//    { 
//      digits.append(stringToken.charAt(tokenPos));
//      tokenPos++;
//    }
//    return digits.toString();
//  }
  
  /**
   * member = string name-separator value
   * 
   * @param name
   */
  private void parseMember()
    throws ParseException
  { 
    if (stringToken==null)
    { throwUnexpected("Expected member name");
    }
    String name=stringToken;
    // log.fine("Member name="+stringToken);
    
    nextToken();

    expect(':');
    parseValue(name);
  }
  
  private boolean isWordChar(int token)
  { 
    boolean ret= 
      Character.isLetterOrDigit(token)
      || ".-+".indexOf(token)>-1;
    
    return ret;
  }
  
  private void readWord()
    throws ParseException
  {
    StringBuilder str=new StringBuilder();
    while (isWordChar(context.getCurrentChar()))
    { 
      str.append(context.getCurrentChar());
      context.advance();
    }
    stringToken=str.toString();
    charToken=TT_WORD;
  }
  
  private void readQuotedString()
    throws ParseException
  {
    
    context.advance();
    StringBuilder str=new StringBuilder();
    while (true)
    {
      boolean consumed=false;
      
      char chr=context.getCurrentChar();
      if (chr=='"')
      { 
        context.advance();
        break;
      }
      else if (chr=='\\')
      { 
        context.advance();
        switch (context.getCurrentChar())
        {
        case '"':
        case '/':  
        case '\\':
          break;
          
        case 'u':
          context.advance();
          
          char[] unicode=new char[4];
          for (int i=0;i<4;i++)
          {
            unicode[i]=context.getCurrentChar();
            context.advance();
          }
          str.append
            (Character.toChars(Integer.parseInt(new String(unicode),16)));
          consumed=true;
          break;
        
          
          
        case 'r':
          str.append('\r');
          consumed=true;
          break;
        case 'n':
          str.append('\n');
          consumed=true;
          break;
        case 'b':
          str.append('\b');
          consumed=true;
          break;
        case 'f':
          str.append('\f');
          consumed=true;
          break;
        case 't':
          str.append('\t');
          consumed=true;
          break;
          
          
        default:
          throwUnexpected("Invalid escape sequence");          
        }
      }
      
      if (!consumed)
      { 
        str.append(context.getCurrentChar());
        context.advance();
      }
    }
    
    stringToken=str.toString();
    charToken='\"';
  }
  
  
  private void expect(char token)
    throws ParseException
  { 
    if (charToken!=token)
    { throwUnexpected(token);
    }
    nextToken();
  }

  private void nextToken()
    throws ParseException
  {

    
    while (isWhitespace(context.getCurrentChar()))
    { context.advance();
    }
    
    if (context.isEof())
    {
      charToken=TT_EOF;
      stringToken=null;
      return;
    }
    
    if (context.getCurrentChar()=='"')
    { readQuotedString();
    }
    else if (isWordChar(context.getCurrentChar()))
    { readWord();
    }
    else
    { 
      charToken=context.getCurrentChar();
      stringToken=null;
      if (!context.isEof())
      { context.advance();
      }
    }
//    log.fine("nextToken=["+((char) charToken)+"]:["+stringToken+"]");
  }
  
  /**
   *   ws = *(
   *             %x20 /              ; Space
   *             %x09 /              ; Horizontal tab
   *             %x0A /              ; Line feed or New line
   *             %x0D                ; Carriage return
   *         )
   */
  private boolean isWhitespace(int chr)
  {
    switch (chr)
    {
    case ' ':
    case '\t':
    case '\r':
    case '\n':
      return true;
    default:
      return false;
    }
  }
  
  private void throwUnexpected(String msg)
    throws ParseException
  { 
    throw new ParseException
      ("Unexpected token "+tokenString()+": "+msg,context.getPosition());
  }
  
  private void throwUnexpected()
    throws ParseException
  { 
    throw new ParseException
      ("Unexpected token "+tokenString(),context.getPosition());
  }
  
  private void throwUnexpected(char token)
      throws ParseException
  { 
    throw new ParseException
      ("Unexpected token ["+tokenString()+"], expected ["+token+"]"
      ,context.getPosition()
      );
  }
  
  private String tokenString()
  { return StringUtil.debugFormat((char) charToken);
  }
}
