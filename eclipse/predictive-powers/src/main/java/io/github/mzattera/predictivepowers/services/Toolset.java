/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Basic implementation of a {@link Capability}
 * 
 * This is a collection of unrelated tools, backed by a Map.
 */
public class Toolset implements Capability {

	public static final String DEFAULT_ID = "tools";

	@Getter
	@NonNull
	private final String id;

	@Getter
	@Setter
	@NonNull
	private String description;

	@Getter(AccessLevel.PROTECTED)
	private final Map<String, Supplier<? extends Tool>> suppliers = new HashMap<>();

	/**
	 * Creates an empty capability with default ID.
	 */
	public Toolset() {
		this(DEFAULT_ID);
	}

	/**
	 * Creates an empty capability.
	 */
	public Toolset(@NonNull String id) {
		this(id, "Capability: " + id);
	}

	/**
	 * Creates an empty capability.
	 */
	public Toolset(@NonNull String id, @NonNull String description) {
		this(id, description, null);
	}

	/**
	 * Creates a capability with default ID containing all of the given tools.
	 */
	public Toolset(@NonNull Collection<Class<?>> tools) {
		this(DEFAULT_ID, tools);
	}

	/**
	 * Creates a capability containing all of the given tools.
	 */
	public Toolset(@NonNull String id, @NonNull Collection<Class<?>> tools) {
		this(DEFAULT_ID, "Capability: " + id, tools);
	}

	/**
	 * Creates a capability containing all of the given tools.
	 */
	public Toolset(@NonNull String id, @NonNull String description, Collection<Class<?>> tools) {

		this.id = id;
		this.description = description;

		if (tools != null) {
			for (Class<?> c : tools)
				putTool(id, c);
		}
	}

	@Override
	public Collection<String> getToolIds() {
		return suppliers.keySet();
	}

	@Override
	public Tool getNewToolInstance(@NonNull String toolId) {
		if (!initialized)
			throw new IllegalStateException("Capability must be initialized before it can provide tools.");

		Supplier<? extends Tool> s = suppliers.get(toolId);
		if (s == null)
			return null;

		Tool tool = s.get();
		tool.setCapability(this);
		return tool;
	}

	@Override
	public void putTool(@NonNull String toolId, @NonNull Supplier<? extends Tool> supplier) {
		suppliers.put(toolId, supplier);
	}

	public void putTool(@NonNull String toolId, @NonNull Class<?> tool) {
		suppliers.put(id, () -> {
			try {
				return (Tool) tool.getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				return null;
			}
		});
	}

	@Override
	public void removeTool(@NonNull String toolId) {
		suppliers.remove(toolId);
	}

	@Override
	public void clear() {
		suppliers.clear();
	}

	@Override
	public void close() {
	}

	@Getter
	@Setter(AccessLevel.PROTECTED)
	private boolean initialized;

	@Override
	public void init(@NonNull Agent agent) throws ToolInitializationException {
		if (initialized)
			throw new IllegalStateException();
		initialized = true;
	}
}
