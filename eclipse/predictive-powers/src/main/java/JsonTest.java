import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.QnAPair;
import io.github.mzattera.predictivepowers.QuestionService;
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

//			ChatService chat = oai.getChatService();
//			for (int i=0; i<5;++i)
//				chat.chat(i + "");
//			 System.out.println(chat.complete("Who was Alan Turing").toString());
			
			QuestionService qs = oai.getQuestionService();
			
			 for (QnAPair q: qs.getQuestions("Alan Mathison Turing OBE FRS (/ˈtjʊərɪŋ/; 23 June 1912 – 7 June 1954) was an English mathematician, computer scientist, logician, cryptanalyst, philosopher, and theoretical biologist.[6] Turing was highly influential in the development of theoretical computer science, providing a formalisation of the concepts of algorithm and computation with the Turing machine, which can be considered a model of a general-purpose computer.[7][8][9] He is widely considered to be the father of theoretical computer science and artificial intelligence.[10]\r\n"
				 		+ "\r\n"
				 		+ "Born in Maida Vale, London, Turing was raised in southern England. He graduated at King's College, Cambridge, with a degree in mathematics. Whilst he was a fellow at Cambridge, he published a proof demonstrating that some purely mathematical yes–no questions can never be answered by computation and defined a Turing machine, and went on to prove that the halting problem for Turing machines is undecidable. In 1938, he obtained his PhD from the Department of Mathematics at Princeton University. During the Second World War, Turing worked for the Government Code and Cypher School (GC&CS) at Bletchley Park, Britain's codebreaking centre that produced Ultra intelligence. For a time he led Hut 8, the section that was responsible for German naval cryptanalysis. Here, he devised a number of techniques for speeding the breaking of German ciphers, including improvements to the pre-war Polish bomba method, an electromechanical machine that could find settings for the Enigma machine. Turing played a crucial role in cracking intercepted coded messages that enabled the Allies to defeat the Axis powers in many crucial engagements, including the Battle of the Atlantic.[11][12]")) {
				 
				 System.out.println(q.getQuestion());
				 System.out.println(q.getAnswer());
				 System.out.println(q.getContext());
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
