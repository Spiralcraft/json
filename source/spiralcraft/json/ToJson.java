package spiralcraft.json;

import java.io.IOException;
import java.io.StringWriter;

import spiralcraft.common.ContextualException;
import spiralcraft.data.DataComposite;
import spiralcraft.data.DataException;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.reflect.ReflectionType;

import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.ChannelFactory;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.parser.StructNode.StructReflector;
import spiralcraft.lang.reflect.ArrayReflector;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.log.ClassLog;


public class ToJson<Tsource>
  implements ChannelFactory<String,Tsource>
{

  private static final ClassLog log
    =ClassLog.getInstance(ToJson.class);
  
  @Override
  public Channel<String> bindChannel
    (Channel<Tsource> source
    ,Focus<?> focus
    ,Expression<?>[] params
    )
    throws BindException
  { return new ToJsonChannel(source);
  }

  class ToJsonChannel
    extends AbstractChannel<String>
  {
    private spiralcraft.data.Type<Tsource> type;    
    private Channel<Tsource> source;
    private final boolean dataTyped;
    private final boolean dataEncodable;
    private final boolean renderAsIs;
    private Reflector<Tsource> reflector;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ToJsonChannel(Channel<Tsource> source)
      throws BindException
    { 
      super(BeanReflector.<String>getInstance(String.class));
      this.source=source;
      this.reflector=source.getReflector();
      
      if (reflector instanceof DataReflector)
      { type=((DataReflector) reflector).getType();
      }
      else
      { 
        try
        { type=ReflectionType.<Tsource>canonicalType(source.getContentType());
        }
        catch (DataException x)
        { throw new BindException("No data conversion for "+this.source,x);
        }
      }
    
      dataTyped
        =DataComposite.class.isAssignableFrom(source.getContentType());
      
      renderAsIs=reflector instanceof StructReflector
        || ( reflector instanceof ArrayReflector
              && ((ArrayReflector) reflector)
                   .getRootComponentReflector() instanceof StructReflector
           );
      
      dataEncodable=type.isDataEncodable();
      if (dataEncodable)
      { reflector=DataReflector.getInstance(type);
      }
      
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String retrieve()
    {
      try
      {
        java.io.Writer jwriter=new StringWriter();
        DataWriter writer=new DataWriter();
      
        Tsource val=source.get();
        if (val!=null)
        {
        
          Tsource data=null;
          if (dataTyped || renderAsIs)
          { data=val;
          }
          else if (dataEncodable)
          { data=(Tsource) type.toData(val);
          }
          
          if (data!=null)
          { 
            writer.writeToWriter(jwriter,reflector,data);
            if (debug)
            { log.fine("JSON="+jwriter.toString());
            }
            return jwriter.toString();
          }
          else if (!dataEncodable)
          { 
            writer.writeToWriter(jwriter,reflector,val);
            return jwriter.toString();
          }
          else
          { return null;
          }
        }
        else
        { return null;
        }
      }
      catch (ContextualException x)
      { throw new AccessException("Error writing json text",x);
      }
      catch (IOException x)
      { throw new AccessException("Error writing json text",x);
      }
    }

    @Override
    protected boolean store(String val) throws AccessException
    { 
      throw new UnsupportedOperationException
        ("Reverse ToJson operation not supported (yet)");

    }
  }
  

}
