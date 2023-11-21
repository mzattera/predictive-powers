package io.github.mzattera.predictivepowers.openai.client.chat;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Function call that a completion model might return. This is used by the "old"
 * function calling feature (now deprecated in OpenAI API).
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class FunctionCall {

	/**
	 * Custom serializer as Arguments are returned as String, we'd like to have them
	 * as a Map....
	 */
	private static final class ArgumentsSerializer extends StdSerializer<Map<String, Object>> {

		private static final long serialVersionUID = 2127829900119652867L;

		@SuppressWarnings("unused")
		public ArgumentsSerializer() {
			this(null);
		}

		public ArgumentsSerializer(Class<Map<String, Object>> t) {
			super(t);
		}

		@Override
		public void serialize(Map<String, Object> value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {

			ObjectMapper mapper = (ObjectMapper) jgen.getCodec();
			jgen.writeString(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
		}
	}

	/**
	 * Custom de-serializer as arguments are returned as String, we'd like to have
	 * them as a Map....
	 */
	private static final class ArgumentsDeserializer extends JsonDeserializer<Map<String, Object>> {

		@Override
		public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
				throws IOException, JsonProcessingException {

			String json = jsonParser.getText();
			ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
			return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		}
	}

	/**
	 * Name of the function to call.
	 */
	@NonNull
	String name;

	/**
	 * Parameters to use in the call, as name/value pairs.
	 */
	@JsonSerialize(using = ArgumentsSerializer.class)
	@JsonDeserialize(using = ArgumentsDeserializer.class)
	Map<String, Object> arguments;
}