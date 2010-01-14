//
// Copyright (c) 2009 Michael Toth
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

import spiralcraft.text.ParseException;
import spiralcraft.text.ParsePosition;

/**
 * Handles JSON structural elements
 * 
 * @author mike
 *
 */
public interface ContentHandler
{

  void setLocator(ParsePosition position);
  
  
  /**
   * <p>Called when a json-object value is about to be read.
   * </p>
   * 
   * <p>Name will be null when reading the top level object or when
   *   reading a value inside an array
   * </p>
   * 
   * @param name
   * @param type
   * @throws ParseException
   */
  void openObject(String name)
    throws ParseException;
  
  /**
   * Called after a json-object has been fully read. Always called exactly 
   *   as as many times as openObject()
   * 
   * 
   * @param name
   * @throws ParseException
   */
  void closeObject(String name)
    throws ParseException;
  
  /**
   * <p>Called when a json-array value is about to be read.
   * </p>
   * 
   * <p>Name will be null when reading the top level array or when
   *   reading a value inside an array
   * </p>
   * 
   * @param name
   * @param type
   * @throws ParseException
   */
  void openArray(String name)
    throws ParseException;
  
  /**
   * Called after a json-object has been fully read. Always called exactly 
   *   as as many times as openObject()
   * 
   * 
   * @param name
   * @throws ParseException
   */
  void closeArray(String name)
    throws ParseException;
    
  
  /**
   * <p>Called when a null value is read for an object member.
   * </p>
   * 
   * <p>Name will be null when reading a value inside an array
   * </p>
   * 
   * @param name
   * @param type
   * @param primitiveData
   */
  void handleNull(String name)
    throws ParseException;
  
  /**
   * <p>Called when a boolean value is read.
   * </p>
   * 
   * <p>Name will be null when reading a value inside an array
   * </p>
   * 
   * @param name
   * @param type
   * @param primitiveData
   */
  void handleBoolean(String name,boolean value)
    throws ParseException;  
  
  /**
   * <p>Called when a numeric value is read.
   * </p>
   * 
   * <p>Name will be null when reading a value inside an array
   * </p>
   * 
   * @param name
   * @param type
   * @param primitiveData
   */
  void handleNumber(String name,Number value)
    throws ParseException;  
  
  /**
   * <p>Called when a String value is read.
   * </p>
   * 
   * <p>Name will be null when reading a value inside an array
   * </p>
   * 
   * @param name
   * @param type
   * @param primitiveData
   */
  void handleString(String name,String value)
    throws ParseException;  
  
  

  
}
