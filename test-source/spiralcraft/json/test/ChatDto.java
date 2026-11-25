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

/**
 * A data transfer object with camelCase names, as an API client's request
 *   has, and a name that holds a symbol ("@meta") escaped as "__at__meta".
 */
public class ChatDto
{
  private Integer maxTokens=64;
  private String toolCallId="call_1";
  private Double topP=0.5;
  private String __at__meta="m";

  public Integer getMaxTokens()
  { return maxTokens;
  }

  public void setMaxTokens(Integer maxTokens)
  { this.maxTokens=maxTokens;
  }

  public String getToolCallId()
  { return toolCallId;
  }

  public void setToolCallId(String toolCallId)
  { this.toolCallId=toolCallId;
  }

  public Double getTopP()
  { return topP;
  }

  public void setTopP(Double topP)
  { this.topP=topP;
  }

  public String get__at__meta()
  { return __at__meta;
  }

  public void set__at__meta(String value)
  { this.__at__meta=value;
  }
}
