/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.moderations;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Moderation, as defined in the OpenAi API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ModerationResult {

	boolean flagged;
	ModerationCategories categories;
	ModerationCategoryScores categoryScores;
}
