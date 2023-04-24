package io.github.mzattera.predictivepowers.openai.client.moderations;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Categories in a moderation.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ModerationCategories {

	boolean hate;

	@JsonProperty("hate/threatening")
	boolean hateThreatening;

	@JsonProperty("self-harm")
	boolean selfHarm;

	boolean sexual;

	@JsonProperty("sexual/minors")
	boolean sexualMinors;

	boolean violence;

	@JsonProperty("violence/graphic")
	boolean violenceGraphic;
}
