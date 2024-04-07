package io.github.mzattera.predictivepowers.anthropic.client.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.github.mzattera.predictivepowers.services.messages.Base64FilePart;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage.Author;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.predictivepowers.services.messages.FilePart.ContentType;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import io.github.mzattera.predictivepowers.services.messages.TextPart;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@Setter
@ToString
public class Message {

	private final static class MessagePartSerializer extends StdSerializer<List<MessagePart>> {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		public MessagePartSerializer() {
			this(null);
		}

		public MessagePartSerializer(Class<List<MessagePart>> t) {
			super(t);
		}

		@Override
		public void serialize(List<MessagePart> value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {

			if ((value.size() == 1) && (value.get(0) instanceof TextPart)) {
				// Single string message
				gen.writeString(value.get(0).getContent());
			} else {

				// Multi-part message

				gen.writeStartArray();

				for (MessagePart part : value) {
					gen.writeStartObject(); // Start of part

					if (part instanceof TextPart) {
						gen.writeStringField("type", "text");
						gen.writeStringField("text", ((TextPart) part).getContent());
					} else if (part instanceof FilePart) {
						FilePart file = (FilePart) part;
						if (file.getContentType() != ContentType.IMAGE)
							throw new IllegalArgumentException("Only files with content type = IMAGE are supported.");

						gen.writeStringField("type", "image");
						gen.writeObjectFieldStart("source"); // start source obj
						gen.writeStringField("type", "base64");
						gen.writeStringField("media_type", file.getMimeType());

						if (file instanceof Base64FilePart)
							gen.writeStringField("data", ((Base64FilePart) file).getEncodedContent());
						else
							gen.writeStringField("data",
									Base64.getEncoder().encodeToString(file.getInputStream().readAllBytes()));

						gen.writeEndObject(); // end source obj
					} else {
						throw new IllegalArgumentException("Unsupported part type: " + part);
					}

					gen.writeEndObject(); // end of part
				} // for each part

				gen.writeEndArray();
			}
		}
	}

	final static class MessagePartDeserializer extends JsonDeserializer<List<MessagePart>> {

		@Override
		public List<MessagePart> deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			JsonNode node = p.getCodec().readTree(p);

			List<MessagePart> parts = new ArrayList<>();
			for (JsonNode part : node) {
				String type = part.get("type").asText();
				if (!"text".equals(type))
					throw new IllegalArgumentException("Unsupported message part type: " + type);
				parts.add(new TextPart(part.get("text").asText()));
			}

			return parts;
		}

		@Override
		public List<MessagePart> getNullValue(DeserializationContext ctxt) {
			return new ArrayList<>(); // Return empty list on null
		}
	}
	
	public static enum Role {

		USER("user"), ASSISTANT("assistant");

		private final String label;

		private Role(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * Role of the message, either user or assistant. Required.
	 */
	@NonNull
	private Role role;

	/**
	 * Content of the message. Can be a single string or an array of content blocks.
	 * Required.
	 */
	@NonNull
	@Builder.Default
	@JsonSerialize(using = MessagePartSerializer.class)
	@JsonDeserialize(using = MessagePartDeserializer.class)
	private List<MessagePart> content = new ArrayList<>();

	public Message(@NonNull Author author) {
		switch (author) {
		case USER:
			role = Role.USER;
			break;
		case BOT:
			role = Role.ASSISTANT;
			break;
		default:
			throw new IllegalArgumentException();
		}

		content = new ArrayList<>(); // Sometimes default values are off with Lombok
	}

	@JsonIgnore
	public void setContent(String content) {
		this.content.clear();
		if (content != null)
			this.content.add(new TextPart(content));
	}

	public Message(String text) {
		this(Role.USER, text);
	}

	public Message(Role role, String text) {
		this.role = role;
		this.content = new ArrayList<>();
		content.add(new TextPart(text));
	}

	public Message(Role role, List<? extends MessagePart> content) {
		this.role = role;
		this.content = new ArrayList<>(content);
	}

	public Message(MessagesResponse resp) {
		this(resp.getRole(), resp.getContent());
	}

	// TODO URGENT add constructor for images
}
