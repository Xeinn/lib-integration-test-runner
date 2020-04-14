package com.pay360.pfref.testing.integration;

public class IntegrationScriptFileLoadException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IntegrationScriptFileLoadException(String message) {
		super(message);
	}
	
	public IntegrationScriptFileLoadException(String message, Throwable ex) {
		super(message, ex);
	}
}
