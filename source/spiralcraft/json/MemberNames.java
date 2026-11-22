package spiralcraft.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lossless translation between JSON member names and Spiralcraft identifiers.
 *
 * A configurable set of symbols (e.g. "@") are written into identifiers as
 * "__code__", where code is one or more lowercase letters. The sequence "__"
 * is itself reserved: a literal "__" in a JSON name is written as "__us__".
 * Single underscores are never special.
 *
 * Instances are immutable and thread-safe.
 */
public final class MemberNames
{
  private static final String ESCAPE="__";
  private static final String ESCAPE_CODE="us";

  /** "@" as "at" */
  public static final MemberNames DEFAULT
    =new MemberNames(Collections.singletonMap("@","at"));

  private final List<String> symbols;           // longest first
  private final Map<String,String> codeBySymbol;
  private final Map<String,String> symbolByCode;
  private final String firstChars;              // fast-path test for encode

  /**
   * @param symbolToCode literal text -> code (lowercase letters only)
   */
  public MemberNames(Map<String,String> symbolToCode)
  {
    codeBySymbol=new LinkedHashMap<String,String>();
    symbolByCode=new HashMap<String,String>();
    StringBuilder first=new StringBuilder("_");

    for (Map.Entry<String,String> e : symbolToCode.entrySet())
    {
      String symbol=e.getKey();
      String code=e.getValue();
      if (symbol==null || symbol.isEmpty() || symbol.indexOf('_')>=0)
      { throw new IllegalArgumentException
          ("Symbol must be non-empty and contain no '_': ["+symbol+"]");
      }
      if (code==null || !code.matches("[a-z]+"))
      { throw new IllegalArgumentException
          ("Code must be one or more lowercase letters: ["+code+"]");
      }
      if (code.equals(ESCAPE_CODE))
      { throw new IllegalArgumentException
          ("Code '"+ESCAPE_CODE+"' is reserved for the escape sequence");
      }
      if (symbolByCode.put(code,symbol)!=null)
      { throw new IllegalArgumentException("Duplicate code: "+code);
      }
      codeBySymbol.put(symbol,code);
      if (first.indexOf(symbol.substring(0,1))<0)
      { first.append(symbol.charAt(0));
      }
    }
    symbolByCode.put(ESCAPE_CODE,ESCAPE);

    symbols=new ArrayList<String>(codeBySymbol.keySet());
    Collections.sort
      (symbols
      ,new Comparator<String>()
      { 
        @Override
        public int compare(String a,String b)
        { return b.length()-a.length();
        }
      }
      );
    firstChars=first.toString();
  }

  /** JSON member name -> Spiralcraft identifier (reader side) */
  public String toIdentifier(String member)
  {
    if (!needsEncoding(member))
    { return member;
    }

    StringBuilder out=new StringBuilder(member.length()+8);
    int i=0;
    int n=member.length();
    outer:
    while (i<n)
    {
      if (member.startsWith(ESCAPE,i))
      { 
        out.append(ESCAPE).append(ESCAPE_CODE).append(ESCAPE);
        i+=2;
        continue;
      }
      for (String symbol : symbols)
      {
        if (member.startsWith(symbol,i))
        { 
          out.append(ESCAPE).append(codeBySymbol.get(symbol)).append(ESCAPE);
          i+=symbol.length();
          continue outer;
        }
      }
      out.append(member.charAt(i++));
    }
    return out.toString();
  }

  /** Spiralcraft identifier -> JSON member name (writer side) */
  public String toMember(String identifier)
  {
    if (identifier.indexOf(ESCAPE)<0)
    { return identifier;
    }

    StringBuilder out=new StringBuilder(identifier.length());
    int i=0;
    int n=identifier.length();
    while (i<n)
    {
      if (identifier.startsWith(ESCAPE,i)
          && i+2<n
          && isCodeChar(identifier.charAt(i+2))
         )
      {
        int end=identifier.indexOf(ESCAPE,i+2);
        if (end>0)
        {
          String code=identifier.substring(i+2,end);
          String symbol=symbolByCode.get(code);
          if (symbol!=null)
          { 
            out.append(symbol);
            i=end+2;
            continue;
          }
        }
      }
      out.append(identifier.charAt(i++));
    }
    return out.toString();
  }

  private boolean needsEncoding(String s)
  {
    for (int i=0;i<s.length();i++)
    { 
      if (firstChars.indexOf(s.charAt(i))>=0)
      { return true;
      }
    }
    return false;
  }

  private static boolean isCodeChar(char c)
  { return c>='a' && c<='z';
  }
}