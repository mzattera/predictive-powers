package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.Metadata;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a conversation thread with an OpenAI assistant.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class ThreadsRequest extends Metadata {

	@Builder.Default
	private List<Message> messages = new ArrayList<>();
}
