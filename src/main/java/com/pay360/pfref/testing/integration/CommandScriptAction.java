package com.pay360.pfref.testing.integration;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommandScriptAction implements ScriptAction {
	
	private static final Logger logger = LoggerFactory.getLogger(CommandScriptAction.class);
	
	private static final Pattern setVariableCommandPattern = Pattern.compile("(?:(?i:set)\\s+)?\\@([a-zA-Z][a-zA-Z0-9\\_\\-]*)\\s*=\\s*(\\\".*\\\"|'.*'|\\S+.*\\S+)\\s*$");
	private static final Pattern expectResultStatusCommandPattern = Pattern.compile("(?i)expect\\s+(?:result\\s)?+status\\s+(\\d+(?:\\s+\\d+)*)");
	private static final Pattern expectResultValueCommandPattern = Pattern.compile("(?i)expect\\s+(\\S+)\\s*=\\s*(?:\\\"(.*)\\\"|'(.*)'|(\\S+.*\\S+))\\s*$");
	private static final Pattern expectResultValueNotPresentCommandPattern = Pattern.compile("(?i)expect\\s+(\\S+)\\s*(?:is\\s+)?not\\s+present");
	private static final Pattern expectResultHeaderPresentCommandPattern = Pattern.compile("(?i)expect header\\s+(\\S+)\\s*");
	private static final Pattern expectResultHeaderValueCommandPattern = Pattern.compile("(?i)expect header\\s+(\\S+)\\s+value\\s*=\\s*(?:\\\"(.*)\\\"|'(.*)'|(\\S+.*\\S+))\\s*$");
	private static final Pattern waitCommandPattern = Pattern.compile("(?i)wait(?:\\s+before(?:\\s+running(?:\\s+test)?)?)?\\s+for\\s+(\\d+)\\s*(?:s|seconds)$");

	private int lineNumber;
	private String command;
	
	public CommandScriptAction(String command, int lineNumber) {
		
		this.command = command.trim();
		this.lineNumber = lineNumber;
	}
	
	@Override
	public boolean isRequestAction() {
		return false;
	}

	@Override
	public boolean isPostRequestAction() {

		if(waitCommandPattern.matcher(command).matches()) {
			return false;
		}
		
		return true;
	}
	
	@Override
	@SuppressWarnings("squid:S3776")
	public void executeAction(ScriptEvaluationContext context) {

		// Set variable command
		
		Matcher setVariableMatcher = setVariableCommandPattern.matcher(command);
	
		if(setVariableMatcher.matches()) {

			context.setVariable(setVariableMatcher.group(1), setVariableMatcher.groupCount() > 1 ? setVariableMatcher.group(2) : null);
			
			return;
		}
		
		// Expect result status command
		
		Matcher expectResultStatusMatcher = expectResultStatusCommandPattern.matcher(command);
		
		if(expectResultStatusMatcher.matches()) {

			Set<Integer> expectedStatusValues = Arrays.stream(expectResultStatusMatcher.group(1).split("\\s+")).map(Integer::new).collect(Collectors.toSet());
			
			if(!expectedStatusValues.contains(context.getLastResultStatus().value())) {
				
				throw new IntegrationScriptException("Expected one of the following result status values "+expectResultStatusMatcher.group(1)+" but instead received "+ context.getLastResultStatus().value());
			}
			
			return;
		}

		// Expect response value command

		Matcher expectResultValueMatcher = expectResultValueCommandPattern.matcher(command);
		
		if(expectResultValueMatcher.matches()) {

			String resultValue = context.getResultValue(expectResultValueMatcher.group(1));
			String regexValue = coalesce(expectResultValueMatcher.group(2), expectResultValueMatcher.group(3), expectResultValueMatcher.group(4));
			
			if(resultValue == null || !resultValue.matches(regexValue)) {
				
				throw new IntegrationScriptException("Expected the value of " + expectResultValueMatcher.group(1) + "(" + resultValue + ") to match pattern " + regexValue);
			}

			return;
		}
		
		// Expect response value not present

		Matcher expectResultValueNotPresentMatcher = expectResultValueNotPresentCommandPattern.matcher(command);

		if(expectResultValueNotPresentMatcher.matches()) {
			
			if(context.hasResultValue(expectResultValueNotPresentMatcher.group(1))) {
				
				throw new IntegrationScriptException("Expected field " + expectResultValueNotPresentMatcher.group(1) + " to NOT be present");
			}

			return;
		}

		// expect header be present

		Matcher expectResultHeaderPresentMatcher = expectResultHeaderPresentCommandPattern.matcher(command);

		if(expectResultHeaderPresentMatcher.matches()) {

			if(context.getLastResultHeaders().header(expectResultHeaderPresentMatcher.group(1)).isEmpty()) {
				
				throw new IntegrationScriptException("Expected header " + expectResultHeaderPresentMatcher.group(1) + " to be set on response");
			}

			return;
		}

		// expect header value to match regex

		Matcher expectResultHeaderValueMatcher = expectResultHeaderValueCommandPattern.matcher(command);

		if(expectResultHeaderValueMatcher.matches()) {

			if(context.getLastResultHeaders().header(expectResultHeaderValueMatcher.group(1)).isEmpty()) {
				
				throw new IntegrationScriptException("Expected header " + expectResultHeaderValueMatcher.group(1) + " to be set on response");
			}
			
			String regexValue = coalesce(expectResultHeaderValueMatcher.group(2), expectResultHeaderValueMatcher.group(3), expectResultHeaderValueMatcher.group(4));

			if(context.getLastResultHeaders().header(expectResultHeaderValueMatcher.group(1)).stream().noneMatch(e -> e.matches(regexValue))) {
				
				throw new IntegrationScriptException("Expected at least one header value for header " + expectResultHeaderValueMatcher.group(1) + " to match regular expression " + regexValue);
			}

			return;
		}
		
		Matcher waitValueMatcher = waitCommandPattern.matcher(command);

		if(waitValueMatcher.matches()) {

			String waitFor = waitValueMatcher.group(1);
			
			logger.debug("waiting for {} seconds", waitFor);
			try {
				Thread.sleep(Long.parseLong(waitFor) * 1000);
			}
			catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	@SafeVarargs
	public static <T> T coalesce(T... params)
	{
		return Arrays.stream(params).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public int getLineNumber() {

		return lineNumber;
	}
}
