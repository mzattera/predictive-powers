package io.github.mzattera.predictivepowers.examples;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.CompletionService;

public class DefaultConfigurationExample {

	public static void main(String[] args) {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint()) {

			CompletionService cs = endpoint.getCompletionService();

			// Set "model" parameter in default request, this will affect all further calls
			cs.getDefaultReq().setModel("text-curie-001");

			// this call now uses text-curie-001 model
			System.out.println(cs.complete("Alan Turing was").getText());
			
		} // closes endpoint
	}
}
