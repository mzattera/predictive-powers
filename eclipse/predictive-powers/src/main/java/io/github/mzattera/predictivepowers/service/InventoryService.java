/**
 * 
 */
package io.github.mzattera.predictivepowers.service;

import java.util.List;

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
	
	@NonNull private final OpenAiEndpoint ep;
	
	public List<Model> listModels() {
		return ep.getClient().listModels().getData();
	}

	public Model retrieveModel(String modelId) {
		return ep.getClient().retrieveModel(modelId);
	}

}
