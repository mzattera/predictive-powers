/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mzattera.predictivepowers.openai.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.finetunes.FineTune;
import io.github.mzattera.predictivepowers.openai.client.models.Model;

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

			// Cancel tuning tasks
			List<FineTune> tasks = cli.listFineTunes();
			for (FineTune t : tasks) {
				String status = t.getStatus();
				if (status.equals("pending") || status.equals("running")) {
					status = cli.cancelFineTune(t.getId()).getStatus();
					System.out.println("Deleting task: " + t.getId() + " => " + status);
				}
			}

			// Delete uploaded files
			List<File> files = cli.listFiles();
			for (File f : files) {
				System.out.println("Deleting file: " + f.getId() + " => " + cli.deleteFile(f.getId()).isDeleted());
			}

			// TODO Delete fine tunes models still there...
			Set<String> models = new HashSet<>();
			for (Model m : cli.listModels()) {
				models.add(m.getId());
			}
			List<FineTune> tunes = cli.listFineTunes();
			for (FineTune t : tunes) {
				if (t.getFineTunedModel() == null)
					continue;

				if (models.contains(t.getFineTunedModel())) {
					System.out.println("Deleting tuned model: " + t.getFineTunedModel() + " => "
							+ cli.deleteFineTuneModel(t.getFineTunedModel()).isDeleted());
				} else {
					System.out.println("		Tuned model: " + t.getFineTunedModel() + " deleteed already.");
				}
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}
