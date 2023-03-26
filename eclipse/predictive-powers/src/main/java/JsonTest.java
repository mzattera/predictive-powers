import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.ChatService;
import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonTest {
	private final static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public static void main(String[] args) {

		try {
			OpenAiEndpoint oai = OpenAiEndpoint.getInstance();

//			CompletionsRequest r = new CompletionsRequest("davinci");
//			System.out.println("[===\n"+mapper.writeValueAsString(r)+"\n===]");
//			r.getLogitBias().put("fifio", 3);
//			System.out.println("[===\n"+mapper.writeValueAsString(r)+"\n===]");

//			for (Model m : oai.getInventoryService().listModels()) {
//				System.out.println(m.getId());
//			}

//			System.out.println(oai.getCompletionService().complete("How high is Mt. Everest?").toString());

			ChatService chat = oai.getChatService();
//			for (int i=0; i<5;++i)
//				chat.chat(i + "");
			 System.out.println(chat.complete("Who was Alan Turing").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
