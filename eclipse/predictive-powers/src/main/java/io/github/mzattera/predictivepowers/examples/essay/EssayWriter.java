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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.examples.essay.EssayWriter.Essay.Section;
import io.github.mzattera.predictivepowers.google.client.GoogleClient;
import io.github.mzattera.predictivepowers.google.endpoint.GoogleEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
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

	private final static Logger LOG = LoggerFactory.getLogger(EssayWriter.class);

//	private final static String GOOGLE_ENGINE_ID = "113b5d06917b7435f"; // Scientific articles search
	private final static String GOOGLE_ENGINE_ID = GoogleClient.getEngineId(); // WWW search

	/** Length of an essay in tokens */
//	private final static int ESSAY_LENGTH = 10000;

	/** Approximate length of an essay session in tokens */
	private final static int SECTION_LENGTH = 1000; // 1000tk ~ 1 page of text)

	/**
	 * If true, "summarizes" pages before embedding them by trying to remove data
	 * not relevant to the essay
	 */
	private final static boolean SUMMARIZE = false;

	/** Model to use for text completion. */
	private static final String COMPLETION_MODEL = "gpt-4";

	/** Model to use for writing content. */
	private final static String WRITER_MODEL = "gpt-3.5-turbo-16k";

	// TODO remove all debug log

	/** When googling for a section, how many links to ask for */
	private static final int LINKS_PER_QUERY = 5;

	/** Minimum length of a web page to be considered (chars) */
	private final static int MINIMUM_PAGE_SIZE = 500;

	/** Numbers of threads for parallel execution. */
	private final static int THREAD_POOL_SIZE = 5;

	/**
	 * Maximum time allowed to download one page (milliseconds). Use a negative
	 * value to set no timeout.
	 */
	private final static int DOWNLOAD_TIMEOUT_MILLIS = 1 * 60 * 1000;

	/** Used for JSON (de)serialization in API calls */
	private final static ObjectMapper JSON_MAPPER;
	static {
		JSON_MAPPER = new ObjectMapper();
		JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		JSON_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	@Getter
	@Setter
	@Builder
//	@NoArgsConstructor
	@RequiredArgsConstructor
	@AllArgsConstructor
	// TODO better method names
	// TODO move out of this class
	public static class Essay {

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

			/**
			 * Embedded texts used to build the section, each with their similarity with the
			 * final section
			 */
			@Builder.Default
			List<Pair<EmbeddedText, Double>> context = new ArrayList<>();

			@Override
			public String toString() {
				return sectonSummary();
			}

			public String sectonSummary() {
				StringBuilder buf = new StringBuilder();
				if (id != null)
					buf.append(id).append(") ");
				buf.append(title == null ? "Untitled" : title.trim()).append("\n");
				if (summary != null)
					buf.append("[Summary: ").append(summary.trim()).append("]\n\n");
				for (Section s : sections)
					buf.append(s.sectonSummary());

				return buf.toString();
			}

			public String sectionContent() {
				StringBuilder buf = new StringBuilder();
				if (id != null)
					buf.append(id).append(") ");
				buf.append(title == null ? "Untitled" : title.trim()).append("\n");
				if (content == null) {
					if (summary != null)
						buf.append("[Summary: ").append(summary.trim()).append("]\n\n");
					buf.append("[This section must yet be competed.]\n\n");
				} else {
					buf.append(content.trim()).append("\n\n");
				}
				if ((context != null) && (context.size() > 0)) {
					Set<SearchResult> links = new HashSet<>();
					buf.append("References for section ").append(id == null ? "" : id).append("\n");
					for (Pair<EmbeddedText, Double> p : context) {
//						if (p.getRight() < 0.85)
//							break; // Only show most relevant links
						EmbeddedText emb = p.getLeft();
						SearchResult link = (SearchResult) emb.get("url");
						if (links.contains(link))
							continue;
						buf.append(p.getRight() > 0.85 ? "* " : "  ").append(link).append("\n");
						links.add(link);
					}
					buf.append("\n");
				}
				for (Section s : sections)
					buf.append(s.sectionContent());

				return buf.toString();
			}
		}

		String title;
		String description;

		@Builder.Default
		List<Section> chapters = new ArrayList<>();

		public void serialize(String fileName) throws IOException {
			serialize(new File(fileName));
		}

		public void serialize(File file) throws IOException {
			String json = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
			FileUtil.writeFile(file, json);
		}

		public static Essay deserialize(String fileName) throws IOException {
			return deserialize(new File(fileName));
		}

		public static Essay deserialize(File json) throws IOException {
			return JSON_MAPPER.readValue(FileUtil.readFile(json), Essay.class);
		}

		@Override
		public String toString() {
			return essaySummary();
		}

		public String essaySummary() {
			StringBuilder buf = new StringBuilder();
			buf.append(title == null ? "Untitled" : title).append("\n\n");
			if (description != null)
				buf.append("[Description: ").append(description.trim()).append("]\n\n");
			for (Section s : chapters)
				buf.append(s.sectonSummary());

			return buf.toString();
		}

		public String essayContent() {
			StringBuilder buf = new StringBuilder();
			buf.append(title == null ? "Untitled" : title).append("\n\n");
			for (Section s : chapters)
				buf.append(s.sectionContent());

			return buf.toString();
		}
	}

	private OpenAiEndpoint openAi;
	private GoogleEndpoint google;
	private KnowledgeBase kb = new KnowledgeBase();
	private final Essay essay;

	public EssayWriter(@NonNull String title, @NonNull String description) {
		essay = Essay.builder().title(title).description(description).build();
		initializeEndPoints();
	}

	public EssayWriter(File json) throws IOException {
		essay = Essay.deserialize(json);
		initializeEndPoints();
	}

	// TODO Urgent Remove
	private void initializeEndPoints() {

//		OkHttpClient client = ApiClient.getDefaultHttpClient(OpenAiClient.getApiKey(), 5 * 60 * 1000,
//				OpenAiClient.DEFAULT_KEEP_ALIVE_MILLIS, OpenAiClient.DEFAULT_MAX_IDLE_CONNECTIONS);
//
//		// Creates an OpenAi client that automatically retries call on HTTP error 400.
//		// This is not done by the defaut client as HTTP 400 is returned in same cases
//		// because of malformed requests
//		okhttp3.OkHttpClient.Builder builder = client.newBuilder();
//		builder.addInterceptor(new Interceptor() { // Handles error 400 (request rate limit reached)
//			@Override
//			public Response intercept(Chain chain) throws IOException {
//				Request request = chain.request();
//				Response response = chain.proceed(request);
//
//				if (response.code() == 400)
//					LOG.error(response.body().string());
//
//				return response;
//			}
//		});
//
//		openAi = new OpenAiEndpoint(new OpenAiClient(builder.build()));

		openAi = new OpenAiEndpoint();
		google = new GoogleEndpoint(new GoogleClient(GOOGLE_ENGINE_ID, GoogleClient.getApiKey()));
	}

	public static void main(String[] args) {

		try {
			if ((args.length != 2) && (args.length != 3)) // TODO better checks
				throw new IllegalArgumentException("Usage..."); // TODO print usage

			switch (args[0]) {
			case "-s":
				// Write structure from description passed as argument
				createStructure(new File(args[1]));
				break;
			case "-d":
				// Downloads content for an essay, and saves corresponding knowledge base
				createKnowledgeBase(new File(args[1]), Integer.parseInt(args[2]));
				break;
			case "-w":
				// writes an essay
				write(new File(args[1]), new File(args[2]));
				break;
			case "-a":
				// Starts from essay description and writes the essay, saving structure and KB
				// in
				// the process.
				File description = new File(args[1]);
				File structure = createStructure(description);
				File kb = createKnowledgeBase(structure, Integer.parseInt(args[2]));
				File essay = write(structure, kb);
				System.out.println("Essay completed: " + essay.getCanonicalPath());
				break;
			case "-j":
				// Starts from essay structure and writes the essay, saving the KB in
				// the process.
				structure = new File(args[1]);
				kb = createKnowledgeBase(structure, Integer.parseInt(args[2]));
				essay = write(structure, kb);
				System.out.println("Essay completed: " + essay.getCanonicalPath());
				break;
			default:
				throw new IllegalArgumentException("Usage..."); // TODO print usage
			}
		} catch (Exception e) {
			LOG.error("Error!", e);
		} finally {
			System.out.println("\n\nCompleted.");
		}
	}

	/**
	 * From a description provided in a file, creates essay structure, saved as JSON
	 * file, then saves a summary of the essay as well.
	 * 
	 * @param input
	 * @return The JSON file with essay structure.
	 * @throws IOException
	 */
	public static File createStructure(File input) throws IOException {
		System.out.println("Creating essay structure from description in: " + input.getCanonicalPath() + "...");
		try (EssayWriter writer = new EssayWriter("My Essay", FileUtil.readFile(input))) {
			writer.createStructure();
			System.out.println(writer.essay.essaySummary());
			FileUtil.writeFile(FileUtil.newFile(input, "sumary.txt"), writer.essay.essaySummary());
			File json = FileUtil.newFile(input, "essay.json");
			writer.essay.serialize(json);
			return json;
		}
	}

	/**
	 * Downloads content relevant for an essay and stores it in the knowledge base.
	 * 
	 * @param structure JSON file with the structure of the essay.
	 * @param maxLinks  Maximum web pages to download for the research.
	 * @return A file with the newly created knowledge base.
	 * @throws IOException
	 */
	public static File createKnowledgeBase(File structure, int maxLinks) throws IOException {
		System.out.println("Creating knowledge base for essay described in: " + structure.getCanonicalPath() + "...");
		try (EssayWriter writer = new EssayWriter(structure)) {
			// Googling
			List<SearchResult> links = writer.google(maxLinks);
			for (SearchResult link : links) {
				System.out.println(link.toString());
			}

			// Downloading
			writer.kb.drop();
			writer.download(links);
			File result = FileUtil.newFile(structure, "essay_kb.object");
			writer.kb.save(result);

			return result;
		}
	}

	/**
	 * Writs an essay, then saves it as a text file.
	 * 
	 * @param structure Structure of the essay, it will be populated with content
	 *                  and stored as new JSON file.
	 * @param knowledge Populated knowledge base to use fro writing the essay.
	 * @return A File pointing to the complete essay.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static File write(File structure, File knowledge) throws IOException, ClassNotFoundException {
		System.out.println("Writing essay described in " + structure.getCanonicalPath() + " using KB "
				+ knowledge.getCanonicalPath() + " ...");
		try (EssayWriter writer = new EssayWriter(structure)) {
			writer.kb = KnowledgeBase.load(knowledge);

			// Writing
			writer.write();
			String content = writer.essay.essayContent();

			System.out.println(content);
			File result = FileUtil.newFile(structure, "essay.txt");
			FileUtil.writeFile(result, content);
			writer.essay.serialize(FileUtil.newFile(structure, "essay_complete.json"));

			return result;
		}
	}

	/**
	 * Starting form the essay description, creates its structure of chapters and
	 * sections, already filled with respective summaries.
	 * 
	 * @param description
	 * @return
	 * @throws JsonProcessingException
	 * @throws JsonMappingException
	 */
	public void createStructure() throws JsonProcessingException {

		String description = essay.description;
		if ((description == null) || (description.trim().length() == 0))
			throw new IllegalArgumentException("A description for the essay must be provided.");

		System.out.println("Creating essay structure from description:\n" + description + "\n");

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(COMPLETION_MODEL);
		chatSvc.setTemperature(40.0);

		chatSvc.setPersonality(
				"You are an assistant helping a writer to create the structure of an essay. The essay structure is made of an array of chapters, each chapter contains an array of sections. Always return the structure using this JSON format; here is an example of the format:\n"
						+ "\n" + "{\n" + "	\"chapters\": [{\n" + "		\"title\": \"Title for first chapter\",\n"
						+ "		\"summary\": \"Summary of first chapter.\",\n" + "		\"sections\": [{\n"
						+ "				\"title\": \"Title for first section of first chapter\",\n"
						+ "				\"summary\": \"Summary of this section\"\n" + "			},\n" + "			{\n"
						+ "				\"title\": \"Title for second section of first chapter\",\n"
						+ "				\"summary\": \"Summary of this section\"\n" + "			}\n" + "		]\n"
						+ "	}, {\n" + "		\"title\": \"Title for second chapter\",\n"
						+ "		\"summary\": \"Summary of second chapter.\",\n" + "		\"sections\": [{\n"
						+ "			\"title\": \"Title for first section of second chapter\",\n"
						+ "			\"summary\": \"Summary of this section\"\n" + "		}, {\n"
						+ "			\"title\": \"Title for second section of second chapter\",\n"
						+ "			\"summary\": \"Summary of this section\"\n" + "		}]\n" + "	}]\n" + "}\n" + "\n"
						+ "Do not create sections inside sections. Titles must not include section numbering or 'Chapter' or 'Section'.");

		Map<String, String> params = new HashMap<>();
		params.put("description", description);
		TextCompletion resp = chatSvc.complete(CompletionService.fillSlots(
				"You are tasked with creating the structure of a book based on the provided description. The book should consist of several chapters, each containing a title, a summary, and a list of sections. Each section should have a title and a summary. Please ensure that sections are not nested within each other.\n"
						+ "\n"
						+ "If possible, try to make the summaries of the chapters and sections at least 100 words long to provide substantial content for the book's outline.\n"
						+ "\n"
						+ "Think it through step by step and list all chapters and the sections they contain. Return the result using JSON format.\n"
						+ "\n" + "User Description: {{description}}",
				params));

		Essay created = JSON_MAPPER.readValue(resp.getText(), Essay.class);
		essay.chapters = new ArrayList<>(created.chapters);

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
	 * Google pages relevant to essay content.
	 * 
	 * @return List of links relevant to essay content.
	 */
	public List<SearchResult> google() {
		return google(Integer.MAX_VALUE);
	}

	/**
	 * Google pages relevant to essay content.
	 * 
	 * @param maxLinks Maximum number of links to return.
	 * @return List of links relevant to essay content.
	 */
	public List<SearchResult> google(int maxLinks) {

		System.out.println("Searching content online...");

		// Google each section in parallel

		List<Callable<List<Pair<SearchResult, Integer>>>> tasks = new ArrayList<>();
		for (Section chapter : essay.chapters) {
			tasks.add(() -> google(chapter));
			for (Section section : chapter.sections) {
				tasks.add(() -> google(section));
			}
		}
		List<Pair<SearchResult, Integer>> allLinks = parallelExecution(tasks);
		System.out.println("Total of " + allLinks.size() + " found.");

		// Collects links, starting from top-rank results and avoiding duplicates

		allLinks.sort(new Comparator<>() {

			@Override
			public int compare(Pair<SearchResult, Integer> o1, Pair<SearchResult, Integer> o2) {
				return Integer.compare(o1.getRight(), o2.getRight());
			}
		});

		Set<SearchResult> result = new HashSet<>();
		for (Pair<SearchResult, Integer> l : allLinks) {
			if (result.size() >= maxLinks)
				break;
			if (!result.contains(l.getLeft()))
				result.add(l.getLeft());
		}

		if (allLinks.size() != result.size())
			System.out.println("Keeping " + result.size() + " of " + allLinks.size() + " links.");
		return new ArrayList<>(result);
	}

	/**
	 * Google pages relevant to content for given section.
	 * 
	 * @return List of links relevant to essay content. Each link is returned in a
	 *         pair together with its ran in search results.
	 */
	public List<Pair<SearchResult, Integer>> google(Section section) {

		System.out.println("Searching relevant pages for section " + section.id + ") " + section.title);

		// First create a list of search queries, based on what we want to search

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(COMPLETION_MODEL);
		chatSvc.setTemperature(50.0);

		final String prompt = "Given the below essay structure and target section, generate a list of search engine queries that can be used to search the topic corresponding to the section on the Internet."
				+ " Each query is a short sentence or a short list of key terms relevant for the section topic."
				+ " Include terms to provide a context for the topic, as contained by the essay structure, so that the query is clearly related to the essay content and contextualized accordingly."
				+ " Be creative and provide at least 10 queries."
				+ " Strictly provide results as a JSON array of strings.\n\n" + "Essay structure:\n\n {{essay}}\n\n"
				+ "Target section: {{id}}) {{title}} - {{summary}}";
		Map<String, String> params = new HashMap<>();
		params.put("essay", essay.essaySummary());
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
		// Loops until the section has been successfully googled
		while (true) { // TODO add max retries?
			try {
				queries = JSON_MAPPER.readValue(chatSvc.complete(msgs).getText(), new TypeReference<List<String>>() {
				});
				break;
			} catch (JsonProcessingException e) { // Retry if GPT returned bad JSON
				LOG.warn("Retrying because of malformed JSON while googling section " + section.id, e);
			}
		}

		// Now submit each query and collect returned links
		List<Pair<SearchResult, Integer>> result = new ArrayList<>();
		for (String query : queries) {

			System.out.println("Googling pages for: " + query);
			List<SearchResult> links;
			try {
				links = google.getSearchService().search(query, LINKS_PER_QUERY);
			} catch (Exception e) {
				// Skip single query that fails
				LOG.warn("Ignoring error while searching for: " + query, e);
				continue;
			}

			for (int i = 0; i < links.size(); ++i) {
				result.add(new ImmutablePair<>(links.get(i), i));
			}
		}

		return result;
	}

	/**
	 * Download content of given links and add them into the knowledge base.
	 * Downloads happen in parallel.
	 * 
	 * Notice the knowledge base is NOT cleared.
	 * 
	 * @param links
	 */
	public void download(List<SearchResult> links) {

		System.out.println("Downloading " + links.size() + " pages.");

		// Retrieve contents for each link in parallel and save it in the knowledge base
		List<Callable<List<EmbeddedText>>> tasks = new ArrayList<>();
		for (SearchResult link : links) {
			tasks.add(() -> download(link));
		}
		kb.insert(parallelExecution(tasks));
	}

	/**
	 * Downloads the content of a web page and save it into the Knowledge Base.
	 * 
	 * @param link
	 * @return
	 */
	private List<EmbeddedText> download(SearchResult link) {
		EmbeddingService embSvc = openAi.getEmbeddingService();

		// Make such that 10 embedded texts can fit the prompt, when writing a section
		// TODO fine tune
		int writerSize = openAi.getModelService().getContextSize(WRITER_MODEL);
		int embSize = openAi.getModelService().getContextSize(embSvc.getModel());
		embSvc.setMaxTextTokens(Math.min(embSize, (writerSize - SECTION_LENGTH) / 10));

		LOG.debug("Size: " + embSvc.getMaxTextTokens());

		System.out.println("Downloading page: " + link);
		String content = null;
		try {
			content = cleanup(ExtractionUtil.fromUrl(link.getLink(), DOWNLOAD_TIMEOUT_MILLIS));
		} catch (Exception e) { // Skip page if download error occurs
			LOG.error("Error downloading " + link.getLink(), e);
			return new ArrayList<>();
		}

		LOG.debug("Content downloaded " + link);

		// TODO: tune this better maybe
		if (content.length() < MINIMUM_PAGE_SIZE) { // Some links do not work or cannot be read or have warning messages
			LOG.info("Content too short, skipping " + link.getLink());
			return new ArrayList<>();
		}

		if (SUMMARIZE) {
			content = summarize(content);
			LOG.debug("Content summarized " + link);
		}

		List<EmbeddedText> result = embSvc.embed(content);
		for (EmbeddedText emb : result) {
			emb.set("url", link); // saving link to the page, useful when writing references
			LOG.debug(">>> Embedding added for {}", link.getTitle());
		}

		LOG.debug("Downloaded " + link);
		return result;
	}

	/**
	 * Summarizes given text, regardless its length.
	 * 
	 * @param text
	 * @return
	 */
	// TODO remove
	private String summarize(String text) {

		String prompt = "Web page content below:\n" + "\n" + "{{text}}";

		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel("gpt-3.5-turbo-16k");
		chatSvc.setTemperature(0.0);

		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage(Role.SYSTEM,
				"You are an assistant reformatting web pages downloaded from the Internet. You follow instruction strictly and you do not add information that are not provided to you directly on web pages."));
		msgs.add(new ChatMessage(Role.USER, "You are tasked to remove from the below web page:\n\n"
				+ "1. Links to web site\n" + "2. email addresses \n" + "3. addresses\n"
				+ "4. page navigation items, such as menus and breadcrumbs\n" + "5. text provided in tabular format.\n"
				+ "6. text referring to images, such as image captions, description.\n" + "7. section numbering.\n"
				+ "8. adverts, including invites to subscribe to services\n" + "9. references to scientific articles\n"
				+ "10. copyrights and disclaimers\n"
				+ "11. links to share content or follow users on social media, including podcasts.\n" + "\n"
				+ "Leave the rest of the text untouched."));
//		msgs.add(new ChatMessage(Role.SYSTEM, "Web page content below:\n" + "\n"
//				+ "Cognition: A Guide to Parkinson's Disease\n" + "READ NOW\n" + " PODCASTS\n"
//				+ "Episode 27: More Than Movement: Addressing Cognitive and Behavioral Challenges in Caring for PD\n"
//				+ "LISTEN NOW\n" + " PODCASTS\n" + "Episode 65: Recognizing Non-motor Symptoms in PD\n" + "LISTEN NOW\n"
//				+ "Related Blog Posts\n" + " TIPS FOR DAILY LIVING\n"
//				+ "Mental Health Tips for Cognition, Mood and Sleep\n" + "READ NOW \n" + " CAREGIVER CORNER\n"
//				+ "Care Partner Deep Dive: Three Experts Discuss Sleep, Cognition and Mood in Parkinson's\n"
//				+ "READ NOW \n" + " ADVANCING RESEARCH\n"
//				+ "Tips from the Pros: Maintaining Cognitive Brain Health in Parkinson's Disease\n" + "READ NOW \n"
//				+ "Parkinson's Foundation Helpline\n"
//				+ "Contact 1-800-4PD-INFO or Helpline@Parkinson.org for answers to your Parkinson\u2019s questions.\n"
//				+ "\n" + "Skip to main content\n" + "CALL OUR HELPLINE: 1-800-4PD-INFO (473-4636)\n" + "Espa\u00F1ol\n"
//				+ "About Us\n" + "Blog\n" + "In Your Area\n" + "\n" + "Search\n" + "DONATE\n" + "\n"
//				+ "Home  Health Topics A-Z Parkinson\u2019s Disease: Causes, Symptoms, And Treatments\n"
//				+ "Parkinson\u2019s Disease: Causes, Symptoms, and Treatments\n"
//				+ "Parkinson\u2019s disease is a brain disorder that causes unintended or uncontrollable movements, such as shaking, stiffness, and difficulty with balance and coordination.\n"
//				+ "\n"
//				+ "Symptoms usually begin gradually and worsen over time. As the disease progresses, people may have difficulty walking and talking. They may also have mental and behavioral changes, sleep problems, depression, memory difficulties, and fatigue.\n"
//				+ "\n" + "What causes Parkinson\u2019s disease?\n"
//				+ "The most prominent signs and symptoms of Parkinson\u2019s disease occur when nerve cells in the basal ganglia, an area of the brain that controls movement, become impaired and/or die. Normally, these nerve cells, or neurons, produce an important brain chemical known as dopamine. When the neurons die or become impaired, they produce less dopamine, which causes the movement problems associated with the disease. Scientists still do not know what causes the neurons to die.\n"
//				+ "\n" + "a computer generated graphic of the brain with labels pointing to the basal ganglia.\n" + "\n"
//				+ "References fro this section:\n" + "\n"
//				+ "Using machine learning to modify and enhance the daily living questionnaire.\n"
//				+ "Panovka P, Salman Y, Hel-Or H, Rosenblum S, Toglia J, Josman N, Adamit T.\n"
//				+ "Digit Health. 2023 Apr 25;9:20552076231169818. doi: 10.1177/20552076231169818. eCollection 2023 Jan-Dec.\n"
//				+ "\n"
//				+ "A New Instrument Combines Cognitive and Social Functioning Items for Detecting Mild Cognitive Impairment and Dementia in Parkinson's Disease.\n"
//				+ "Yu YW, Tan CH, Su HC, Chien CY, Sung PS, Lin TY, Lee TL, Yu RL.\n"
//				+ "Front Aging Neurosci. 2022 Jun 16;14:913958. doi: 10.3389/fnagi.2022.913958. eCollection 2022.\n"
//				+ "\n" + "Social Cognition in Patients with Early-Onset Parkinson's Disease.\n"
//				+ "Seubert-Ravelo AN, Y\u00E1\u00F1ez-T\u00E9llez MG, Lazo-Barriga ML, Calder\u00F3n Vallejo A, Mart\u00EDnez-Cort\u00E9s CE, Hern\u00E1ndez-Galv\u00E1n A.\n"
//				+ "Parkinsons Dis. 2021 Jan 7;2021:8852087. doi: 10.1155/2021/8852087. eCollection 2021.\n" + "\n"
//				+ "Feasibility of a Cognitive Training Game in Parkinson's Disease: The Randomized Parkin'Play Study.\n"
//				+ "van de Weijer SCF, Duits AA, Bloem BR, de Vries NM, Kessels RPC, K\u00F6hler S, Tissingh G, Kuijf ML.\n"
//				+ "Eur Neurol. 2020;83(4):426-432. doi: 10.1159/000509685. Epub 2020 Aug 5.\n" + "\n"
//				+ "Impairment in Theory of Mind in Parkinson's Disease Is Explained by Deficits in Inhibition.\n"
//				+ "Foley JA, Lancaster C, Poznyak E, Borejko O, Niven E, Foltynie T, Abrahams S, Cipolotti L.\n"
//				+ "Parkinsons Dis. 2019 May 28;2019:5480913. doi: 10.1155/2019/5480913. eCollection 2019.\n" + "\n"
//				+ "\n" + "The doctor may prescribe other medicines to treat Parkinson\u2019s symptoms.\n" + "\n" + "\n"
//				+ "Treatment\n" + "Other therapies that may help manage Parkinson\u2019s symptoms include:\n" + "\n"
//				+ "Physical, occupational, and speech therapies, which may help with gait and voice disorders, tremors and rigidity, and decline in mental functions\n"
//				+ "A healthy diet to support overall wellness\n"
//				+ "Exercises to strengthen muscles and improve balance, flexibility, and coordination\n"
//				+ "Massage therapy to reduce tension\n" + "Yoga and tai chi to increase stretching and flexibility\n"
//				+ "Support for people living with Parkinson\u2019s disease\n"
//				+ "While the progression of Parkinson\u2019s is usually slow, eventually a person\u2019s daily routines may be affected. Activities such as working, taking care of a home, and participating in social activities with friends may become challenging. Experiencing these changes can be difficult, but support groups can help people cope. These groups can provide information, advice, and connections to resources for those living with Parkinson\u2019s disease, their families, and caregivers. The organizations listed below can help people find local support groups and other resources in their communities.\n"
//				+ "\n" + "Similar articles\n"
//				+ "Conscious attention defect and inhibitory control deficit in Parkinson's Disease-Mild Cognitive Impairment: A comparison study with Amnestic Mild Cognitive Impairment multiple domain.\n"
//				+ "Cammisuli DM, Sportiello MT.\n"
//				+ "Psychiatr Danub. 2017 Dec;29(4):437-445. doi: 10.24869/psyd.2017.437.\n" + "PMID: 29197200\n"
//				+ "Mild cognitive impairment, psychiatric symptoms, and executive functioning in patients with Parkinson's disease.\n"
//				+ "Petkus AJ, Filoteo JV, Schiehser DM, Gomez ME, Hui JS, Jarrahi B, McEwen S, Jakowec MW, Petzinger GM.\n"
//				+ "Int J Geriatr Psychiatry. 2020 Apr;35(4):396-404. doi: 10.1002/gps.5255. Epub 2020 Jan 23.\n"
//				+ "PMID: 31894601\n" + "\n" + "\n" + "Sign up for email updates\n"
//				+ "Receive weekly tips and resources on Alzheimer's disease and related dementias from NIA's Alzheimers.gov\n"
//				+ "\n" + "Email Address\n" + "For more information about Parkinson\u2019s disease\n"
//				+ "National Institute of Neurological Disorders and Stroke (NINDS)\n" + "800-352-9424\n"
//				+ "braininfo@ninds.nih.gov\n" + "www.ninds.nih.gov\n" + "\n"
//				+ "National Institute of Environmental Health Sciences (NIEHS)\n" + "919-541-3345\n"
//				+ "webcenter@niehs.nih.gov\n" + "www.niehs.nih.gov/health/topics/conditions/parkinson\n" + "\n" + "\n"
//				+ "This content is provided by the NIH National Institute on Aging (NIA). NIA scientists and other experts review this content to ensure it is accurate and up to date.\n"
//				+ "\n" + "Content reviewed: April 14, 2022\n"
//				+ "Three publications laying fanned out, titled What\u2019s On Your Plate, Clinical Trials and Older Adults, and Caring for a Person with Alzheimer\u2019s Disease. Click the link to check out and order our free publications.\n"
//				+ "We're here to help! Contact our information centers by phone or email.\n"
//				+ "Woman looking at phone with social icons floating above. Text reads, Stay connected! Join NIA email lists and find us on social media\n"
//				+ "Articles\n" + "Older couple playing ping pong\n"
//				+ "Participating in Activities You Enjoy As You Age\n"
//				+ "Note that says, \u201CToday: Quit Smoking\u201D\n" + "\n" + "\n" + "A-Z health topics\n"
//				+ "Clinical trials\n" + "Careers\n" + "Research divisions & contacts\n" + "Staff directory\n"
//				+ "Workforce diversity\n" + "NIH COVID-19 information\n" + "Contact NIA\n" + "\n"
//				+ "Email iconniaic@nia.nih.gov\n" + "Phone icon800-222-2225\n" + "Contact iconContact us\n"
//				+ "Follow us\n" + "\n" + "Facebook iconFacebook\n" + "Twitter iconTwitter\n" + "LinkedIn\n"
//				+ "YouTube iconYouTube\n" + "Newsletters\n" + "\n"
//				+ "Sign up to receive updates and resources delivered to your inbox.\n" + "\n" + "Sign up\n"
//				+ "nia.nih.gov\n" + "\n" + "LEARN MORE\n" + "Share on Facebook\n" + "Share on Twitter\n"
//				+ "Share on LinkedIn\n" + "\n" + "BACK TO TOP\n" + "Be the First to Know\n"
//				+ "Get the latest news about PD research, resources and community initiatives \u2014 straight to your inbox.\n"
//				+ "\n" + "EMAIL ADDRESS\n" + "Email Address\n"
//				+ "FL: 200 SE 1st Street, Ste 800, Miami, FL 33131, USA\n" + "\n"
//				+ "NY: 1359 Broadway, Ste 1509, New York, NY 10018, USA\n" + "\n"
//				+ "Call our Helpline: 1-800-4PD-INFO (473-4636)\n" + "CONNECT WITH US\n" + "twitter\n" + "linkedin\n"
//				+ "facebook\n" + "instagram\n" + "youtube\n" + "Press Room\n" + "Online Community\n" + "Online Store\n"
//				+ "PD Library\n" + "Privacy & Terms\n" + "Contact Us\n" + "Charity Navigator Logo\n"
//				+ "Excellence in Giving - Certified - Transparent\n" + "Guidestar Platinum Transparency Badge\n"
//				+ "BBB Badge\n" + "Validated Organization CAF\n"
//				+ "\u00A9 2023 Parkinson's Foundation The Parkinson's Foundation is a 501(c)(3) nonprofit organization. EIN: 13-1866796\n"
//				+ "contact@parkinson.org", "example_user", null));
//		msgs.add(new ChatMessage(Role.SYSTEM, "Parkinson\u2019s Disease: Causes, Symptoms, and Treatments\n"
//				+ "Parkinson\u2019s disease is a brain disorder that causes unintended or uncontrollable movements, such as shaking, stiffness, and difficulty with balance and coordination.\n"
//				+ "\n"
//				+ "Symptoms usually begin gradually and worsen over time. As the disease progresses, people may have difficulty walking and talking. They may also have mental and behavioral changes, sleep problems, depression, memory difficulties, and fatigue.\n"
//				+ "\n" + "What causes Parkinson\u2019s disease?\n"
//				+ "The most prominent signs and symptoms of Parkinson\u2019s disease occur when nerve cells in the basal ganglia, an area of the brain that controls movement, become impaired and/or die. Normally, these nerve cells, or neurons, produce an important brain chemical known as dopamine. When the neurons die or become impaired, they produce less dopamine, which causes the movement problems associated with the disease. Scientists still do not know what causes the neurons to die.\n"
//				+ "The doctor may prescribe other medicines to treat Parkinson\u2019s symptoms:\n" + "\n"
//				+ "Treatment\n" + "Other therapies that may help manage Parkinson\u2019s symptoms include:\n" + "\n"
//				+ "Physical, occupational, and speech therapies, which may help with gait and voice disorders, tremors and rigidity, and decline in mental functions\n"
//				+ "A healthy diet to support overall wellness\n"
//				+ "Exercises to strengthen muscles and improve balance, flexibility, and coordination\n"
//				+ "Massage therapy to reduce tension\n" + "Yoga and tai chi to increase stretching and flexibility\n"
//				+ "Support for people living with Parkinson\u2019s disease\n"
//				+ "While the progression of Parkinson\u2019s is usually slow, eventually a person\u2019s daily routines may be affected. Activities such as working, taking care of a home, and participating in social activities with friends may become challenging. Experiencing these changes can be difficult, but support groups can help people cope. These groups can provide information, advice, and connections to resources for those living with Parkinson\u2019s disease, their families, and caregivers. The organizations listed below can help people find local support groups and other resources in their communities.",
//				"example_assistant", null));
		msgs.add(new ChatMessage(Role.USER, prompt)); // Placeholder

		// Splits text in p parts, so each part fits the model context
		Tokenizer counter = openAi.getModelService().getTokenizer(chatSvc.getModel());
		int tok = counter.count(text); // Length of text
		int instTok = counter.count(msgs); // Length of instructions so far
		int p = tok / (openAi.getModelService().getContextSize(chatSvc.getModel()) - instTok) + 1;

		LOG.debug("Text: " + tok + " Instructons: " + instTok + " Splits: " + p);

		Map<String, String> params = new HashMap<>();
		StringBuilder result = new StringBuilder();
		for (String part : LlmUtil.splitByTokens(text, tok / p, counter)) {
			msgs.remove(msgs.size() - 1);
			params.put("text", part);
			msgs.add(new ChatMessage(Role.USER, CompletionService.fillSlots(prompt, params)));
			try {
				result.append(chatSvc.complete(msgs).getText()).append('\n');
			} catch (Exception e) {
				// On error we just return the page as it is
				LOG.error("Error while summarizing", e);
				result.append(part).append('\n');
			}
		}

		String summary = result.substring(0, result.length() - 1);
		return summary;
	}

	/**
	 * (Re)writes the entire essay, based on its structure and the content in the
	 * knowledge base.
	 */
	public void write() {
		System.out.println("Writing essay...");

		String summary = essay.essaySummary();
		List<Callable<List<Section>>> tasks = new ArrayList<>();
		for (Section chapter : essay.chapters) {
			if (chapter.sections.size() == 0) {
				// No (sub)sections, let's generate content for the chapter itself
				tasks.add(() -> write(chapter, summary));
			} else {
				chapter.content = ""; // TODO write a short intro instead
			}
			for (Section section : chapter.sections) {
				tasks.add(() -> write(section, summary));
			}
		}

		parallelExecution(tasks);
	}

	/**
	 * (Re)writes a section of the essay, based on its structure and the content in
	 * the knowledge base.
	 */
	private List<Section> write(Section section, String summary) {

		System.out.println("Writing section " + section.id + ") " + section.title);

		String prompt = "<context>{{context}}</context>\n\n" + "<summary>{{summary}}</summary>";
		Map<String, String> params = new HashMap<>();
		params.put("summary", section.summary);

		EmbeddingService embSvc = openAi.getEmbeddingService();
		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(WRITER_MODEL);
		chatSvc.setTemperature(0.0);

		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(new ChatMessage(Role.SYSTEM,
				"You will be provided with a context and the summary of a section of an essay, both demimited by XML tags."
						+ " Your task is to use the content of the context to write the entire section of the essay."
						+ " Use a professional style."
						+ " Avoid repetitions but be creative and produce a text of at leat 500 words, with all relevant information from the context.\n\n"));
		msgs.add(new ChatMessage(Role.USER, "")); // Placeholder

		List<Pair<EmbeddedText, Double>> knowledge = kb.search(embSvc.embed(section.summary).get(0), 50, 0);

		// See how much context can fit the prompt
		Tokenizer counter = openAi.getModelService().getTokenizer(chatSvc.getModel());
		int ctxSize = openAi.getModelService().getContextSize(chatSvc.getModel());
		StringBuilder buf = new StringBuilder();
		String context = "";
		int i = 0;
		for (; i < knowledge.size(); ++i) {

			EmbeddedText emb = knowledge.get(i).getLeft();
			buf.append(emb.getText()).append("\n\n");
			String c = cleanup(buf.toString()); // TODO URGENT embedded text should have been cleaned up already
			params.put("context", c);

			msgs.remove(msgs.size() - 1);
			msgs.add(new ChatMessage(Role.USER, CompletionService.fillSlots(prompt, params)));
			if (((int) (SECTION_LENGTH * 1.5) + counter.count(msgs)) > ctxSize)
				break;
			context = c; // Saves last context that would fit
		}

		// OK, now build the actual context
		msgs.remove(msgs.size() - 1);
		params.put("context", context);
		msgs.add(new ChatMessage(Role.USER, CompletionService.fillSlots(prompt, params)));

		// Add generated content to the section
		section.content = chatSvc.complete(msgs).getText();

		// Save context (references) from most relevant
		EmbeddedText summaryEmbedding = embSvc.embed(section.content).get(0);
		section.context = new ArrayList<>();
		for (int j = 0; j < i; ++j) {
			EmbeddedText emb = knowledge.get(j).getLeft();
			section.context.add(new ImmutablePair<>(emb, emb.similarity(summaryEmbedding)));
		}
		section.context.sort(new Comparator<>() {

			@Override
			public int compare(Pair<EmbeddedText, Double> o1, Pair<EmbeddedText, Double> o2) {
				return -Double.compare(o1.getRight(), o2.getRight());
			}
		});

		return section.sections; // Not used, just needed for parallelExecution()
	}

	/**
	 * Executes a list of tasks in parallel. Each task is supposed to return a list
	 * of results, all results are collected together and returned.
	 * 
	 * @param timeout If this is > 0 will force execution of each task to terminate
	 *                within the given amount of time (seconds)
	 */
	private static <T> List<T> parallelExecution(List<Callable<List<T>>> tasks) {
		List<T> result = new ArrayList<>();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		List<Future<List<T>>> futures = new ArrayList<>();
		try {

			// Queue all tasks for execution
			for (Callable<List<T>> task : tasks) {
				futures.add(executor.submit(task));
			}

			// Collect results
			for (Future<List<T>> f : futures) {
				try {
					List<T> l = f.get();
					if (l != null)
						result.addAll(l);
				} catch (Exception e) {
					LOG.error("Error executing task", e);
				}
			}

		} finally {
			try {
				executor.shutdownNow(); // All threads should have finished by now
			} catch (Exception e) {
				LOG.warn("Error closing executor", e);
			}
		}

		return result;
	}

	private static String cleanup(String txt) {
		txt = txt.replaceAll("\\[[0-9\\,\\-\\s]+\\]", ""); // Remove references to articles
		txt = txt.replaceAll("\\n{3,}", "\n\n"); // Remove long sequences of newlines

		return txt;
	}

	@Override
	public void close() {
		openAi.close();
		google.close();
	}
}
