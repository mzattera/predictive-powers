package io.github.mzattera.predictivepowers.anthropic.client.messages;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.github.mzattera.predictivepowers.anthropic.client.Usage;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message.MessagePartDeserializer;
import io.github.mzattera.predictivepowers.anthropic.client.messages.Message.Role;
import io.github.mzattera.predictivepowers.services.messages.MessagePart;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class MessagesResponse {

	/**
	 * Unique object identifier. Required.
	 */
	@NonNull
	private String id;

	/**
	 * Object type. For Messages, this is always "message". Required.
	 */
	@NonNull
	private String type;

	/**
	 * Conversational role of the generated message. This will always be
	 * "assistant". Required.
	 */
	@NonNull
	private Role role;

	/**
	 * Content generated by the model. This is an array of content blocks. Required.
	 */
	@NonNull
	@Builder.Default
	@JsonDeserialize(using = MessagePartDeserializer.class)
	private List<MessagePart> content = new ArrayList<>();

	/**
	 * The model that handled the request. Required.
	 */
	@NonNull
	private String model;

	/**
	 * The reason that we stopped. This may be "end_turn", "max_tokens", or
	 * "stop_sequence". Required.
	 */
	@NonNull
	private String stopReason;

	/**
	 * Which custom stop sequence was generated, if any. Required.
	 */
	private String stopSequence;

	/**
	 * Billing and rate-limit usage information. Required.
	 */
	@NonNull
	private Usage usage;
}
