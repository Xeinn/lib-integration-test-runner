package com.pay360.pfref.testing.integration;

public class IntegrationScriptFormatException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IntegrationScriptFormatException(String message) {
		super(message);
	}
	
	public IntegrationScriptFormatException(String message, Throwable ex) {
		super(message, ex);
	}
}
