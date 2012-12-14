//
// Copyright (c) 1998,2007 Michael Toth
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
package spiralcraft.json;


import spiralcraft.common.ContextualException;
import spiralcraft.data.DeltaTuple;
import spiralcraft.data.Tuple;
import spiralcraft.data.Aggregate;
import spiralcraft.data.Field;
import spiralcraft.data.Type;
import spiralcraft.data.DataException;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.lang.PrimitiveReflector;

import spiralcraft.text.ParseException;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.EmptyIterator;
import spiralcraft.util.string.StringUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.IterationCursor;
import spiralcraft.lang.IterationDecorator;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.Signature;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.parser.Struct;
import spiralcraft.lang.spi.SimpleChannel;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.URI;

import java.util.Iterator;
//import java.util.HashMap;

public class DataWriter
{ 
    
  protected static final ClassLog log
    =ClassLog.getInstance(DataWriter.class);
  protected static final Level debugLevel
    =ClassLog.getInitialDebugLevel(DataWriter.class, null);
  
  public <T> void writeToURI
    (URI resourceUri
    ,Channel<T> source
    )
    throws IOException,ContextualException
  {
    writeToResource
      (Resolver.getInstance().resolve(resourceUri)
      ,source
      );
  }

  public <T> void writeToResource
    (Resource resource
    ,Channel<T> source
    )
    throws IOException,ContextualException
  {
    OutputStream out=resource.getOutputStream();
    writeToOutputStream(out,source);
    if (out!=null)
    {
      out.flush();
      out.close();
    }
  }
  
  public <T> void writeToWriter
    (java.io.Writer writer
    ,Channel<T> source
    )
    throws IOException,ContextualException
  { 
    try
    { new Context(writer).write(source);
    }
    catch (ParseException x)
    { throw new DataException("Error writing data "+x,x);
    }
  }
  
  public <T> void writeToWriter
    (java.io.Writer writer
    ,Reflector<T> reflector
    ,T data
    )
    throws IOException,ContextualException
  { 
    try
    { new Context(writer).write(reflector,data);
    }
    catch (ParseException x)
    { throw new DataException("Error writing data "+x,x);
    }
  }
  
  public <T> void writeToOutputStream
    (OutputStream out
    ,Channel<T> source
    )
    throws ContextualException
  {
    try
    { new Context(out).write(source);
    }
    catch (ParseException x)
    { throw new DataException("Error writing data "+x,x);
    }
    
  }
    
  
  
}

@SuppressWarnings("unchecked") // Mostly runtime type resolution
class Context
{
  protected static final ClassLog log
    =ClassLog.getInstance(DataWriter.class);
  protected static final Level logLevel
    =ClassLog.getInitialDebugLevel(DataWriter.class, Level.INFO);


//  private static final URI STANDARD_NAMESPACE_URI
//    =URI.create("class:/spiralcraft/data/types/standard/");

  private final JsonWriter writer;
  private Frame currentFrame; 
  
  public Context(OutputStream out)
  { 
    try
    { writer=new JsonWriter(new OutputStreamWriter(out,"UTF-8"));
    }
    catch (UnsupportedEncodingException x)
    { throw new RuntimeException(x);
    }
  }
  
  public Context(Writer writer)
  {
    this.writer=new JsonWriter(writer);
  }
  
  public <T> void write(Channel<T> source)
    throws ParseException,ContextualException
  {
    T data=source.get();
    if (data==null)
    { return;
    }

    currentFrame=createFrame(source.getReflector(),data,null);

    //writer.startDocument();
        
    while (currentFrame!=null)
    { currentFrame.next();
    }
    //writer.endDocument();
  }
  
  public <T> void write(Reflector<T> reflector,T data)
    throws ParseException,ContextualException
  {
    if (data==null)
    { return;
    }

    currentFrame=createFrame(reflector,data,null);

    //writer.startDocument();
        
    while (currentFrame!=null)
    { currentFrame.next();
    }
    //writer.endDocument();
  }

  @SuppressWarnings("rawtypes")
  public <T> Frame createFrame(Reflector<T> reflector,T data,String memberName)
    throws DataException,ContextualException
  {
    if (data instanceof Tuple)
    { return new TupleFrame((Tuple) data,memberName);
    }
    else if (data instanceof Aggregate)
    { return new AggregateFrame((Aggregate<?>) data,memberName);
    }
    else if (data instanceof Struct)
    { return new ObjectFrame(((Struct) data).getReflector(),data,memberName);
    }
    else if (data.getClass().isArray() 
             || data instanceof Iterable
            )
    { 
      try
      {
        return new ArrayFrame
            (reflector
            ,new SimpleChannel<T>(reflector,data,true)
              .<IterationDecorator>decorate(IterationDecorator.class)
            ,memberName
            );
      }
      catch (BindException x)
      { throw new DataException("Error converting "+data+" to data",x);
      }
    }
    else if (reflector instanceof PrimitiveReflector)
    { 
      return new PrimitiveFrame
        ( ((PrimitiveReflector) reflector).getType(),memberName,data,false);
    }
    else if (reflector.getStringConverter()!=null
             || reflector.getContentType()==String.class
             )
    { return new ValueFrame(reflector,memberName,data,false);
    }
    else
    { return new ObjectFrame(reflector,data,memberName);
    }
      
    
    
  }
  
  /**
   * Holder for an element of the data tree
   */
  abstract class Frame
  { 
//    protected String qName;
    protected final Frame parentFrame;
    protected final String indentString;

    public abstract void next()
      throws ParseException,ContextualException;
    
    public String getNamespace(URI uri)
    { 
      if (parentFrame!=null)
      { return parentFrame.getNamespace(uri);
      }
      else
      { return null;
      }
    }

    public boolean isNamespaceUsed(String namespace)
    { 
      if (parentFrame!=null)
      { return parentFrame.isNamespaceUsed(namespace);
      }
      else
      { return false;
      }
    }
      
    public Frame()
    { 
      parentFrame=currentFrame;
      if (parentFrame!=null)
      { indentString=parentFrame.getIndentString().concat("  ");
      }
      else
      { indentString="";
      }
    }
    
    public String getIndentString()
    { return indentString;
    }

    protected final void finish()
    { currentFrame=parentFrame;
    }
    
    @SuppressWarnings("rawtypes")
    protected void writeValue(String name,Type type,Object value)
      throws ParseException
    { 
      if (value!=null)
      { 
        if (value instanceof Boolean)
        { writer.handleBoolean(name,(Boolean) value);
        }
        else if (value instanceof Number)
        { writer.handleNumber(name,(Number) value);
        } 
        else
        { writer.handleString(name,type.toString(value));
        }
      }
    }
    
    @SuppressWarnings("rawtypes")
    protected void writeValue(String name,Reflector reflector,Object value)
      throws ParseException
    { 
      if (value!=null)
      { 
        if (value instanceof Boolean)
        { writer.handleBoolean(name,(Boolean) value);
        }
        else if (value instanceof Number)
        { writer.handleNumber(name,(Number) value);
        } 
        else
        { 
          if (reflector.getStringConverter()!=null)
          { writer.handleString(name,reflector.getStringConverter().toString(value));
          }
          else
          { writer.handleString(name,value.toString());
          }
        }
      }
    }    
    
//    protected void writeWhitespace(String str)
//      throws ParseException
//    { 
//      if (str!=null)
//      { writer.ignorableWhitespace(str.toCharArray(),0,str.length());
//      }
//    }
    
    
  }
  
  abstract class GenericFrame<T>
    extends Frame
  {
    protected final String memberName;
    protected final Reflector<T> reflector;

    public GenericFrame(Reflector<T> reflector,String memberName)
    { 
      this.reflector=reflector;
      this.memberName=memberName;
    }
    
    protected final void startType()
      throws ParseException
    {
//      writeWhitespace("\r\n");
//      writeWhitespace(indentString);
      
      
      // writer.openObject(memberName);
    }
    
    
    protected final void endType(boolean addLine)
      throws ParseException
    {
//      if (addLine)
//      {
//        writeWhitespace("\r\n");
//        writeWhitespace(indentString);
//      }
      
// Do nothing b/c no extra element required in json
      // writer.closeObject(memberName);
    }    
    
    
  }

  @SuppressWarnings("rawtypes")
  abstract class TypeFrame
    extends GenericFrame
  {
    protected final Type type;
    protected final String typeName;
    protected final URI typeNamespace;
//    private final HashMap<URI,String> namespaceMap
//      =new HashMap<URI,String>();
//    private final HashMap<String,URI> reverseNamespaceMap
//      =new HashMap<String,URI>();
    
    public TypeFrame(Type type,String memberName)
      throws BindException
    {
      super(DataReflector.getInstance(type),memberName);

      this.type=type;
      URI typeUri=type.getURI();
      String[] path=StringUtil.tokenize(typeUri.getPath(),"/");
      
      typeName=path[path.length-1];  

      typeNamespace=typeUri.resolve(".");

      // (make namespaces work)
//      String namespace=getNamespace(typeNamespace);
//      if (namespace==null)
//      { namespace=makeNamespace(typeNamespace);
//      }
//      if (namespace!=null)
//      { memberName=namespace+"."+typeName;
//      }
//      else
//      { memberName=typeName;
//      }
    }

//    private String makeNamespace(URI uri)
//    {
//      String namespace=null;
//      if (uri.equals(STANDARD_NAMESPACE_URI))
//      { return null;
//      }
//      
//      String[] path=StringUtil.tokenize(uri.getPath(),"/");
//      if (path.length>0)
//      { namespace=path[path.length-1];
//      }
//      else
//      { namespace="local";
//      }
//      
//      String usedNamespace=namespace;        
//      int disambiguator=2;
//      while (isNamespaceUsed(namespace))
//      { namespace=usedNamespace.concat(Integer.toString(disambiguator++));
//      }
//      namespaceMap.put(uri,namespace);
//      reverseNamespaceMap.put(namespace,uri);
//      if (attributes==NULL_ATTRIBUTES)
//      { attributes=new AttributesImpl();
//      }
//      attributes.addAttribute
//        (null
//        ,namespace
//        ,"xmlns:"+namespace
//        ,null
//        ,uri.toString()
//        );
//      return namespace;   
//    }

//    @Override
//    public boolean isNamespaceUsed(String namespace)
//    { 
//      if (reverseNamespaceMap.get(namespace)!=null)
//      { return true;
//      }
//      else
//      { return super.isNamespaceUsed(namespace);
//      }
//    }
//    
//    @Override
//    public String getNamespace(URI uri)
//    {
//      String namespace=namespaceMap.get(uri);
//      if (namespace==null)
//      { namespace=super.getNamespace(uri);
//      }
//      return namespace;    
//        
//    }
    

    
    
  }

  class ValueFrame<T>
    extends GenericFrame<T>
  {
    private final T value;
    private final boolean singleContext;
    
    public ValueFrame
      (Reflector<T> reflector
      ,String memberName
      ,T value
      ,boolean singleContext
      )
      throws BindException
    {
      super(reflector,memberName);
      this.value=value;
      this.singleContext=singleContext; 
    }
    
    @Override
    public void next()
      throws ParseException
    {
      if (!singleContext)
      { startType();
      }
      
      try
      {  
        writeValue(memberName,reflector,value);
        if (logLevel.isFine())
        { log.fine("Wrote "+memberName+"="+value);
        }
      }
      catch (IllegalArgumentException x)
      { 
        throw new ParseException
          ("Error writing value ["+value+"] for "+reflector.getTypeURI()
          ,writer.getLocator()
          ,x);
      }
      
      if (!singleContext)
      { endType(false);
      }
      finish();
      return;
    }
  }
  
  class PrimitiveFrame
    extends TypeFrame
  {
    private final Object value;
    private final boolean singleContext;
    
    public PrimitiveFrame
      (Type<?> type
      ,String memberName
      ,Object value
      ,boolean singleContext
      )
      throws BindException
    {
      super(type,memberName);
      this.value=value;
      this.singleContext=singleContext; 
    }
    
    @Override
    public void next()
      throws ParseException
    {
      if (!singleContext)
      { startType();
      }
      
      try
      {  
        writeValue(memberName,type,value);
      }
      catch (IllegalArgumentException x)
      { 
        throw new ParseException
          ("Error writing value ["+value+"] for "+type.getURI()
          ,writer.getLocator()
          ,x);
      }
      
      if (!singleContext)
      { endType(false);
      }
      
      finish();
      return;
    }
  }
  

  @SuppressWarnings("rawtypes")
  class TupleFrame
    extends TypeFrame
  {
    private final Tuple tuple;
    private Iterator<? extends Field> fieldIterator;
    private boolean empty=true;
    
    public TupleFrame(Tuple tuple,String memberName)
      throws BindException
    { 
      super(tuple.getType(),memberName);
      this.tuple=tuple;
    }
    
    
    @Override
    public void next()
      throws ParseException,DataException
    {
      if (fieldIterator==null)
      { 
        startType();
        writer.openObject(memberName);
        if (tuple instanceof DeltaTuple)
        { 
          if (logLevel.isFine())
          { log.fine("Writing DeltaTuple "+tuple.getType().getURI());
          }
          Field[] dirtyFields=((DeltaTuple) tuple).getDirtyFields();
          if (dirtyFields!=null)
          {
            fieldIterator
              =ArrayUtil.iterator(((DeltaTuple) tuple).getDirtyFields());
          }
          else 
          { fieldIterator=new EmptyIterator<Field>();
          }
        }
        else
        { 
          if (tuple.getType()!=null)
          { 
            if (logLevel.isFine())
            { log.fine("Writing Tuple "+tuple.getType().getURI());
            }
            // Make sure we include base type Fields
            fieldIterator
              =tuple.getType().getFieldSet().fieldIterable().iterator();
          }
          else
          { 
            if (logLevel.isFine())
            { log.fine("Writing untyped Tuple "+tuple.getFieldSet());
            }
            log.fine("Writing untyped tuple "+tuple.getFieldSet());
            fieldIterator=tuple.getFieldSet().fieldIterable().iterator();
          }
        }
      }
      else if (fieldIterator.hasNext())
      {
        
        Field field=fieldIterator.next();
        if (logLevel.isFine())
        { log.fine("Starting field "+field.getURI());
        }
        if (field.getValue(tuple)!=null)
        {
          empty=false;
          if (field.getType().isAggregate())
          { currentFrame=new AggregateFieldFrame(tuple,field);
          }
          else
          { currentFrame=new SimpleFieldFrame(tuple,field);
          }
        }
      }
      else
      {
        endType(!empty);
        writer.closeObject(memberName);
        finish();
        return;
      }
    }
    
  }
  

  class AggregateFrame
    extends TypeFrame
  {
    private final Aggregate<?> aggregate;
    private Iterator<?> aggregateIterator;
//    private int index;
  
    public AggregateFrame(Aggregate<?> aggregate,String memberName)
      throws ContextualException
    { 
      super(aggregate.getType(),memberName);
      this.aggregate=aggregate;
    }
  
  
    @Override
    public void next()
      throws ParseException,ContextualException
    {
      if (aggregateIterator==null)
      { 
        startType();
        writer.openArray(memberName);
        aggregateIterator=aggregate.iterator();
//        index=0;
      }
      else if (aggregateIterator.hasNext())
      {
        Object object=aggregateIterator.next();
        currentFrame
          =createFrame
            (DataReflector.getInstance(aggregate.getType().getContentType())
            ,object
            ,null
            );
//        index++;
      }
      else
      {
        endType(true);
        writer.closeArray(memberName);
        finish();
        return;
      }
    }
  
  }

  @SuppressWarnings("rawtypes")
  class ObjectFrame<T>
    extends GenericFrame<T>
  {
    private final Channel<T> data;
    private boolean empty=true;
    private Iterator<Signature> fieldIterator;
    
    public ObjectFrame(Reflector reflector,T data,String memberName)
      throws ContextualException
    { 
      super(reflector,memberName);
      this.data=new SimpleChannel<T>(reflector,data,true);
    }
    
    
    @Override
    public void next()
      throws ParseException,ContextualException
    {
      if (fieldIterator==null)
      { 
        startType();
        writer.openObject(memberName);
        fieldIterator
          =reflector.getProperties(data)
            .iterator();
        if (!fieldIterator.hasNext())
        { log.fine("NO PROPS: "+reflector+" : "+data.get());
        }
        
      }
      else if (fieldIterator.hasNext())
      {
        Signature sig=fieldIterator.next();
        if (logLevel.isFine())
        { log.fine("Starting property "+reflector.getTypeURI()+"#"+sig.getName());
        }
        
        Channel propChannel
          =data.resolve(new SimpleFocus(null),sig.getName(),null);
        if (propChannel.getContentType()==Class.class)
        { return;
        }
        Object fieldVal=propChannel.get();
        if (fieldVal!=null)
        {
          empty=false;
          currentFrame
            =createFrame(propChannel.getReflector(),fieldVal,sig.getName());
        }
      }
      else
      {
        endType(!empty);
        writer.closeObject(memberName);
        finish();
        return;
      }
    }
    
  }  
  
  class ArrayFrame<A,T>
    extends GenericFrame<A>
  {
    private final IterationDecorator<A,T> deco;
    private IterationCursor<T> iteration;
    
//    private int index;
  
    public ArrayFrame(Reflector<A> reflector,IterationDecorator<A,T> deco,String memberName)
    { 
      super(reflector,memberName);
      this.deco=deco;
    }
  
  
    @Override
    public void next()
      throws ParseException,ContextualException
    {
      if (iteration==null)
      { 
        startType();
        iteration=deco.iterator();
        writer.openArray(memberName);
//        index=0;
      }
      else if (iteration.hasNext())
      {
        T object=iteration.next();
        currentFrame=createFrame(deco.getComponentReflector(),object,null);
//        index++;
      }
      else
      {
        endType(true);
        writer.closeArray(memberName);
        finish();
        return;
      }
    }
  
  }
  

  @SuppressWarnings("rawtypes")
  abstract class FieldFrame
    extends Frame
  {
    protected final Field field;
    protected final Tuple tuple;
    
    protected boolean opened;

    public FieldFrame(Tuple tuple,Field field)
    {
      this.field=field;
      this.tuple=tuple;
    }
    
    protected final void openField()
      throws ParseException
    {
//      writeWhitespace("\r\n");
//      writeWhitespace(indentString);
//      writer.startElement(null,null,field.getName(),NULL_ATTRIBUTES);
    }
    
    protected final void closeField(boolean addLine)
      throws ParseException
    { 
      if (addLine)
      {
//        writeWhitespace("\r\n");
//        writeWhitespace(indentString);
      }
//      writer.endElement(null,null,field.getName());
    }

    
  }

  @SuppressWarnings("rawtypes")
  class AggregateFieldFrame
    extends FieldFrame
  {
    
    private Iterator<?> iterator;
    private Type componentType;
    private boolean hasOne;
//    private int index;
    
    public AggregateFieldFrame(Tuple tuple,Field field)
    { 
      super(tuple,field);
      componentType=field.getType().getContentType();
    }
    
    
    @Override
    public void next()
      throws ParseException,ContextualException
    {
      Aggregate value=(Aggregate) field.getValue(tuple);
      if (value==null)
      { 
        finish();
        return;
      }
      
      if (!opened)
      {
        opened=true;
        openField();
        writer.openArray(field.getName());
        iterator=value.iterator();
//        index=0;
      }
      else if (iterator.hasNext())
      {
        hasOne=true;
        Object item=iterator.next();
        currentFrame
          =createFrame
            (DataReflector.getInstance(componentType),item,null);
//        System.out.println("Aggregate Field: iterating "+item);
//        index++;
      }
      else
      {
        writer.closeArray(field.getName());
        closeField(hasOne);
        finish();
        return;
      }
    }
  }
  

  @SuppressWarnings("rawtypes")
  class SimpleFieldFrame
    extends FieldFrame
  {
    
    private boolean primitive;
    
    public SimpleFieldFrame(Tuple tuple,Field field)
    { super(tuple,field);
    }
    
    @Override
    public void next()
      throws ParseException,ContextualException
    {
      Object value=field.getValue(tuple);
      if (value==null)
      { 
        finish();
        return;
      }
      
      if (!opened)
      {
        opened=true;
        openField();

        currentFrame
          =createFrame
            (DataReflector.getInstance(field.getType()),value,field.getName());
      }
      else
      {
        closeField(!primitive);
        finish();
        return;
      }
    }
  }
  
}

