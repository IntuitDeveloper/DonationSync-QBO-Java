package com.intuit.developer.donationapp.oauth2.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.intuit.developer.donationapp.oauth2.client.OAuth2PlatformClientFactory;
import com.intuit.developer.donationapp.oauth2.helper.QBOServiceHelper;
import com.intuit.developer.donationapp.oauth2.model.CustomerDomain;
import com.intuit.developer.donationapp.oauth2.model.CustomerForm;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.data.Invoice;
import com.intuit.ipp.data.Payment;
import com.intuit.ipp.services.DataService;

/**
 * @author dderose
 *
 */
@Controller
public class QBOController {
	
	@Autowired
	OAuth2PlatformClientFactory factory;
	
	@Autowired
    public QBOServiceHelper helper;

	
	private static final Logger logger = Logger.getLogger(QBOController.class);
	
	
    /**
     * Retrieves list of customers(Donors) from QuickBooks
     * @param model
     * @param session
     * @return
     */
    @RequestMapping("/syncCustomer")
    public String getCustomers(Model model, HttpSession session) {
    	
        //list with Persons
        List<CustomerDomain>  personsList= new ArrayList<CustomerDomain>();
        
        String sql = "select * from customer startposition 1 maxresults 10";
        List<Customer> customers = (ArrayList<Customer>) helper.queryData(session, sql);
        
        for (Customer customer: customers) {
        	CustomerDomain customerDomain = new CustomerDomain(customer.getId(), customer.getDisplayName());
        	personsList.add(customerDomain);
        }

       CustomerForm form = new CustomerForm();
       form.setPersonList((ArrayList<CustomerDomain>) personsList);
       form.setAmt("50");       
       model.addAttribute("wrapper", form);

       return "customer";
    }
    
    /**
     * Creates an invoice (Pledge) for the customer selected in the UI
     * 
     * @param wrapper
     * @param model
     * @param session
     * @return
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST, params="action=pledge")
	public String addPledge(@ModelAttribute CustomerForm wrapper, Model model, HttpSession session) {
    	
    	for (CustomerDomain person: wrapper.getPersonList()) {
    	
    		if(person.getId() != null) {

    			String realmId = (String)session.getAttribute("realmId");
		    	if (StringUtils.isEmpty(realmId)) {
		    		logger.error("Relam id is null ");
		    	}
		    	String accessToken = (String)session.getAttribute("access_token");
		        try {
		        	
		        	//get DataService
		    		DataService service = helper.getDataService(realmId, accessToken);
					
					// create invoice
		    		Invoice invoice = helper.getInvoiceFields(service, person.getId(), wrapper.getAmt(), session);
					Invoice invoiceOut = service.add(invoice);
					session.setAttribute("invoiceId", invoiceOut.getId());
					return "pledge";
					
				}
		        catch(Exception e) {
		        	logger.error("Error while calling QBO API :: " + e.getMessage());
		        }
    		}
    	}
		return "connected";
    	
    }
    
    /**
     * Creates a payment (Donation) for the customer selected in the UI
     * 
     * @param wrapper
     * @param model
     * @param session
     * @return
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST, params="action=donation")
	public String makeDonation(@ModelAttribute CustomerForm wrapper, Model model, HttpSession session) {
    	
    	for (CustomerDomain person: wrapper.getPersonList()) {
    	
    		if(person.getId() != null) {
		    	
		    	String realmId = (String)session.getAttribute("realmId");
		    	if (StringUtils.isEmpty(realmId)) {
		    		logger.error("Relam id is null ");
		    	}
		    	String accessToken = (String)session.getAttribute("access_token");
		    	try {
		        	
		        	//get DataService
		    		DataService service = helper.getDataService(realmId, accessToken);
					
					// create payment
		    		Payment payment = helper.getPaymentFields(service, person.getId(), wrapper.getAmt(), session);
		    		Payment paymentOut = service.add(payment);
					session.setAttribute("paymentId", paymentOut.getId());
					
					return "donation";
	    		} catch(Exception e) {
	    			logger.error("Error while calling QBO API :: " + e.getMessage());
		        }
    		}
    	}
		return "donation";
    	
    }
    
}
