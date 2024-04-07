package io.github.mzattera.predictivepowers.anthropic.client;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class Usage {

	/**
	 * The number of input tokens which were used. Required.
	 */
	private int inputTokens;

	/**
	 * The number of output tokens which were used. Required.
	 */
	private int outputTokens;
}
