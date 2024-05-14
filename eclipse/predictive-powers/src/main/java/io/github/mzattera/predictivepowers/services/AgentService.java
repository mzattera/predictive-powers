/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.services;

import java.util.List;

import lombok.NonNull;

/**
 * This interface describes a service that provides {@link Agent}s to the user.
 * 
 * The interface assumes agents are persisted and shared among users. However,
 * it does not provide methods to deal with specific users; for example, it does
 * not provide methods to handle user permissions to create, modify or use
 * agents. As these features will heavily depend on the solution being
 * implemented, they are delegated to actual implementations of this interface.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public interface AgentService extends AiService {

	/**
	 * 
	 * @return All existing agents that this service can provide.
	 */
	List<? extends Agent> listAgents();

	/**
	 * Creates a new agent.
	 * 
	 * @param name        Agent name.
	 * @param description Optional description for the agent.
	 * @param personality Agent personality (instructions).
	 * 
	 * @return Newly created agent.
	 */
	Agent createAgent(@NonNull String name, String description, String personality);

	/**
	 * Creates a new agent.
	 * 
	 * @param name        Agent name.
	 * @param description Optional description for the agent.
	 * @param personality Agent personality (instructions).
	 * @param model       Model to use for the agent, if different from that set for
	 *                    this service.
	 * 
	 * @return Newly created agent.
	 */
	Agent createAgent(@NonNull String name, String description, String personality, String model);

	/**
	 * Gets the "default" agent. The purpose if this method is return an agent any
	 * user can use, avoiding the creation of a new agent each time. This in the
	 * assumption agents are persisted and can be shared between users.
	 * 
	 * @return An implementation of an agent ready to use; depending on the
	 *         implementation a new agent might be created, or an existing one
	 *         reused.
	 */
	Agent getAgent();

	/**
	 * 
	 * @param agentId
	 * @return An agent from its unique ID.
	 */
	Agent getAgent(@NonNull String agentId);

	/**
	 * 
	 * @param name
	 * @return An agent by its name; notice name might not be unique. It is up to
	 *         the implementation to return any agent or throw an exception if there
	 *         is a name conflict.
	 */
	Agent getAgentByName(@NonNull String name);

	/**
	 * Deletes an agent.
	 * 
	 * @param agentId
	 * @return True if and only if agent was successfully deleted.
	 */
	boolean deleteAgent(@NonNull String agentId);
}