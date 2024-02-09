/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.chat.OpenAiToolCall;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class RunStepDetail {

	@NonNull
	private RunStep.Type type;

	private MessageCreationRunStep messageCreation;

	List<OpenAiToolCall> toolCalls;
}
