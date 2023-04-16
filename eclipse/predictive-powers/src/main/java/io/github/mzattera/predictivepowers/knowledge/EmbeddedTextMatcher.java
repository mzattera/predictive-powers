/**
 * 
 */
package io.github.mzattera.predictivepowers.knowledge;

import io.github.mzattera.predictivepowers.service.EmbeddedText;

/**
 * This is used to define a matching rule for embedded text.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
public interface EmbeddedTextMatcher {

	/**
	 * 
	 * @param e
	 * @return True if given embedded text is a match for this rule.
	 */
	public boolean match(EmbeddedText e);
}
