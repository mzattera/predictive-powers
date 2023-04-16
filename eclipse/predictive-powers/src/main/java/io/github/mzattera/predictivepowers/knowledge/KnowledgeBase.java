/**
 * 
 */
package io.github.mzattera.predictivepowers.knowledge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import io.github.mzattera.predictivepowers.service.EmbeddedText;
import io.github.mzattera.predictivepowers.service.EmbeddingService;

/**
 * A knowledge base contains information in form of embedded text that can be
 * easily searched (using text similarity).
 * 
 * Each knowledge base can be divided in domains, and operations limited
 * performed only to a single domain, to improve performance. By default, a
 * domain called "_default" is created inside each KnowledgeBase.
 * 
 * Notice that only one instance of embedded text can exist in a knowledge base.
 * That is same piece of embedded text (even if with different properties) can
 * exist only once in the knowledge base.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class KnowledgeBase {

	public static final String DEFAULT_DOMAIN = "_default";

	private final Map<String, Set<EmbeddedText>> domains = new HashMap<>();

	public KnowledgeBase() {
		createDomain(DEFAULT_DOMAIN);
	}

	/**
	 * 
	 * Adds a domain to the KnowledgeBase (or does nothing if it already exists).
	 * 
	 * @param domain Name of the domain (case sensitive).
	 */
	public void createDomain(String domain) {
		if (!domains.containsKey(domain))
			domains.put(domain, new HashSet<>());
	}

	/**
	 * Deletes a domain and all of its data from the KnowledgeBase (or does nothing
	 * if it does not exists).
	 * 
	 * @param domain Name of the domain (case sensitive).
	 */
	public void dropDomain(String domain) {
		domains.remove(domain);
	}

	/**
	 * 
	 * @return The list of domains in this KnowledgeBase.
	 */
	public Set<String> listDomains() {
		return domains.keySet();
	}

	/**
	 * Adds embedded text to the default domain.
	 * 
	 * @param e
	 */
	public void insert(EmbeddedText e) {
		insert(DEFAULT_DOMAIN, e);
	}

	/**
	 * Adds embedded text to given domain. The domain must exist already.
	 * 
	 * @param domain Domain name.
	 * @param e
	 */
	public void insert(String domain, EmbeddedText e) {
		if (!domains.containsKey(domain))
			throw new IllegalArgumentException("Domain: " + domain + " does not exist.");

		delete(e); // avoids duplicates
		domains.get(domain).add(e);
	}

	/**
	 * Removes given text from all domains.
	 * 
	 * @param e
	 */
	public void delete(String txt) {
		delete(EmbeddedText.builder().text(txt).build());
	}

	/**
	 * Removes given text from a specific domain.
	 * 
	 * @param domain Removes only from this domain.
	 * @param e
	 */
	public void delete(String domain, String txt) {
		delete(domain, EmbeddedText.builder().text(txt).build());
	}

	/**
	 * Removes embedded text from all domains.
	 * 
	 * @param e
	 */
	public void delete(EmbeddedText e) {
		for (Set<EmbeddedText> s : domains.values())
			delete(s, e);
	}

	/**
	 * Removes embedded text from a specific domain.
	 * 
	 * @param domain Removes only from this domain.
	 * @param e
	 */
	public void delete(String domain, EmbeddedText e) {
		delete(domains.get(domain), e);
	}

	/**
	 * Removes embeddings from all domains.
	 * 
	 * @param m A matcher that defines a matching rule; all matching embeddings will
	 *          be removed.
	 */
	public void delete(EmbeddedTextMatcher m) {
		for (Set<EmbeddedText> s : domains.values())
			delete(s, m);
	}

	/**
	 * Removes embeddings from a specific domain.
	 * 
	 * @param domain Removes only from this domain.
	 * @param m      A matcher that defines a matching rule; all matching embeddings
	 *               will be removed.
	 */
	public void delete(String domain, EmbeddedTextMatcher m) {
		delete(domains.get(domain), m);
	}

	private static void delete(Set<EmbeddedText> s, EmbeddedText e) {
		s.remove(e);
	}

	private static void delete(Set<EmbeddedText> s, EmbeddedTextMatcher m) {
		Iterator<EmbeddedText> it = s.iterator();
		while (it.hasNext()) {
			EmbeddedText e = it.next();
			if (m.match(e))
				it.remove();
		}
	}

	/**
	 * @param m A matcher that defines a matching rule; all matching embeddings will
	 *          be returned.
	 * @return All embeddings matching given rule.
	 */
	public List<EmbeddedText> query(EmbeddedTextMatcher m) {
		List<EmbeddedText> result = new ArrayList<>();

		for (Set<EmbeddedText> s : domains.values())
			result.addAll(query(s, m));

		return result;
	}

	/**
	 * @param domain Query only this domain.
	 * @param m      A matcher that defines a matching rule; all matching embeddings
	 *               will be returned.
	 * @return All embeddings matching given rule.
	 */
	public List<EmbeddedText> query(String domain, EmbeddedTextMatcher m) {
		return query(domains.get(domain), m);
	}

	private static List<EmbeddedText> query(Set<EmbeddedText> s, EmbeddedTextMatcher m) {
		List<EmbeddedText> result = new ArrayList<>();

		for (EmbeddedText e : s)
			if (m.match(e))
				result.add(e);

		return result;
	}

	// TODO: add a search filtered by a matcher

	/**
	 * Searches in the knowledge base, returning the list of embeddings most similar
	 * to given query.
	 * 
	 * @param query  Embedded text representing the search target.
	 * @param limit  Maximum number of results to return.
	 * @param offset How many results to skip (for pagination).
	 */
	public List<Pair<EmbeddedText, Double>> search(EmbeddedText query, int limit, int offset) {
		List<Pair<EmbeddedText, Double>> result = new ArrayList<>();

		for (Set<EmbeddedText> s : domains.values())
			search(s, query, result, limit + offset);

		return skip(result, offset);
	}

	/**
	 * Searches in given domain, returning the list of embeddings most similar to
	 * given query.
	 * 
	 * @param domain
	 * @param query  Embedded text representing the search target.
	 * @param limit  Maximum number of results to return.
	 * @param offset How many results to skip (for pagination).
	 */
	public List<Pair<EmbeddedText, Double>> search(String domain, EmbeddedText e, int limit, int offset) {
		List<Pair<EmbeddedText, Double>> result = new ArrayList<>();
		search(domains.get(domain), e, result, limit + offset);
		return skip(result, offset);
	}

	/**
	 * Searches in a set of embeddings, returning the list of those most similar to
	 * query.
	 * 
	 * @param set    The embeddings used for the search.
	 * @param query  Embedded text representing the search target.
	 * @param result List of results, sorted by decreasing similarity. If this is
	 *               not empty, result from the search will be merged with those
	 *               provided.
	 * @param limit  Maximum number of results to return.
	 */
	private static void search(Set<EmbeddedText> set, EmbeddedText query, List<Pair<EmbeddedText, Double>> result,
			int limit) {

		// Shortens results, if needed
		while (result.size() > limit)
			result.remove(result.size() - 1);

		for (EmbeddedText e : set) {

			double similarity = 0.0;
			try { // takes care of the embedding not being created using same model
				similarity = e.similarity(query);
			} catch (IllegalArgumentException ex) {
				continue;
			}

			int i = 0;
			for (; i < result.size(); ++i) {
				Pair<EmbeddedText, Double> p = result.get(i);
				if (p.getRight() < similarity) {
					// Insert here
					result.add(i, new ImmutablePair<EmbeddedText, Double>(e, similarity));
					if (result.size() > limit)
						result.remove(result.size() - 1);
					break;
				}
			}

			if ((result.size() < limit) && (i >= result.size())) {
				// add to the bottom
				result.add(new ImmutablePair<EmbeddedText, Double>(e, similarity));
			}
		} // for each embedding
	}

	private static List<Pair<EmbeddedText, Double>> skip(List<Pair<EmbeddedText, Double>> result, int offset) {

		if (result.size() <= offset) {
			result.clear();
		} else {
			for (int i = 0; i < offset; ++i)
				result.remove(0);
		}

		return result;
	}

	/**
	 * Adds the content of given file to this knowledge base.
	 * 
	 * @param fileName
	 * @param es       A service to use to create embeddings from the file content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void add(String fileName, EmbeddingService es) throws IOException, SAXException, TikaException {
		add(DEFAULT_DOMAIN, new File(fileName), es);
	}

	/**
	 * Adds the content of given file to this knowledge base.
	 * 
	 * @param file
	 * @param es   A service to use to create embeddings from the file content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void add(File file, EmbeddingService es) throws IOException, SAXException, TikaException {
		add(DEFAULT_DOMAIN, file, es);
	}

	/**
	 * Adds the content of given file to given domain this knowledge base.
	 * 
	 * @param domain
	 * @param fileName
	 * @param es       A service to use to create embeddings from the file content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void add(String domain, String fileName, EmbeddingService es)
			throws IOException, SAXException, TikaException {
		add(domain, new File(fileName), es);
	}

	/**
	 * Adds the content of given file to given domain in this knowledge base.
	 * 
	 * @param file
	 * @param es   A service to use to create embeddings from the file content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void add(String domain, File file, EmbeddingService es) throws IOException, SAXException, TikaException {
		String content = ExtractionUtils.getText(file);
		for (EmbeddedText e : es.embed(content)) {
			e.set("fileName", file.getName());
			insert(domain, e);
		}
	}

	/**
	 * Adds the content of given folder and its sub-folders to this knowledge base.
	 * 
	 * @param folderName
	 * @param es         A service to use to create embeddings from the file
	 *                   content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void addFolder(String folderName, EmbeddingService es) throws IOException, SAXException, TikaException {
		addFolder(DEFAULT_DOMAIN, new File(folderName), es);
	}

	/**
	 * Adds the content of given folder and its sub-folders to given domain in this
	 * knowledge base.
	 * 
	 * @param folderName
	 * @param es         A service to use to create embeddings from the file
	 *                   content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void addFolder(String domain, String folderName, EmbeddingService es)
			throws IOException, SAXException, TikaException {
		addFolder(domain, new File(folderName), es);
	}

	/**
	 * Adds the content of given folder and its subfolders to this knowledge base.
	 * 
	 * @param folder
	 * @param es     A service to use to create embeddings from the file content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void addFolder(File folder, EmbeddingService es) throws IOException, SAXException, TikaException {
		addFolder(DEFAULT_DOMAIN, folder, es);
	}

	/**
	 * Adds the content of given folder and its subfolders to given domain in this
	 * knowledge base.
	 * 
	 * @param folder
	 * @param es     A service to use to create embeddings from the file content.
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	public void addFolder(String domain, File folder, EmbeddingService es)
			throws IOException, SAXException, TikaException {
		if (!folder.isDirectory() || !folder.canRead()) {
			throw new IOException("Folder cannot be read from: " + folder.getCanonicalPath());
		}

		for (File f : folder.listFiles()) {
			if (f.isFile())
				add(domain, f, es);
			else
				addFolder(domain, f, es);
		}
	}
}