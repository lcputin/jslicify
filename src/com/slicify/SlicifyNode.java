package com.slicify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Provides a simple interface for Java applications to access Slicify Node booking service.
 * @author slicify
 *
 */
public class SlicifyNode {

	private static final String SERVER = "secure.slicify.com";
	private static final String SERVICEURL = "https://" + SERVER + "/Service/BookingService.asmx";
	
	private HttpsGet HttpsGet = new HttpsGet(SERVICEURL);

	/**
	 * Set your www.slicify.com username. Must be set before any methods are called.
	 * @param username
	 */
	public void setUsername(String username)
	{
		HttpsGet.Username = username;
	}

	/**
	 * Set your www.slicify.com password. Must be set before any methods are called.
	 * @param password
	 */
	public void setPassword(String password)
	{
		HttpsGet.Password = password;
	}
	
	/**
	 * Book a machine. Pass in the specifications for the type of machine you require. If a machine is available, it will be
	 * booked immediately and a booking reference (booking ID) is returned.
	 * @param minCores
	 * @param minRam
	 * @param maxPrice
	 * @param bits
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public int bookNode(int minCores, int minRam, double maxPrice, int bits, int minEcu) throws Exception
	{
		String targetOP = "BookMachine";
		
		if(minCores < 1 || minCores > 64)
			throw new IllegalArgumentException("Minimum cores must be between 1 & 64");
		if(minRam < 0 || minRam > 256*1024)
			throw new IllegalArgumentException("Minimum RAM must be between 0 and 262144 (mb)");
		if(maxPrice < 0 || maxPrice > 2)
			throw new IllegalArgumentException("Maximum price must be between 0 and 2.0 ($/hour)");
		if(bits != 32 && bits != 64)
			throw new IllegalArgumentException("Bits must be set to either 32 or 64");
		if(minEcu < 1)
			throw new IllegalArgumentException("Minimum ECU must be greater than 1");
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("minCores", minCores);
		params.put("minRam", minRam);
		params.put("maxPrice", maxPrice);
		params.put("bits", bits);
		params.put("minECU", minEcu);
		String urlParameters = createUrlParameters(params);

		//need to parse the first field, which will be an integer booking reference
		HttpsGet.ParseOnQuery = true;
		HttpsGet.query(targetOP, urlParameters);		
		String result = HttpsGet.parseReply(0);			
		return Integer.parseInt(result);		
	}
	
	/**
	 * Return a list of all bookings that are currently active for this user
	 * @return List of all currently active booking IDs.
	 * @throws Exception 
	 */
	public List<Integer> getActiveBookingIDs() throws Exception
	{
		//create result container
		List<Integer> result = new ArrayList<Integer>();
		
		//get the XML back from the web service
		HttpsGet.ParseOnQuery = true;
		HttpsGet.query("GetActiveBookingIDs", "");
		Document document = HttpsGet.XMLDoc;
		
		//extract list of booking IDs
		NodeList bookingFields = document.getElementsByTagName("int");
		for(int i=0; i<bookingFields.getLength(); i++)
		{
			String bookingID = bookingFields.item(i).getTextContent();
			result.add(Integer.parseInt(bookingID));
		}
		
		return result;
	}
	
	/**
	 * Get the booking status for a particular booking ID. Status will be "Ready" when the VM is ready for use.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public String getBookingStatus(int bookingID) throws Exception
	{
		return runBookingOperation("GetBookingStatus", bookingID, true);
	}
	
	/**
	 * Get the SSH login password to access this machine.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public String getBookingPassword(int bookingID) throws Exception
	{
		return runBookingOperation("GetBookingPassword", bookingID, true);
	}

	/**
	 * Get the SUDO/root password for this machine.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public String getSudoPassword(int bookingID) throws Exception
	{
		return runBookingOperation("GetSudoPassword", bookingID, true);
	}

	/**
	 * Get a textual description of the hardware on which this machine runs.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public String getMachineSpec(int bookingID) throws Exception
	{
		return runBookingOperation("GetMachineSpec", bookingID, true);
	}

	/**
	 * Get the number of hardware cores assigned to the virtual machine for this booking.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public int getCoreCount(int bookingID) throws Exception
	{
		String sCores = runBookingOperation("GetCoreCount", bookingID, true);
		return Integer.parseInt(sCores);
	}

	/**
	 * Get the approximate ECU benchmark for the machine with this booking ID.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public int getECU(int bookingID) throws Exception
	{
		String sECU = runBookingOperation("GetECU", bookingID, true);
		return Integer.parseInt(sECU);
	}

	/**
	 * Get a textual description of the reason that this booking was closed.
	 * @param bookingID
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public String getCloseReason(int bookingID) throws Exception
	{
		return runBookingOperation("GetCloseReason", bookingID, true);
	}

	/**
	 * Cancel the booking.
	 * @param bookingID
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void cancelBooking(int bookingID) throws Exception
	{
		runBookingOperation("CancelBooking", bookingID, false);
	}
	
	
	/**
	 * Wait until Node is in "Ready" status, indicating it is ready for user
	 */
	public void waitReady(int bookingID) throws Exception {
		String status = "Unknown";
		while(!status.equals("Ready"))
		{
			//check it hasnt faulted
			if(status.equals("Closed"))
				throw new Exception("Machine wasnt able to be booked succesfully");
			
			//check the status every 10 seconds
			Thread.sleep(10000);					
			status = getBookingStatus(bookingID);
		}
	}
		
	
	
	private String runBookingOperation(String targetOP, int bookingID, boolean parse) throws Exception
	{		
		if(bookingID < 0)
			throw new IllegalArgumentException("Booking ID must be > 0");
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bookingID", bookingID);
		String urlParameters = createUrlParameters(params);

		//need to parse the first field, which will be an integer booking reference
		HttpsGet.ParseOnQuery = parse;
		HttpsGet.query(targetOP, urlParameters);
		if(parse)
			return HttpsGet.parseReply(0);
		else
			return null;
	}
	
	private String createUrlParameters(Map<String, Object> params)
	{
		StringBuilder sb = new StringBuilder();
		for(Entry<String, Object> param : params.entrySet())
		{
			sb.append(param.getKey() + "=" + param.getValue() + "&");
		}
		
		String temp = sb.toString();
		return temp.substring(0, temp.length()-1);
	}

}
