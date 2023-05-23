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

package io.github.mzattera.predictivepowers.knowledge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import io.github.mzattera.predictivepowers.services.EmbeddedText;
import lombok.NonNull;

/**
 * A knowledge base contains information in form of embedded text that can be
 * easily searched (using text similarity).
 * 
 * Each knowledge base can be divided in domains, and operations performed only
 * in a single domain, to improve performance. By default, a domain called
 * "_default" is created inside each KnowledgeBase.
 * 
 * Notice that only one instance of embedded text can exist in a knowledge base.
 * That is same piece of embedded text (even if with different properties) can
 * exist only once in the knowledge base.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public class KnowledgeBase implements Serializable {

	private static final long serialVersionUID = 42424242L;

	public static final String DEFAULT_DOMAIN = "_default";

	/** Locks used for thread safety */
	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();

	private final Map<String, Set<EmbeddedText>> domains = new HashMap<>();

	public KnowledgeBase() {
		createDomain(DEFAULT_DOMAIN);
	}

	/**
	 * Stores this knowledge base in a file.
	 * 
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void save(String fileName) throws FileNotFoundException, IOException {
		save(new File(fileName));
	}

	/**
	 * Stores this knowledge base in a file.
	 * 
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void save(File file) throws FileNotFoundException, IOException {
		writeLock.lock();
		try {
			try (FileOutputStream fos = new FileOutputStream(file);
					ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				oos.writeObject(this);
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Reads a previously saved knowledge base from file.
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static KnowledgeBase load(String fileName) throws ClassNotFoundException, IOException {
		return load(new File(fileName));
	}

	/**
	 * Reads a previously saved knowledge base from file.
	 * 
	 * @param file
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static KnowledgeBase load(File file) throws ClassNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
			return (KnowledgeBase) ois.readObject();
		}
	}

	/**
	 * 
	 * Adds a domain to the KnowledgeBase (or does nothing if it already exists).
	 * 
	 * @param domain Name of the domain (case sensitive).
	 */
	public void createDomain(String domain) {
		writeLock.lock();
		try {
			if (!domains.containsKey(domain))
				domains.put(domain, new HashSet<>());
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Deletes a domain and all of its data from the KnowledgeBase (or does nothing
	 * if it does not exists).
	 * 
	 * @param domain Name of the domain (case sensitive).
	 */
	public void dropDomain(String domain) {
		writeLock.lock();
		try {
			domains.remove(domain);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * 
	 * @return The list of domains in this KnowledgeBase.
	 */
	public List<String> listDomains() {
		readLock.lock();
		try {
			return new ArrayList<String>(domains.keySet());
		} finally {
			readLock.unlock();
		}
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
		writeLock.lock();
		try {
			insert(domains.get(domain), e);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Adds embedded text to the default domain.
	 * 
	 * @param e
	 */
	public void insert(Collection<EmbeddedText> e) {
		insert(DEFAULT_DOMAIN, e);
	}

	/**
	 * Adds embedded text to given domain. The domain must exist already.
	 * 
	 * @param domain Domain name.
	 * @param e
	 */
	public void insert(String domain, Collection<EmbeddedText> e) {
		writeLock.lock();
		try {
			Set<EmbeddedText> set = domains.get(domain);
			for (EmbeddedText t : e)
				insert(set, t);
		} finally {
			writeLock.unlock();
		}
	}

	private static void insert(@NonNull Set<EmbeddedText> set, @NonNull EmbeddedText e) {
		// We do not synch as this is private so, if you end up here, you should have a
		// write lock already
		set.add(e);
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
		writeLock.lock();
		try {
			for (Set<EmbeddedText> s : domains.values())
				delete(s, e);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Removes embedded text from a specific domain.
	 * 
	 * @param domain Removes only from this domain.
	 * @param e
	 */
	public void delete(String domain, EmbeddedText e) {
		writeLock.lock();
		try {
			delete(domains.get(domain), e);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Removes embeddings from all domains.
	 * 
	 * @param m A matcher that defines a matching rule; all matching embeddings will
	 *          be removed.
	 */
	public void delete(EmbeddedTextMatcher m) {
		writeLock.lock();
		try {
			for (Set<EmbeddedText> s : domains.values())
				delete(s, m);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Removes embeddings from a specific domain.
	 * 
	 * @param domain Removes only from this domain.
	 * @param m      A matcher that defines a matching rule; all matching embeddings
	 *               will be removed.
	 */
	public void delete(String domain, EmbeddedTextMatcher m) {
		writeLock.lock();
		try {
			delete(domains.get(domain), m);
		} finally {
			writeLock.unlock();
		}
	}

	private static void delete(@NonNull Set<EmbeddedText> s, @NonNull EmbeddedText e) {
		// We do not synch as this is private so, if you end up here, you should have a
		// write lock already
		s.remove(e);
	}

	private static void delete(@NonNull Set<EmbeddedText> s, @NonNull EmbeddedTextMatcher m) {
		// We do not synch as this is private so, if you end up here, you should have a
		// write lock already
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

		readLock.lock();
		try {
			for (Set<EmbeddedText> s : domains.values())
				result.addAll(query(s, m));

			return result;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * @param domain Query only this domain.
	 * @param m      A matcher that defines a matching rule; all matching embeddings
	 *               will be returned.
	 * @return All embeddings matching given rule.
	 */
	public List<EmbeddedText> query(String domain, EmbeddedTextMatcher m) {
		readLock.lock();
		try {
			return query(domains.get(domain), m);
		} finally {
			readLock.unlock();
		}
	}

	private static List<EmbeddedText> query(Set<EmbeddedText> s, EmbeddedTextMatcher m) {
		// We do not synch as this is private so, if you end up here, you should have a
		// read lock already
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

		readLock.lock();
		try {
			for (Set<EmbeddedText> s : domains.values())
				search(s, query, result, limit + offset);

			return skip(result, offset);
		} finally {
			readLock.unlock();
		}
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
	public List<Pair<EmbeddedText, Double>> search(String domain, EmbeddedText query, int limit, int offset) {
		List<Pair<EmbeddedText, Double>> result = new ArrayList<>();

		readLock.lock();
		try {
			search(domains.get(domain), query, result, limit + offset);
			return skip(result, offset);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Searches in a set of embeddings, returning the list of those most similar to
	 * query.
	 * 
	 * @param set    The embeddings used for the search.
	 * @param query  Embedded text representing the search target.
	 * @param result List of results, sorted by decreasing similarity. If this is
	 *               not empty, result from the search will be merged with those
	 *               already already in the list.
	 * @param limit  Maximum number of results to return.
	 */
	private static void search(@NonNull Set<EmbeddedText> set, @NonNull EmbeddedText query,
			List<Pair<EmbeddedText, Double>> result, int limit) {
		// We do not synch as this is private so, if you end up here, you should have a
		// read lock already

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
				// add to the end
				result.add(new ImmutablePair<EmbeddedText, Double>(e, similarity));
			}
		} // for each embedding
	}

	/**
	 * Skip results from top of the list, this is used for pagination.
	 * 
	 * @param result
	 * @param offset
	 * @return
	 */
	private static List<Pair<EmbeddedText, Double>> skip(List<Pair<EmbeddedText, Double>> result, int offset) {

		if (result.size() <= offset) {
			result.clear();
		} else {
			for (int i = 0; i < offset; ++i)
				result.remove(0);
		}

		return result;
	}
}