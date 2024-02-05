/**
 * 
 */
package io.github.mzattera.predictivepowers.services.messages;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a {@link MessagePart} that contains only text.
 * 
 * @author Massimiliano "Maxi" Zattera.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class TextPart implements MessagePart {

	@NonNull
	protected String content;
}
