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

/**
 * Basic implementation of {@link ToolProvider} which is backed by a Map.
 */
public class SimpleToolProvider implements ToolProvider {

	@Getter(AccessLevel.PROTECTED)
	private Map<String, Supplier<? extends Tool>> suppliers = new HashMap<>();

	/**
	 * Creates an empty provider.
	 */
	public SimpleToolProvider() {
	}

	/**
	 * Creates a provider containing all of the given tools.
	 */
	public SimpleToolProvider(Collection<? extends Tool> tools) {
		for (Tool tool : tools)
			suppliers.put(tool.getId(), () -> {
				try {
					return (Tool) tool.getClass().getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new RuntimeException("Error instanciating tool: " + tool.getId(), e);
				}
			});
	}

	@Override
	public Collection<String> getToolIds() {
		return suppliers.keySet();
	}

	@Override
	public Tool getTool(@NonNull String toolId) {
		Supplier<? extends Tool> s = suppliers.get(toolId);
		if (s == null)
			return null;
		return s.get();
	}

	@Override
	public void addTool(@NonNull String toolId, @NonNull Supplier<? extends Tool> provider) {
		suppliers.put(toolId, provider);
	}

	@Override
	public void removeTool(@NonNull String toolId) {
		suppliers.remove(toolId);
	}

	@Override
	public void clear() {
		suppliers.clear();
	}
}
