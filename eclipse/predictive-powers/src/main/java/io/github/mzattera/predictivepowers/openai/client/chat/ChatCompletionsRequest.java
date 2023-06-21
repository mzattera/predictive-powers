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
package io.github.mzattera.predictivepowers.openai.client.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest.FunctionCallSetting.FunctionCallSerializer;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameters for a request to /chat/completions API.
 * 
 * @author Massmiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class ChatCompletionsRequest {

	/**
	 * Possible response options for a model when function calling is available.
	 */
	private enum FunctionCallMode {
		/** The model does not call a function. */
		NONE,

		/** The model can pick between an end-user or calling a function */
		AUTO,

		/** The model will call a function */
		FUNCTION
	}

	/**
	 * Instruct model whether to produce function calls or not.
	 */
	@Getter
	@Setter
	@Builder
	@ToString
	@JsonSerialize(using = FunctionCallSerializer.class)
	public static class FunctionCallSetting {

		/**
		 * Provides custom serialization.
		 */
		static final class FunctionCallSerializer extends StdSerializer<FunctionCallSetting> {

			private static final long serialVersionUID = -4506958348962250647L;

			public FunctionCallSerializer() {
				this(null);
			}

			public FunctionCallSerializer(Class<FunctionCallSetting> t) {
				super(t);
			}

			@Override
			public void serialize(FunctionCallSetting value, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {

				switch (value.getMode()) {
				case NONE:
					jgen.writeString("none");
					break;
				case AUTO:
					jgen.writeString("auto");
					break;
				case FUNCTION:
					jgen.writeStartObject();
					jgen.writeStringField("name", value.getName());
					jgen.writeEndObject();
					break;
				default:
					throw new IllegalArgumentException();
				}
			}
		}

		/** USe this to indicate function_call = none */
		public final static FunctionCallSetting NONE = new FunctionCallSetting(FunctionCallMode.NONE, null);

		/** USe this to indicate function_call = auto */
		public final static FunctionCallSetting AUTO = new FunctionCallSetting(FunctionCallMode.AUTO, null);

		/** How should the model handle function call options. */
		@NonNull
		final FunctionCallMode mode;

		final String name;

		/**
		 * Use this to indicate function_call = {"name": "my_function"}
		 * 
		 * @param name
		 */
		public FunctionCallSetting(String my_function) {
			this(FunctionCallMode.FUNCTION, my_function);
		}

		private FunctionCallSetting(FunctionCallMode mode, String name) {
			this.mode = mode;
			this.name = name;
		}
	}

	@NonNull
	String model;

	@NonNull
	@Builder.Default
	List<ChatMessage> messages = new ArrayList<>();

	/**
	 * Notice: it seems there is HTTP 400 error if this is an empty list.
	 */
	List<Function> functions;

	FunctionCallSetting functionCall;

	Double temperature;
	Double topP;
	Integer n;

	// TODO: Add support for streaming input at least in direct API calls, if so
	// make sure services do not stream
	final boolean stream = false;

	List<String> stop;

	/**
	 * Higher-level functions in the library will try to calculate this
	 * automatically if it is null when submitting a request.
	 */
	Integer maxTokens;

	Double presencePenalty;
	Double frequencyPenalty;
	Map<String, Integer> logitBias;
	String user;

	public enum Degrees {
		celsius, fahrenheit
	};

	public static class CreatePersonParameters {

		@JsonProperty(required = true)
		public String name;

		public Integer age;
		public double weight;
		public Degrees temperature;
	}

	public static class GetCurrentWeatherParameters {

		@JsonProperty(required = true)
		@JsonPropertyDescription("The city and state, e.g. San Francisco, CA")
		public String location;

		public Degrees unit;

		@JsonPropertyDescription("Unique API code, this is an integer which must be passed and it is always equal to 6.")
		public int code;
	}

	public static void main(String[] args) throws JsonProcessingException {

//		Function f = Function.builder().name("createPerson").description("A test for now")
//				.parameters(CreatePersonParameters.class).build();
		Function f = Function.builder().name("createPerson").description("A test for now")
				.parameters(GetCurrentWeatherParameters.class).build();

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(f);
		System.out.println(json);

		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(FunctionCallSetting.NONE);
		System.out.println(json);

		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new FunctionCallSetting("banane"));
		System.out.println(json);

		ChatMessage.FunctionCall fc = ChatMessage.FunctionCall.builder().name("testCall").build();
		Map<String, Object> ar = new HashMap<>();
		ar.put("name", "pippo");
		ar.put("value", 6);
		fc.setArguments(ar);
		json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fc);
		System.out.println(json);

//		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
//			OpenAiChatService cs = ep.getChatService();
//
//			List<ChatMessage> msgs = new ArrayList<>();
//			msgs.add(ChatMessage.builder().role(Role.USER).content("Hi!").build());
//			String resp = cs.complete(msgs).getText();
//			System.out.println(resp);
//		}

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {

			ChatCompletionsRequest req = new ChatCompletionsRequest();
			req.setModel("gpt-3.5-turbo-0613");

			List<ChatMessage> msgs = new ArrayList<>();
			msgs.add(ChatMessage.builder().role(Role.USER)
					.content("How is the weather like in Dallas in Celsius units?").build());
			req.setMessages(msgs);

			List<Function> fl = new ArrayList<>();
			fl.add(Function.builder().name("get_current_weather")
					.description("Get the current weather in a given location")
					.parameters(GetCurrentWeatherParameters.class).build());
			req.setFunctions(fl);

			System.out.println(req.toString());

//			OpenAiChatService cs = ep.getChatService();
//			String resp = cs.complete(msgs).getText();

			ChatCompletionsResponse resp = ep.getClient().createChatCompletion(req);

			System.out.println(resp.toString());
			for (Entry<String, Object> e : resp.getChoices().get(0).getMessage().getFunctionCall().getArguments()
					.entrySet()) {
				System.out
						.println(e.getKey() + ": " + e.getValue() + " - " + e.getValue().getClass().getCanonicalName());
			}
		}
	}
}
