package io.github.mzattera.predictivepowers.openai.client;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.NonNull;

public enum SortOrder {

	ASCENDING("asc"), DESCENDING("desc");

	private final @NonNull String label;

	private SortOrder(@NonNull String label) {
		this.label = label;
	}

	@Override
	@JsonValue
	public String toString() {
		return label;
	}
}