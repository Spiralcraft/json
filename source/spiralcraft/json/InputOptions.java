package spiralcraft.json;

/**
 * Controls aspects of the conversion from JSON data to internal types
 */
public class InputOptions
{
  public boolean ignoreUnrecognizedFields=false;
  public char[] camelCaseSeparators= {'-','_'};
}