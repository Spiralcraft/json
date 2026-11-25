//
// Copyright (c) 1998,2026 Michael Toth
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
package spiralcraft.json.test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import spiralcraft.json.DataWriter;
import spiralcraft.lang.reflect.BeanReflector;

/**
 * A bean with List and array properties of several declared item types,
 *   to check that the items are written by the class of each item.
 */
public class ListDto
{
  public static class Item
  {
    private String name;
    public Item() {}
    public static Item of(String name) { Item i=new Item(); i.name=name; return i; }
    public String getName() { return name; }
    public void setName(String name) { this.name=name; }
  }

  public static class SubItem
    extends Item
  {
    private int rank;
    public SubItem() {}
    public static SubItem of(String name,int rank)
    { SubItem i=new SubItem(); i.setName(name); i.rank=rank; return i; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank=rank; }
  }

  public enum Color { RED,GREEN }

  private List<String> names=new ArrayList<String>(Arrays.asList("a","b"));
  private List<Item> items=new ArrayList<Item>();
  private List<Object> mixed=new ArrayList<Object>();
  private Item[] itemArray=new Item[] { SubItem.of("s",3),Item.of("t") };
  private int[] ints=new int[] { 1,2 };
  private List<Object> scalars=new ArrayList<Object>();

  public ListDto()
  {
    items.add(Item.of("i1"));
    items.add(SubItem.of("i2",2));
    mixed.add("x");
    mixed.add(7);
    mixed.add(Item.of("m"));
    mixed.add(null);
    scalars.add(true);
    scalars.add('c');
    scalars.add(Color.GREEN);
  }

  public List<String> getNames() { return names; }
  public void setNames(List<String> names) { this.names=names; }
  public List<Item> getItems() { return items; }
  public void setItems(List<Item> items) { this.items=items; }
  public List<Object> getMixed() { return mixed; }
  public void setMixed(List<Object> mixed) { this.mixed=mixed; }
  public Item[] getItemArray() { return itemArray; }
  public void setItemArray(Item[] itemArray) { this.itemArray=itemArray; }
  public int[] getInts() { return ints; }
  public void setInts(int[] ints) { this.ints=ints; }
  public List<Object> getScalars() { return scalars; }
  public void setScalars(List<Object> scalars) { this.scalars=scalars; }

  private List<List<Item>> nested=new ArrayList<List<Item>>();
  private List<Item> empty=new ArrayList<Item>();
  { nested.add(new ArrayList<Item>(Arrays.asList(Item.of("n")))); }
  public List<List<Item>> getNested() { return nested; }
  public void setNested(List<List<Item>> nested) { this.nested=nested; }
  public List<Item> getEmpty() { return empty; }
  public void setEmpty(List<Item> empty) { this.empty=empty; }

  /**
   * A list as the root value, of items of different classes
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static String writeRoot()
    throws Exception
  {
    StringWriter out=new StringWriter();
    DataWriter writer=new DataWriter();
    writer.setExcludes(new HashSet<java.net.URI>());
    List list=new ArrayList();
    list.add(Item.of("r"));
    list.add(SubItem.of("q",1));
    list.add("s");
    writer.writeToWriter
      (out,BeanReflector.getInstance(ArrayList.class),list);
    return out.toString();
  }

  /**
   * The bean, written directly by a DataWriter
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static String write()
    throws Exception
  {
    StringWriter out=new StringWriter();
    DataWriter writer=new DataWriter();
    writer.setExcludes(new HashSet<java.net.URI>());
    writer.writeToWriter
      (out,BeanReflector.getInstance(ListDto.class),new ListDto());
    return out.toString();
  }
}
