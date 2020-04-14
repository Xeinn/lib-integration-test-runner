package com.pay360.pfref.testing.integration;

public interface ScriptAction {

	void executeAction(ScriptEvaluationContext context);
	boolean isPostRequestAction();
	boolean isRequestAction();
	int getLineNumber();
}
