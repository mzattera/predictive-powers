/*
 * Copyright 2024 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.services;

import java.util.List;
import java.util.stream.Collectors;

import com.openai.core.JsonValue;
import com.openai.models.beta.assistants.Assistant;
import com.openai.models.beta.assistants.AssistantCreateParams;
import com.openai.models.beta.assistants.AssistantCreateParams.Metadata;

import io.github.mzattera.predictivepowers.EndpointException;
import io.github.mzattera.predictivepowers.openai.util.OpenAiUtil;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.AgentService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link AgentService} based on OpenAI Assistants API.
 */
public class OpenAiAgentService implements AgentService {

	public static final String DEFAULT_MODEL = OpenAiChatService.DEFAULT_MODEL;

	@NonNull
	@Getter
	private final OpenAiEndpoint endpoint;

	// Model used by default when creating a new agent.
	@NonNull
	@Getter
	@Setter
	private String model = DEFAULT_MODEL;

	public OpenAiAgentService(@NonNull OpenAiEndpoint endpoint) {
		this(endpoint, DEFAULT_MODEL);
	}

	public OpenAiAgentService(@NonNull OpenAiEndpoint endpoint, @NonNull String model) {
		this.endpoint = endpoint;
		this.model = model;
	}

	@Override
	public List<String> getAgentIDs() throws EndpointException {

		try {
			return endpoint.getClient().beta().assistants().list().autoPager().stream().map(a -> a.id())
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public OpenAiAssistant createAgent(@NonNull String name, String description, String personality) {
		return createAgent(name, description, this.model, personality, true, false);
	}

	@Override
	public Agent createAgent(@NonNull String name, String description, String personality, String model) {
		return createAgent(name, description, model, personality, true, false);
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
		return createAgent(name, description, this.model, personality, true, false);
	}

	/**
	 * 
	 * @param name
	 * @param description
	 * @param personality
	 * @param model       Model to use for the agent. If this is not null, it will
	 *                    override the model for this service.
	 * @param persist     If this is true, the agent is marked such that it is not
	 *                    deleted by automated cleanup tasks (see CleanupUtil).
	 * @param isDefault   If true, this indicates this is a "default" assistant
	 *                    returned by {@link #getAgent()}.
	 * @return
	 */
	public OpenAiAssistant createAgent(@NonNull String name, String description, String model, String personality,
			boolean persist, boolean isDefault) throws EndpointException {

		try {
			// Mark this newly created agent such that CleanupUtil does not delete it.
			Metadata metadata = AssistantCreateParams.Metadata.builder() //
					.putAdditionalProperty("_persist", JsonValue.from(persist ? "true" : "false")) //
					.putAdditionalProperty("_isDefaultAgent", JsonValue.from(isDefault ? "true" : "false")).build();

			Assistant assistant = endpoint.getClient().beta().assistants().create(AssistantCreateParams.builder() //
					.model(model == null ? this.model : model) //
					.name(name) //
					.description(description) //
					.instructions(personality) //
					.metadata(metadata).build());

			return new OpenAiAssistant(this, assistant.id());
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public OpenAiAssistant getAgent() throws EndpointException {

		try {
			for (String id : getAgentIDs()) {
				OpenAiAssistant assistant = getAgent(id);
				if (assistant == null) // paranoid
					continue;

				// More paranoid guards
				JsonValue def = assistant.getAssistantData().metadata().orElse(Assistant.Metadata.builder().build()) //
						._additionalProperties().get("_isDefaultAgent");
				boolean isDefault = (def.isMissing() || def.isNull()) ? false : "true".equals(def.toString());

				if (isDefault && model.equals(assistant.getModel()))
					return assistant;
				else
					assistant.close();
				;
			}

			// Create a persisted default agent for this model
			return createAgent("OpenAI Assistant [" + model + "]", //
					"This is the \"default\" assistant implemented using OpenAI " + model
							+ " model and the assistants API.", //
					"You are an helpful and polite assistant.", //
					true, true);
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public OpenAiAssistant getAgent(@NonNull String agentId) throws EndpointException {
		try {
			return new OpenAiAssistant(this, agentId);
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public boolean deleteAgent(@NonNull String agentId) throws EndpointException {
		try {
			return endpoint.getClient().beta().assistants().delete(agentId)._deleted().asBoolean().get();
		} catch (Exception e) {
			throw OpenAiUtil.toEndpointException(e);
		}
	}

	@Override
	public void close() {
	}
}
