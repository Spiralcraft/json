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

import java.io.IOException;

import spiralcraft.text.ParseException;
import spiralcraft.text.ParsePosition;
import spiralcraft.util.string.StringUtil;

public class JsonWriter
 implements ContentHandler
{

  public static final String escape(String input)
  {
    StringBuilder out=new StringBuilder();
    for (char c:input.toCharArray())
    { 
      switch (c)
      {
      case '\"':
      case '\'':
      case '\\':
        out.append("\\");
        out.append(c);
        continue;
      case '\n':
        out.append("\\n");
        continue;
      case '\b':
        out.append("\\b");
        continue;
      case '\t':
        out.append("\\t");
        continue;
      case '\r':
        out.append("\\r");
        continue;
      case '\f':
        out.append("\\f");
        continue;
      }

      if (Character.isISOControl(c))
      {
        out.append("\\u"+StringUtil.prepad(Integer.toHexString(c),'0',4));
        continue;
      }
      
      out.append(c);
    }
    return out.toString();
  }
  
  private final java.io.Writer out;
  private ParsePosition position;
//  private int level=0;
  private String indent="";  
  private StackFrame currentFrame=new StackFrame(null);
  
  public JsonWriter(java.io.Writer out)
  { this.out=out;
  }
  
  @Override
  public void openArray(String name) throws ParseException
  {
    try
    {
      writeMember(name);
      nextLine();
      out.write("[");
      pushLevel();
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }
      

  }

  @Override
  public void openObject(String name) throws ParseException
  {
    try
    {
      writeMember(name);
      nextLine();
      out.write("{");
      pushLevel();
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }
  }

  
  @Override
  public void closeArray(String name) throws ParseException
  {
    try
    { 
      popLevel();
      nextLine();
      out.write("]");
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }

  }

  @Override
  public void closeObject(String name) throws ParseException
  {
    try
    { 
      popLevel();
      nextLine();
      out.write("}");
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }

  }  
  
  @Override
  public void handleString(String name,String value)
      throws ParseException
  {
    try
    { 
      writeMember(name);
      out.write('\"');
      out.write(escape(value));
      out.write('\"');

    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }
  }

  @Override
  public void handleBoolean(String name,boolean value)
     throws ParseException    
  { 
    try
    { 
      writeMember(name);
      if (value)
      { out.write("true");
      }
      else
      { out.write("false");
      }
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }
  }

  @Override
  public void handleNumber(String name,Number value)
     throws ParseException    
  { 
    try
    { 
      writeMember(name);
      out.write(value.toString());
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }
  }
  
  @Override
  public void handleNull(String name)
     throws ParseException    
  { 
    try
    { 
      
      writeMember(name);
      out.write("null");
    }
    catch (IOException x)
    { throw new ParseException("Error writing json output",position,x);
    }
  }  
  
  private void writeMember(String name)
    throws IOException
  {
    nextLine();
    if (currentFrame.elementCount>0)
    { out.write(",");
    }
    currentFrame.elementCount++;
    if (name!=null)
    {
      out.write('\"');
      out.write(name);
      out.write("\" :  ");   
    }
  }


  @Override
  public void setLocator(ParsePosition position)
  { this.position=position;
  }
  
  public ParsePosition getLocator()
  { return this.position;
  }
  
  private void nextLine()
    throws IOException
  {
    newLine();
    out.write(indent);
  }
  
  private void newLine()
    throws IOException
  { 
    out.write("\r\n");
  }

  private void pushLevel()
    throws IOException
  { 
//    level++;
    indent=indent+"  ";
    currentFrame=new StackFrame(currentFrame);
    
  }
  
  private void popLevel()
    throws IOException
  { 
//    level--;
    indent=indent.substring(0,indent.length()-2);
    currentFrame=currentFrame.parent;
  }
}

class StackFrame
{ 
  public final StackFrame parent;
  public int elementCount;
  
  public StackFrame(StackFrame parent)
  { this.parent=parent;
  }
  
}
