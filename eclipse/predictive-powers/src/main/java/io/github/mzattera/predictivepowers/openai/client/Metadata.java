/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client;

import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * This is an object with a set of name/values pairs.
 * 
 * It is meant mostly to be extended by classes which require metadata. 
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@ToString
public class Metadata {

	/**
	 * Set of 16 key-value pairs that can be attached to an object. This can be
	 * useful for storing additional information about the object in a structured
	 * format. Keys can be a maximum of 64 characters long and values can be a
	 * maximum of 512 characters long.
	 */
	@Builder.Default
	private Map<String, String> metadata = new HashMap<>();
}
