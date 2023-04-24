/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.finetunes;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Fine-tunes hyper parameters, as defined by OpenAi API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class Hyperparams {

	int batchSize;
	double learningRateMultiplier;
	int nEpochs;
	double promptLossWeight;
}
