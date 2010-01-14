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


import spiralcraft.data.DataComposite;
import spiralcraft.data.DeltaTuple;
import spiralcraft.data.Tuple;
import spiralcraft.data.Aggregate;
import spiralcraft.data.Field;
import spiralcraft.data.Type;
import spiralcraft.data.DataException;

import spiralcraft.text.ParseException;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.EmptyIterator;
import spiralcraft.util.string.StringUtil;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;

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
  
  public void writeToURI
    (URI resourceUri
    ,DataComposite data
    )
    throws IOException,DataException
  {
    writeToResource
      (Resolver.getInstance().resolve(resourceUri)
      ,data
      );
  }

  public void writeToResource
    (Resource resource
    ,DataComposite data
    )
    throws IOException,DataException
  {
    OutputStream out=resource.getOutputStream();
    writeToOutputStream(out,data);
    if (out!=null)
    {
      out.flush();
      out.close();
    }
  }
  
  public void writeToWriter(java.io.Writer writer,DataComposite data)
    throws IOException,DataException
  { 
    try
    { new Context(writer).write(data);
    }
    catch (ParseException x)
    { throw new DataException("Error writing data "+x,x);
    }
  }
  
  public void writeToOutputStream
    (OutputStream out
    ,DataComposite data
    )
    throws DataException
  {
    try
    { new Context(out).write(data);
    }
    catch (ParseException x)
    { throw new DataException("Error writing data "+x,x);
    }
    
  }
    
  
  
}

@SuppressWarnings("unchecked") // Mostly runtime type resolution
class Context
{

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
  
  public void write(DataComposite data)
    throws ParseException,DataException
  {
    if (data==null)
    { 
      throw new IllegalArgumentException
        ("Cannot write a null DataComposite object");
    }
    //writer.startDocument();
    if (data.isTuple())
    { currentFrame=new TupleFrame(data.asTuple(),null);
    }
    else
    { currentFrame=new AggregateFrame(data.asAggregate(),null);
    }
    while (currentFrame!=null)
    { currentFrame.next();
    }
    //writer.endDocument();
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
      throws ParseException,DataException;
    
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
    
//    protected void writeWhitespace(String str)
//      throws ParseException
//    { 
//      if (str!=null)
//      { writer.ignorableWhitespace(str.toCharArray(),0,str.length());
//      }
//    }
    
    protected void pushCompositeFrame(DataComposite data,String memberName)
    {
      if (data.isTuple())
      { currentFrame=new TupleFrame(data.asTuple(),memberName);
      }
      else
      { currentFrame=new AggregateFrame(data.asAggregate(),memberName);
      }
    }
    
  }

  abstract class TypeFrame
    extends Frame
  {
    protected final Type type;
    protected final String typeName;
    protected final URI typeNamespace;
    protected final String memberName;
//    private final HashMap<URI,String> namespaceMap
//      =new HashMap<URI,String>();
//    private final HashMap<String,URI> reverseNamespaceMap
//      =new HashMap<String,URI>();
    
    public TypeFrame(Type type,String memberName)
    {
      this.memberName=memberName;

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
  
  class TupleFrame
    extends TypeFrame
  {
    private final Tuple tuple;
    private Iterator<? extends Field> fieldIterator;
    private boolean empty=true;
    
    public TupleFrame(Tuple tuple,String memberName)
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
          if (DataWriter.debugLevel.canLog(Level.FINE))
          { DataWriter.log.fine("Writing DeltaTuple "+tuple.getType().getURI());
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
            if (DataWriter.debugLevel.canLog(Level.FINE))
            { DataWriter.log.fine("Writing Tuple "+tuple.getType().getURI());
            }
            // Make sure we include base type Fields
            fieldIterator
              =tuple.getType().getFieldSet().fieldIterable().iterator();
          }
          else
          { 
            if (DataWriter.debugLevel.canLog(Level.FINE))
            { DataWriter.log.fine("Writing untyped Tuple "+tuple.getFieldSet());
            }
            DataWriter.log.fine("Writing untyped tuple "+tuple.getFieldSet());
            fieldIterator=tuple.getFieldSet().fieldIterable().iterator();
          }
        }
      }
      else if (fieldIterator.hasNext())
      {
        
        Field field=fieldIterator.next();
        if (DataWriter.debugLevel.canLog(Level.FINE))
        { DataWriter.log.fine("Starting field "+field.getURI());
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
    private int index;
  
    public AggregateFrame(Aggregate<?> aggregate,String memberName)
    { 
      super(aggregate.getType(),memberName);
      this.aggregate=aggregate;
    }
  
  
    @Override
    public void next()
      throws ParseException,DataException
    {
      if (aggregateIterator==null)
      { 
        startType();
        writer.openArray(memberName);
        aggregateIterator=aggregate.iterator();
        index=0;
      }
      else if (aggregateIterator.hasNext())
      {
        Object object=aggregateIterator.next();
        if (object instanceof DataComposite)
        { pushCompositeFrame((DataComposite) object,null);
        }
        else
        { 
          if (type.getContentType().isDataEncodable())
          { 
            DataComposite data
              =type.getContentType().toData(object);
            pushCompositeFrame(data,null);
          }
          else
          { currentFrame=new PrimitiveFrame(type.getContentType(),null,object,false);
          }
        }
        index++;
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

  class AggregateFieldFrame
    extends FieldFrame
  {
    
    private Iterator<?> iterator;
    private Type componentType;
    private boolean hasOne;
    private int index;
    
    public AggregateFieldFrame(Tuple tuple,Field field)
    { 
      super(tuple,field);
      componentType=field.getType().getContentType();
    }
    
    
    @Override
    public void next()
      throws ParseException,DataException
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
        index=0;
      }
      else if (iterator.hasNext())
      {
        hasOne=true;
        Object item=iterator.next();
//        System.out.println("Aggregate Field: iterating "+item);
        if (item instanceof DataComposite)
        { currentFrame=new TupleFrame(((DataComposite) item).asTuple(),field.getName());
        }
        else
        { 
          if (componentType.isDataEncodable())
          { 
            DataComposite data
              =componentType.toData(item);
            pushCompositeFrame(data,null);
          }
          else
          { currentFrame=new PrimitiveFrame(componentType,null,item,false);
          }
          
        }
        index++;
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
  
  class SimpleFieldFrame
    extends FieldFrame
  {
    
    private boolean primitive;
    
    public SimpleFieldFrame(Tuple tuple,Field field)
    { super(tuple,field);
    }
    
    @Override
    public void next()
      throws ParseException,DataException
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

        
        if (value instanceof DataComposite)
        { currentFrame=new TupleFrame((Tuple) value,field.getName());
        }
        else
        { 
          if (field.getType().isDataEncodable())
          { 
            Type ftype=field.getType();
            DataComposite data
              =ftype.toData(value);
            pushCompositeFrame(data,field.getName());
          }
          else
          { 
            primitive=true;
            currentFrame=new PrimitiveFrame(field.getType(),field.getName(),value,true);
          }
        }
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

