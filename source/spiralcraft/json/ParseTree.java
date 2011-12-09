//
// Copyright (c) 1998,2005 Michael Toth
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



import spiralcraft.text.ParseException;
import spiralcraft.text.ParsePosition;


/**
 * A lightweight parse tree of an JSON block. Reads JSON into a tree structure.
 */
public class ParseTree
  implements ContentHandler
{
  
  private Node _root;
  private Node _currentElement;
  private ParsePosition position=new ParsePosition();
  
  public static ParseTree createTree(Node root)
  { return new ParseTree(root);
  }
  

  public ParseTree()
  { 
    _root=new DocumentNode();
    _currentElement=_root;
  }

  public ParseTree(Node root)
  { 
    _root=root;
    _currentElement=_root;
  }

  public void playEvents(ContentHandler handler)
    throws JsonException
  { _root.playEvents(handler);
  }

  public Node getRoot()
  { return _root;
  }
  
  @Override
  public void openObject
    (String name)
    throws ParseException
  {
    // Element element=new Element(uri,localName,qName,attributes);
    Node node=new ObjectNode(name);
    _currentElement.addChild(node);
    _currentElement=node;
    node.setPosition(position.clone());
  }

  @Override
  public void closeObject
    (String name)
    throws ParseException
  { 
    // updatePosition();
    _currentElement=_currentElement.getParent();
  }
  
  @Override
  public void openArray
    (String name)
    throws ParseException
  {
    // Element element=new Element(uri,localName,qName,attributes);
    Node node=new ArrayNode(name);
    _currentElement.addChild(node);
    _currentElement=node;
    node.setPosition(position.clone());
  }

  @Override
  public void closeArray
    (String name)
    throws ParseException
  { 
    // updatePosition();
    _currentElement=_currentElement.getParent();
  }  
  
  @Override
  public void handleString
    (String name
    ,String value
    )
    throws ParseException
  { 
    // updatePosition();
    StringNode node=new StringNode(name,value);
    node.setPosition(position.clone());
    _currentElement.addChild(node);
  }

  @Override
  public void handleNumber
    (String name
    ,Number value
    )
    throws ParseException
  { 
    // updatePosition();
    NumberNode node=new NumberNode(name,value);
    node.setPosition(position.clone());
    _currentElement.addChild(node);
  }

  @Override
  public void handleBoolean
    (String name
    ,boolean value
    )
    throws ParseException
  { 
    // updatePosition();
    BooleanNode node=new BooleanNode(name,value);
    node.setPosition(position.clone());
    _currentElement.addChild(node);
  }

  @Override
  public void handleNull
    (String name
    )
    throws ParseException
  { 
    // updatePosition();
    NullNode node=new NullNode(name);
    node.setPosition(position.clone());
    _currentElement.addChild(node);
  }

  @Override
  public void setLocator(
    ParsePosition position)
  { this.position=position.clone();
  }

}
