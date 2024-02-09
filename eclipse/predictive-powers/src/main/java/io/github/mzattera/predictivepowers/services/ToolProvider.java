/**
 * 
 */
package io.github.mzattera.predictivepowers.services;

import java.util.Collection;
import java.util.function.Supplier;

import lombok.NonNull;

/**
 * A tool provider provides executable instances of {@link Tool}s to
 * ({@link Agent}s.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public interface ToolProvider {

	/**
	 * Get tools available from this provider.
	 * 
	 * Notice this is expected to be an unmodifiable list; use other methods to
	 * populate tools list properly.
	 * 
	 * @return List of tool IDs.
	 */
	Collection<String> getToolIds();

	/**
	 * Get a new instance of a tool.
	 * 
	 * @param toolId Unique Id for the tool;
	 * 
	 * @return A new instance of the tool, or null if a Supplier for that tool was
	 *         never added.
	 */
	Tool getTool(@NonNull String toolId);

	/**
	 * Add one tool to the list of tools available from this provider.
	 * 
	 * @param toolId   Unique id for the tool.
	 * @param provider A factory method to create instances of the too, when needed.
	 */
	void addTool(@NonNull String toolId, @NonNull Supplier<? extends Tool> provider);

	/**
	 * Remove one tool from list of tools available from this provider.
	 * 
	 * @param id The unique ID for the tool.
	 */
	void removeTool(@NonNull String toolId);

	/**
	 * Remove all tools from this provider/
	 */
	void clear();
}
