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

package io.github.mzattera.predictivepowers.examples.essay;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.examples.essay.EssayWriter.EssayStructure.Section;
import io.github.mzattera.predictivepowers.google.endpoint.GoogleEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.openai.client.OpenAiClient;
import io.github.mzattera.predictivepowers.openai.endpoint.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.services.ChatMessage;
import io.github.mzattera.predictivepowers.services.ChatMessage.Role;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.ModelService.Tokenizer;
import io.github.mzattera.predictivepowers.services.SearchResult;
import io.github.mzattera.predictivepowers.services.TextCompletion;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.FileUtil;
import io.github.mzattera.util.LlmUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class EssayWriter implements Closeable {
	
	// TODO remve al ldebug log

	private final static Logger LOG = LoggerFactory.getLogger(EssayWriter.class);

	private static final double CHAT_TEMPERATURE = 20.0;

	/** Minimim lenght of a web page to be conidered (chars) */
	private static final int MINIMUM_PAGE_SIZE = 500;

	/** Length of an essay in tokens */
	public final static int ESSAY_LENGTH = 2500;

	/** Length of an essay session in tokens */
	public final static int SECTION_LENGTH = ESSAY_LENGTH / 15;

	/** Chat model to use */
//	public final String CHAT_MODEL = "gpt-3.5-turbo-16k";
	public final String CHAT_MODEL = "gpt-3.5-turbo";

	/** Used for JSON (de)serialization in API calls */
	@Getter
	private final static ObjectMapper jsonMapper;
	static {
		jsonMapper = new ObjectMapper();
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		jsonMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
	@AllArgsConstructor
	public static class EssayStructure {

		@Getter
		@Setter
		@Builder
		@NoArgsConstructor
		@RequiredArgsConstructor
		@AllArgsConstructor
		public static class Section {
			String id;

			@NonNull
			String title;

			String summary;
			String content;

			@Builder.Default
			List<Section> sections = new ArrayList<>();

			@Override
			public String toString() {
				return summary();
			}

			public String summary() {
				StringBuilder buf = new StringBuilder();
				if (id != null)
					buf.append(id).append(") ");
				buf.append(title==null?"Untitled":title.trim()).append("\n");
				if (summary != null)
					buf.append("[Summary: ").append(summary.trim()).append("]\n\n");
				for (Section s : sections)
					buf.append(s.summary());

				return buf.toString();
			}

			public String content() {
				StringBuilder buf = new StringBuilder();
				if (id != null)
					buf.append(id).append(") ");
				buf.append(title==null?"Untitled":title.trim()).append("\n");
				if (content == null) {
					if (summary != null)
						buf.append("[Summary: ").append(summary.trim()).append("]\n\n");
					buf.append("This section must yet be competed.\n\n");
				} else {
					buf.append(content.trim()).append("\n\n");
				}
				for (Section s : sections)
					buf.append(s.content());

				return buf.toString();
			}
		}

		String title;
		String description;

		@Builder.Default
		List<Section> chapters = new ArrayList<>();

		public void serialize(String fileName) throws UnsupportedEncodingException, FileNotFoundException, IOException {
			String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
			FileUtil.writeFile(fileName, json);
		}

		public static EssayStructure deserialize(String fileName)
				throws UnsupportedEncodingException, FileNotFoundException, IOException {
			return deserialize(new File(fileName));
		}

		public static EssayStructure deserialize(File json)
				throws JsonMappingException, JsonProcessingException, IOException {
			return jsonMapper.readValue(FileUtil.readFile(json), EssayStructure.class);
		}

		@Override
		public String toString() {
			return getSummary();
		}

		public String getSummary() {
			StringBuilder buf = new StringBuilder();
			buf.append(title == null ? "Untitled" : title).append("\n\n");
			if (description != null)
				buf.append("[Description: ").append(description.trim()).append("]\n\n");
			for (Section s : chapters)
				buf.append(s.summary());

			return buf.toString();
		}

		public String getContent() {
			StringBuilder buf = new StringBuilder();
			buf.append(title == null ? "Untitled" : title).append("\n\n");
			for (Section s : chapters)
				buf.append(s.content());

			return buf.toString();
		}
	}

	private final OpenAiEndpoint openAi = new OpenAiEndpoint(new OpenAiClient(null, 3 * 60 * 1000,
			OpenAiClient.DEFAULT_KEEP_ALIVE_MILLIS, OpenAiClient.DEFAULT_MAX_IDLE_CONNECTIONS));
	private final GoogleEndpoint google = new GoogleEndpoint();
	private KnowledgeBase kb = null;
	private EssayStructure essay = new EssayStructure();

	public EssayWriter(@NonNull String title) {
		essay = EssayStructure.builder().title(title).build();
	}

	public EssayWriter(@NonNull String title, String description) {
		essay = EssayStructure.builder().title(title).description(description).build();
	}

	public EssayWriter(File json) throws JsonMappingException, JsonProcessingException, IOException {
		essay = EssayStructure.deserialize(json);
	}

	public static void main(String[] args) throws IOException, SAXException, TikaException {

		try {
			if (args.length != 2)
				throw new IllegalArgumentException("Usage..."); // TODO print usage

			switch (args[0]) {
			case "-s": // Write structure from description
				File input = new File(args[1]);
				System.out.println("Creating essay structure from description in: " + input.getCanonicalPath());
				try (EssayWriter writer = new EssayWriter("My Essay", FileUtil.readFile(input))) {
					writer.createStructure();
					System.out.println(writer.essay.getSummary());
					FileUtil.writeFile(FileUtil.newFile(input, "Essay Summary.txt"), writer.essay.getSummary());
				}
				break;
			default:
				throw new IllegalArgumentException("Usage..."); // TODO print usage
			}
		} finally {
			System.out.println("\n\nCompleted.");
		}
	}

	/**
	 * Starting form the essay description, creates its structure of chapters and
	 * sections, already filled with respective summaries.
	 * 
	 * @param description
	 * @return
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	public void createStructure() throws JsonMappingException, JsonProcessingException {

		String description = essay.description;
		if ((description == null) || (description.trim().length() == 0))
			throw new IllegalArgumentException("A description for the essay must be provided.");

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel("gpt-3.5-turbo");
		chatSvc.setTemperature(40.0);

		chatSvc.setPersonality("You are an assistant helping a writer to create the structure of an essay. The essay structure is made of an array of chapters, each chapter contains an array of sections. Always return the structure using this JSON format; here is an example of the format:\r\n"
				+ "\r\n"
				+ "{\r\n"
				+ "	\"chapters\": [{\r\n"
				+ "		\"title\": \"Title for first chapter\",\r\n"
				+ "		\"summary\": \"Summary of first chapter.\",\r\n"
				+ "		\"sections\": [{\r\n"
				+ "				\"title\": \"Title for first section of first chapter\",\r\n"
				+ "				\"summary\": \"Summary of this section\"\r\n"
				+ "			},\r\n"
				+ "			{\r\n"
				+ "				\"title\": \"Title for second section of first chapter\",\r\n"
				+ "				\"summary\": \"Summary of this section\"\r\n"
				+ "			}\r\n"
				+ "		]\r\n"
				+ "	}, {\r\n"
				+ "		\"title\": \"Title for second chapter\",\r\n"
				+ "		\"summary\": \"Summary of second chapter.\",\r\n"
				+ "		\"sections\": [{\r\n"
				+ "			\"title\": \"Title for first section of second chapter\",\r\n"
				+ "			\"summary\": \"Summary of this section\"\r\n"
				+ "		}, {\r\n"
				+ "			\"title\": \"Title for second section of second chapter\",\r\n"
				+ "			\"summary\": \"Summary of this section\"\r\n"
				+ "		}]\r\n"
				+ "	}]\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ "Do not create sections inside sections. Titles must not include section numbering or 'Chapter' or 'Section'.");

		Map<String, String> params = new HashMap<>();
		params.put("description", description);
		TextCompletion resp = chatSvc
				.complete(CompletionService.fillSlots("You are tasked with creating the structure of a book based on the provided description. The book should consist of several chapters, each containing a title, a summary, and a list of sections. Each section should have a title and a summary. Please ensure that sections are not nested within each other.\r\n"
						+ "\r\n"
						+ "If possible, try to make the summaries of the chapters and sections at least 100 words long to provide substantial content for the book's outline.\r\n"
						+ "\r\n"
						+ "Think it through step by step and list all chapters and the sections they contain. Return the result using JSON format.\r\n"
						+ "\r\n"
						+ "User Description: {{description}}", params));

		LOG.debug(resp.getFinishReason().toString());
		LOG.debug(resp.getText());
		essay = jsonMapper.readValue(resp.getText(), EssayStructure.class);
		essay.description = description;

		// TODO Remove intro and conclusion if present and recreate them afterwards
		// Alternatively, do not google them but use the summary to create them.

		// Put IDs in sections
		for (int i = 0; i < essay.chapters.size(); ++i) {
			Section chapter = essay.chapters.get(i);
			chapter.id = Integer.toString(i + 1);
			for (int j = 0; j < chapter.sections.size(); ++j) {
				Section section = chapter.sections.get(j);
				section.id = chapter.id + "." + (j + 1);
			}
		}
	}

	/**
	 * Does the main job of writing the essay, starting from its structure of
	 * chapters and sections already filled with respective summaries.
	 * 
	 * @param essay
	 * @return
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 */
	public EssayStructure fillStructure(EssayStructure essay) {

		// Google all pages that might be relevant to build the essay and add their
		// content to the knowledge base
		google(essay);

		// Write one section at a time
		write(essay);

		return essay;
	}

	/**
	 * Google pages relevant to essay content and put their summary into the
	 * knowledge base.
	 * 
	 * @param structure
	 * @param kb
	 * @return
	 * @throws TikaException
	 * @throws SAXException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public List<SearchResult> google(EssayStructure structure) {

		// Google each section in parallel
		ExecutorService executor = Executors.newFixedThreadPool(3); // TODO fine tune; we don't need too many threads
																	// as
																	// we have a request rate anyway..
		List<Future<List<SearchResult>>> futures = new ArrayList<>();
		Set<SearchResult> links = new HashSet<>();
		for (Section chapter : structure.chapters) {
			for (Section section : chapter.sections) {
				futures.add(executor.submit(() -> google(structure, section)));
			}
		}
		for (Future<List<SearchResult>> f : futures)
			try {
				links.addAll(f.get());
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Error while googling", e);
			}
		System.out.println("Total of " + links.size() + " found.");

		// Retrieve contents for each link in parallel and save it in the knowledge base
		kb = new KnowledgeBase();
		int linkCount = 5; // TODO URGENT fix
		for (SearchResult link : links) {
			executor.submit(() -> download(link));
			if (--linkCount == 0)
				break;
		}
		try {
			executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			LOG.error("Error while googling", e);
		}

		try {
			kb.save("D:\\KnowledgeBase.obj");
		} catch (IOException e) {
			LOG.error("Error saving knowledge base", e);
		}

		return new ArrayList<>(links);
	}

	/**
	 * Provides links to web pages that might be relevant to create given section.
	 * 
	 * @param structure
	 * @param section
	 * @return
	 */
	private List<SearchResult> google(EssayStructure structure, Section section) {

		// First create a list of search queries, based on what we want to search

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(CHAT_MODEL);
		chatSvc.setTemperature(50.0);

		final String prompt = "Given the below essay structure and target section, generate a list of search engine queries that can be used to search the topic corresponding to the section on the Internet."
				+ " Each query is a short sentence or a short list of key terms relevant for the section topic."
				+ " Include terms to provide a context for the topic, as contained by the essay structure, so that the query is clearly related to the essay content and contextualized accordingly."
				+ " Be creative and provide at least 10 queries."
				+ " Strictly provide results as a JSON array of strings.\n\n" + "Essay structure:\n\n {{essay}}\n\n"
				+ "Target section: {{id}}) {{title}} - {{summary}}";
		Map<String, String> params = new HashMap<>();
		params.put("essay", structure.toString());
		params.put("id", section.id);
		params.put("title", section.title);
		params.put("summary", section.summary);

		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage(Role.SYSTEM,
				"You are an assistant helping a researcher in finding web pages that are relevant for the essay section they are writing."));
		msgs.add(new ChatMessage(Role.USER, CompletionService.fillSlots(prompt, params)));

		// TODO since we have high temperature do a few iterations which will generate
		// different queries
		List<String> queries;
		while (true) { // Loops until the section has been successfully googled
			try {
				queries = jsonMapper.readValue(chatSvc.complete(msgs).getText(), new TypeReference<List<String>>() {
				});
				break;
			} catch (JsonProcessingException e) { // Retry if GPT returned bad JSON
				LOG.warn("Retrying because of malformed JSON while googling section " + section.id, e);
			} catch (Exception e) { // In all other cases, fail gracefully
				LOG.error("Error googling section " + section.id, e); // TODO maybe fail not so gracefully as the
																		// section cannot be completed
				queries = new ArrayList<>();
				break;
			}
		}

		// Now submit each query in parallel and collect returned links
		ExecutorService executor = Executors.newFixedThreadPool(1); // TODO fine tune; we don't need too many threads as
																	// we have a request rate anyway..
		List<Future<List<SearchResult>>> futures = new ArrayList<>();
		Set<SearchResult> result = new HashSet<>();
		for (String query : queries) {
			System.out.println("Googleing for [" + section.id + "]: " + query);
			futures.add(executor.submit(() -> google.getSearchService().search(query, 5))); // TODO fine tune
		}
		for (Future<List<SearchResult>> f : futures) {
			try {
				result.addAll(f.get());
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Error googling section " + section.id, e);
			}
		}
		executor.shutdownNow(); // All threads should have completed by now.

		return new ArrayList<>(result);
	}

	/**
	 * Downloads the content of a web page and save it into the Knowledge Base.
	 * 
	 * @param link
	 */
	public void download(SearchResult link) {
		EmbeddingService embSvc = openAi.getEmbeddingService();
		embSvc.setMaxTextTokens((openAi.getModelService().getContextSize(CHAT_MODEL) - ESSAY_LENGTH) / 25);

		System.out.println("Downloading " + link.getTitle() + " - " + link.getLink());
		String content = "";
		try {
			content = ExtractionUtil.fromUrl(link.getLink()).trim();
		} catch (Exception e) {
			// TODO: LOG.error("Error downloading " + link.getLink(), e);
			LOG.error("Error downloading " + link.getLink());
		}

		// TODO: tune this better maybe
		if (content.length() < MINIMUM_PAGE_SIZE) { // Some links do not work or cannot be read or have warning messages
			LOG.warn("Content too short, skipping " + link.getLink());
			return;
		}

		content = summarize(content);
		for (EmbeddedText emb : embSvc.embed(content)) {
			emb.set("url", link.getLink());
			kb.insert(emb);
		}

		System.out.println("Downloaded " + link.getTitle() + " - " + link.getLink());
	}

	/**
	 * Summarizes given text, regardless its length.
	 * 
	 * @param text
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	private String summarize(String text) {

		String prompt = "Web page content below:\n" + "\n" + "{{text}}";

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(CHAT_MODEL);
		chatSvc.setTemperature(CHAT_TEMPERATURE);

		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage(Role.SYSTEM,
				"You are an assistant summarizing web pages downloaded from the Internet Your task is to remove text which is not part of the main page content. You follow instruction strictly and you do not add information that are not provided to you directly on web pages."));
		msgs.add(new ChatMessage(Role.USER,
				"Given the content of a web page, remove all and only text not related to the main page content. Examples of  text to remove are:\n"
						+ "\n" + "1. Links to web site\n" + "2. email addresses \n" + "3. addresses\n"
						+ "4. page navigation items, such as menus and breadcrumbs\n"
						+ "5. text provided in tabular format.\n"
						+ "6. text referring to images, such as image captions, description.\n"
						+ "7. section numbering.\n"
						+ "8. isolated short sentences not related to the main page content.\n"
						+ "9. adverts, including invites to subscribe to services\n"
						+ "10. references to scientific articles\n" + "11. copyrights and disclaimers\n"
						+ "12. links to share content or follow users on social media, including podcasts.\n" + "\n"
						+ "Leave the rest of the text untouched."));
		msgs.add(new ChatMessage(Role.SYSTEM, "Web page content below:\n" + "\n"
				+ "Cognition: A Guide to Parkinson's Disease\n" + "READ NOW\n" + " PODCASTS\n"
				+ "Episode 27: More Than Movement: Addressing Cognitive and Behavioral Challenges in Caring for PD\n"
				+ "LISTEN NOW\n" + " PODCASTS\n" + "Episode 65: Recognizing Non-motor Symptoms in PD\n" + "LISTEN NOW\n"
				+ "Related Blog Posts\n" + " TIPS FOR DAILY LIVING\n"
				+ "Mental Health Tips for Cognition, Mood and Sleep\n" + "READ NOW \n" + " CAREGIVER CORNER\n"
				+ "Care Partner Deep Dive: Three Experts Discuss Sleep, Cognition and Mood in Parkinson's\n"
				+ "READ NOW \n" + " ADVANCING RESEARCH\n"
				+ "Tips from the Pros: Maintaining Cognitive Brain Health in Parkinson's Disease\n" + "READ NOW \n"
				+ "Parkinson's Foundation Helpline\n"
				+ "Contact 1-800-4PD-INFO or Helpline@Parkinson.org for answers to your Parkinson\u2019s questions.\n"
				+ "\n" + "Skip to main content\n" + "CALL OUR HELPLINE: 1-800-4PD-INFO (473-4636)\n" + "Espa\u00F1ol\n"
				+ "About Us\n" + "Blog\n" + "In Your Area\n" + "\n" + "Search\n" + "DONATE\n" + "\n"
				+ "Home  Health Topics A-Z Parkinson\u2019s Disease: Causes, Symptoms, And Treatments\n"
				+ "Parkinson\u2019s Disease: Causes, Symptoms, and Treatments\n"
				+ "Parkinson\u2019s disease is a brain disorder that causes unintended or uncontrollable movements, such as shaking, stiffness, and difficulty with balance and coordination.\n"
				+ "\n"
				+ "Symptoms usually begin gradually and worsen over time. As the disease progresses, people may have difficulty walking and talking. They may also have mental and behavioral changes, sleep problems, depression, memory difficulties, and fatigue.\n"
				+ "\n" + "What causes Parkinson\u2019s disease?\n"
				+ "The most prominent signs and symptoms of Parkinson\u2019s disease occur when nerve cells in the basal ganglia, an area of the brain that controls movement, become impaired and/or die. Normally, these nerve cells, or neurons, produce an important brain chemical known as dopamine. When the neurons die or become impaired, they produce less dopamine, which causes the movement problems associated with the disease. Scientists still do not know what causes the neurons to die.\n"
				+ "\n" + "a computer generated graphic of the brain with labels pointing to the basal ganglia.\n" + "\n"
				+ "References fro this section:\n" + "\n"
				+ "Using machine learning to modify and enhance the daily living questionnaire.\n"
				+ "Panovka P, Salman Y, Hel-Or H, Rosenblum S, Toglia J, Josman N, Adamit T.\n"
				+ "Digit Health. 2023 Apr 25;9:20552076231169818. doi: 10.1177/20552076231169818. eCollection 2023 Jan-Dec.\n"
				+ "\n"
				+ "A New Instrument Combines Cognitive and Social Functioning Items for Detecting Mild Cognitive Impairment and Dementia in Parkinson's Disease.\n"
				+ "Yu YW, Tan CH, Su HC, Chien CY, Sung PS, Lin TY, Lee TL, Yu RL.\n"
				+ "Front Aging Neurosci. 2022 Jun 16;14:913958. doi: 10.3389/fnagi.2022.913958. eCollection 2022.\n"
				+ "\n" + "Social Cognition in Patients with Early-Onset Parkinson's Disease.\n"
				+ "Seubert-Ravelo AN, Y\u00E1\u00F1ez-T\u00E9llez MG, Lazo-Barriga ML, Calder\u00F3n Vallejo A, Mart\u00EDnez-Cort\u00E9s CE, Hern\u00E1ndez-Galv\u00E1n A.\n"
				+ "Parkinsons Dis. 2021 Jan 7;2021:8852087. doi: 10.1155/2021/8852087. eCollection 2021.\n" + "\n"
				+ "Feasibility of a Cognitive Training Game in Parkinson's Disease: The Randomized Parkin'Play Study.\n"
				+ "van de Weijer SCF, Duits AA, Bloem BR, de Vries NM, Kessels RPC, K\u00F6hler S, Tissingh G, Kuijf ML.\n"
				+ "Eur Neurol. 2020;83(4):426-432. doi: 10.1159/000509685. Epub 2020 Aug 5.\n" + "\n"
				+ "Impairment in Theory of Mind in Parkinson's Disease Is Explained by Deficits in Inhibition.\n"
				+ "Foley JA, Lancaster C, Poznyak E, Borejko O, Niven E, Foltynie T, Abrahams S, Cipolotti L.\n"
				+ "Parkinsons Dis. 2019 May 28;2019:5480913. doi: 10.1155/2019/5480913. eCollection 2019.\n" + "\n"
				+ "\n" + "The doctor may prescribe other medicines to treat Parkinson\u2019s symptoms.\n" + "\n" + "\n"
				+ "Treatment\n" + "Other therapies that may help manage Parkinson\u2019s symptoms include:\n" + "\n"
				+ "Physical, occupational, and speech therapies, which may help with gait and voice disorders, tremors and rigidity, and decline in mental functions\n"
				+ "A healthy diet to support overall wellness\n"
				+ "Exercises to strengthen muscles and improve balance, flexibility, and coordination\n"
				+ "Massage therapy to reduce tension\n" + "Yoga and tai chi to increase stretching and flexibility\n"
				+ "Support for people living with Parkinson\u2019s disease\n"
				+ "While the progression of Parkinson\u2019s is usually slow, eventually a person\u2019s daily routines may be affected. Activities such as working, taking care of a home, and participating in social activities with friends may become challenging. Experiencing these changes can be difficult, but support groups can help people cope. These groups can provide information, advice, and connections to resources for those living with Parkinson\u2019s disease, their families, and caregivers. The organizations listed below can help people find local support groups and other resources in their communities.\n"
				+ "\n" + "Similar articles\n"
				+ "Conscious attention defect and inhibitory control deficit in Parkinson's Disease-Mild Cognitive Impairment: A comparison study with Amnestic Mild Cognitive Impairment multiple domain.\n"
				+ "Cammisuli DM, Sportiello MT.\n"
				+ "Psychiatr Danub. 2017 Dec;29(4):437-445. doi: 10.24869/psyd.2017.437.\n" + "PMID: 29197200\n"
				+ "Mild cognitive impairment, psychiatric symptoms, and executive functioning in patients with Parkinson's disease.\n"
				+ "Petkus AJ, Filoteo JV, Schiehser DM, Gomez ME, Hui JS, Jarrahi B, McEwen S, Jakowec MW, Petzinger GM.\n"
				+ "Int J Geriatr Psychiatry. 2020 Apr;35(4):396-404. doi: 10.1002/gps.5255. Epub 2020 Jan 23.\n"
				+ "PMID: 31894601\n" + "\n" + "\n" + "Sign up for email updates\n"
				+ "Receive weekly tips and resources on Alzheimer's disease and related dementias from NIA's Alzheimers.gov\n"
				+ "\n" + "Email Address\n" + "For more information about Parkinson\u2019s disease\n"
				+ "National Institute of Neurological Disorders and Stroke (NINDS)\n" + "800-352-9424\n"
				+ "braininfo@ninds.nih.gov\n" + "www.ninds.nih.gov\n" + "\n"
				+ "National Institute of Environmental Health Sciences (NIEHS)\n" + "919-541-3345\n"
				+ "webcenter@niehs.nih.gov\n" + "www.niehs.nih.gov/health/topics/conditions/parkinson\n" + "\n" + "\n"
				+ "This content is provided by the NIH National Institute on Aging (NIA). NIA scientists and other experts review this content to ensure it is accurate and up to date.\n"
				+ "\n" + "Content reviewed: April 14, 2022\n"
				+ "Three publications laying fanned out, titled What\u2019s On Your Plate, Clinical Trials and Older Adults, and Caring for a Person with Alzheimer\u2019s Disease. Click the link to check out and order our free publications.\n"
				+ "We're here to help! Contact our information centers by phone or email.\n"
				+ "Woman looking at phone with social icons floating above. Text reads, Stay connected! Join NIA email lists and find us on social media\n"
				+ "Articles\n" + "Older couple playing ping pong\n"
				+ "Participating in Activities You Enjoy As You Age\n"
				+ "Note that says, \u201CToday: Quit Smoking\u201D\n" + "\n" + "\n" + "A-Z health topics\n"
				+ "Clinical trials\n" + "Careers\n" + "Research divisions & contacts\n" + "Staff directory\n"
				+ "Workforce diversity\n" + "NIH COVID-19 information\n" + "Contact NIA\n" + "\n"
				+ "Email iconniaic@nia.nih.gov\n" + "Phone icon800-222-2225\n" + "Contact iconContact us\n"
				+ "Follow us\n" + "\n" + "Facebook iconFacebook\n" + "Twitter iconTwitter\n" + "LinkedIn\n"
				+ "YouTube iconYouTube\n" + "Newsletters\n" + "\n"
				+ "Sign up to receive updates and resources delivered to your inbox.\n" + "\n" + "Sign up\n"
				+ "nia.nih.gov\n" + "\n" + "LEARN MORE\n" + "Share on Facebook\n" + "Share on Twitter\n"
				+ "Share on LinkedIn\n" + "\n" + "BACK TO TOP\n" + "Be the First to Know\n"
				+ "Get the latest news about PD research, resources and community initiatives \u2014 straight to your inbox.\n"
				+ "\n" + "EMAIL ADDRESS\n" + "Email Address\n"
				+ "FL: 200 SE 1st Street, Ste 800, Miami, FL 33131, USA\n" + "\n"
				+ "NY: 1359 Broadway, Ste 1509, New York, NY 10018, USA\n" + "\n"
				+ "Call our Helpline: 1-800-4PD-INFO (473-4636)\n" + "CONNECT WITH US\n" + "twitter\n" + "linkedin\n"
				+ "facebook\n" + "instagram\n" + "youtube\n" + "Press Room\n" + "Online Community\n" + "Online Store\n"
				+ "PD Library\n" + "Privacy & Terms\n" + "Contact Us\n" + "Charity Navigator Logo\n"
				+ "Excellence in Giving - Certified - Transparent\n" + "Guidestar Platinum Transparency Badge\n"
				+ "BBB Badge\n" + "Validated Organization CAF\n"
				+ "\u00A9 2023 Parkinson's Foundation The Parkinson's Foundation is a 501(c)(3) nonprofit organization. EIN: 13-1866796\n"
				+ "contact@parkinson.org", "example_user", null));
		msgs.add(new ChatMessage(Role.SYSTEM, "Parkinson\u2019s Disease: Causes, Symptoms, and Treatments\n"
				+ "Parkinson\u2019s disease is a brain disorder that causes unintended or uncontrollable movements, such as shaking, stiffness, and difficulty with balance and coordination.\n"
				+ "\n"
				+ "Symptoms usually begin gradually and worsen over time. As the disease progresses, people may have difficulty walking and talking. They may also have mental and behavioral changes, sleep problems, depression, memory difficulties, and fatigue.\n"
				+ "\n" + "What causes Parkinson\u2019s disease?\n"
				+ "The most prominent signs and symptoms of Parkinson\u2019s disease occur when nerve cells in the basal ganglia, an area of the brain that controls movement, become impaired and/or die. Normally, these nerve cells, or neurons, produce an important brain chemical known as dopamine. When the neurons die or become impaired, they produce less dopamine, which causes the movement problems associated with the disease. Scientists still do not know what causes the neurons to die.\n"
				+ "The doctor may prescribe other medicines to treat Parkinson\u2019s symptoms:\n" + "\n"
				+ "Treatment\n" + "Other therapies that may help manage Parkinson\u2019s symptoms include:\n" + "\n"
				+ "Physical, occupational, and speech therapies, which may help with gait and voice disorders, tremors and rigidity, and decline in mental functions\n"
				+ "A healthy diet to support overall wellness\n"
				+ "Exercises to strengthen muscles and improve balance, flexibility, and coordination\n"
				+ "Massage therapy to reduce tension\n" + "Yoga and tai chi to increase stretching and flexibility\n"
				+ "Support for people living with Parkinson\u2019s disease\n"
				+ "While the progression of Parkinson\u2019s is usually slow, eventually a person\u2019s daily routines may be affected. Activities such as working, taking care of a home, and participating in social activities with friends may become challenging. Experiencing these changes can be difficult, but support groups can help people cope. These groups can provide information, advice, and connections to resources for those living with Parkinson\u2019s disease, their families, and caregivers. The organizations listed below can help people find local support groups and other resources in their communities.",
				"example_assistant", null));
		msgs.add(new ChatMessage(Role.USER, prompt)); // Placeholder

		// Splits text in p parts, so each part fits the model context
		Tokenizer counter = openAi.getModelService().getTokenizer(chatSvc.getModel());
		int tok = counter.count(text); // Length of text
		int instTok = counter.count(msgs); // Length of instructions so far
		int p = tok / (openAi.getModelService().getContextSize(chatSvc.getModel()) - instTok) + 1;

		Map<String, String> params = new HashMap<>();
		StringBuilder result = new StringBuilder();
		for (String part : LlmUtil.splitByTokens(text, tok / p, counter)) {
			msgs.remove(msgs.size() - 1);
			params.put("text", part);
			msgs.add(new ChatMessage(Role.USER, CompletionService.fillSlots(prompt, params)));
			while (true) { // Looks until correctly executed
				try {
					result.append(chatSvc.complete(msgs).getText()).append('\n');
					break;
				} catch (Exception e) {
// TODO:					LOG.error("Error while summarizing", e);
					LOG.error("Error while summarizing {}", e.getMessage());
					// Most likely we exceed the rate limit; just wait a while before resubmitting
					try {
						Thread.sleep(10 * 1000);
					} catch (InterruptedException e1) {
					}
				}
			}
		}

//		System.out.println("===[ TEXT ]==========\n" + text);
		String summary = result.substring(0, result.length() - 1);
//		System.out.println("====================\n" + summary);
		return summary;
	}

	private void write(EssayStructure structure) {
		for (int i = 0; i < structure.chapters.size(); ++i) {
			Section chapter = structure.chapters.get(i);
			for (int j = 0; j < chapter.sections.size(); ++j) {
				Section section = chapter.sections.get(j);
				write(structure, section);
			}
		}
	}

	private void write(EssayStructure structure, Section section) {

		String prompt = "Write given section of the essay using web page contents listed below."
				+ " The section content should fit the rest of the essay; summary of the essay is also provided below."
				+ " In writing the section, use only listed content and no other information."
				+ " Include facts, entities and events that are in the text only if they are relevant for the topic of the section."
				+ " Output only the section content, leaving the title out."
				+ " Limit the length of the secton to about " + (SECTION_LENGTH * 3 / 4) + " words." + "\n\n"
				+ "Essay summary:\n\n {{essay}}\n\n" + "Section title: \"" + section.id + " " + section.title + "\"\n\n"
				+ "Section summary:  \"" + section.summary + "\"\n\n";
		EmbeddingService embSvc = openAi.getEmbeddingService();

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(CHAT_MODEL);
		chatSvc.setTemperature(CHAT_TEMPERATURE);

		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage(Role.SYSTEM,
				"You are an assistant helping a writer to create an essay. When creating text, you follow instructions very closely and use only information provided to you."));
		msgs.add(new ChatMessage(Role.USER, prompt));
		msgs.add(new ChatMessage(Role.USER, "")); // Placeholder

		// TODO guard if query embedding is longer than 0
		List<Pair<EmbeddedText, Double>> context = kb.search(embSvc.embed(section.summary).get(0), 50, 0);

		// See how much context can fit the prompt
		Tokenizer counter = openAi.getModelService().getTokenizer(chatSvc.getModel());
		int ctxSize = openAi.getModelService().getContextSize(chatSvc.getModel());
		StringBuilder ctx = new StringBuilder();
		int i = 0;
		for (; i < context.size(); ++i) {
			EmbeddedText emb = context.get(i).getLeft();
			msgs.remove(2);
			ctx.append("Content of web page " + emb.get("url")).append("\n").append(emb.getText()).append("\n\n");
			msgs.add(new ChatMessage(Role.USER, ctx.toString()));
			if ((SECTION_LENGTH + counter.count(msgs)) > ctxSize)
				break;
		}

		// OK, now build the actual context
		// TODO do it better
		ctx.setLength(0);
		msgs.remove(2);
		for (int j = 0; j < i; ++j) {
			EmbeddedText emb = context.get(j).getLeft();
			ctx.append("Content of web page " + emb.get("url")).append("\n").append(emb.getText()).append("\n\n");
		}
		msgs.add(new ChatMessage(Role.USER, ctx.toString()));

		System.out.println("====[ Summarizing content for section " + section.id + " " + section.title + "\n");
		System.out.println(ctx.toString() + "\n");

		// Add the generated content to the section
		while (true) { // Looks until correctly executed
			try {
				section.content = chatSvc.complete(msgs).getText();
				break;
			} catch (Exception e) {
				LOG.error("Error while writing section " + section.id, e);
			}
		}

		System.out.println("====[ Content\n");
		System.out.println(section.content + "\n");
	}

	@Override
	public void close() {
		openAi.close();
		google.close();
	}
}
