/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.files;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * OpenAI File data, as returned by /files API.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
@Getter
@Setter
@ToString
public final class File {

    String id;
    String object;
    long bytes;
    long createdAt;
    String filename;
    String purpose;
}