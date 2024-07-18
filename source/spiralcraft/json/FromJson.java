package spiralcraft.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;

import spiralcraft.common.callable.BinaryFunction;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.ChannelFactory;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.parser.Struct;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.SourcedChannel;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.log.Level;
import spiralcraft.text.ParseException;
import spiralcraft.util.refpool.URIPool;
import spiralcraft.util.string.DateToString;
import spiralcraft.util.string.StringConverter;

public class FromJson<Ttarget,Tsource>
  implements ChannelFactory<Ttarget,Tsource>
{

  static enum InputType
  { BYTEARRAY
    ,STRING
    ,BINARY_STREAM
  }
  
  private Reflector<Ttarget> resultType;
  private boolean debug;
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
  
  private InputOptions inputOptions = new InputOptions();
  
  public InputOptions getInputOptions()
  { return inputOptions;
  }
  
  public void setInputOptions(InputOptions p)
  { this.inputOptions=p;
  }
  
  public FromJson(Class<Ttarget> clazz)
  { this(BeanReflector.<Ttarget>getInstance(clazz));
  }
  
  public FromJson(Reflector<Ttarget> resultType)
  { this.resultType=resultType;
  }

  @SuppressWarnings("unchecked")
  public FromJson(Struct struct)
  {
    resultType=(Reflector<Ttarget>) struct.getReflector();
  }

  /**
   * Log information as types are mapped and data is read
   * 
   * @param debug
   */
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  public void setIgnoreUnrecognizedFields(boolean ignoreUnrecognizedFields)
  { this.inputOptions.ignoreUnrecognizedFields=ignoreUnrecognizedFields;
  }
  
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public BinaryFunction<byte[],Ttarget,Ttarget,ParseException> getBinaryFn()
    throws BindException
  { return (BinaryFunction) getFn((Reflector<Tsource>) BeanReflector.getInstance(byte[].class));
  }
    
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public BinaryFunction<String,Ttarget,Ttarget,ParseException> getStringFn()
    throws BindException
  { return (BinaryFunction) getFn((Reflector<Tsource>) BeanReflector.getInstance(String.class));
  }
    
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public BinaryFunction<InputStream,Ttarget,Ttarget,ParseException> getInputStreamFn()
    throws BindException
  { return (BinaryFunction) getFn((Reflector<Tsource>) BeanReflector.getInstance(InputStream.class));
  }

  private BinaryFunction<Tsource,Ttarget,Ttarget,ParseException> getFn(final Reflector<Tsource> inputR)
    throws BindException
  {
    return new BinaryFunction<Tsource,Ttarget,Ttarget,ParseException>()
    {
      final ThreadLocalChannel<Tsource> inputChannel
        =new ThreadLocalChannel<Tsource>
          (inputR);
      final ThreadLocalChannel<Ttarget> constructor
        =new ThreadLocalChannel<Ttarget>(resultType);
      
      @SuppressWarnings("unchecked")
      final Channel<Ttarget> outputChannel
        =new FromJsonChannel((Channel<Tsource>) inputChannel,constructor);   
      
      @Override
      public Ttarget evaluate(Tsource input,Ttarget root)
        throws ParseException
      { 
        try
        { 
          inputChannel.push(input);
          constructor.push(root);
          return outputChannel.get();
        }
        finally
        { 
          constructor.pop();
          inputChannel.pop();
        }
      }
    };
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Channel<Ttarget> bindChannel(
    Channel<Tsource> source,
    Focus<?> focus,
    Expression<?>[] arguments)
    throws BindException
  { 
    @SuppressWarnings("rawtypes")
    Channel constructor
      =arguments!=null && arguments.length>0
      ?focus.bind(arguments[0])
      :null;
    return new FromJsonChannel(source,constructor);
  }

  
  class FromJsonChannel
    extends SourcedChannel<Tsource,Ttarget>
  {

    private InputType inputType;
    private Channel<Ttarget> constructor;
    
    FromJsonChannel(Channel<Tsource> source,Channel<Ttarget> constructor)
      throws BindException
    { 
      super(resultType,source);
      this.constructor=constructor;
      if (source.getContentType()==byte[].class)
      { inputType=InputType.BYTEARRAY;
      }
      else if (source.getContentType()==String.class)
      { inputType=InputType.STRING;
      }
      else if (source.getContentType()==InputStream.class)
      { inputType=InputType.BINARY_STREAM;
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
      Reader input=null;
      try
      {
        switch (inputType)
        {
          case STRING:
            input=new StringReader((String) source.get());
            break;
          case BYTEARRAY:
            input=new InputStreamReader(new ByteArrayInputStream((byte[]) source.get()));
            break;
          case BINARY_STREAM:
            input=new InputStreamReader((InputStream) source.get());
            break;
          default:
            throw new AccessException("Unrecognized input type "+inputType);
        }
        
        try
        {
          DataReader reader
            =new DataReader
              (resultType
              ,constructor!=null?constructor.get():null
              );
          reader.setSerializerMap(serializerMap);
          reader.setInputOptions(inputOptions);
          reader.setDebug(FromJson.this.debug);
          Parser parser=new Parser(input,reader);
          parser.parse();
          return (Ttarget) reader.getValue();
        }
        catch (ParseException x)
        { throw new AccessException(new JsonException("Error parsing input",x));
        }
      }
      finally
      {
        if (input!=null)
        { 
          try
          { input.close();
          }
          catch (IOException x)
          { log.log(Level.INFO,"Closing JSON stream threw exception",x); 
          }
        }
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
