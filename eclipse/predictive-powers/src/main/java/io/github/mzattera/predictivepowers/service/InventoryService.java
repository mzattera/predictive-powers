/**
 * 
 */
package io.github.mzattera.predictivepowers.service;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A class that exposes model inventory services.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@RequiredArgsConstructor
public class InventoryService {

	@NonNull
	private final OpenAiEndpoint ep;

	public Model[] listModels() {
		return ep.getClient().listModels();
	}

	public Model retrieveModel(String modelId) {
		return ep.getClient().retrieveModel(modelId);
	}

}
