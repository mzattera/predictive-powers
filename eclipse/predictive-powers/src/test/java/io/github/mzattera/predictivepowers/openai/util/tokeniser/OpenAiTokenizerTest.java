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

package io.github.mzattera.predictivepowers.openai.util.tokeniser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiModelService;

/**
 * Test for OpenAI tokenizer.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class OpenAiTokenizerTest {

	private final static OpenAiModelService svc = (new OpenAiEndpoint()).getModelService();

	@Test
	public void test01() throws JsonMappingException, JsonProcessingException {

		String json = "{\r\n"
				+ "  \"model\" : \"gpt-3.5-turbo-0613\",\r\n" + "  \"messages\" : [ {\r\n"
				+ "    \"role\" : \"system\",\r\n"
				+ "    \"content\" : \"You are an agent supporting user with weather forecasts\"\r\n" + "  }, {\r\n"
				+ "    \"role\" : \"user\",\r\n" + "    \"content\" : \"Where is Dallas TX?\"\r\n" + "  } ],\r\n"
				+ "  \"stream\" : false,\r\n" + "  \"max_tokens\" : 2048\r\n" + "}";
		ChatCompletionsRequest req = OpenAiClient.JSON_MAPPER.readValue(json, ChatCompletionsRequest.class);
		assertEquals(json, OpenAiClient.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(req));
		assertEquals(25, svc.getTokenizer(req.getModel()).count(req.getMessages()));
		
	}

	@Test
	public void test02() throws JsonMappingException, JsonProcessingException {

		String json = "{\r\n"
				+ "  \"model\" : \"gpt-3.5-turbo-0613\",\r\n"
				+ "  \"messages\" : [ {\r\n"
				+ "    \"role\" : \"system\",\r\n"
				+ "    \"content\" : \"You are an agent supporting user with weather forecasts\"\r\n"
				+ "  }, {\r\n"
				+ "    \"role\" : \"user\",\r\n"
				+ "    \"content\" : \"Where is Dallas TX?\"\r\n"
				+ "  }, {\r\n"
				+ "    \"role\" : \"assistant\",\r\n"
				+ "    \"content\" : \"Dallas, TX is located in the southern part of the United States. It is the third-largest city in Texas and the ninth-largest city in the United States. Dallas is located in North Texas, about 250 miles north of the Gulf of Mexico.\"\r\n"
				+ "  }, {\r\n"
				+ "    \"role\" : \"user\",\r\n"
				+ "    \"content\" : \"How is the weather like there?\"\r\n"
				+ "  } ],\r\n"
				+ "  \"functions\" : [ {\r\n"
				+ "    \"name\" : \"get_current_weather\",\r\n"
				+ "    \"description\" : \"Get the current weather in a given location.\",\r\n"
				+ "    \"parameters\" : {\r\n"
				+ "      \"$schema\" : \"http://json-schema.org/draft-04/schema#\",\r\n"
				+ "      \"title\" : \"Get Current Weather Parameters\",\r\n"
				+ "      \"type\" : \"object\",\r\n"
				+ "      \"additionalProperties\" : false,\r\n"
				+ "      \"properties\" : {\r\n"
				+ "        \"location\" : {\r\n"
				+ "          \"type\" : \"string\",\r\n"
				+ "          \"description\" : \"The city and state, e.g. San Francisco, CA\"\r\n"
				+ "        },\r\n"
				+ "        \"unit\" : {\r\n"
				+ "          \"type\" : \"string\",\r\n"
				+ "          \"enum\" : [ \"CELSIUS\", \"FARENHEIT\" ],\r\n"
				+ "          \"description\" : \"Temperature unit (Celsius or Farenheit). This is optional.\"\r\n"
				+ "        },\r\n"
				+ "        \"fooParameter\" : {\r\n"
				+ "          \"type\" : \"integer\"\r\n"
				+ "        },\r\n"
				+ "        \"code\" : {\r\n"
				+ "          \"type\" : \"integer\",\r\n"
				+ "          \"description\" : \"Unique API code, this is an integer which must be passed and it is always equal to 6.\"\r\n"
				+ "        }\r\n"
				+ "      },\r\n"
				+ "      \"required\" : [ \"location\", \"code\" ]\r\n"
				+ "    }\r\n"
				+ "  } ],\r\n"
				+ "  \"stream\" : false,\r\n"
				+ "  \"max_tokens\" : 2048\r\n"
				+ "}";
		ChatCompletionsRequest req = OpenAiClient.JSON_MAPPER.readValue(json, ChatCompletionsRequest.class);
		assertEquals(json, OpenAiClient.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(req));
		assertEquals(25, svc.getTokenizer(req.getModel()).count(req.getMessages()));
		
	}
}
