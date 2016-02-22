package com.realtek.nasfun.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown=true) 
public class RtCGIContentFilePropertyResponse {
	public String path;
	public int status;
	public String response_type;
	
	@Override
	public String toString() {
		return "RtCGIContentFilePropertyResponse [status=" + status + ", path=" + path + ", response_type="
				+ response_type + "]";
	}
}
