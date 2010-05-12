package spiralcraft.json;

import java.io.IOException;
import java.io.StringWriter;

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
    
    @SuppressWarnings("unchecked")
    public ToJsonChannel(Channel<Tsource> source)
      throws BindException
    { 
      super(BeanReflector.<String>getInstance(String.class));
      this.source=source;
      if (source.getReflector() instanceof DataReflector)
      { type=((DataReflector) source.getReflector()).getType();
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
      
      dataEncodable=type.isDataEncodable();
    }

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
        
          DataComposite data=null;
          if (dataTyped)
          { 
            data=(DataComposite) val;
          }
          else if (dataEncodable)
          {
            data=type.toData(val);
          }
          
          if (data!=null)
          {
            writer.writeToWriter(jwriter,data);
            if (debug)
            { log.fine("JSON="+jwriter.toString());
            }
            return jwriter.toString();
          }
          else if (!dataEncodable)
          { return JsonWriter.escape(type.toString(val));
          }
          else
          { return null;
          }
        }
        else
        { return null;
        }
      }
      catch (DataException x)
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
