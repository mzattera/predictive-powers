/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

/**
 * Marker interface for classes that can be parts of a {@link ChatMessage}.
 * 
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
public interface MessagePart {

	/**
	 * 
	 * @return A string representation of the content of this message part. Notice
	 *         not all parts are easily representable as text (e.g. a file).
	 */
	String getContent();
}
