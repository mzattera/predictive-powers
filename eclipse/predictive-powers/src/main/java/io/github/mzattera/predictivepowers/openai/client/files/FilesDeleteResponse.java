/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.files;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from /files DELETE API.
 * 
 * @author Massimiliano "Maxi" Zattera.
 *
 */
@Getter
@Setter
@ToString
public class FilesDeleteResponse {

    String id;
    String object;
    boolean deleted;    
}
