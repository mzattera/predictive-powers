package io.github.mzattera.predictivepowers.openai.client.chat;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.github.mzattera.predictivepowers.openai.client.chat.FunctionChoice.FunctionCallSerializer;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Instruct model whether to produce function calls or not.
 */
@Getter
@Setter
@Builder
@ToString
@JsonSerialize(using = FunctionCallSerializer.class)
public class FunctionChoice {

	/** Use this to indicate function_call = none */
	public final static FunctionChoice NONE = new FunctionChoice(Mode.NONE, null);

	/** USe this to indicate function_call = auto */
	public final static FunctionChoice AUTO = new FunctionChoice(Mode.AUTO, null);

	/**
	 * Possible response options for a model when function calling is available.
	 */
	private static enum Mode {
		/** The model does not call a function. */
		NONE,

		/** The model can pick between amessage or calling a function */
		AUTO,

		/** The model will call a function */
		FUNCTION
	}

	/**
	 * Provides custom serialization.
	 */
	static final class FunctionCallSerializer extends StdSerializer<FunctionChoice> {

		private static final long serialVersionUID = -4506958348962250647L;

		public FunctionCallSerializer() {
			this(null);
		}

		public FunctionCallSerializer(Class<FunctionChoice> t) {
			super(t);
		}

		@Override
		public void serialize(FunctionChoice value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonProcessingException {

			switch (value.getMode()) {
			case NONE:
				jgen.writeString("none");
				break;
			case AUTO:
				jgen.writeString("auto");
				break;
			case FUNCTION:
				jgen.writeStartObject();
				jgen.writeStringField("name", value.getName());
				jgen.writeEndObject();
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
	}

	/** How should the model handle function call options. */
	@NonNull
	final Mode mode;

	final String name;

	/**
	 * Use this to indicate function_call = {"name": "my_function"}
	 * 
	 * @param name
	 */
	public FunctionChoice(Function my_function) {
		this(Mode.FUNCTION, my_function.getName());
	}

	/**
	 * Use this to indicate function_call = {"name": "my_function"}
	 * 
	 * @param name
	 */
	public FunctionChoice(String my_function) {
		this(Mode.FUNCTION, my_function);
	}

	private FunctionChoice(Mode mode, String name) {
		this.mode = mode;
		this.name = name;
	}
}
