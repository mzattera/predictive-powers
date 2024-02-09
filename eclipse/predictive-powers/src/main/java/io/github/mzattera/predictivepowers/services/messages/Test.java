package io.github.mzattera.predictivepowers.services.messages;

public class Test {

	@FunctionalInterface
	interface Processor {
		void process(String str);
	}

	public static void executeProcessor(Processor processor, String value) {
		processor.process(value);
	}

	public static void main(String[] args) {
		executeProcessor(str -> System.out.println(str.toUpperCase()), "hello");
		executeProcessor(System.out::println, "hello");
	}
}
