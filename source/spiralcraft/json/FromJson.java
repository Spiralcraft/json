package spiralcraft.json;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.ChannelFactory;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.spi.SourcedChannel;
import spiralcraft.text.ParseException;

public class FromJson<Ttarget,Tsource>
  implements ChannelFactory<Ttarget,Tsource>
{

  static enum InputType
  { BYTEARRAY
    ,STRING
  }
  
  private Reflector<Ttarget> resultType;
  
  
  public FromJson(Reflector<Ttarget> resultType)
  { this.resultType=resultType;
  }
  
  @Override
  public Channel<Ttarget> bindChannel(
    Channel<Tsource> source,
    Focus<?> focus,
    Expression<?>[] arguments)
    throws BindException
  { return new FromJsonChannel(source);
  }

  
  class FromJsonChannel
    extends SourcedChannel<Tsource,Ttarget>
  {

    private InputType inputType;
    
    FromJsonChannel(Channel<Tsource> source)
      throws BindException
    { 
      super(resultType,source);
      if (source.getContentType()==byte[].class)
      { inputType=InputType.BYTEARRAY;
      }
      else if (source.getContentType()==String.class)
      { inputType=InputType.STRING;
      }
      else
      { 
        throw new BindException
          ("Unrecognized input type "+source.getContentType().getName()
          );
      }
        
        
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected Ttarget retrieve()
    {
      Reader input;
      switch (inputType)
      {
        case STRING:
          input=new StringReader((String) source.get());
          break;
        case BYTEARRAY:
          input=new InputStreamReader(new ByteArrayInputStream((byte[]) source.get()));
          break;
        default:
          throw new AccessException("Unrecognized input type "+inputType);
      }
      
      try
      {
        DataReader reader=new DataReader(resultType,null);
        Parser parser=new Parser(input,reader);
        parser.parse();
        return (Ttarget) reader.getValue();
      }
      catch (ParseException x)
      { throw new AccessException(x);
      }
    }

    @Override
    protected boolean store(
      Ttarget val)
      throws AccessException
    {
      // TODO Auto-generated method stub
      return false;
    }
  }
  
}
