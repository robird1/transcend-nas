package com.realtek.nasfun.api;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * POJO for CGI response
 * @author mark.yang
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true) 
public class CGIResponse {
	@JsonIgnoreProperties(ignoreUnknown=true) 
	public static class Item{
		private String id;
		private String name;
		private String type;
		private String thumbtype;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getThumbtype() {
			return thumbtype;
		}
		public void setThumbtype(String thumbtype) {
			this.thumbtype = thumbtype;
		}
		@Override
		public String toString() {
			return "Item [id=" + id + ", name=" + name + ", type=" + type
					+ ", thumbtype=" + thumbtype + "]";
		}
		
	}
	
	private Item self;
	private Item parent;
	private ArrayList<Item> children;
	private String responseType;
	private int totalNumberOfChildren;
	private int totalNumberOfItems;
	private int startIndexOfChildren;
	public Item getSelf() {
		return self;
	}
	public void setSelf(Item self) {
		this.self = self;
	}
	public Item getParent() {
		return parent;
	}
	public void setParent(Item parent) {
		this.parent = parent;
	}
	public ArrayList<Item> getChildren() {
		return children;
	}
	public void setChildren(ArrayList<Item> children) {
		this.children = children;
	}
	public String getResponseType() {
		return responseType;
	}
	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public int getTotalNumberOfChildren() {
		return totalNumberOfChildren;
	}
	public void setTotalNumberOfChildren(int totalNumberOfChildren) {
		this.totalNumberOfChildren = totalNumberOfChildren;
	}
	public int getTotalNumberOfItems() {
		return totalNumberOfItems;
	}
	public void setTotalNumberOfItems(int totalNumberOfItems) {
		this.totalNumberOfItems = totalNumberOfItems;
	}
	public int getStartIndexOfChildren() {
		return startIndexOfChildren;
	}
	public void setStartIndexOfChildren(int startIndexOfChildren) {
		this.startIndexOfChildren = startIndexOfChildren;
	}
	
	@Override
	public String toString() {
		return "CGIResponse [self=" + self + ", parent=" + parent
				+ ", children=" + children + ", responseType=" + responseType
				+ ", totalNumberOfChildren=" + totalNumberOfChildren
				+ ", totalNumberOfItems=" + totalNumberOfItems
				+ ", startIndexOfChildren=" + startIndexOfChildren + "]";
	}
	
	
}
