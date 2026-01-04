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
package io.github.mzattera.predictivepowers.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import io.github.mzattera.predictivepowers.services.ChatService;
import io.github.mzattera.predictivepowers.services.messages.ChatMessage;
import lombok.Getter;
import lombok.NonNull;

/**
 * This class represents a short term memory used to store conversations, made
 * of {@link ChatMessage}s.
 * 
 * This class is thread safe.
 * 
 * @author Luna
 */
public class ShortTermMemory {

	// --- Event Classes ---

	/**
	 * This class represents an even that is fired when messages are added to this
	 * memory.
	 */
	public static class MessageAddedEvent extends EventObject {
		private static final long serialVersionUID = 1L;

		@Getter
		private final @Nullable ChatService service;

		@Getter
		private final @NonNull List<ChatMessage> messages;

		/**
		 * 
		 * @param source   Source of the event.
		 * @param service  The service adding messages to the conversation (optional).
		 * @param messages
		 */
		public MessageAddedEvent(@NonNull Object source, @Nullable ChatService service,
				@NonNull List<? extends ChatMessage> messages) {
			super(source);
			this.service = service;
			this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
		}
	}

	/**
	 * This class represents an even that is fired when this memory is cleared.
	 */
	public static class MemoryClearedEvent extends EventObject {
		private static final long serialVersionUID = 1L;

		@Getter
		private final @Nullable ChatService service;

		public MemoryClearedEvent(@NonNull Object source, @Nullable ChatService service) {
			super(source);
			this.service = service;
		}
	}

	// --- Listener Interfaces ---

	/**
	 * This is the interface all listeners for messages added events must implement.
	 */
	public interface ItemAddedListener extends EventListener {
		void onMessageAdded(MessageAddedEvent event);
	}

	/**
	 * This is the interface all listeners for memory cleared events must implement.
	 */
	public interface MemoryClearedListener extends EventListener {
		void onMemoryCleared(MemoryClearedEvent event);
	}

	private final List<ChatMessage> messages = new ArrayList<>();
	private final List<ItemAddedListener> itemListeners = new CopyOnWriteArrayList<>();
	private final List<MemoryClearedListener> clearListeners = new CopyOnWriteArrayList<>();

	/**
	 * Returns an unmodifiable, thread-safe snapshot of the current state of the
	 * memory.
	 */
	public List<ChatMessage> getMessages() {
		synchronized (messages) {
			List<ChatMessage> snapshot = new ArrayList<>(messages);
			return Collections.unmodifiableList(snapshot);
		}
	}

	/**
	 * Add messages to the conversation and notifies listeners.
	 * 
	 * @param service     The service adding the messages.
	 * @param newMessages List of messages to be added.
	 */
	public void add(@Nullable ChatService service, @NonNull List<? extends ChatMessage> newMessages) {
		synchronized (messages) {
			this.messages.addAll(newMessages);
		}
		fireItemAdded(service, newMessages);
	}

	/**
	 * Clears all messages from the memory and notifies listeners.
	 * 
	 * @param service The service performing the clear action.
	 */
	public void clear(@Nullable ChatService service) {
		synchronized (messages) {
			this.messages.clear();
		}
		fireMemoryCleared(service);
	}

	// --- Listener Management ---

	public void addItemAddedListener(@NonNull ItemAddedListener listener) {
		itemListeners.add(listener);
	}

	public void removeItemAddedListener(ItemAddedListener listener) {
		itemListeners.remove(listener);
	}

	public void addMemoryClearedListener(@NonNull MemoryClearedListener listener) {
		clearListeners.add(listener);
	}

	public void removeMemoryClearedListener(MemoryClearedListener listener) {
		clearListeners.remove(listener);
	}

	// --- Event Dispatchers ---

	protected void fireItemAdded(@Nullable ChatService service, @NonNull List<? extends ChatMessage> messages) {
		if (itemListeners.isEmpty())
			return;
		MessageAddedEvent event = new MessageAddedEvent(this, service, messages);
		for (ItemAddedListener listener : itemListeners) {
			listener.onMessageAdded(event);
		}
	}

	protected void fireMemoryCleared(@Nullable ChatService service) {
		if (clearListeners.isEmpty())
			return;
		MemoryClearedEvent event = new MemoryClearedEvent(this, service);
		for (MemoryClearedListener listener : clearListeners) {
			listener.onMemoryCleared(event);
		}
	}
}