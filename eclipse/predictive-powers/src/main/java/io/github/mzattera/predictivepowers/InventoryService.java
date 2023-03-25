/**
 * 
 */
package io.github.mzattera.predictivepowers;

import java.util.List;

import io.github.mzattera.predictivepowers.client.openai.models.Model;
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
		return ep.getClient().models();
	}

	public Model retrieveModel(String modelId) {
		return ep.getClient().models(modelId);
	}

}
