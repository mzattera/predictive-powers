/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.moderations;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response provided by /moderations OpenAi API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class ModerationsResponse {

    String id;
    String model;
	List<ModerationResult> results;
}
