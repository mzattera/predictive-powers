package io.github.mzattera.predictivepowers.openai.client.chat;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tool call that a completion model might return. This is used by the new
 * parallel function calling capability.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
//	@AllArgsConstructor
@ToString
public class ToolCall {

	public enum Type {
		FUNCTION("function");

		private final @NonNull String label;

		private Type(@NonNull String label) {
			this.label = label;
		}

		@Override
		@JsonValue
		public String toString() {
			return label;
		}
	}

	/**
	 * The ID of the tool call.
	 */
	@NonNull
	String Id;

	/**
	 * The type of the tool. Currently, only function is supported.
	 */
	@NonNull
	Type type;

	/**
	 * Name of the function to call.
	 */
	@NonNull
	FunctionCall function;

	public ToolCall(FunctionCall call) {
		this("fake_id", Type.FUNCTION, call);
	}
}
