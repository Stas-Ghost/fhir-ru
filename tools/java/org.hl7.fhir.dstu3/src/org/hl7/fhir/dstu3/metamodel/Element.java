package org.hl7.fhir.dstu3.metamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the reference model of FHIR
 * 
 * A resource is nothing but a set of elements, where every element has a 
 * name, maybe a stated type, maybe an id, and either a value or child elements 
 * (one or the other, but not both or neither)
 * 
 * @author Grahame Grieve
 *
 */
public class Element {

	private List<String> comments;// not relevant for production, but useful in documentation
	private String name;
	private String type;
	private String value;
	private int index = -1;
	private List<Element> children;
	private Property property;

	public Element(String name) {
		super();
		this.name = name;
	}

	public Element(String name, Property property) {
		super();
		this.name = name;
		this.property = property;
	}

	public Element(String name, Property property, String type, String value) {
		super();
		this.name = name;
		this.property = property;
		this.type = type;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		if (type == null)
			return property.getType(name);
		else
		  return type;
	}

	public String getValue() {
		return value;
	}

	public boolean hasChildren() {
		return !(children == null || children.isEmpty());
	}

	public List<Element> getChildren() {
		if (children == null)
			children = new ArrayList<Element>();
		return children;
	}

	public boolean hasComments() {
		return !(comments == null || comments.isEmpty());
	}

	public List<String> getComments() {
		if (comments == null)
			comments = new ArrayList<String>();
		return comments;
	}

	public Property getProperty() {
		return property;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setType(String type) {
		this.type = type;

	}

	public boolean hasValue() {
		return value != null;
	}

	public List<Element> getChildrenByName(String name) {
		List<Element> res = new ArrayList<Element>();
		if (hasChildren()) {
			for (Element child : children)
				if (name.equals(child.getName()))
					res.add(child);
		}
		return res;
	}

	public void numberChildren() {
		if (children == null)
			return;
		
		String last = "";
		int index = 0;
		for (Element child : children) {
			if (child.getProperty().isList()) {
			  if (last.equals(child.getName())) {
			  	index++;
			  } else {
			  	last = child.getName();
			  	index = 0;
			  }
		  	child.index = index;
			} else {
				child.index = -1;
			}
			child.numberChildren();
		}	
	}

	public int getIndex() {
		return index;
	}

	public boolean hasIndex() {
		return index > -1;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getChildValue(String name) {
		if (children == null)
			return null;
		for (Element child : children) {
			if (name.equals(child.getName()))
				return child.getValue();
		}
  	return null;
	}

	public List<Element> getChildren(String name) {
		List<Element> res = new ArrayList<Element>(); 
		for (Element child : children) {
			if (name.equals(child.getName()))
				res.add(child);
		}
		return res;
	}

	
}
