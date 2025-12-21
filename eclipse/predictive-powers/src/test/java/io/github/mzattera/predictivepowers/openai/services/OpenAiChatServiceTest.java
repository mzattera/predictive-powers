/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

package io.github.mzattera.predictivepowers.openai.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionAudioParam;
import com.openai.models.chat.completions.ChatCompletionAudioParam.Format;
import com.openai.models.chat.completions.ChatCompletionAudioParam.Voice;
import com.openai.models.chat.completions.ChatCompletionCreateParams.Modality;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionFunctionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;

import io.github.mzattera.predictivepowers.TestConfiguration;
import io.github.mzattera.predictivepowers.examples.FunctionCallExample.GetCurrentWeatherTool;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService.OpenAiModelMetaData.CallType;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.Tool;
import io.github.mzattera.predictivepowers.services.ToolInitializationException;
import io.github.mzattera.predictivepowers.services.Toolset;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.FinishReason;
import io.github.mzattera.predictivepowers.services.messages.JsonSchema;
import io.github.mzattera.predictivepowers.services.messages.ToolCallResult;
import io.github.mzattera.predictivepowers.util.ResourceUtil;
import lombok.ToString;

/**
 * Test the OpenAI chat API & Service
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiChatServiceTest {

	// TODO add tests to check all the methods to manipulate tools -> in generic
	// agent test

	private static List<Pair<OpenAiEndpoint, String>> svcs;

	@BeforeAll
	static void init() {
		svcs = TestConfiguration.getChatServices().stream() //
				.filter(p -> p.getLeft() instanceof OpenAiEndpoint) //
				.map(p -> new ImmutablePair<OpenAiEndpoint, String>((OpenAiEndpoint) p.getLeft(), p.getRight())) //
				.collect(Collectors.toList());
	}

	@AfterAll
	static void tearDown() {
		TestConfiguration.close(svcs.stream().map(p -> p.getLeft()).collect(Collectors.toList()));
	}

	static Stream<Pair<OpenAiEndpoint, String>> services() {
		return svcs.stream();
	}

	// Reasoning services to be tested (for refusal)
	static Stream<String> reasoning() {
		List<String> l = new ArrayList<>();
		if (TestConfiguration.TEST_OPENAI_SERVICES)
			l.add("o3-mini");
		return l.stream();
	}

	// Default service (for things we need to check once)
	static Stream<String> defaultModel() {
		List<String> l = new ArrayList<>();
		if (TestConfiguration.TEST_OPENAI_SERVICES)
			l.add(OpenAiChatService.DEFAULT_MODEL);
		return l.stream();
	}

	//////////////////////////////////////////////////////////////////////
	/// HISTORY
	//////////////////////////////////////////////////////////////////////

	@ParameterizedTest
	@MethodSource("defaultModel")
	@DisplayName("Check completions not affecting history.")
	public void testHistoryComplete(String model) throws Exception {
		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiChatService cs = ep.getChatService(model);) {
			assertEquals(0, cs.getUnmodifiableHistory().size());

			// Personality
			String personality = "You are a smart and nice 	agent.";
			cs.setPersonality(personality);
			assertEquals(personality, cs.getPersonality());

			// In completion, we do not consider history, but we consider personality.
			String question = "How high is Mt.Everest?";
			ChatCompletion resp = cs.complete(question);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertEquals(0, cs.getUnmodifiableHistory().size());
			assertEquals(2, cs.getDefaultRequest().messages().size());
			assertTrue(cs.getDefaultRequest().messages().get(0).isDeveloper());
			assertEquals(personality, getText(cs.getDefaultRequest().messages().get(0)));
			assertTrue(cs.getDefaultRequest().messages().get(1).isUser());
			assertEquals(question, getText(cs.getDefaultRequest().messages().get(1)));
			assertTrue(cs.getDefaultRequest().maxCompletionTokens().isEmpty());
		}
	}

	private String getText(ChatCompletionMessageParam msg) {
		if (msg.isUser()) {
			ChatCompletionUserMessageParam m = msg.asUser();
			if (m.content().isText())
				return m.content().asText();
			else
				return m.content().asArrayOfContentParts().stream().map(p -> p.asText().text())
						.collect(Collectors.joining());
		}
		if (msg.isDeveloper()) {
			ChatCompletionDeveloperMessageParam m = msg.asDeveloper();
			if (m.content().isText())
				return m.content().asText();
			else
				return m.content().asArrayOfContentParts().stream().map(p -> p.text()).collect(Collectors.joining());
		}
		if (msg.isAssistant()) {
			ChatCompletionAssistantMessageParam m = msg.asAssistant();
			if (m.content().isEmpty())
				return null;

			if (m.content().get().isText())
				return m.content().get().asText();
			else
				return m.content().get().asArrayOfContentParts().stream().map(p -> p.asText().text())
						.collect(Collectors.joining());
		}

		throw new IllegalArgumentException(msg.toString());
	}

	@ParameterizedTest
	@MethodSource("defaultModel")
	@DisplayName("Check chat and history management.")
	@Disabled // // TODO URGENT Remove after tokenizer is fixed
	public void testHistoryChat(String model) throws Exception {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiChatService cs = ep.getChatService(model);
				OpenAiModelService modelService = ep.getModelService();) {

			// Personality, history length and conversation steps limits ////////////

			// Personality
			String personality = "You are a smart and nice agent.";
			cs.setPersonality(personality);
			assertEquals(personality, cs.getPersonality());

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.addMessageToHistory("" + i);
			}

			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(2);

			String question = "How high is Mt.Everest?";
			ChatCompletion resp = cs.chat(question);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertEquals(3, cs.getUnmodifiableHistory().size());
			assertTrue(cs.getUnmodifiableHistory().get(0).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(0)), "" + 9);
			assertTrue(cs.getUnmodifiableHistory().get(1).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(1)), question);
			assertTrue(cs.getUnmodifiableHistory().get(2).isAssistant());
			assertEquals(getText(cs.getUnmodifiableHistory().get(2)), resp.getText());
			assertEquals(3, cs.getDefaultRequest().messages().size());
			assertTrue(cs.getDefaultRequest().messages().get(0).isDeveloper());
			assertEquals(getText(cs.getDefaultRequest().messages().get(0)), personality);
			assertTrue(cs.getDefaultRequest().messages().get(1).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(1)), "" + 9);
			assertTrue(cs.getDefaultRequest().messages().get(2).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(2)), question);
			assertTrue(cs.getDefaultRequest().maxCompletionTokens().isEmpty());

			// NO personality, history length and conversation steps limits ////////////
			// Also testing maxTokens

			// Fake history
			cs.clearConversation();
			assertEquals(cs.getUnmodifiableHistory().size(), 0);
			for (int i = 0; i < 10; ++i) {
				cs.addMessageToHistory("" + i);
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(100);

			resp = cs.chat(question);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertEquals(3, cs.getUnmodifiableHistory().size());
			assertTrue(cs.getUnmodifiableHistory().get(0).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(0)), "" + 9);
			assertTrue(cs.getUnmodifiableHistory().get(1).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(1)), question);
			assertTrue(cs.getUnmodifiableHistory().get(2).isAssistant());
			assertEquals(getText(cs.getUnmodifiableHistory().get(2)), resp.getText());
			assertEquals(2, cs.getDefaultRequest().messages().size());
			assertTrue(cs.getDefaultRequest().messages().get(0).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(0)), "" + 9);
			assertTrue(cs.getDefaultRequest().messages().get(1).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(1)), question);
			assertEquals(cs.getDefaultRequest().maxCompletionTokens().orElse(0L), 100);

			// Personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.addMessageToHistory("" + i);
			}
			cs.setPersonality(personality);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(12); // this should accomodate personality and question

			resp = cs.chat(question);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertEquals(3, cs.getUnmodifiableHistory().size());
			assertTrue(cs.getUnmodifiableHistory().get(0).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(0)), "" + 9);
			assertTrue(cs.getUnmodifiableHistory().get(1).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(1)), question);
			assertTrue(cs.getUnmodifiableHistory().get(2).isAssistant());
			assertEquals(getText(cs.getUnmodifiableHistory().get(2)), resp.getText());
			assertEquals(2, cs.getDefaultRequest().messages().size());
			assertTrue(cs.getDefaultRequest().messages().get(0).isDeveloper());
			assertEquals(getText(cs.getDefaultRequest().messages().get(0)), personality);
			assertTrue(cs.getDefaultRequest().messages().get(1).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(1)), question);
			assertTrue(cs.getDefaultRequest().maxCompletionTokens().isEmpty());

			// NO personality, history length and conversation tokens limits ////////////

			// Fake history
			cs.clearConversation();
			for (int i = 0; i < 10; ++i) {
				cs.addMessageToHistory("" + i);
			}
			cs.setPersonality(null);
			cs.setMaxNewTokens(null);
			cs.setMaxHistoryLength(3);
			cs.setMaxConversationSteps(9999);
			cs.setMaxConversationTokens(12);

			resp = cs.chat(question);
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertEquals(3, cs.getUnmodifiableHistory().size());
			assertTrue(cs.getUnmodifiableHistory().get(0).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(0)), "" + 9);
			assertTrue(cs.getUnmodifiableHistory().get(1).isUser());
			assertEquals(getText(cs.getUnmodifiableHistory().get(1)), question);
			assertTrue(cs.getUnmodifiableHistory().get(2).isAssistant());
			assertEquals(getText(cs.getUnmodifiableHistory().get(2)), resp.getText());
			assertEquals(1, cs.getDefaultRequest().messages().size());
			assertTrue(cs.getDefaultRequest().messages().get(0).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(0)), question);
			assertTrue(cs.getDefaultRequest().maxCompletionTokens().isEmpty());

			// Completion with no personality
			cs.setPersonality(null);
			cs.setMaxNewTokens(null);
			resp = cs.complete(question);
			assertEquals(resp.getFinishReason(), FinishReason.COMPLETED);
			assertEquals(cs.getUnmodifiableHistory().size(), 3);
			assertEquals(1, cs.getDefaultRequest().messages().size());
			assertTrue(cs.getDefaultRequest().messages().get(0).isUser());
			assertEquals(getText(cs.getDefaultRequest().messages().get(1)), question);
			assertTrue(cs.getDefaultRequest().maxCompletionTokens().isEmpty());

		} // Close endpoint
	}

	@ParameterizedTest
	@MethodSource("defaultModel")
	@DisplayName("Check chat and history management with exception.")
	public void testHistoryException(String model) {
		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiChatService cs = ep.getChatService(model);) {

			// Personality, history length and conversation steps limits ////////////

			// Personality
			cs.setPersonality(null);

			// Fake history
			for (int i = 0; i < 10; ++i) {
				cs.addMessageToHistory("" + i);
			}
			assertEquals(10, cs.getUnmodifiableHistory().size());

			cs.setMaxHistoryLength(1);
			cs.setModel("gpt-bananas");

			String question = "How high is Mt.Everest?";

			// Should fail because context wrong model name
			assertThrows(Exception.class, () -> cs.chat(question));

			// If chat fails, history is not changed
			assertEquals(10, cs.getUnmodifiableHistory().size());
		}
	}

	/**
	 * Test removal of tool call results at beginning of conversations without
	 * corresponding calls is performed, or it will cause errors.
	 * 
	 * @throws ToolInitializationException
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings("deprecation")
	@ParameterizedTest
	@MethodSource("defaultModel")
	public void testCallResultsOnTop(String model) throws JsonProcessingException, ToolInitializationException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiChatService svc = ep.getChatService(model);
				OpenAiModelService modelSvc = ep.getModelService();) {

			svc.setModel(model);
			svc.clearConversation();
			svc.setPersonality(null);

			Tool tool = new GetCurrentWeatherTool();
			String toolId = tool.getId();
			svc.addCapability(new Toolset(List.of(tool)));

			if (modelSvc.getSupportedCallType(model) == CallType.FUNCTIONS) {
				svc.addMessageToHistory(ChatCompletionMessageParam.ofFunction( //
						ChatCompletionFunctionMessageParam.builder() //
								.content("OK") //
								.name(toolId).build() //
				));
				assertEquals(1, svc.getUnmodifiableHistory().size());

				// This will fail if history is not cleaned up properly
				ChatCompletion resp = svc.chat("Hi");
				assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			}

			if (modelSvc.getSupportedCallType(model) == CallType.TOOLS) {
				svc.addMessageToHistory(ChatCompletionMessageParam.ofTool( //
						ChatCompletionToolMessageParam.builder() //
								.content("OK") //
								.toolCallId("_too_call_123").build() //
				));
				assertEquals(1, svc.getUnmodifiableHistory().size());

				// This will fail if history is not cleaned up properly
				ChatCompletion resp = svc.chat("Hi");
				assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	/// PARALLEL CALL
	//////////////////////////////////////////////////////////////////////

	// Services allowing parallel tool calls
	static Stream<String> parallelCalls() {
		List<String> l = new ArrayList<>();
		for (Pair<OpenAiEndpoint, String> p : services().collect(Collectors.toList())) {
			String model = p.getRight();
			if ("o3-mini".equals(model))
				// We know this does not work
				continue;
			try (OpenAiModelService ms = p.getLeft().getModelService()) {
				if (ms.getSupportedCallType(model) == CallType.TOOLS)
					l.add(model);
			}
		}
		return l.stream();
	}

	@ToString
	private static class Pojo {
		@JsonProperty(required = true)
		@JsonPropertyDescription("Thios field must always be present and strictly populated with data coming from user. Do not make this up.")
		public int required;
	}

	@ParameterizedTest
	@MethodSource("parallelCalls")
	@DisplayName("Tests multiple tool calls")
	public void testParallelCalls(String model) throws ToolInitializationException, JsonProcessingException {

		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiChatService cs = ep.getChatService(model);) {

			cs.setPersonality("You are an helpful assistant.");
			cs.addCapability(new Toolset(List.of(new GetCurrentWeatherTool())));

			// This should generate a single call for 2 tools
			ChatCompletion reply = cs.chat("What is the temperature in London and Zurich?");
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertTrue(reply.hasToolCalls());
			assertEquals(2, reply.getToolCalls().size());

			// Test responding to both
			List<ToolCallResult> results = new ArrayList<>();
			results.add(new ToolCallResult(reply.getToolCalls().get(0), "10°C"));
			results.add(new ToolCallResult(reply.getToolCalls().get(1), "20°C"));
			reply = cs.chat(new ChatMessage(results));
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertTrue(reply.getText().contains("10"));
			assertTrue(reply.getText().contains("20"));
			assertEquals(5, cs.getUnmodifiableHistory().size());
		}
	}

	//////////////////////////////////////////////////////////////////////
	/// DIVERSE FILE TYPES
	//////////////////////////////////////////////////////////////////////

	@DisplayName("Test image URLs in messages.")
	@ParameterizedTest
	@MethodSource("defaultModel")
	public void testImgUrls(String model) throws MalformedURLException, URISyntaxException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint(); OpenAiChatService svc = endpoint.getChatService(model);) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("Is there any grass in this image?");
			msg.getParts().add(FilePart.fromUrl(
					"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
					"image/jpeg"));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("YES"));

			// Sends another message to show that the image is kept in history correctly
			resp = svc.chat("What material the pathway in the picture is made of?");
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("WOOD"));

		} // Close endpoint
	}

	@DisplayName("Test image in messages.")
	@ParameterizedTest
	@MethodSource("defaultModel")
	public void testImgFiles(String model) throws MalformedURLException, URISyntaxException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint(); OpenAiChatService svc = endpoint.getChatService(model);) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("Is there any grass in this image?");
			msg.getParts().add(new FilePart(
					ResourceUtil.getResourceFile("Gfp-wisconsin-madison-the-nature-boardwalk-LOW.png"), "image/png"));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("YES"));

			// Sends another message to show that the image is kept in history correctly
			resp = svc.chat("What material the pathway in the picture is made of?");
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("WOOD"));

		} // Close endpoint
	}

	@DisplayName("Test PDF in messages.")
	@ParameterizedTest
	@MethodSource("defaultModel")
	public void testPdfFiles(String model) throws MalformedURLException, URISyntaxException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint(); OpenAiChatService svc = endpoint.getChatService(model);) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("Extract the first sentence from this file:");
			msg.getParts().add(new FilePart(ResourceUtil.getResourceFile("Test.pdf")));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("ORCS"));

			// Sends another message to show that the image is kept in history correctly
			resp = svc.chat("What about the race described later?");
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("ELVES"));

		} // Close endpoint
	}

	static Stream<String> audioModels() {
		List<String> l = new ArrayList<>();
		if (TestConfiguration.TEST_OPENAI_SERVICES)
			l.add("gpt-4o-mini-audio-preview");
		return l.stream();
	}

	@DisplayName("Test audio in messages.")
	@ParameterizedTest
	@MethodSource("audioModels")
	public void testIAudioFiles(String model) throws MalformedURLException, URISyntaxException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint(); OpenAiChatService svc = endpoint.getChatService(model);) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("What does this message says?");
			msg.getParts().add(new FilePart(ResourceUtil.getResourceFile("Hello.mp3")));
			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("HELLO"));

			// Sends another message to show that the image is kept in history correctly
			resp = svc.chat("Is it robotic or human?");
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getText().toUpperCase().contains("HUMAN"));

		} // Close endpoint
	}

	@DisplayName("Test audio creation.")
	@ParameterizedTest
	@MethodSource("audioModels")
	public void testIAudioCreation(String model) throws URISyntaxException, IOException {

		try (OpenAiEndpoint endpoint = new OpenAiEndpoint(); OpenAiChatService svc = endpoint.getChatService(model);) {

			// Uses an image as input.
			ChatMessage msg = new ChatMessage("Read out loud: \"Hello Maxi\".");

			// Request audio output
			svc.setDefaultRequest(svc.getDefaultRequest().toBuilder() //
					.modalities(List.of(Modality.TEXT, Modality.AUDIO)) //
					.audio(ChatCompletionAudioParam.builder() //
							.format(Format.MP3).voice(Voice.ONYX).build()) //
					.build());

			ChatCompletion resp = svc.chat(msg);
			System.out.println(resp.getText());
			assertEquals(FinishReason.COMPLETED, resp.getFinishReason());
			assertTrue(resp.getMessage().hasFileContent(ContentType.AUDIO));
			for (FilePart part : resp.getMessage().getFileContent(ContentType.AUDIO)) {
				File tmp = File.createTempFile("audio_" + model + "_", ".mp3");
				Files.copy(part.getInputStream(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
				System.out.println(tmp.getCanonicalPath());
			}
		} // Close endpoint
	}

	//////////////////////////////////////////////////////////////////////
	/// OTHER
	//////////////////////////////////////////////////////////////////////

	@ParameterizedTest
	@MethodSource("reasoning")
	@DisplayName("Test refusal")
	public void testRefusal(String model) throws JsonProcessingException {
		try (OpenAiEndpoint ep = new OpenAiEndpoint(); OpenAiChatService cs = ep.getChatService(model);) {

			JsonSchema schema = JsonSchema.fromSchema(Pojo.class);
			cs.setPersonality("You are an agent that automates data processing. "
					+ "You will be provided by the user with some information that you must extract and output as JSON. "
					+ "Strictly use the following JSON schema when outputting the data:\n\n" //
					+ schema.asJsonSchema());
			cs.setResponseFormat(schema);

			ChatCompletion reply = cs.chat("Banana!");
			assertEquals(FinishReason.COMPLETED, reply.getFinishReason());
			assertTrue(reply.getText().contains("**The model generated a refusal**"));
		}

	}

	// we use this since it's easy to build a prompt that overflows
	static Stream<String> smallCtxModel() {
		List<String> l = new ArrayList<>();
		if (TestConfiguration.TEST_OPENAI_SERVICES) {
//			l.add("gpt-3.5-turbo");
			l.add("gpt-4-0613");
		}
		return l.stream();
	}

	@ParameterizedTest
	@MethodSource("smallCtxModel")
	@DisplayName("Test automated recovery from length limits too high")
	public void testMaxTknLimit(String model) throws Exception {

		try (OpenAiEndpoint ep = new OpenAiEndpoint();
				OpenAiChatService s = ep.getChatService(model);
				OpenAiModelService modelSvc = ep.getModelService()) {

			Tokenizer tok = modelSvc.getTokenizer(model);
			int len = modelSvc.getContextSize(model) - modelSvc.getMaxNewTokens(model) / 2;
			StringBuilder sb = new StringBuilder();
			while (tok.count(sb.toString()) < len)
				sb.append(". /");

			s.setMaxNewTokens(modelSvc.getMaxNewTokens(model) - 1);
			s.complete(sb.toString()); // If this does not error, the recovering due to context overflow works.
		}
	}
}
