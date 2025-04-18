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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.examples.essay.EssayWriter.Essay.Section;
import io.github.mzattera.predictivepowers.google.client.GoogleClient;
import io.github.mzattera.predictivepowers.google.client.GoogleEndpoint;
import io.github.mzattera.predictivepowers.knowledge.KnowledgeBase;
import io.github.mzattera.predictivepowers.openai.client.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatMessage.Role;
import io.github.mzattera.predictivepowers.openai.services.OpenAiChatService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiEmbeddingService;
import io.github.mzattera.predictivepowers.openai.services.OpenAiTokenizer;
import io.github.mzattera.predictivepowers.services.CompletionService;
import io.github.mzattera.predictivepowers.services.EmbeddedText;
import io.github.mzattera.predictivepowers.services.EmbeddingService;
import io.github.mzattera.predictivepowers.services.Link;
import io.github.mzattera.predictivepowers.services.messages.ChatCompletion;
import io.github.mzattera.util.ExtractionUtil;
import io.github.mzattera.util.FileUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class writes an essay, starting from a draft description of its contents
 * and googling the Internet for relevant information.
 * 
 * This is is an example from the predictive-powers library
 * (https://github.com/mzattera/predictive-powers) and needs the library to work
 * properly.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public class EssayWriter implements Closeable {

	// Our internal log; is uses logback, so it can easily be configured.
	private final static Logger LOG = LoggerFactory.getLogger(EssayWriter.class);

	// You must create a Google Programmable Search Engine, to be used for web
	// searches and put its ID in the OS environment variable ""GOOGLE_ENGINE_ID"
	private final static String GOOGLE_ENGINE_ID = GoogleClient.getEngineId();

	/** Approximate length of an essay session in tokens */
	private final static int SECTION_LENGTH_TOKENS = 1000; // 1000tk ~ 1 page of text)

	/** Model to use for text completion. */
	private static final String COMPLETION_MODEL = "gpt-3.5-turbo";

	/** Model to use for writing content. */
	private final static String WRITER_MODEL = "gpt-3.5-turbo-16k";

	/** When googling for a section, how many links do we ask for? */
	private static final int LINKS_PER_QUERY = 5;

	/** Minimum length of a web page to be considered relevant (chars) */
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

	/**
	 * The Essay class models an Essay.
	 * 
	 * Its structure allows for a multi-level tree-structure, but currently the code
	 * only uses 1 level (chapters divided into sections).
	 */
	@Getter
	@Setter
	@Builder
	@RequiredArgsConstructor
	@AllArgsConstructor
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
			 * Embedded texts used to build the section.
			 */
			@Builder.Default
			List<EmbeddedText> context = new ArrayList<>();

			/**
			 * Relevance (0.0-1.0) of each embedded text used to build the section.
			 */
			@Builder.Default
			List<Double> contextWeight = new ArrayList<>();

			@Override
			public String toString() {
				return getFormattedSummary();
			}

			/** @return nicely formatted title of this section. */
			public String getFormattedTitle() {
				StringBuilder buf = new StringBuilder();
				if (id != null)
					buf.append(id).append(" ");
				buf.append(title == null ? "Untitled" : title.trim());
				return buf.toString();
			}

			/** @return nicely formatted short representation of this section. */
			public String getFormattedSummary() {
				StringBuilder buf = new StringBuilder();
				buf.append(getFormattedTitle()).append("\n");
				if (summary != null)
					buf.append("[Summary: ").append(summary.trim()).append("]\n\n");
				for (Section s : sections)
					buf.append(s.getFormattedSummary());

				return buf.toString();
			}

			/** @return full text of this section. */
			public String getFormattedContent() {
				StringBuilder buf = new StringBuilder();
				buf.append(getFormattedTitle()).append("\n");
				if (content == null) {
					if (summary != null)
						buf.append("[Summary: ").append(summary.trim()).append("]\n\n");
					buf.append("[This section must yet be competed.]\n\n");
				} else {
					buf.append(content.trim()).append("\n\n");
				}
				if ((context != null) && (context.size() > 0)) {
					Set<Link> links = new HashSet<>();
					buf.append("References for section ").append(id == null ? "" : id).append("\n");
					for (int i = 0; i < context.size(); ++i) {
						EmbeddedText emb = context.get(i);
						double weight = contextWeight.get(i);
						Link link = (Link) emb.get("url");
						if (links.contains(link))
							continue;
						buf.append(weight > 0.85 ? "* " : "  ").append(link).append("\n");
						links.add(link);
					}
					buf.append("\n");
				}
				for (Section s : sections)
					buf.append(s.getFormattedContent());

				return buf.toString();
			}
		}

		String title;
		String description;

		/** All chapters in the essay. */
		@Builder.Default
		List<Section> chapters = new ArrayList<>();

		@Override
		public String toString() {
			return getFormattedSummary();
		}

		/** @return Nicely formatted summary of the Essay. */
		public String getFormattedSummary() {
			StringBuilder buf = new StringBuilder();
			buf.append(title == null ? "Untitled" : title).append("\n\n");
			if (description != null)
				buf.append("[Description: ").append(description.trim()).append("]\n\n");
			for (Section s : chapters)
				buf.append(s.getFormattedSummary());

			return buf.toString();
		}

		/** @return Nicely formatted content of the Essay. */
		public String getFormattedContent() {
			StringBuilder buf = new StringBuilder();
			buf.append(title == null ? "Untitled" : title).append("\n\n");
			for (Section s : chapters)
				buf.append(s.getFormattedContent());

			return buf.toString();
		}

		// Methods to read/write the essay from in JSON format

		public static Essay fromFile(String fileName) throws IOException {
			return fromFile(new File(fileName));
		}

		public static Essay fromFile(File json) throws IOException {
			return fromJson(FileUtil.readFile(json));
		}

		public static Essay fromJson(String json) throws IOException {
			return JSON_MAPPER.readValue(json, Essay.class);
		}

		public void toFile(String fileName) throws IOException {
			toFile(new File(fileName));
		}

		public void toFile(File file) throws IOException {
			String json = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
			FileUtil.writeFile(file, json);
		}
	}

	/** The Essay the EssayWriter is currently writing. */
	protected Essay essay;

	// Endpoints used by the agent

	protected OpenAiEndpoint openAi;
	protected GoogleEndpoint google;

	// The agent's knowledge base
	protected KnowledgeBase kb = new KnowledgeBase();

	public EssayWriter(@NonNull String title, @NonNull String description) {
		essay = Essay.builder().title(title).description(description).build();
		initializeEndPoints();
	}

	public EssayWriter(File json) throws IOException {
		essay = Essay.fromFile(json);
		initializeEndPoints();
	}

	private void initializeEndPoints() {
		openAi = new OpenAiEndpoint();
		google = new GoogleEndpoint(new GoogleClient(GOOGLE_ENGINE_ID, GoogleClient.getApiKey()));
	}

	/**
	 * The main method orchestrates execution, based on the input the user provides
	 * in the command line.
	 */
	public static void main(String[] args) {

		try {

			switch (args[0]) {
			case "-s":
				// Writes structure from description passed as argument
				if (args.length != 2)
					throw new IllegalArgumentException();
				createStructure(new File(args[1]));
				break;
			case "-d":
				// Downloads content for an essay, given its structure and number of links to
				// follow, then populates and saves corresponding knowledge base
				if (args.length != 3)
					throw new IllegalArgumentException();
				createKnowledgeBase(new File(args[1]), Integer.parseInt(args[2]));
				break;
			case "-w":
				// writes an essay starting from its structure and an already populated
				// knowledge base
				if (args.length != 3)
					throw new IllegalArgumentException();
				write(new File(args[1]), new File(args[2]));
				break;
			case "-a":
				// Starts from essay description and writes the essay, saving structure and
				// knowledge base
				// in the process
				if (args.length != 3)
					throw new IllegalArgumentException();
				File description = new File(args[1]);
				File structure = createStructure(description);
				File kb = createKnowledgeBase(structure, Integer.parseInt(args[2]));
				File essay = write(structure, kb);
				System.out.println("Essay completed: " + essay.getCanonicalPath());
				break;
			case "-j":
				// Starts from essay structure and writes the essay, saving the knowledge base
				// in the
				// process.
				if (args.length != 3)
					throw new IllegalArgumentException();
				structure = new File(args[1]);
				kb = createKnowledgeBase(structure, Integer.parseInt(args[2]));
				essay = write(structure, kb);
				System.out.println("Essay completed: " + essay.getCanonicalPath());
				break;
			default:
				throw new IllegalArgumentException();
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
	 */
	public static File createStructure(File input) throws IOException {
		System.out.println("Creating essay structure from description in: " + input.getCanonicalPath() + "...");
		try (EssayWriter writer = new EssayWriter("My Essay", FileUtil.readFile(input))) {
			writer.createStructure();
			System.out.println(writer.essay.getFormattedSummary());
			FileUtil.writeFile(FileUtil.newFile(input, "sumary.txt"), writer.essay.getFormattedSummary());
			File json = FileUtil.newFile(input, "essay.json");
			writer.essay.toFile(json);
			return json;
		}
	}

	/**
	 * Downloads content relevant for an essay and stores it in the knowledge base.
	 * 
	 * @param structure JSON file with the structure of the essay.
	 * @param maxLinks  Maximum web pages to download for the research.
	 * @return A file with the newly created knowledge base.
	 */
	public static File createKnowledgeBase(File structure, int maxLinks) throws IOException {
		System.out.println("Creating knowledge base for essay described in: " + structure.getCanonicalPath() + "...");
		try (EssayWriter writer = new EssayWriter(structure)) {
			// Googling
			List<Link> links = writer.google(maxLinks);
			for (Link link : links) {
				System.out.println(link.toString());
			}

			// Downloading
			writer.kb.drop(); // Empties the KB
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
	 * @param knowledge Populated knowledge base to use for writing the essay.
	 * @return A File pointing to the complete essay.
	 */
	public static File write(File structure, File knowledge) throws IOException, ClassNotFoundException {
		System.out.println("Writing essay described in " + structure.getCanonicalPath() + " using KB "
				+ knowledge.getCanonicalPath() + " ...");
		try (EssayWriter writer = new EssayWriter(structure)) {
			writer.kb = KnowledgeBase.load(knowledge);

			// Writing
			writer.write();
			String content = writer.essay.getFormattedContent();

			System.out.println(content);
			File result = FileUtil.newFile(structure, "essay.txt");
			FileUtil.writeFile(result, content);
			writer.essay.toFile(FileUtil.newFile(structure, "essay_complete.json"));

			return result;
		}
	}

	/**
	 * Starting form the essay description, creates its structure of chapters and
	 * sections, already filled with respective summaries.
	 */
	public void createStructure() throws JsonProcessingException {

		String description = essay.description;
		if ((description == null) || (description.trim().length() == 0))
			throw new IllegalArgumentException("A description for the essay must be provided.");

		System.out.println("Creating essay structure from description:\n" + description + "\n");

		// Instantiate a service to create the essay structure
		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(COMPLETION_MODEL);
		chatSvc.setTemperature(40.0);

		// Set agent personality, instruct it to return JSON and provide one example
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

		// Invoke the agent to create the structure,
		// using the draft in description
		Map<String, String> params = new HashMap<>();
		params.put("description", description);
		ChatCompletion resp = chatSvc.complete(CompletionService.fillSlots(
				"You are tasked with creating the structure of a book based on the provided description. The book should consist of several chapters, each containing a title, a summary, and a list of sections. Each section should have a title and a summary. Please ensure that sections are not nested within each other.\n"
						+ "\n"
						+ "If possible, try to make the summaries of the chapters and sections at least 100 words long to provide substantial content for the book's outline.\n"
						+ "\n"
						+ "Think it through step by step and list all chapters and the sections they contain. Return the result using JSON format.\n"
						+ "\n" + "User Description: {{description}}",
				params));

		// Transform returned JSON into an Essay
		// which is then copied into the local instance.
		Essay created = JSON_MAPPER.readValue(resp.getText(), Essay.class);
		essay.chapters = new ArrayList<>(created.chapters);

		// Put IDs (chapter numbers) in sections (=chapters)
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
	 * Googles pages relevant to essay content.
	 * 
	 * @return List of links relevant to essay content.
	 */
	public List<Link> google() {
		return google(Integer.MAX_VALUE);
	}

	/**
	 * Googles pages relevant to essay content.
	 * 
	 * @param maxLinks Maximum number of links to return.
	 * @return List of links relevant to essay content.
	 */
	public List<Link> google(int maxLinks) {

		System.out.println("Searching content online...");

		// Googles each section in parallel, that is what parallelExecution() does

		List<Callable<List<Pair<Link, Integer>>>> tasks = new ArrayList<>();
		for (Section chapter : essay.chapters) {
			if (chapter.sections.size() == 0) // Google chapter
				tasks.add(() -> google(chapter, chapter));
			else { // Google sections
				for (Section section : chapter.sections) {
					tasks.add(() -> google(chapter, section));
				}
			}
		}
		List<Pair<Link, Integer>> allLinks = parallelExecution(tasks);
		System.out.println("Total of " + allLinks.size() + " found.");

		// Collects links, starting from top-rank results and avoiding duplicates

		allLinks.sort(new Comparator<>() {

			@Override
			public int compare(Pair<Link, Integer> o1, Pair<Link, Integer> o2) {
				return Integer.compare(o1.getRight(), o2.getRight());
			}
		});

		Set<Link> result = new HashSet<>();
		for (Pair<Link, Integer> l : allLinks) {
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
	 * Google pages relevant to content for given section in a chapter.
	 * 
	 * @return List of links relevant to essay content. Each link is returned in a
	 *         pair together with its rank in search results.
	 */
	public List<Pair<Link, Integer>> google(Section chapter, Section section) {

		System.out.println("Searching relevant pages for section " + section.id + ") " + section.title);

		// First create a list of search queries, based on what we want to search

		// Instanciates model to create Google searches
		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(COMPLETION_MODEL);
		chatSvc.setTemperature(50.0);

		// Dynamically build the prompt
		final String prompt = "Given the below chapter summary, section title and section summary, provided in XML tags, generate a list of search engine queries that can be used to search the topic corresponding to the section on the Internet."
				+ " Each query is a short sentence or a short list of key terms relevant for the section topic."
				+ " Include terms to provide a context for the topic, as described by the chapter summary, so that the query is clearly related to the chapter content."
				+ " Be creative and provide exactly 5 queries."
				+ " Strictly provide results as a JSON array of strings.\n\n"
				+ "<chapter_summary>{{chapter_summary}}</chapter_summary>\n\n"
				+ "<section_title>{{section_title}}</section_title>\n\n"
				+ "<section_summary>{{section_summary}}</section_summary>";

		// Provide data to fill slots in the prompt template
		Map<String, String> params = new HashMap<>();
		params.put("chapter_summary", chapter.summary);
		params.put("section_title", section.title);
		params.put("section_summary", section.summary);

		// Prepares the conversation; notice the call to fill the slots in the prompt
		// template
		List<OpenAiChatMessage> msgs = new ArrayList<>();
		msgs.add(new OpenAiChatMessage(Role.DEVELOPER,
				"You are an assistant helping a researcher in finding web pages that are relevant for the essay section they are writing."));
		msgs.add(new OpenAiChatMessage(Role.USER, CompletionService.fillSlots(prompt, params)));

		// Loops until the section has been successfully googled
		List<String> queries;
		while (true) {
			try {
				queries = JSON_MAPPER.readValue(chatSvc.complete(msgs).getText(), new TypeReference<List<String>>() {
				});
				break;
			} catch (JsonProcessingException e) { // Retry if GPT returned bad JSON
				LOG.warn("Retrying because of malformed JSON while googling section " + section.id, e);
			}
		}

		// Now submit each query and collect returned links
		List<Pair<Link, Integer>> result = new ArrayList<>();
		for (String query : queries) {

			System.out.println("Googling pages for: " + query);
			List<Link> links;
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
	 * @param links The links to download.
	 */
	public void download(List<Link> links) {

		System.out.println("Downloading " + links.size() + " pages.");

		// Retrieve contents for each link in parallel and save it in the knowledge base
		List<Callable<List<EmbeddedText>>> tasks = new ArrayList<>();
		for (Link link : links) {
			tasks.add(() -> download(link));
		}
		kb.insert(parallelExecution(tasks));
	}

	/**
	 * Download the content of a web page and save it into the Knowledge Base.
	 * 
	 * @param link Link to the page to download.
	 * @return The downloaded page, already embedded to be added to the knowledge
	 *         base.
	 */
	private List<EmbeddedText> download(Link link) {

		// Instantiate the service used to embed the downloaded pages
		OpenAiEmbeddingService embSvc = openAi.getEmbeddingService();

		// Set the maximum size for each embedded text chunk;
		// the following calculation ensures approximately 15 embeddings
		// will be used to compose each section.
		// Notice how the ModelService is fetched from the OpenAIEndpoint (openAI)
		// to retrieve the maximum prompt size for the completion model and the
		// embedding model.
		int writerSize = openAi.getModelService().getContextSize(WRITER_MODEL);
		int embSize = openAi.getModelService().getContextSize(embSvc.getModel());
		embSvc.setDefaultTextTokens(Math.min(embSize, (writerSize - SECTION_LENGTH_TOKENS) / 15));

		System.out.println("Downloading page: " + link);

		// Download the page content as a String
		String content = null;
		try {
			content = cleanup(ExtractionUtil.fromUrl(link.getUrl(), DOWNLOAD_TIMEOUT_MILLIS));
		} catch (Exception e) {
			// If an error occurs during page download, the page is skipped
			LOG.error("Error downloading " + link.getUrl(), e);
			return new ArrayList<>();
		}

		if (content.length() < MINIMUM_PAGE_SIZE) { // Some links do not work or cannot be read or have warning messages
			LOG.info("Content too short, skipping " + link.getUrl());
			return new ArrayList<>();
		}

		// Embed the downloaded content
		List<EmbeddedText> result = embSvc.embed(content);
		for (EmbeddedText emb : result) {
			emb.set("url", link); // saving link to the page, useful when writing references
		}

		return result;
	}

	/**
	 * (Re)writes the entire essay, based on its structure and the content in the
	 * knowledge base.
	 */
	public void write() {
		System.out.println("Writing essay...");

		List<Callable<List<Section>>> tasks = new ArrayList<>();
		for (Section chapter : essay.chapters) {
			if (chapter.sections.size() == 0) {
				// No (sub)sections, let's generate content for the chapter itself
				tasks.add(() -> write(chapter));
			} else {
				chapter.content = ""; // TODO write a short intro instead
			}
			for (Section section : chapter.sections) {
				tasks.add(() -> write(section));
			}
		}

		parallelExecution(tasks);
	}

	/**
	 * (Re)writes a section of the essay, based on its structure and the content in
	 * the knowledge base.
	 */
	private List<Section> write(Section section) {

		System.out.println("Writing section " + section.getFormattedTitle());

		// Instanciate services
		EmbeddingService embSvc = openAi.getEmbeddingService();
		OpenAiChatService chatSvc = openAi.getChatService();
		chatSvc.setModel(WRITER_MODEL);
		chatSvc.setTemperature(0.0);

		String prompt = "<context>{{context}}</context>\n\n" + "<summary>{{summary}}</summary>";
		Map<String, String> params = new HashMap<>();
		params.put("summary", section.summary);

		// This is the prompt used for creating the section
		List<OpenAiChatMessage> msgs = new ArrayList<>();
		msgs.add(new OpenAiChatMessage(Role.DEVELOPER,
				"You will be provided with a context and the summary of a section of an essay, both delimited by XML tags."
						+ " Your task is to use the content of the context to write the entire section of the essay."
						+ " Use a professional style." + " Avoid content repetitions but be detailed."
						+ " Output only the section content, not its title, do not create subsections."
						+ " Do not make up missing information or put placeholders for data you do not have."
						+ " Only if enough information is available in the content, produce a text at least "
						+ SECTION_LENGTH_TOKENS + " tokens long.\n\n"));
		OpenAiChatMessage ctxMsg = new OpenAiChatMessage(Role.USER, "");
		msgs.add(ctxMsg); // Placeholder

		// Searches the knowledge base for relevant content
		// (= builds the context)
		List<Pair<EmbeddedText, Double>> knowledge = kb.search(embSvc.embed(section.summary).get(0), 50, 0);

		// See how much context can fit the prompt
		OpenAiTokenizer counter = openAi.getModelService().getTokenizer(chatSvc.getModel());
		int ctxSize = openAi.getModelService().getContextSize(chatSvc.getModel());
		StringBuilder buf = new StringBuilder();
		String context = ""; 
		int i = 0;
		for (; i < knowledge.size(); ++i) {

			EmbeddedText emb = knowledge.get(i).getLeft();
			buf.append(emb.getText()).append("\n\n");
			String c = cleanup(buf.toString());
			params.put("context", c);

			ctxMsg.setContent(CompletionService.fillSlots(prompt, params));

			if (((int) (SECTION_LENGTH_TOKENS * 1.5) + counter.count(msgs)) > ctxSize)
				break;
			context = c; // Saves last context that would fit
		}

		// OK, now build the actual context
		ctxMsg.setContent(context);

		// Add generated content to the section
		section.content = chatSvc.complete(msgs).getText();

		// Save context (references) from most similar to the context (it means it was
		// used to cret it)
		EmbeddedText summaryEmbedding = embSvc.embed(section.content).get(0);
		List<Pair<EmbeddedText, Double>> ctx = new ArrayList<>();
		for (int j = 0; j < i; ++j) {
			EmbeddedText emb = knowledge.get(j).getLeft();
			ctx.add(new ImmutablePair<>(emb, emb.similarity(summaryEmbedding)));
		}
		ctx.sort(new Comparator<>() {

			@Override
			public int compare(Pair<EmbeddedText, Double> o1, Pair<EmbeddedText, Double> o2) {
				return -Double.compare(o1.getRight(), o2.getRight());
			}
		});
		for (Pair<EmbeddedText, Double> p : ctx) {
			section.context.add(p.getLeft());
			section.contextWeight.add(p.getRight());
		}

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
