/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.moderations;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Scores for categories in a moderation.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ModerationCategoryScores {


	double hate;

	@JsonProperty("hate/threatening")
	double hateThreatening;

	@JsonProperty("self-harm")
	double selfHarm;

	double sexual;

	@JsonProperty("sexual/minors")
	double sexualMinors;

	double violence;

	@JsonProperty("violence/graphic")
	double violenceGraphic;
}
