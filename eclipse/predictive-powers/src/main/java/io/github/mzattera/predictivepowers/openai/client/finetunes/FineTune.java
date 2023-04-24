/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.finetunes;

import java.util.List;

import io.github.mzattera.predictivepowers.openai.client.files.File;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A fine-tunes, as represented by OpenAi API.
 *  
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public class FineTune {

    String id;
    String object;
    String model;
    long createdAt;
    List<FineTuneEvent> events;
    String fineTunedModel;
    Hyperparams hyperparams;
    String organizationId;
    List<File> resultFiles;
    String status;
    List<File> validationFiles;
	List<File> trainingFiles;
    long updatedAt;
}
