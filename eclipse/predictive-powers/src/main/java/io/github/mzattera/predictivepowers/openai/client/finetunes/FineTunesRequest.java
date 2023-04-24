/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.finetunes;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Request for fine-tunes OpenAi API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class FineTunesRequest {

	@NonNull
	String trainingFile;
	
	String validationFile;
	String model;
	Integer nEpochs;
	Integer batchSize;
	Double learningRateMultiplier;
	Double promptLossWeight;
	Boolean computeClassificationMetrics;
	Integer classificationNClasses;
	String classificationPositiveClass;
	List<Double> classificationBetas;
	String suffix;
}
