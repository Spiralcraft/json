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

import spiralcraft.text.ParseException;

public class NumberNode
  extends Node
{

  public NumberNode(String name,Number value)
  { 
    super(Type.NUMBER);
    setName(name);
    set(value);
  }
  
  @Override
  public void playEvents(
    ContentHandler handler)
    throws JsonException
  {
    try
    { handler.handleNumber(getName(),(Number) get());
    }
    catch (ParseException x)
    { throw new JsonException("Parse Exception",x);
    }
  }

}
