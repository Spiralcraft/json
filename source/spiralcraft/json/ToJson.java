package spiralcraft.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import spiralcraft.common.ContextualException;
import spiralcraft.common.callable.BinaryFunction;
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
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.util.refpool.URIPool;
import spiralcraft.util.string.DateToString;
import spiralcraft.util.string.StringConverter;


public class ToJson<Tsource>
  implements ChannelFactory<String,Tsource>
{

  @SuppressWarnings("unused")
  private static final ClassLog log
    =ClassLog.getInstance(ToJson.class);
  
  private boolean pojoMode;
  private boolean debug;
  private boolean addFormat=true;
  private HashMap<URI,StringConverter<?>> serializerMap
    =new HashMap<>();
  { 
    serializerMap.put
      (BeanReflector.getInstance(Date.class).getTypeURI()
      ,new DateToString("yyyy-MM-dd'T'HH:mm:ssXXX")
      );
    serializerMap.put
      (URIPool.create("class:/spiralcraft/data/types/standard/Date")
      ,new DateToString("yyyy-MM-dd'T'HH:mm:ssXXX")
      );
  }
  
  private HashSet<URI> excludes=new HashSet<>();
  { excludes.add(URIPool.create("class:/spiralcraft/data/types/standard/Class"));
  }
  
  private Reflector<Tsource> inputR;
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  public void setAddFormat(boolean addFormat)
  { this.addFormat=addFormat;
  }
  
  
  public ToJson()
  {
  }
  
  public ToJson(Class<Tsource> clazz)
  { 
    this(BeanReflector.<Tsource>getInstance(clazz));
    this.pojoMode=true;
  }
  
  public ToJson(Reflector<Tsource> inputR)
  { this.inputR=inputR;
  }
  
  public BinaryFunction<OutputStream,Tsource,Integer,IOException> getOutputStreamFn()
    throws BindException
  {
    return new BinaryFunction<OutputStream,Tsource,Integer,IOException>()
        {
          final ThreadLocalChannel<Tsource> inputChannel
            =new ThreadLocalChannel<Tsource>
              (inputR);
          
          final Channel<String> output = new ToJsonChannel(inputChannel);

          @Override
          public Integer evaluate(
            OutputStream out,
            Tsource sourceVal)
            throws IOException
          { 
            inputChannel.push(sourceVal);
            String result;
            try
            { 
              result=output.get();
              byte[] outB=result.getBytes(StandardCharsets.UTF_8);
              out.write(outB);
              return outB.length;
              
            }
            finally
            { inputChannel.pop();
            }
            
          }
        };
  }
  
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
      
      if (ToJson.this.debug)
      { log.fine("ToJson: reflector="+reflector);
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
        writer.setAddFormat(addFormat);
        writer.setSerializerMap(serializerMap);
        writer.setExcludes(excludes);
        
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
