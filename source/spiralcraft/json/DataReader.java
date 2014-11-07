//
// Copyright (c) 2011 Michael Toth
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



import java.util.LinkedList;
import java.util.List;

import spiralcraft.common.ContextualException;
import spiralcraft.data.DataComposite;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.lang.AccessException;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.CollectionDecorator;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Reflector;
import spiralcraft.lang.SimpleFocus;
import spiralcraft.lang.spi.AbstractChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.ClassLog;
import spiralcraft.text.ParseException;
import spiralcraft.text.ParsePosition;
import spiralcraft.util.string.StringConverter;


/**
 * Reads JSON into a spiralcraft.data structure
 */
public class DataReader
  implements ContentHandler
{
  private static final ClassLog log
    =ClassLog.getInstance(DataReader.class);
  
  private Object _root;
  private Reflector<?> _rootReflector;
  
  private Frame<?> _currentFrame;
  private ParsePosition position=new ParsePosition();
  
  private boolean ignoreUnrecognizedFields;
  private boolean debug;
  
//  private boolean _started;
  private Focus<?> focus=new SimpleFocus<Void>(null);
  
  public static DataReader createReader(DataComposite root)
    throws ContextualException
  { return new DataReader(DataReflector.getInstance(root.getType()),root);
  }
  
  public static DataReader createReader(Reflector<?> reflector)
  { return new DataReader(reflector,null);
  }
  
  
  @SuppressWarnings("rawtypes")
  public DataReader(Reflector<?> rootReflector,Object root)
  { 
    this._currentFrame=new RootFrame();
    this._root=root;
    this._rootReflector=rootReflector;
  }
  
  public void setIgnoreUnrecognizedFields(boolean ignore)
  { this.ignoreUnrecognizedFields=ignore;
  }

  void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  public Object getValue()
  { return _currentFrame.value;
  }
  
  @Override
  public void openObject
    (String name)
    throws ParseException
  { _currentFrame.openObject(name);
  }

  @Override
  public void closeObject
    (String name)
    throws ParseException
  { 
    _currentFrame.closeObject(name);
    _currentFrame.pop();
  }
  
  @Override
  public void openArray
    (String name)
    throws ParseException
  { _currentFrame.openArray(name);
  }

  @Override
  public void closeArray
    (String name)
    throws ParseException
  { 
    _currentFrame.closeArray(name);
    _currentFrame.pop();
  }  
  
  @Override
  public void handleString
    (String name
    ,String value
    )
    throws ParseException
  { _currentFrame.handleString(name,value);
  }

  @Override
  public void handleNumber
    (String name
    ,Number value
    )
    throws ParseException
  { _currentFrame.handleNumber(name,value);
  }

  @Override
  public void handleBoolean
    (String name
    ,boolean value
    )
    throws ParseException
  { _currentFrame.handleBoolean(name,value);
  }

  @Override
  public void handleNull
    (String name
    )
    throws ParseException
  { _currentFrame.handleNull(name);
  }

  @Override
  public void setLocator(
    ParsePosition position)
  { this.position=position.clone();
  }
  
  protected String mapName(String name)
  {
    int i=-1;
    while ( (i=name.indexOf('_')) >=0)
    { 
      name=name.substring(0,i)
        +Character.toUpperCase(name.charAt(i+1))
        +name.substring(i+2);
    }
    return name;
  }

  class Frame<T>
  {
    final Frame<?> parent;
    T value;
    Reflector<T> reflector;
    Channel<T> channel;
    
    Frame(Frame<?> parent)
    { this.parent=parent;
    }
    
    void openObject(String name)
      throws ParseException
    { 
      throw new UnsupportedOperationException
        ("Can't accept an object: "+getClass());
    }
    
    void closeObject(String name)
      throws ParseException
    { 
      throw new UnsupportedOperationException
        ("Can't accept an object: "+getClass());
    }
    
    void openArray(String name)
      throws ParseException
    { 
      throw new UnsupportedOperationException
        ("Can't accept an array: "+getClass());
    }
    
    void closeArray(String name)
      throws ParseException
    { 
      throw new UnsupportedOperationException
        ("Can't accept an array: "+getClass());
    }
    
    void handlePrimitive(String name,Object value)
      throws ParseException
    {
    }
    
    void handleString(String name,String value)
      throws ParseException
    { handlePrimitive(name,value);
      
    }
    
    void handleNumber(String name,Number value)
      throws ParseException
    { handlePrimitive(name,value);
      
    }

    void handleBoolean(String name,boolean value)
      throws ParseException
    { handlePrimitive(name,value);
    }
    
    void handleNull(String name)
      throws ParseException
    { handlePrimitive(name,null);
    }
    
    void assertNoMember(String name)
    { 
      if (name!=null)
      { throw new UnsupportedOperationException("Member not permitted here");
      }
    }
    
    void push(Frame<?> frame)
    { _currentFrame=frame;
    }
    
    void pop()
    { _currentFrame=_currentFrame.parent;
    }
    
    void addValue(String name,Object value)
      throws ContextualException
    { throw new UnsupportedOperationException("Can't have values");
    }
    
    @SuppressWarnings("unchecked")
    void initValue(Object value)
    { this.value=(T) value;
    }
    
    Channel<T> getChannel()
    { 
      if (channel==null)
      { channel=new FrameChannel();
      }
      return channel;
    }
    
    class FrameChannel
      extends AbstractChannel<T>
    {

      FrameChannel()
      { super(reflector);
      }
      
      @Override
      protected T retrieve()
      { return value;
      }

      @Override
      protected boolean store(
        T val)
        throws AccessException
      { 
        value=val;
        return true;
      }
    }
  }
  
  class RootFrame<T>
    extends Frame<T>
  {
    RootFrame()
    { super(null);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    void openObject(String name)
    { 
      assertNoMember(name);
      push(new ObjectFrame(this,null,_rootReflector));
      _currentFrame.initValue(_root);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    void openArray(String name)
    { 
      assertNoMember(name);
      push(new ArrayFrame(this,null,_rootReflector));
      _currentFrame.initValue(_root);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    void addValue(String name,Object value)
      throws ContextualException
    { 
      assertNoMember(name);
      this.value=(T) value;
    }
  }

  class DummyFrame
    extends Frame<Void>
  {
    
    DummyFrame(Frame<?> parent)
    { super(parent);
    }
    
    @Override
    void addValue(String name,Object value)
    {
    }
    
    @Override
    void openObject(String name)
    { push(new DummyFrame(this));
    }

    @Override
    void closeObject(String name)
    { 
    }
    
    @Override
    void handlePrimitive(String name,Object value)
      throws ParseException
    {
    }
    
    @Override
    void openArray(String name)
    { push(new DummyFrame(this));
    }
    
    @Override
    void closeArray(String name)
    { 
    }
    
  }
  
  class ArrayFrame<T>
    extends Frame<T>
  {
    
    private List<Object> collection;

    @SuppressWarnings("rawtypes")
    private CollectionDecorator decorator;
    
    @SuppressWarnings("rawtypes")
    private StringConverter converter;
    private String memberName;
    
    ArrayFrame(Frame<?> parent,String memberName,Reflector<T> reflector)
    { 
      super(parent);
      this.memberName=memberName;
      this.reflector=reflector;
    }
    
    @Override
    void addValue(String name,Object value)
      throws ContextualException
    {
      assertNoMember(name);
      ensureCollection();
      collection.add(value);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    void openObject(String name)
      throws ParseException
    { 
      try
      {
        assertNoMember(name);
        ensureCollection();
        push(new ObjectFrame(this,null,decorator.getComponentReflector()));        
      }
      catch (ContextualException x)
      { throw new ParseException
          ("Error determining type for member of "
          +reflector.getTypeURI()
          ,position
          ,x
          );
      }
      
    }
    
    @Override
    void handlePrimitive(String name,Object value)
      throws ParseException
    {
      try
      { ensureCollection();
      }
      catch (BindException x)
      { throw new ParseException(position,x);
      }
      if (value==null)
      { collection.add(null);
      }
      else
      {
        if (converter!=null)
        { collection.add(converter.fromString(value.toString()));
        }
        else if (decorator.getComponentReflector().getContentType()
                  .equals(String.class)
                )
        { collection.add(value.toString());
        }
        else
        { 
          throw new ParseException
            ("Can't convert ["+value+"] ("+value.getClass().getName()+") to a "
              +decorator.getComponentReflector().getContentType()
            ,position
            );
        }
      }
    }
    
    @SuppressWarnings("unchecked")
    void ensureCollection() 
      throws BindException
    {
      
      if (decorator==null)
      { 
        decorator=getChannel().decorate(CollectionDecorator.class);
        if (decorator==null)
        { throw new BindException("Not a collection "+reflector.getTypeURI());
        }
        collection=new LinkedList<Object>();
        converter=decorator.getComponentReflector().getStringConverter();
      }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    void closeArray(String name)
      throws ParseException
    { 
      try
      { 
        ensureCollection();
        value=(T) decorator.newCollection();
        value=(T) decorator.addAll(value,collection.iterator());
        parent.addValue(memberName,value);
      }
      catch (ContextualException x)
      { throw new ParseException("Error adding value "+name+": "+value,position,x);
      }
    }
  }
  
  class ObjectFrame<T>
    extends Frame<T>
  {
    private String memberName;
    
    ObjectFrame(Frame<?> parent,String memberName,Reflector<T> reflector)
    { 
      super(parent);
      this.memberName=memberName;
      this.reflector=reflector;
    }
    
    @Override
    void addValue(String name,Object value)
      throws ContextualException
    {
      ensureValue();
      getChannel().resolve(focus,name,null).set(value);
    }
    

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    void openObject(String name)
      throws ParseException
    { 
      try
      {
        String mappedName=mapName(name);
        Channel prop=mapProperty(mappedName);
        if (prop!=null)
        { push(new ObjectFrame(this,mappedName,prop.getReflector()));        
        }
        else
        { push(new DummyFrame(this));
        }
      }
      catch (ContextualException x)
      { throw new ParseException("Error resolving property "+name,position,x);
      }
      
    }
    
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    void openArray(String name)
      throws ParseException
    { 
      try
      {
        String mappedName=mapName(name);
        Channel prop=mapProperty(mappedName);
        if (prop!=null)
        { push(new ArrayFrame(this,mappedName,prop.getReflector()));        
        }
        else
        { push(new DummyFrame(this));
        }
      }
      catch (ContextualException x)
      { throw new ParseException("Error resolving property "+name,position,x);
      }
      
    }    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    void handlePrimitive(String name,Object value)
      throws ParseException
    {
      try
      {
        String mappedName=mapName(name);
        Channel prop=mapProperty(mappedName);
        if (prop!=null)
        {
          if (value==null)
          { prop.set(null);
          }
          else
          {
            StringConverter<?> converter=prop.getReflector().getStringConverter();
            if (converter!=null)
            { prop.set(converter.fromString(value.toString()));
            }
            else 
            { throw new ParseException
                ("Can't convert "+name+": "+value+" to a "+prop.getContentType()
                ,position
                );
            }
          }
        }
      }
      catch (ContextualException x)
      { throw new ParseException("Error handling "+name+": "+value,position,x);
      }
      
    }
    
    Channel<?> mapProperty(String mappedName)
      throws ContextualException
    {
      ensureValue();
      try
      {
        Channel<?> prop=getChannel().resolve(focus,mappedName,null);
        if (debug)
        { log.fine("Mapped member '"+mappedName+"' to "+prop );
        }
        return prop;
      }
      catch (ContextualException x)
      { 
        if (!ignoreUnrecognizedFields)
        { throw x;
        }
        else
        { 
          if (debug)
          { log.fine("Ignoring member '"+mappedName+"'");
          }
        }
      }
      return null;
    }
    
    @Override
    void closeObject(String name)
      throws ParseException
    { 
      try
      { parent.addValue(memberName,value);
      }
      catch (ContextualException x)
      { throw new ParseException("Error adding value "+name+": "+value,position,x);
      }
    }
    
    void ensureValue()
      throws ContextualException
    { 
      if (value==null)
      {
        Channel<T> constructor
          =LangUtil.constructorFor(reflector,new SimpleFocus<Void>(null));
         
        if (constructor!=null)
        { value=constructor.get();
        }
      }
      
    }
    
  }
  
}
