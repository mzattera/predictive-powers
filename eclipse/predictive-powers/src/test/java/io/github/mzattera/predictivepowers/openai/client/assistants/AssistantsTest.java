/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.assistants;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.openai.client.DataList;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.SortOrder;
import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiTool;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.Agent;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.messages.ToolCall;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.util.ResourceUtil;
import lombok.NonNull;

/**
 * Tests for agents API.
 */
public class AssistantsTest {

	public static final String DEFAULT_MODEL = "gpt-4-turbo-preview";

	/**
	 * Tests creating agents.
	 */
	@Test
	void testCreateAssistant() {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			// Test creation
			AssistantsRequest original = createAssistantRequest("uno");
			Assistant copy = ep.getClient().createAssistant(original);

			// Verify field by field that the two instances match
			testAgentMatch(original, copy);
			
			// test modify and retrieve
			original = createAssistantRequest("two");
			copy = ep.getClient().createAssistant(original);
			testAgentMatch(original, ep.getClient().retrieveAssistant(copy.getId()));			
		}
	}

	private void testAgentMatch(AssistantsRequest original, Assistant copy) {
		assertNotNull(copy.getId());
		assertEquals("assistant", copy.getObject());
		assertNotNull(copy.getCreatedAt());
		assertEquals(original.getModel(), copy.getModel());
		assertEquals(original.getName(), copy.getName());
		assertEquals(original.getDescription(), copy.getDescription());
		assertEquals(original.getInstructions(), copy.getInstructions());
		assertNotNull(copy.getTools());
		assertEquals(original.getTools().size(), copy.getTools().size());
		for (int i = 0; i < original.getTools().size(); ++i)
			assertEquals(original.getTools().get(i).getId(), copy.getTools().get(i).getId());
		assertNotNull(copy.getFileIds());
		assertEquals(original.getFileIds().size(), copy.getFileIds().size());
		for (int i = 0; i < original.getFileIds().size(); ++i)
			assertEquals(original.getFileIds().get(i), copy.getFileIds().get(i));
		assertNotNull(copy.getMetadata());
		assertEquals(original.getMetadata().size(), copy.getMetadata().size());
		for (Entry<String, String> e : original.getMetadata().entrySet())
			assertEquals(e.getValue(), copy.getMetadata().get(e.getKey()));
	}

	/**
	 * Create unique assistant based on values of s.
	 * 
	 * @param s
	 * @return
	 */
	private static AssistantsRequest createAssistantRequest(String s) {

		List<OpenAiTool> tools = new ArrayList<>();
		tools.add(OpenAiTool.CODE_INTERPRETER);
		tools.add(OpenAiTool.RETRIEVAL);
		tools.add(new OpenAiTool(new Tool() {

			@Override
			public String getId() {
				return "aTool";
			}

			@Override
			public String getDescription() {
				return "Tool Description";
			}

			@Override
			public Class<?> getParameterSchema() {
				return Tool.NoParameters.class;
			}

			@Override
			public void init(@NonNull Agent agent) throws ToolInitializationException {
			}

			@Override
			public ToolCallResult invoke(@NonNull ToolCall call) throws Exception {
				return null;
			}

			@Override
			public void close() throws Exception {
			}
		}));

		Map<String, String> meta = new HashMap<>();
		meta.put("value", s);

		return AssistantsRequest.builder() //
				.model(DEFAULT_MODEL) //
				.name(s) //
				.description("Assistant created using seed " + s) //
				.instructions("Some instructions using seed " + s) //
				.tools(tools) //
				.fileIds(new ArrayList<>()) //
				.metadata(meta) //
				.build();
	}

	/**
	 * Tests listing and deleting agents.
	 */
	@Test
	void testListAndDeleteAgents() {

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiClient cli = ep.getClient();
			DataList<Assistant> list;
			List<Assistant> agents;

			// Get ID of last agent we created until now
			String last = null;
			list = cli.listAssistants(SortOrder.DESCENDING, 1, null, null);
			assertNotNull(list);
			agents = list.getData();
			assertNotNull(agents);
			assertTrue(agents.size() <= 1);
			if (agents.size() == 1)
				last = agents.get(0).getId();

			// Create 3 agents
			List<String> agentIds = new ArrayList<>();
			for (int i = 0; i < 3; ++i) {
				agentIds.add(createAgent(ep, i + 100).getId());
			}

			list = cli.listAssistants(SortOrder.ASCENDING, //
					agentIds.size() + 5, //
					null, //
					agentIds.get(0));
			assertNotNull(list);
			list(list);
			assertFalse(list.hasMore());
			agents = list.getData();
			assertNotNull(agents);
			assertEquals(agentIds.size() - 1, agents.size());
			assertEquals(agentIds.get(agentIds.size() - 1), agents.get(agents.size() - 1).getId());

			list = cli.listAssistants(SortOrder.DESCENDING, //
					agentIds.size() - 1, //
					null, agentIds.get(agentIds.size() - 1));
			assertNotNull(list);
			list(list);
			assertTrue(list.hasMore());
			agents = list.getData();
			assertNotNull(agents);
			assertEquals(agentIds.size() - 1, agents.size()); // It fetches agents created before the test as well
			assertEquals(agentIds.get(0), agents.get(agents.size() - 1).getId());

			// Notice when listing, pagination does not go backjward, it starts from the
			// beginning always
			agents = fetchAll(ep, SortOrder.ASCENDING, //
					agentIds.get(agentIds.size() - 1), //
					null);
			assertNotNull(agents);
			list(agents);
			assertEquals(agentIds.get(0), agents.get(agents.size() - 2).getId());

			list = cli.listAssistants(SortOrder.ASCENDING, //
					1, //
					null, //
					agentIds.get(0));
			assertNotNull(list);
			list(list);
			agents = list.getData();
			assertNotNull(agents);
			assertEquals(1, agents.size());
			assertEquals(agentIds.get(1), agents.get(0).getId());

			// Check we can delete the 3 agents and go back to last
			for (String id : agentIds) {
				assertTrue(cli.deleteAssistant(id).isDeleted());
			}
			list = cli.listAssistants(SortOrder.DESCENDING, 1, null, null);
			assertNotNull(list);
			agents = list.getData();
			assertNotNull(agents);
			assertTrue(agents.size() <= 1);
			if (agents.size() == 1)
				assertEquals(last, agents.get(0).getId());
			else
				assertNull(last);
		}
	}

	private Assistant createAgent(OpenAiEndpoint ep, int i) {
		AssistantsRequest req = createAssistantRequest("assistant number XOX " + i);
		return ep.getClient().createAssistant(req);
	}

	/**
	 * Fetches ALL agents given some constraints. This is because before+limit do
	 * not go back from before that many steps, but fetch all data since beginning
	 * of time.
	 * 
	 * @param order
	 * @param limit
	 * @param before
	 * @param after
	 * @return
	 */
	private List<Assistant> fetchAll(OpenAiEndpoint ep, SortOrder order, String before, String after) {

		List<Assistant> result = new ArrayList<>();

		DataList<Assistant> l = ep.getClient().listAssistants(order, 20, before, after);
		if (l.getData().size() == 0)
			return result;

		while (true) {
			result.addAll(l.getData());
			if (l.hasMore())
				l = ep.getClient().listAssistants(order, 20, before, result.get(result.size() - 1).getId());
			else
				break;
		}
		return result;
	}

	private void list(DataList<Assistant> l) {
		list(l.getData());
		System.out.println("Has More: " + l.hasMore());
	}

	private void list(List<Assistant> l) {
		System.out.println();
		for (int i = 0; i < l.size(); ++i) {
			Assistant a = l.get(i);
			System.out.println(i + "\t" + a.getId() + "\t" + a.getName() + "\t" + a.getCreatedAt());
		}
	}
	
	@Test
	void testFiles() throws IOException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiClient cli = ep.getClient();
			
			File f = cli.uploadFile(ResourceUtil.getResourceFile("banana.txt"), "assistants");
			Assistant agent = ep.getClient().createAssistant(createAssistantRequest("file-test"));
			File af = cli.createAssistantFile(agent.getId(), f.getId());
			assertNotNull(af);
			assertEquals(f.getId(), af.getId());
			
			// TODO add pagination tests
			
			List<File> files = cli.listAssistantFiles(agent.getId()).getData();
			assertNotNull(files);
			assertEquals(1,files.size());
			
			af = cli.retrieveAssistantFile(agent.getId(), f.getId());
			assertNotNull(af);
			assertEquals(f.getId(), af.getId());

			cli.deteAssistantFile(agent.getId(), f.getId());
			files = cli.listAssistantFiles(agent.getId()).getData();
			assertNotNull(files);
			assertEquals(0,files.size());
		}
	}
}
