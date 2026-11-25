//
// Copyright (c) 1998,2026 Michael Toth
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
package spiralcraft.json.test;

import java.io.StringWriter;
import java.util.HashSet;

import spiralcraft.json.BooleanNode;
import spiralcraft.json.DataWriter;
import spiralcraft.json.MemberNames;
import spiralcraft.json.Node;
import spiralcraft.json.NullNode;
import spiralcraft.json.NumberNode;
import spiralcraft.json.StringNode;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.util.string.CamelSnakeConverter;

/**
 * Writes a bean with a DataWriter directly, as Java code does, rather
 *   than through ToJson, which converts a bean to a tuple first.
 */
public class DirectWrite
{
  private static final MemberNames SNAKE
    =MemberNames.DEFAULT.wrap(CamelSnakeConverter.INSTANCE);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static String write(Object data,MemberNames names)
    throws Exception
  {
    StringWriter out=new StringWriter();
    DataWriter writer=new DataWriter();
    writer.setExcludes(new HashSet<java.net.URI>());
    writer.setMemberNames(names);
    writer.writeToWriter
      (out,BeanReflector.getInstance(data.getClass()),data);
    return out.toString();
  }

  /**
   * A bean, its names converted to snake_case
   */
  public static String write(Object data)
    throws Exception
  { return write(data,SNAKE);
  }

  /**
   * A bean, its names as they are
   */
  public static String writeAsIs(Object data)
    throws Exception
  { return write(data,null);
  }

  /**
   * A Node by itself
   */
  public static String writeNode(Node node)
    throws Exception
  { return write(node,SNAKE);
  }

  /**
   * A string, a number, a boolean and a null node, each as the value of a
   *   ToolDto's property, separated by a space
   */
  public static String writeScalars()
    throws Exception
  {
    return write(new ToolDto(new StringNode(null,"s")))
      +" "+write(new ToolDto(new NumberNode(null,12)))
      +" "+write(new ToolDto(new BooleanNode(null,true)))
      +" "+write(new ToolDto(new NullNode(null)));
  }
}
