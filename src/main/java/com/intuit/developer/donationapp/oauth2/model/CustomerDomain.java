package com.intuit.developer.donationapp.oauth2.model;

public class CustomerDomain {
	
	private String id;
	
	private String name;
	
	private String amt;
	
	private Boolean selected;
	
	public CustomerDomain() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CustomerDomain(String id, String name) {
		this.id = id;
		this.name = name;
		
	}
	
	

	public CustomerDomain(String id, String name, String amt, Boolean selected) {
		super();
		this.id = id;
		this.name = name;
		this.amt = amt;
		this.selected = selected;
	}

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

	public String getAmt() {
		return amt;
	}

	public void setAmt(String amt) {
		this.amt = amt;
	}

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	
	
	
	

}
