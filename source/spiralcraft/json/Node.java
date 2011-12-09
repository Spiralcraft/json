//
// Copyright (c) 2011 Michael Toth
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


import java.io.StringReader;
import java.io.StringWriter;

import spiralcraft.text.ParseException;
import spiralcraft.text.ParsePosition;
import spiralcraft.util.tree.AbstractNode;

/**
 * 
 * @author mike
 *
 */
public abstract class Node
  extends AbstractNode<Node,Object>
{
  
  private Type type;
  
  protected Node(Type type)
  { this.type=type;
  }
  
  public Type getType()
  { return type;
  }
  
  /**
   * Read a String containing JSON into a parse tree
   * 
   * @param json
   * @return
   */
  public static Node parse(String json)
    throws ParseException
  {
    ParseTree tree=new ParseTree();
    new Parser(new StringReader(json),tree).parse();
    return tree.getRoot();
  }
  
  public static String formatToString(Node root)
    throws JsonException
  {
    StringWriter out=new StringWriter();
    root.playEvents(new JsonWriter(out));
    return out.toString();
  }
    
  private ParsePosition position;
  private String name;
  
  protected void playChildEvents(ContentHandler handler)
      throws JsonException
    {
      for (Node child:this)
      { child.playEvents(handler);
      }
    }

  public abstract void playEvents(ContentHandler handler)
    throws JsonException;
  
  public Node getChild(String name)
  {
    for (Node child:this)
    {
      if (name==child.getName() 
          || (name!=null && name.equals(child.getName()))
         )
      { return child;
      }
    }
    return null;
  }
  
  public String getName()
  { return name;
  }
  
  public void setName(String name)
  { this.name=name;
  }
  
  public void setPosition(ParsePosition position)
  { this.position=position;
  }
  
  public ParsePosition getPosition()
  { return position;
  }
}
