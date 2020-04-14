package com.pay360.pfref.testing.integration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;


import reactor.core.publisher.Mono;

public class RequestScriptAction implements ScriptAction {

	private static final Logger logger = LoggerFactory.getLogger(RequestScriptAction.class);

	WebClient client;

	private String method;
	private String url;
	private Map<String,String> headers;
	private String content;
	private int lineNumber;

	public RequestScriptAction(String method, String url, Map<String,String> headers, String content, int lineNumber) {

		client = WebClient.builder().filter(logRequest()).build();

		this.method = method;
		this.content = content;
		this.url = url;
		this.headers = headers;
		this.lineNumber = lineNumber;
	}


	@Override
	public int getLineNumber() {

		return lineNumber;
	}
	
	@Override
	public boolean isRequestAction() {
		return true;
	}

	@Override
	public boolean isPostRequestAction() {
		return false;
	}
	
	@Override
	public void executeAction(ScriptEvaluationContext context) {
		
		try {
	
			String processedMethod = context.substituteVariables(method).toUpperCase();
			String processedUrl = context.substituteVariables(url);
			String processedContent = context.substituteVariables(content);
			
			Map<String,String> processedHeaders = headers.entrySet().stream()
				.collect(Collectors.toMap(
						e -> context.substituteVariables(e.getKey()),
						e -> context.substituteVariables(e.getValue())));
			
			if(processedMethod.contentEquals("GET")) {
			
				// Issue the get request & set the context lastResponse value to null
				
				ClientResponse response = client.get().uri(new URI(processedUrl)).headers(headerSet -> headerSet.setAll(processedHeaders)).exchange().block();

				context.setLastResultStatus(response.statusCode());
				context.setLastResultHeaders(response.headers());
				context.setLastResultContent(response.body(BodyExtractors.toMono(String.class)).block());

				return;
			}

			if(processedMethod.contentEquals("POST" )) {
	
				// Issue the post request
	
				ClientResponse response = client.post().uri(new URI(processedUrl)).headers(headerSet -> headerSet.setAll(processedHeaders)).body(BodyInserters.fromObject(processedContent)).exchange().block();
				
				// Set the last response status, content & headers values
				
				context.setLastResultStatus(response.statusCode());
				context.setLastResultHeaders(response.headers());
				context.setLastResultContent(response.body(BodyExtractors.toMono(String.class)).block());
	
				return;
			}
	
			if(processedMethod.equals("PUT")) {
	
				// Issue the put request
				
				ClientResponse response = client.put().uri(new URI(processedUrl)).headers(headerSet -> headerSet.setAll(processedHeaders)).body(BodyInserters.fromObject(processedContent)).exchange().block();
	
				// Set the last response status, content & headers values
	
				context.setLastResultStatus(response.statusCode());
				context.setLastResultHeaders(response.headers());
				context.setLastResultContent(response.body(BodyExtractors.toMono(String.class)).block());
	
				return;
			}
			
			if(processedMethod.equals("DELETE")) {
			
				// Issue the put request

				ClientResponse response = client.delete().uri(new URI(processedUrl)).headers(headerSet -> headerSet.setAll(processedHeaders)).exchange().block();
				
				context.setLastResultStatus(response.statusCode());
				context.setLastResultHeaders(response.headers());
				context.setLastResultContent(response.body(BodyExtractors.toMono(String.class)).block());
			}
		}
		catch(URISyntaxException ex) {
			
			throw new IntegrationScriptFormatException("Bad URL format " + ex.getMessage());
		}
	}
	
    // This method returns filter function which will log request data
    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> logger.debug("    {}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }
    
    
}
