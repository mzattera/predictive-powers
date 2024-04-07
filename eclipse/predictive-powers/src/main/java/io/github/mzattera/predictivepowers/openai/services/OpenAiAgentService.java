/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.mzattera.predictivepowers.openai.client.DataList;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.SortOrder;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.AgentService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link AgentService} based on OpenAI assistants.
 */
public class OpenAiAgentService implements AgentService {

	public static final String DEFAULT_MODEL = "gpt-4-turbo-preview";

	@NonNull
	@Getter
	private final OpenAiEndpoint endpoint;

	// Model used by default when creating a new agent.
	@NonNull
	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	@Override
	public List<OpenAiAssistant> listAgents() {

		List<Assistant> l = DataList.getCompleteList( //
				(last) -> endpoint.getClient().listAssistants(SortOrder.ASCENDING, null, null, last) //
		);

		return l.stream().map(a -> new OpenAiAssistant(this, a)).collect(Collectors.toList());
	}

	@Override
	public OpenAiAssistant createAgent(@NonNull String name, String description, String personality) {
		return createAgent(name, description, personality, true, false);
	}

	/**
	 * 
	 * @param name
	 * @param description
	 * @param personality
	 * @param persist     If this is true, the agent is marked such that it is not
	 *                    deleted by automated cleanup tasks (see CleanupUtil).
	 * @param isDefault   If true, this indicates this is a "default" assistant
	 *                    returned by {@link #getAgent()}.
	 * @return
	 */
	public OpenAiAssistant createAgent(@NonNull String name, String description, String personality, boolean persist,
			boolean isDefault) {

		// Mark this newly created agent such that CleanupUtil does not delete it.
		Map<String, String> metadata = new HashMap<>();
		metadata.put("_persist", Boolean.toString(persist));
		metadata.put("_isDefaultAgent", Boolean.toString(isDefault));

		Assistant assistant = endpoint.getClient().createAssistant(AssistantsRequest.builder() //
				.model(model) //
				.name(name) //
				.description(description) //
				.instructions(personality) //
				.metadata(metadata) //
				.build());

		return new OpenAiAssistant(this, assistant);
	}

	@Override
	public OpenAiAssistant getAgent() {

		for (Agent a : listAgents()) {
			OpenAiAssistant assistant = (OpenAiAssistant) a;
			if ("true".equals(assistant.getMetadata().get("_isDefaultAgent")) && model.equals(assistant.getModel()))
				return assistant;
		}

		// Create a persisted default agent for this model
		return createAgent("OpenAI Assistant [" + model + "]", //
				"This is the \"default\" assistant implemented using OpenAI " + model
						+ " model and the assistants API.", //
				"You are an helpful and polite assistant.", //
				true, true);
	}

	@Override
	public OpenAiAssistant getAgent(@NonNull String agentId) {
		return new OpenAiAssistant(this, agentId);
	}

	@Override
	public OpenAiAssistant getAgentByName(@NonNull String name) {

		for (Agent a : listAgents()) {
			OpenAiAssistant assistant = (OpenAiAssistant) a;
			if (name.equals(assistant.getName()))
				return assistant;
		}
		return null;
	}

	@Override
	public boolean deleteAgent(@NonNull String agentId) {
		return endpoint.getClient().deleteAssistant(agentId).isDeleted();
	}

	public OpenAiAgentService(@NonNull OpenAiEndpoint endpoint) {
		this(endpoint, DEFAULT_MODEL);
	}

	public OpenAiAgentService(@NonNull OpenAiEndpoint endpoint, @NonNull String model) {
		this.endpoint = endpoint;
		this.model = model;
	}

	@Override
	public void close() {
	}
}
