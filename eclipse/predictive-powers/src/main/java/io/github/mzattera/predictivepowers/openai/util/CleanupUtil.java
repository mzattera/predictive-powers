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

import java.util.List;
import java.util.Scanner;

import io.github.mzattera.predictivepowers.openai.client.DataList;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.client.SortOrder;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningJob;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;

/**
 * Deletes all files and all model fine-tunes.
 * 
 * !!! USE WITH CARE !!!
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public class CleanupUtil {

	public static void main(String[] args) {

		try (Scanner console = new Scanner(System.in)) {

			System.out.print(
					"*** You are going to delete *all* ASSISTANTS not marked as permanent, uploaded FILES, cancel FINE TUNING JOBS, and FINE TUNED MODELS from your account !!! ***\n");
			System.out.print("Type \"yes\" to continue: ");
			if (!console.nextLine().equals("yes")) {
				System.out.print("Aborted.");
				System.exit(-1);
			}
		}

		try (OpenAiEndpoint ep = new OpenAiEndpoint()) {
			OpenAiClient cli = ep.getClient();

			// Cancel tuning tasks
			System.out.println("Cancelling FineTunes...");
			List<FineTuningJob> tasks = DataList.getCompleteList((last) -> cli.listFineTuningJobs(null, last));
			for (FineTuningJob task : tasks) {
				String status = task.getStatus();
				if (status.equals("pending") || status.equals("running")) {
					status = cli.cancelFineTuning(task.getId()).getStatus();
					System.out.println("Cancelling task: " + task.getId() + " => " + status);
				} else {
					System.out.println("\tTask cannot be cancelled: " + task.getId() + " => " + status);
				}
			}

			// Delete fine tunes models
			System.out.println("Deleting Models...");
			for (Model m : cli.listModels()) {
				if (!"openai".equals(m.getOwnedBy()) && !"openai-internal".equals(m.getOwnedBy())
						&& !"system".equals(m.getOwnedBy())
				) // custom model
					System.out.println("Deleting fine tuned model: " + m.getId() + " => "
							+ cli.deleteFineTunedModel(m.getId()).isDeleted());
			}

			// Here one should delete threads and runs, but there is no way to do it in the
			// API (get full list)

			// Delete assistants: assistants file are cascaded deleted
			System.out.println("Deleting Assistants...");
			List<Assistant> l = DataList
					.getCompleteList((last) -> cli.listAssistants(SortOrder.ASCENDING, null, null, last));
			for (Assistant a : l) {
				System.out.println(
						"Deleting assistant: " + a.getId() + " => " + cli.deleteAssistant(a.getId()).isDeleted());
			}

			// Delete uploaded files
			System.out.println("Deleting Files...");
			List<File> files = cli.listFiles();
			for (File f : files) {
				System.out.println("Deleting file: " + f.getId() + " => " + cli.deleteFile(f.getId()).isDeleted());
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
