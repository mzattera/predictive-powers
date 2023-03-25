import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.client.Model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonTest {
	String name = "Maxi";
	int id = 123;

	public static void main(String[] args) {

		try {
			JsonTest inst = new JsonTest();
			ObjectMapper mapper = new ObjectMapper();

			String json = mapper.writeValueAsString(inst);
			System.out.println(json);

			OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
			System.out.println(oai.getApiKey());
			
			for (Model m:oai.listModels()) {
				System.out.println(m.id);
			}
			
			System.out.println(oai.retrieveModel("text-davinci-001"));
			System.out.println(oai.retrieveModel("banana"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
