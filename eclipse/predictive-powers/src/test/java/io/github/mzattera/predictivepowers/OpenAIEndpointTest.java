package io.github.mzattera.predictivepowers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OpenAIEndpointTest {

	@Test
	void test() {
		Exception exception = assertThrows(java.lang.NullPointerException.class,
				() -> OpenAiEndpoint.getInstance((String)null));
		assertEquals("apiKey is marked non-null but is null", exception.getMessage());
	}
}
