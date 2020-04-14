package com.pay360.pfref.testing.integration;

public class IntegrationScriptException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IntegrationScriptException(String message) {
		super(message);
	}
	
	public IntegrationScriptException(String message, Throwable ex) {
		super(message, ex);
	}
}
