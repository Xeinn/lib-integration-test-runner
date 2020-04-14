package uk.co.xeinn.pfref.testing.integration;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse.Headers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pay360.poc.jpath.pathwriter.JPath;

public class ScriptEvaluationContext {

	private static final Logger logger = LoggerFactory.getLogger(ScriptEvaluationContext.class);

	private static final Pattern constantExpressionPattern = Pattern.compile("(\\\"|').+(\\\"|')");

	private static final ObjectMapper json = new ObjectMapper();

	private HttpStatus lastResultStatus;
	private String lastResultContent;
	private JsonNode parsedResultContent;
	private Map<String,String> variables;
	private String scriptFilename;
	private Headers lastResultHeaders;

	public HttpStatus getLastResultStatus() {
		return lastResultStatus;
	}
	public void setLastResultStatus(HttpStatus lastResultStatus) {
		logger.debug("Setting result status {}", lastResultStatus);
		this.lastResultStatus = lastResultStatus;
	}

	public String getLastResultContent() {
		return lastResultContent;
	}

	public void setLastResultContent(String lastResultContent) {
		logger.debug("Setting result content {}", lastResultContent);
		
		// Parse the result content

		try {

			this.parsedResultContent = json.readTree(lastResultContent);

			this.lastResultContent = lastResultContent;
		}
		catch(IOException ex) {

			throw new IntegrationScriptFormatException("Result content is not valid json.  Error message - " + ex.getMessage(), ex);
		}
	}

	public Map<String,String> getVariables() {
		return variables;
	}

	public void setVariables(Map<String,String> variables) {
		this.variables = variables;
	}
	
	public String substituteVariables(String input) {
		
		String updatedInput = input;
		
		for(Map.Entry<String,String> val : variables.entrySet()) {
			updatedInput = updatedInput.replaceAll("(?i)\\{\\{"+val.getKey()+"\\}\\}", val.getValue());
		}
		
		return updatedInput;
	}

	public void setVariable(String variableName, String variableExpression) {

		try {

			String evaluatedValue = evaluateVariableExpression(variableExpression);
			
			logger.debug("Setting variable @{} to value {}", variableName, evaluatedValue);
			
			variables.put(variableName, evaluatedValue);
		}
		catch(IntegrationScriptFormatException ex) {

			// replace the thrown exception, adding a bit of extra error context information

			throw new IntegrationScriptFormatException("Failed to set variable @"+variableName+ " to expression . " + ex.getMessage(), ex);
		}
	}

	/**
	 * Extracts a result value from the last response stored in the context.  The value to be extracted is specified
	 * using dot notation i.e. rootObject.subObject.field
	 * 
	 * @param expression - the field path/name of the value to be extracted from the result
	 * @return Value of the specified field, if no value at the specified path an exception is thrown
	 */
	public String getResultValue(String expression) {
		
		// check if expression is a value reference
		
		if(parsedResultContent == null) {
			
			throw new IntegrationScriptFormatException("There is no result data to evaluate.");
		}
		
		try {
			
			return JPath.get(parsedResultContent, expression, String.class);
		}
		catch(IOException ex) {

			throw new IntegrationScriptFormatException("The value '"+expression+"' is not present in the result data.", ex);
		}
	}

	/**
	 * Checks for the presence of a result value from the last response stored in the context.  The value
	 * to be check for is specified using dot notation i.e. rootObject.subObject.field
	 * 
	 * @param expression - the field path/name of the value to be checked
	 * @return true if the value is present, false otherwise
	 */
	public boolean hasResultValue(String expression) {
		
		// check if expression is a value reference
		
		if(parsedResultContent == null) {
			
			throw new IntegrationScriptFormatException("There is no result data to evaluate.");
		}
		
		return JPath.exists(parsedResultContent, expression);
	}	

	
	/***
	 * Evaluates an expression to be used to set the value of a variable.  The expression can be either a constant value or a reference
	 * to a result in the result data stored in the context
	 * 
	 * @param  expression - Constant value or reference to a result value
	 * @return Value of the constant or the specified field, if the constant is a bad format or there is no value at the specified
	 *         path an exception is thrown
	 */
	private String evaluateVariableExpression(String expression) {
		
		if(expression == null) {
			
			throw new IntegrationScriptFormatException("Expression cannot be null.");
		}
		
		try {

			// check if expression is a constant expression
	
			if(constantExpressionPattern.matcher(expression).matches()) {

				return json.readValue(expression, String.class);
			}

		}
		catch(IOException ex) {

			throw new IntegrationScriptFormatException("Invalid format for constant value '"+expression+"' error message - " + ex.getMessage(), ex);
		}

		return getResultValue(expression);
	}

	public String getScriptFilename() {
		return scriptFilename;
	}

	public void setScriptFilename(String scriptFilename) {
		this.scriptFilename = scriptFilename;
	}

	public Headers getLastResultHeaders() {
		return lastResultHeaders;
	}

	public void setLastResultHeaders(Headers lastResultHeaders) {
		this.lastResultHeaders = lastResultHeaders;
	}
}
