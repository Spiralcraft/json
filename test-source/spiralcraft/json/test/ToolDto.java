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
//
package spiralcraft.json.test;

import spiralcraft.json.Node;
import spiralcraft.text.ParseException;

/**
 * A data transfer object holding a literal JSON subtree, as a request's
 *   tool definition holds a JSON Schema. The default instance holds SCHEMA,
 *   one Node shared by every instance.
 */
public class ToolDto
{
  public static final Node SCHEMA;
  static
  {
    try
    {
      SCHEMA=Node.parse
        ("{\"type\":\"object\",\"additionalProperties\":false"
         +",\"properties\":{\"locationName\":{\"type\":\"string\"}}}"
        );
    }
    catch (ParseException x)
    { throw new ExceptionInInitializerError(x);
    }
  }

  private final Node toolSchema;

  public ToolDto()
  { this.toolSchema=SCHEMA;
  }

  public ToolDto(Node toolSchema)
  { this.toolSchema=toolSchema;
  }

  public Node getToolSchema()
  { return toolSchema;
  }
}
