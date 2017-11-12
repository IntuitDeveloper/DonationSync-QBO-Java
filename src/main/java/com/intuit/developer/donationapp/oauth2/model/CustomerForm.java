package com.intuit.developer.donationapp.oauth2.model;

import java.util.ArrayList;

public class CustomerForm {

	private ArrayList<CustomerDomain> personList;
	private String amt;

	public ArrayList<CustomerDomain> getPersonList() {
		return personList;
	}

	public void setPersonList(ArrayList<CustomerDomain> personList) {
		this.personList = personList;
	}

	public String getAmt() {
		return amt;
	}

	public void setAmt(String amt) {
		this.amt = amt;
	}

	
	
	
}
