/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Requirted action to progress a Run in the Runs API.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class RequiredAction {

	public enum Type {
		SUBMIT_TOOL_OUTPUTS("submit_tool_outputs");

		private final String label;

		private Type(String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	@NoArgsConstructor
	@RequiredArgsConstructor
	@Builder
	@Getter
	@Setter
	@ToString
	public static final class SubmitToolOutputs {

		@Builder.Default
		@NonNull
		private List<OpenAiToolCall> toolCalls = new ArrayList<>();
	}

	@NonNull
	private Type type;

	@NonNull
	private SubmitToolOutputs submitToolOutputs;
}
