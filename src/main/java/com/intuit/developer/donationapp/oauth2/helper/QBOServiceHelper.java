package com.intuit.developer.donationapp.oauth2.helper;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intuit.developer.donationapp.oauth2.client.OAuth2PlatformClientFactory;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.AccountBasedExpenseLineDetail;
import com.intuit.ipp.data.AccountTypeEnum;
import com.intuit.ipp.data.BillableStatusEnum;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.Invoice;
import com.intuit.ipp.data.Item;
import com.intuit.ipp.data.Line;
import com.intuit.ipp.data.LineDetailTypeEnum;
import com.intuit.ipp.data.LinkedTxn;
import com.intuit.ipp.data.Payment;
import com.intuit.ipp.data.PaymentMethod;
import com.intuit.ipp.data.PaymentTypeEnum;
import com.intuit.ipp.data.PhysicalAddress;
import com.intuit.ipp.data.PrintStatusEnum;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.data.SalesItemLineDetail;
import com.intuit.ipp.data.TxnTypeEnum;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.exception.InvalidTokenException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.ipp.util.DateUtils;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;

@Service
public class QBOServiceHelper {
	
	@Autowired
	OAuth2PlatformClientFactory factory;
	
	private static final Logger logger = Logger.getLogger(QBOServiceHelper.class);

	public DataService getDataService(String realmId, String accessToken) throws FMSException {
		
    	String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";

		Config.setProperty(Config.BASE_URL_QBO, url);
		//create oauth object
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		//create context
		Context context = new Context(oauth, ServiceType.QBO, realmId);

		// create dataservice
		return new DataService(context);
	}
		
	/**
	 * Creates request for adding Invoice in QuickBooks Online
	 * 
	 * @param service
	 * @param customerId
	 * @param amt
	 * @param session
	 * @return
	 * @throws FMSException
	 * @throws ParseException
	 */
	public Invoice getInvoiceFields(DataService service, String customerId, String amt, HttpSession session) 
			throws FMSException, ParseException {
		Invoice invoice = new Invoice();
		
		// Mandatory Fields
		invoice.setDocNumber(RandomStringUtils.randomAlphanumeric(5));

		try {
			invoice.setTxnDate(DateUtils.getCurrentDateTime());
		} catch (ParseException e) {
			throw new FMSException("ParseException while getting current date.");
		}

		Customer customer = getCustomer(service,customerId );
		invoice.setCustomerRef(getCustomerRef(customer));
		
		invoice.setTxnStatus("Payable");
		invoice.setBalance(new BigDecimal(amt));

		invoice.setBillAddr(getPhysicalAddress());

		List<Line> invLine = new ArrayList<Line>();
		Line line = new Line();
		line.setDescription("Donation");
		line.setAmount(new BigDecimal(amt));
		line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
		
		SalesItemLineDetail silDetails = new SalesItemLineDetail();
		
		String sql = "select * from item where name = 'Donation'";
		List<Item> items = (ArrayList<Item>) queryData(session, sql);
		
		if (items.isEmpty()) {
			return null;
		}
		Item item = items.get(0); 
		silDetails.setItemRef(getItemRef(item));

		line.setSalesItemLineDetail(silDetails);
		invLine.add(line);
		invoice.setLine(invLine);

		invoice.setRemitToRef(getCustomerRef(customer));
		invoice.setPrintStatus(PrintStatusEnum.NEED_TO_PRINT);
		invoice.setTotalAmt(new BigDecimal(amt));
		invoice.setFinanceCharge(false);
		
		return invoice;
	}
	
	private static Customer getCustomer(DataService service, String customerId) throws FMSException, ParseException {
		Customer customerIn = new Customer();
		customerIn.setId(customerId);
		Customer customerOut = service.findById(customerIn);
		return customerOut;
	}
	
	private static Invoice getInvoice(DataService service, String invoiceId) throws FMSException {
		Invoice invoiceIn = new Invoice();
		invoiceIn.setId(invoiceId);
		Invoice invoiceOut = service.findById(invoiceIn);
		return invoiceOut;
	}
	
	private static ReferenceType getCustomerRef(Customer customer) {
		ReferenceType customerRef = new ReferenceType();
		customerRef.setName(customer.getDisplayName());
		customerRef.setValue(customer.getId());
		return customerRef;
	}
	
	private static PhysicalAddress getPhysicalAddress() {
		PhysicalAddress billingAdd = new PhysicalAddress();
		billingAdd.setLine1("123 Main St");
		billingAdd.setCity("Mountain View");
		billingAdd.setCountry("United States");
		billingAdd.setCountrySubDivisionCode("CA");
		billingAdd.setPostalCode("94043");
		return billingAdd;
	}
	
	private static ReferenceType getItemRef(Item item) {
		ReferenceType itemRef = new ReferenceType();
		itemRef.setName(item.getName());
		itemRef.setValue(item.getId());
		return itemRef;
	}
	
	/**
	 * Creates request for adding Payment in QuickBooks Online
	 * 
	 * @param service
	 * @param customerId
	 * @param amt
	 * @param session
	 * @return
	 * @throws FMSException
	 * @throws ParseException
	 */
	public Payment getPaymentFields(DataService service, String customerId, String amt, HttpSession session) 
			throws FMSException, ParseException {
		
		Payment payment = new Payment();
		try {
			payment.setTxnDate(DateUtils.getCurrentDateTime());
		} catch (ParseException e) {
			throw new FMSException("ParseException while getting current date.");
		}
		
		List<LinkedTxn> linkedTxnList = new ArrayList<LinkedTxn>();
		LinkedTxn linkedTxn = new LinkedTxn();
		String invoiceId = (String)session.getAttribute("invoiceId");
		if(!invoiceId.isEmpty()) {
			Invoice invoice = getInvoice(service, invoiceId);
			linkedTxn.setTxnId(invoice.getId());
			linkedTxn.setTxnType(TxnTypeEnum.INVOICE.value());
			linkedTxnList.add(linkedTxn);
		}
        
		Line line1 = new Line();
		line1.setAmount(new BigDecimal(amt));
		line1.setDetailType(LineDetailTypeEnum.ACCOUNT_BASED_EXPENSE_LINE_DETAIL);
		
		AccountBasedExpenseLineDetail accountBasedExpenseLineDetail1 = new AccountBasedExpenseLineDetail();
		Account expenseAccount1 = getExpenseBankAccount(service);
		accountBasedExpenseLineDetail1.setAccountRef(getAccountRef(expenseAccount1));
		ReferenceType taxCodeRef = new ReferenceType();
		taxCodeRef.setValue("NON");
		accountBasedExpenseLineDetail1.setTaxCodeRef(taxCodeRef);
		accountBasedExpenseLineDetail1.setBillableStatus(BillableStatusEnum.NOT_BILLABLE);

		line1.setAccountBasedExpenseLineDetail(accountBasedExpenseLineDetail1);
		line1.setLinkedTxn(linkedTxnList);
		
		List<Line> lineList = new ArrayList<Line>();
		lineList.add(line1);
		payment.setLine(lineList);

		Account depositAccount = getAssetAccount(service);
		payment.setDepositToAccountRef(getAccountRef(depositAccount));

		Customer customer = getCustomer(service, customerId);
		payment.setCustomerRef(getCustomerRef(customer));

		PaymentMethod paymentMethod = getPaymentMethod(service);
		payment.setPaymentMethodRef(getPaymentMethodRef(paymentMethod));
		
		payment.setPaymentType(PaymentTypeEnum.CREDIT_CARD);
		payment.setTotalAmt(new BigDecimal(amt));
		return payment;
	}
	
	private static Account getExpenseBankAccount(DataService service) throws FMSException {
		List<Account> accounts = (List<Account>) service.findAll(new Account());
		if (!accounts.isEmpty()) {
			Iterator<Account> itr = accounts.iterator();
			while (itr.hasNext()) {
				Account account = itr.next();
				if (account.getAccountType().equals(AccountTypeEnum.EXPENSE)) {
					return account;
				}
			}
		}
		return null;
	}
	
	private static ReferenceType getAccountRef(Account account) {
		ReferenceType accountRef = new ReferenceType();
		accountRef.setName(account.getName());
		accountRef.setValue(account.getId());
		return accountRef;
	}
	
	private static Account getAssetAccount(DataService service)  throws FMSException{
		List<Account> accounts = (List<Account>) service.findAll(new Account());
		if (!accounts.isEmpty()) {
			Iterator<Account> itr = accounts.iterator();
			while (itr.hasNext()) {
				Account account = itr.next();
				if (account.getAccountType().equals(AccountTypeEnum.OTHER_CURRENT_ASSET)) {
					return account;
				}
			}
		}
		return null;
	}
	
	private static PaymentMethod getPaymentMethod(DataService service) throws FMSException {
		List<PaymentMethod> paymentMethods = (List<PaymentMethod>) service.findAll(new PaymentMethod());
		 if (!paymentMethods.isEmpty()) { 
			 return paymentMethods.get(0); 
		 }
		return null;
	}
	
	private static ReferenceType getPaymentMethodRef(PaymentMethod paymentMethod) {
		ReferenceType paymentMethodRef = new ReferenceType();
		paymentMethodRef.setValue(paymentMethod.getId());
		return paymentMethodRef;
	}
	
    /**
     * Queries data from QuickBooks
     * 
     * @param session
     * @param sql
     * @return
     */
    public List<? extends IEntity> queryData(HttpSession session, String sql) {

    	String realmId = (String)session.getAttribute("realmId");
    	if (StringUtils.isEmpty(realmId)) {
    		logger.error("Relam id is null ");
    	}
    	String accessToken = (String)session.getAttribute("access_token");
    	
    	try {
        	
        	//get DataService
    		DataService service = getDataService(realmId, accessToken);
			
			// get data
			QueryResult queryResult = service.executeQuery(sql);
			return queryResult.getEntities();
		}
	        /*
	         * Handle 401 status code - 
	         * If a 401 response is received, refresh tokens should be used to get a new access token,
	         * and the API call should be tried again.
	         */
	        catch (InvalidTokenException e) {			
				logger.error("Error while calling executeQuery :: " + e.getMessage());
				
				//refresh tokens
	        	logger.info("received 401 during companyinfo call, refreshing tokens now");
	        	OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
	        	String refreshToken = (String)session.getAttribute("refresh_token");
	        	
				try {
					BearerTokenResponse bearerTokenResponse = client.refreshToken(refreshToken);
					session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
		            session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());
		            
		            //call company info again using new tokens
		            logger.info("calling companyinfo using new tokens");
		            DataService service = getDataService(realmId, accessToken);
					
					// get data
					QueryResult queryResult = service.executeQuery(sql);
					return queryResult.getEntities();
					 
				} catch (OAuthException e1) {
					logger.error("Error while calling bearer token :: " + e.getMessage());
					
				} catch (FMSException e1) {
					logger.error("Error while calling company currency :: " + e.getMessage());
				}
	            
			} catch (FMSException e) {
				List<Error> list = e.getErrorList();
				list.forEach(error -> logger.error("Error while calling executeQuery :: " + error.getMessage()));
			}
		return null;
    }
}
