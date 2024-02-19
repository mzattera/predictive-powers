/**
 * 
 */
package io.github.mzattera.predictivepowers.openai.client.threads;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Massimiliano "Maxi" Zattera
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ToolOutputsRequest {

	@Builder.Default
	List<ToolOutput> toolOutputs = new ArrayList<>();
}
