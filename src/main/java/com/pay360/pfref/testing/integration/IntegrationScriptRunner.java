package com.pay360.pfref.testing.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;


public class IntegrationScriptRunner {

	private static final Logger logger = LoggerFactory.getLogger(IntegrationScriptRunner.class);

	Pattern variableAssignmentPattern = Pattern.compile("\\s*\\@([a-zA-Z0-9_\\-]+)\\s*=\\s*(.*)");
	Pattern headerPattern = Pattern.compile("([\\w-]+): (.*)");
	Pattern commandPattern = Pattern.compile("^#>\\s*(.*)");
	Pattern requestPattern = Pattern.compile("^(GET|POST|PUT|HEAD|DELETE|PATCH|OPTIONS)\\s+(.*)\\s+(HTTP\\/[0-9]\\.[0-9])$");
	Pattern urlPattern = Pattern.compile("^(?:(?:https?|ftp):\\/\\/)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z_\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z_\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z_\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z_\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z_\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:\\/[^\\s]*)?$");

	private List<ScriptAction> scriptActions;
	private List<String> templateLines;
	
	public boolean runScript(String filename) {
		
		// load file
		
		String testTemplate = loadTestFileFromResource(filename);

		if(testTemplate == null) {
			
			throw new IntegrationScriptFileLoadException("Could not load integration test script file");
		}

		// turn into lines
		
		templateLines = Arrays.asList(testTemplate.split("\\R", -1));

		// scan for variable assignments

		Map<String,String> variables = templateLines.stream()
			.map(e -> variableAssignmentPattern.matcher(e))
			.filter(Matcher::matches)
			.collect(Collectors.toMap(k -> k.group(1), v -> v.group(2)));
		
		// build script actions
		
		scriptActions = new ArrayList<>();
		
		// parse into actions

		int currentLine = 0;
		while(currentLine < templateLines.size()) {
			
			currentLine = parseScriptActions(currentLine);
		}
		
		ScriptEvaluationContext context = new ScriptEvaluationContext();
		
		context.setScriptFilename(filename);
		
		context.setVariables(variables);
		
		List<ScriptAction> postRequestActions = new ArrayList<>();

		// reuse current line
		currentLine = 0;
		try {
		
			for(ScriptAction action : scriptActions) {
	
				if(action.isPostRequestAction()) {
					postRequestActions.add(action);
				}
				else {
					currentLine = action.getLineNumber();
					action.executeAction(context);
				}
				
				if(action.isRequestAction()) {
					
					for(ScriptAction postRequestAction : postRequestActions) {
						currentLine = postRequestAction.getLineNumber();
						postRequestAction.executeAction(context);
					}
					
					postRequestActions.clear();
				}
			}
		}
		catch(Exception ex) {

			// Add the filename of the script to any exception thrown
			
			throw new IntegrationScriptException("Error in integration test script: '" + filename + "' at line " + (currentLine+1), ex);
		}
		
		return true;
	}

	
	private int parseScriptActions(int currentIndex) {
		
		// check if the line is the start of a request

		int newIndex = currentIndex;
		
		newIndex += parseScriptCommand(newIndex);
		newIndex += parseRequest(newIndex);

		// If nothing was parsed then advance one line
		
		return newIndex == currentIndex ? currentIndex + 1 : newIndex;
	}

	private int parseScriptCommand(int currentIndex) {

		Matcher matcher = commandPattern.matcher(templateLines.get(currentIndex));
		
		if(matcher.matches()) {
			
			scriptActions.add(new CommandScriptAction(matcher.group(1), currentIndex));
			
			return 1;
		}
		
		return 0;
	}
	
	private int parseRequest(int currentIndex) {

		Matcher matcher = requestPattern.matcher(templateLines.get(currentIndex));
		
		if(matcher.matches()) {
			
			int requestLine = currentIndex + 1;

			Map<String,String> headers = new HashMap<>();
			
			while(requestLine < templateLines.size()) {
				
				// if template line is a blank line then break

				if(templateLines.get(requestLine).trim().isEmpty()) {
					break;
				}

				// if template line is not a header then throw a format error

				headers.putAll(parseHeader(templateLines.get(requestLine)));
				
				++requestLine;
			}
			
			// if we ran out of input throw a format error

			if(requestLine >= templateLines.size()) {
				throw new IntegrationScriptFormatException("Invalid format for http request");
			}
			
			List<String> body = new ArrayList<>();

			++requestLine;
			
			while(requestLine < templateLines.size()) {
				
				// if template line is a blank line then break

				if(templateLines.get(requestLine).trim().isEmpty()) {
					break;
				}
				
				// add the line to the body
				
				body.add(templateLines.get(requestLine));
				
				++requestLine;
			}

			logger.debug("Headers:");
			for(Map.Entry<String,String> header : headers.entrySet()) {
				logger.debug("    {} : {}", header.getKey(), header.getValue());
			}

			String bodyValue = String.join("\n", body);
			
			logger.debug("Body:\n{}", bodyValue);

			// Build the web request

			buildWebRequestAction(matcher.group(1), matcher.group(2), headers, bodyValue, currentIndex);
			
			return requestLine - currentIndex;
		}

		return 0;
	}
	
	private Map<String,String> parseHeader(String headerLine) {
		
		Matcher matcher = headerPattern.matcher(headerLine);
		
		if(matcher.matches()) {
			
			return Collections.singletonMap(matcher.group(1), matcher.group(2));
		}
		
		throw new IntegrationScriptFormatException("Invalid format for http request header: '" + headerLine + "'");
	}

	private void buildWebRequestAction(String method, String url, Map<String,String> headers, String content, int currentIndex) {
		
		scriptActions.add(new RequestScriptAction(method, url, headers, content, currentIndex));
	}
	
	
	private String loadTestFileFromResource(String filename) {
		
		InputStream resource = getClass().getClassLoader().getResourceAsStream(filename);
		
		if(resource != null) {
			try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {

				return FileCopyUtils.copyToString(reader);
	        }
			catch (IOException ex) {

	        	return null;
	        }
		}
		
		throw new IntegrationScriptFormatException("Invalid script filename");
	}
}
