package com.realtek.nasfun.api;

/**
 * POJO for sending CGI request
 * @author mark.yang
 *
 */
public class CGIRequest {
	public static class Request{
		private String requestType;
		private String id;
		private String startIndex;
		private String numberOfItems;
		private String ts;
		
		public String getRequestType() {
			return requestType;
		}
		public void setRequestType(String requestType) {
			this.requestType = requestType;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getStartIndex() {
			return startIndex;
		}
		public void setStartIndex(String startIndex) {
			this.startIndex = startIndex;
		}
		public String getNumberOfItems() {
			return numberOfItems;
		}
		public void setNumberOfItems(String numberOfItems) {
			this.numberOfItems = numberOfItems;
		}
		public String getTs() {
			return ts;
		}
		public void setTs(String ts) {
			this.ts = ts;
		}
		@Override
		public String toString() {
			return "Request [requestType=" + requestType + ", id=" + id
					+ ", startIndex=" + startIndex + ", numberOfItems="
					+ numberOfItems + ", ts=" + ts + "]";
		}
	}
	
	private Request request;

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	@Override
	public String toString() {
		return "CGIRequest [request=" + request + "]";
	}
}
