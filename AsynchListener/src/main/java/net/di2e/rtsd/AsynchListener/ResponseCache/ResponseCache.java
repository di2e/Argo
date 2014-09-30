package net.di2e.rtsd.AsynchListener.ResponseCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ResponseCache {

	private HashMap<String, ServiceInfoBean> cache = new HashMap<String, ServiceInfoBean>();
	
	public synchronized void cacheAll(ArrayList<ServiceInfoBean> list) {
		
		for (ServiceInfoBean service : list) {
			cache.put(service.id, service);
		}
		
	}
	
	public synchronized void cache(ServiceInfoBean service) {
		cache.put(service.id, service);
	}
	
	public String toXML() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		buf.append("<cache>\n");
		
		clearExpired();
		for (ServiceInfoBean infoBean : cache.values()) {
			 buf.append(infoBean.toXML());
		}
		
		buf.append("</cache>/n/n");
		return buf.toString();
	}
	
	public String toJSON() {
		JSONObject response = this.toJSONObject();
		
		return response.toString(4);
	}
	
	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		
		clearExpired();
		for (ServiceInfoBean infoBean : cache.values()) {
			 array.add(infoBean.toJSONObject());
		}	
		
		json.put("cache", array);
		
		return json;
		
	}
	
	private synchronized void clearExpired() {
		
		Iterator<Entry<String, ServiceInfoBean>> it = cache.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        ServiceInfoBean infoBean = (ServiceInfoBean) pair.getValue();
	        if (infoBean.isExpired()) it.remove(); // avoids a ConcurrentModificationException
	    }
	    
		
	}

	public String toContractJSON() {
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		
		clearExpired();
		for (ServiceInfoBean infoBean : cache.values()) {
			JSONObject contract = new JSONObject();
			contract.put("contractID", infoBean.serviceContractID);
			contract.put("contractDescription", infoBean.contractDescription);
			array.add(contract);
		}	
		
		json.put("contracts", array);
		
		return json.toString(4);
	}
	
}
