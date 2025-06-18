/*
 * Copyright 2023-2024 Massimiliano "Maxi" Zattera
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

import java.util.Scanner;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.beta.assistants.Assistant;
import com.openai.models.files.FileObject;
import com.openai.models.finetuning.jobs.FineTuningJob;
import com.openai.models.finetuning.jobs.FineTuningJob.Status;
import com.openai.models.models.Model;

import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;

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
			OpenAIClient cli = ep.getClient();

			// Cancel tuning tasks
			System.out.println("Cancelling FineTunes...");
			for (FineTuningJob task : cli.fineTuning().jobs().list().autoPager()) {
				Status status = task.status();
				if ((status == Status.VALIDATING_FILES) || (status == Status.RUNNING)) {
					status = cli.fineTuning().jobs().cancel(task.id()).status();
					System.out.println("Cancelling task: " + task.id() + " => " + status);
				} else {
					System.out.println("\tTask cannot be cancelled: " + task.id() + " => " + status);
				}
			}

			// Delete fine tunes models
			System.out.println("Deleting Models...");
			for (Model m : cli.models().list().data()) {
				if (!"openai".equals(m.ownedBy()) && !"openai-internal".equals(m.ownedBy())
						&& !"system".equals(m.ownedBy())) // custom model
					System.out.println(
							"\tDeleting fine tuned model: " + m.id() + " => " + cli.models().delete(m.id()).deleted());
			}

			// Here one should delete threads and runs, but there is no way to do it in the
			// API (get full list)

			// Delete assistants: assistants file are cascaded deleted
			System.out.println("Deleting Assistants...");
			for (Assistant a : cli.beta().assistants().list().autoPager()) {
				JsonValue def = a.metadata().orElse(Assistant.Metadata.builder().build()) //
						._additionalProperties().get("_persist");
				boolean persist = (def.isMissing() || def.isNull()) ? false : "true".equals(def.toString());
//				boolean persist = false;

				if (persist)
					System.out.println("\tAssistant is persisted: " + a.id());
				else
					try {
					System.out.println("\tDeleting assistant: " + a.id() + " => "
							+ cli.beta().assistants().delete(a.id()).deleted());
					} catch (com.openai.errors.NotFoundException e) {
						// Sometimes happens
						System.out.println("\tDeleting assistant: " + a.id() + " => NOT FOUND!");						
					}
			}

			// Delete uploaded files
			System.out.println("Deleting Files...");
			for (FileObject f : cli.files().list().autoPager()) {
				System.out.println("\tDeleting file: " + f.id() + " => " + cli.files().delete(f.id()).deleted());
			}
			
			System.out.println("\nCleanup completed.yes");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
