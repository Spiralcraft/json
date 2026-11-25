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

import spiralcraft.json.FromJson;
import spiralcraft.json.InputOptions;
import spiralcraft.util.string.CamelSnakeConverter;

/**
 * Reads a ChatDto with a FromJson whose input options have no camelCase
 *   separators, so that only an IdentifierConverter can map a snake_case
 *   name to a member.
 */
public class DirectRead
{
  /**
   * @param converter whether the FromJson has a CamelSnakeConverter
   * @return the members read, or the class of what the read threw
   */
  public static String read(String json,boolean converter)
  {
    try
    {
      FromJson<ChatDto,String> from=new FromJson<ChatDto,String>(ChatDto.class);
      InputOptions options=new InputOptions();
      options.camelCaseSeparators=null;
      from.setInputOptions(options);
      if (converter)
      { from.setIdentifierConverter(CamelSnakeConverter.INSTANCE);
      }
      ChatDto dto=from.getStringFn().evaluate(json,null);
      return dto.getMaxTokens()+" "+dto.getToolCallId()+" "+dto.getTopP()
        +" "+dto.get__at__meta();
    }
    catch (Exception x)
    { return "error "+x.getClass().getName();
    }
  }
}
