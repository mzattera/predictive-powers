/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.util;

import java.util.List;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.finetunes.FineTune;

/**
 * Deletes all files and all model fine-tumes.
 * 
 * !!! USE WITH CARE !!!
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class CleanupUtil {

	public static void main(String[] args) {
		try {

			OpenAiEndpoint ep = OpenAiEndpoint.getInstance();
			OpenAiClient cli = ep.getClient();

			List<File> files = cli.listFiles();
			for (File f : files) {
				System.out.println("Deleting file: " + f.getId() + " => " + cli.deleteFile(f.getId()).isDeleted());
			}

			List<FineTune> tunes = cli.listFineTunes();
			for (FineTune t : tunes) {
				System.out.println("Deleting tuned model: " + t.getFineTunedModel() + " => "
						+ cli.deleteFineTuneModel(t.getFineTunedModel()).isDeleted());
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
