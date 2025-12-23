/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mzattera.predictivepowers.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.ToString;

/**
 * Parses maven tree (verbose or not) showing conflicting dependencies and how
 * conflicts are solved.
 */
public class MavenTreeParser {

	private static final String PART = "([\\w\\.-]+)";
//	private static final String VERSION = "(\\d+(?:\\.\\d+){0,2})";

	private static final Pattern DEP_PATTERN = Pattern
//			.compile("^\\[INFO\\][^\\w]*"+ PART + ":" + PART + ":"  + PART + ":" + VERSION + "(:" + PART + "( \\(([^\\)]))?)?");
	.compile("^\\[INFO\\][^\\w]*"+ PART + ":" + PART + ":"  + PART + ":" + PART + "(:" + PART + "( \\(([^\\)]+))?)?");

	@ToString
	static class DependencyOccurrence {
		@NonNull
		String dependency;
		@NonNull
		String version;
		String message;

		DependencyOccurrence(@NonNull String dependency, @NonNull String version, String message) {
			this.dependency = dependency;
			this.version = version;
			this.message = message;
		}
	}

	public static void analyzeMavenTree(String input) {
		// Map of "groupId:artifactId" -> Set of unique occurrences
		Map<String, Set<DependencyOccurrence>> dependencyMap = new TreeMap<>();

		input.lines().forEach(line -> {

			Matcher matcher = DEP_PATTERN.matcher(line);
			if (matcher.find()) {
				String key = matcher.group(1) + ":" + matcher.group(2);
				String version = matcher.group(4);
				String message = matcher.group(8); // Will be null if not verbose or no message

				DependencyOccurrence occurrence = new DependencyOccurrence(key, version, message);
				dependencyMap.computeIfAbsent(key, k -> new HashSet<>()).add(occurrence);
			} else {
				System.err.println("\tSkipped as not matched: " + line);
			}
		});

		printResults(dependencyMap);
	}

	private static void printResults(Map<String, Set<DependencyOccurrence>> dependencyMap) {
		List<String> noConflict = new ArrayList<>();
		List<String> possibleConflicts = new ArrayList<>();
		List<String> fixes = new ArrayList<>();

		for (var entry : dependencyMap.entrySet()) {
			String depName = entry.getKey();
			Set<DependencyOccurrence> occurrences = entry.getValue();

			// Get unique versions only for conflict detection
			Set<String> uniqueVersions = new TreeSet<>(
					occurrences.stream().map(o -> o.version).collect(java.util.stream.Collectors.toSet()));

			if (uniqueVersions.size() == 1) {
				noConflict.add(depName + " -> " + uniqueVersions.iterator().next());
			} else {
				possibleConflicts.add(depName + " -> " + String.join(", ", uniqueVersions));

				// Find latest version
				String latest = uniqueVersions.stream().max(MavenTreeParser::compareVersions).orElse("");

				// Find any specific resolution messages Maven provided for this dependency
				String resolutionContext = occurrences.stream().filter(o -> o.message != null)
						.map(o -> "[" + o.version + ": " + o.message + "]").distinct()
						.collect(java.util.stream.Collectors.joining(" "));

				String fixLine = depName + " -> " + latest;
				if (!resolutionContext.isEmpty()) {
					fixLine += " (Maven logic: " + resolutionContext + ")";
				}
				fixes.add(fixLine);
			}
		}

		System.out.println("### No Conflict");
		noConflict.forEach(System.out::println);

		System.out.println("\n### Possible Conflicts");
		possibleConflicts.forEach(System.out::println);

		System.out.println("\n### Fixes");
		fixes.forEach(System.out::println);
	}

	private static int compareVersions(String v1, String v2) {
		String[] parts1 = v1.split("[-.]");
		String[] parts2 = v2.split("[-.]");
		int length = Math.max(parts1.length, parts2.length);
		for (int i = 0; i < length; i++) {
			int p1 = i < parts1.length ? tryParse(parts1[i]) : 0;
			int p2 = i < parts2.length ? tryParse(parts2[i]) : 0;
			if (p1 != p2)
				return Integer.compare(p1, p2);
		}
		return 0;
	}

	private static int tryParse(String s) {
		try {
			return Integer.parseInt(s.replaceAll("\\D", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static void main(String[] args) {
		String rawInput = 
			"[INFO] io.github.mzattera:predictive-powers:jar:0.6.0.preview\r\n"
			+ "[INFO] +- org.projectlombok:lombok:jar:1.18.42:provided\r\n"
			+ "[INFO] +- com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:compile\r\n"
			+ "[INFO] |  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.20:compile (version managed from 2.20)\r\n"
			+ "[INFO] |  \\- com.fasterxml.jackson.core:jackson-core:jar:2.20.1:compile (version managed from 2.20.1)\r\n"
			+ "[INFO] +- com.kjetland:mbknor-jackson-jsonschema_2.13:jar:1.0.39:compile\r\n"
			+ "[INFO] |  +- org.scala-lang:scala-library:jar:2.13.1:compile\r\n"
			+ "[INFO] |  +- org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:jar:1.3.50:compile\r\n"
			+ "[INFO] |  |  +- org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:jar:1.3.50:runtime\r\n"
			+ "[INFO] |  |  |  +- org.jetbrains.kotlin:kotlin-scripting-common:jar:1.9.10:runtime (version managed from 1.3.50)\r\n"
			+ "[INFO] |  |  |  |  \\- (org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:runtime - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.jetbrains.kotlin:kotlin-scripting-jvm:jar:1.9.10:runtime (version managed from 1.3.50)\r\n"
			+ "[INFO] |  |  |  |  +- org.jetbrains.kotlin:kotlin-script-runtime:jar:1.9.10:runtime (version managed from 1.9.10)\r\n"
			+ "[INFO] |  |  |  |  +- (org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:runtime - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- (org.jetbrains.kotlin:kotlin-scripting-common:jar:1.9.10:runtime - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:runtime - version managed from 1.3.50; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- org.jetbrains.kotlinx:kotlinx-coroutines-core:jar:1.1.1:runtime\r\n"
			+ "[INFO] |  |  \\- org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:compile (version managed from 1.3.50; scope not updated to compile)\r\n"
			+ "[INFO] |  |     +- org.jetbrains.kotlin:kotlin-stdlib-common:jar:1.9.10:compile (version managed from 1.9.10)\r\n"
			+ "[INFO] |  |     \\- org.jetbrains:annotations:jar:13.0:compile\r\n"
			+ "[INFO] |  +- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:compile - version managed from 2.10.1; omitted for duplicate)\r\n"
			+ "[INFO] |  +- javax.validation:validation-api:jar:2.0.1.Final:compile\r\n"
			+ "[INFO] |  +- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 1.7.26; omitted for duplicate)\r\n"
			+ "[INFO] |  \\- io.github.classgraph:classgraph:jar:4.8.21:compile\r\n"
			+ "[INFO] +- io.github.mzattera:hf-inference-api:jar:5.0.0:compile\r\n"
			+ "[INFO] |  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile\r\n"
			+ "[INFO] |  +- com.squareup.okhttp3:okhttp:jar:4.12.0:compile\r\n"
			+ "[INFO] |  |  +- com.squareup.okio:okio:jar:3.6.0:compile\r\n"
			+ "[INFO] |  |  |  \\- com.squareup.okio:okio-jvm:jar:3.6.0:compile\r\n"
			+ "[INFO] |  |  |     +- (org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.9.10:compile - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |     \\- (org.jetbrains.kotlin:kotlin-stdlib-common:jar:1.9.10:compile - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.9.10:compile - version managed from 1.8.21; omitted for duplicate)\r\n"
			+ "[INFO] |  +- com.squareup.okhttp3:logging-interceptor:jar:4.12.0:compile\r\n"
			+ "[INFO] |  |  +- (com.squareup.okhttp3:okhttp:jar:4.12.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.9.10:compile - version managed from 1.8.21; omitted for duplicate)\r\n"
			+ "[INFO] |  +- com.google.code.gson:gson:jar:2.13.1:compile (version managed from 2.10.1)\r\n"
			+ "[INFO] |  |  \\- com.google.errorprone:error_prone_annotations:jar:2.35.1:compile (version managed from 2.38.0)\r\n"
			+ "[INFO] |  +- io.gsonfire:gson-fire:jar:1.9.0:compile\r\n"
			+ "[INFO] |  |  \\- (com.google.code.gson:gson:jar:2.13.1:compile - version managed from 2.10.1; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.commons:commons-lang3:jar:3.18.0:compile (version managed from 3.18.0)\r\n"
			+ "[INFO] |  +- org.openapitools:jackson-databind-nullable:jar:0.2.8:compile\r\n"
			+ "[INFO] |  |  \\- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:compile - version managed from 2.20.0; omitted for duplicate)\r\n"
			+ "[INFO] |  \\- jakarta.ws.rs:jakarta.ws.rs-api:jar:2.1.6:compile\r\n"
			+ "[INFO] +- ai.djl.huggingface:tokenizers:jar:0.36.0:compile\r\n"
			+ "[INFO] |  \\- ai.djl:api:jar:0.36.0:compile\r\n"
			+ "[INFO] |     +- (com.google.code.gson:gson:jar:2.13.1:compile - version managed from 2.13.1; omitted for duplicate)\r\n"
			+ "[INFO] |     +- net.java.dev.jna:jna:jar:5.17.0:compile\r\n"
			+ "[INFO] |     +- org.apache.commons:commons-compress:jar:1.28.0:compile (version managed from 1.27.1)\r\n"
			+ "[INFO] |     |  +- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |     |  \\- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.20.0; omitted for duplicate)\r\n"
			+ "[INFO] |     \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.17; omitted for duplicate)\r\n"
			+ "[INFO] +- com.openai:openai-java:jar:2.1.0:compile\r\n"
			+ "[INFO] |  +- com.openai:openai-java-client-okhttp:jar:2.1.0:compile\r\n"
			+ "[INFO] |  |  +- com.openai:openai-java-core:jar:2.1.0:compile\r\n"
			+ "[INFO] |  |  |  +- (com.fasterxml.jackson.core:jackson-core:jar:2.20.1:compile - version managed from 2.18.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:compile - version managed from 2.18.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (com.google.errorprone:error_prone_annotations:jar:2.35.1:compile - version managed from 2.33.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.9.10:compile - version managed from 1.8.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (com.fasterxml.jackson.core:jackson-annotations:jar:2.20:runtime - version managed from 2.18.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- com.fasterxml.jackson.datatype:jackson-datatype-jdk8:jar:2.20.1:runtime (version managed from 2.18.2)\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-core:jar:2.20.1:runtime - version managed from 2.20.1; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:runtime - version managed from 2.20.1; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar:2.20.1:runtime (version managed from 2.18.2)\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-annotations:jar:2.20:runtime - version managed from 2.20; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-core:jar:2.20.1:runtime - version managed from 2.20.1; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:runtime - version managed from 2.20.1; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- com.fasterxml.jackson.module:jackson-module-kotlin:jar:2.20.1:runtime (version managed from 2.18.2)\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:runtime - version managed from 2.20.1; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-annotations:jar:2.20:runtime - version managed from 2.20; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- org.jetbrains.kotlin:kotlin-reflect:jar:1.9.10:runtime (version managed from 2.0.21)\r\n"
			+ "[INFO] |  |  |  |     \\- (org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:runtime - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.apache.httpcomponents.core5:httpcore5:jar:5.2.4:runtime\r\n"
			+ "[INFO] |  |  |  +- org.apache.httpcomponents.client5:httpclient5:jar:5.3.1:runtime\r\n"
			+ "[INFO] |  |  |  |  +- (org.apache.httpcomponents.core5:httpcore5:jar:5.2.4:runtime - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  +- org.apache.httpcomponents.core5:httpcore5-h2:jar:5.2.4:runtime\r\n"
			+ "[INFO] |  |  |  |  |  \\- (org.apache.httpcomponents.core5:httpcore5:jar:5.2.4:runtime - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- (org.slf4j:slf4j-api:jar:2.0.17:runtime - version managed from 1.7.36; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- com.github.victools:jsonschema-generator:jar:4.38.0:runtime\r\n"
			+ "[INFO] |  |  |  |  +- com.fasterxml:classmate:jar:1.7.0:runtime\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-core:jar:2.20.1:runtime - version managed from 2.17.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  +- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:runtime - version managed from 2.17.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- (org.slf4j:slf4j-api:jar:2.0.17:runtime - version managed from 2.0.16; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- com.github.victools:jsonschema-module-jackson:jar:4.38.0:runtime\r\n"
			+ "[INFO] |  |  |     \\- (org.slf4j:slf4j-api:jar:2.0.17:runtime - version managed from 2.0.16; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.9.10:compile - version managed from 1.8.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (com.squareup.okhttp3:okhttp:jar:4.12.0:runtime - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (com.squareup.okhttp3:logging-interceptor:jar:4.12.0:runtime - omitted for duplicate)\r\n"
			+ "[INFO] |  \\- org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:1.9.10:compile (version managed from 1.8.0)\r\n"
			+ "[INFO] |     +- (org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:compile - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] |     \\- org.jetbrains.kotlin:kotlin-stdlib-jdk7:jar:1.9.10:compile (version managed from 1.9.10)\r\n"
			+ "[INFO] |        \\- (org.jetbrains.kotlin:kotlin-stdlib:jar:1.9.10:compile - version managed from 1.9.10; omitted for duplicate)\r\n"
			+ "[INFO] +- com.knuddels:jtokkit:jar:1.1.0:compile\r\n"
			+ "[INFO] +- com.google.api-client:google-api-client:jar:1.25.0:compile\r\n"
			+ "[INFO] |  +- com.google.oauth-client:google-oauth-client:jar:1.25.0:compile\r\n"
			+ "[INFO] |  |  +- com.google.http-client:google-http-client:jar:1.25.0:compile\r\n"
			+ "[INFO] |  |  |  +- (com.google.code.findbugs:jsr305:jar:3.0.2:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.apache.httpcomponents:httpclient:jar:4.5.5:compile\r\n"
			+ "[INFO] |  |  |  |  +- org.apache.httpcomponents:httpcore:jar:4.4.9:compile\r\n"
			+ "[INFO] |  |  |  |  +- (commons-logging:commons-logging:jar:1.3.5:compile - version managed from 1.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  |  \\- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- com.google.j2objc:j2objc-annotations:jar:1.1:compile\r\n"
			+ "[INFO] |  |  \\- (com.google.code.findbugs:jsr305:jar:3.0.2:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- com.google.http-client:google-http-client-jackson2:jar:1.25.0:compile\r\n"
			+ "[INFO] |  |  +- (com.google.http-client:google-http-client:jar:1.25.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (com.fasterxml.jackson.core:jackson-core:jar:2.20.1:compile - version managed from 2.9.6; omitted for duplicate)\r\n"
			+ "[INFO] |  \\- com.google.guava:guava:jar:20.0:compile\r\n"
			+ "[INFO] +- com.google.api-client:google-api-client-gson:jar:1.25.0:compile\r\n"
			+ "[INFO] |  +- (com.google.api-client:google-api-client:jar:1.25.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  \\- com.google.http-client:google-http-client-gson:jar:1.25.0:compile\r\n"
			+ "[INFO] |     +- (com.google.http-client:google-http-client:jar:1.25.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] |     \\- (com.google.code.gson:gson:jar:2.13.1:compile - version managed from 2.1; omitted for duplicate)\r\n"
			+ "[INFO] +- com.google.apis:google-api-services-customsearch:jar:v1-rev86-1.25.0:compile\r\n"
			+ "[INFO] |  \\- (com.google.api-client:google-api-client:jar:1.25.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] +- org.apache.tika:tika-core:jar:3.2.3:compile\r\n"
			+ "[INFO] |  +- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.17; omitted for duplicate)\r\n"
			+ "[INFO] |  \\- commons-io:commons-io:jar:2.20.0:compile (version managed from 2.20.0)\r\n"
			+ "[INFO] +- org.apache.tika:tika-parsers-standard-package:jar:3.2.3:compile\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-apple-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- org.apache.tika:tika-parser-zip-commons:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  |  \\- (org.apache.commons:commons-compress:jar:1.28.0:compile - version managed from 1.28.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- com.googlecode.plist:dd-plist:jar:1.28:compile\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-audiovideo-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  \\- com.drewnoakes:metadata-extractor:jar:2.19.0:compile\r\n"
			+ "[INFO] |  |     \\- com.adobe.xmp:xmpcore:jar:6.1.11:compile\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-cad-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-microsoft-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (com.fasterxml.jackson.core:jackson-core:jar:2.20.1:compile - version managed from 2.20.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (com.fasterxml.jackson.core:jackson-databind:jar:2.20.1:compile - version managed from 2.20.0; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-code-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-text-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.codelibs:jhighlight:jar:1.1.0:compile\r\n"
			+ "[INFO] |  |  |  \\- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.7; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.jsoup:jsoup:jar:1.21.2:compile\r\n"
			+ "[INFO] |  |  +- org.ow2.asm:asm:jar:9.8:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.commons:commons-lang3:jar:3.18.0:compile - version managed from 3.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- com.epam:parso:jar:2.0.14:compile\r\n"
			+ "[INFO] |  |  |  \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 1.7.5; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- org.tallison:jmatio:jar:1.5:compile\r\n"
			+ "[INFO] |  |     \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 1.7.25; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-crypto-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- org.bouncycastle:bcjmail-jdk18on:jar:1.81:compile\r\n"
			+ "[INFO] |  |  |  \\- org.bouncycastle:bcpkix-jdk18on:jar:1.81:compile\r\n"
			+ "[INFO] |  |  |     \\- org.bouncycastle:bcutil-jdk18on:jar:1.81:compile\r\n"
			+ "[INFO] |  |  |        \\- (org.bouncycastle:bcprov-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- org.bouncycastle:bcprov-jdk18on:jar:1.81:compile\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-digest-commons:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- commons-codec:commons-codec:jar:1.19.0:compile (version managed from 1.19.0)\r\n"
			+ "[INFO] |  |  +- (org.bouncycastle:bcjmail-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.bouncycastle:bcprov-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-font-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  \\- org.apache.pdfbox:fontbox:jar:3.0.5:compile\r\n"
			+ "[INFO] |  |     +- org.apache.pdfbox:pdfbox-io:jar:3.0.5:compile\r\n"
			+ "[INFO] |  |     |  \\- (commons-logging:commons-logging:jar:1.3.5:compile - version managed from 1.3.5; omitted for duplicate)\r\n"
			+ "[INFO] |  |     \\- (commons-logging:commons-logging:jar:1.3.5:compile - version managed from 1.3.5; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-html-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.jsoup:jsoup:jar:1.21.2:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-image-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (com.drewnoakes:metadata-extractor:jar:2.19.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-xmp-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- com.github.jai-imageio:jai-imageio-core:jar:1.4.0:compile\r\n"
			+ "[INFO] |  |  \\- org.apache.pdfbox:jbig2-imageio:jar:3.0.4:compile\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-mail-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- org.apache.tika:tika-parser-mail-commons:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  |  +- org.apache.james:apache-mime4j-core:jar:0.8.13:compile\r\n"
			+ "[INFO] |  |  |  |  \\- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- org.apache.james:apache-mime4j-dom:jar:0.8.13:compile\r\n"
			+ "[INFO] |  |  |     +- (org.apache.james:apache-mime4j-core:jar:0.8.13:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |     \\- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-text-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.apache.tika:tika-parser-html-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-microsoft-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.17; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-html-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-text-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-xml-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-mail-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- com.pff:java-libpst:jar:0.9.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-zip-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.commons:commons-lang3:jar:3.18.0:compile - version managed from 3.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.apache.poi:poi:jar:5.4.1:compile\r\n"
			+ "[INFO] |  |  |  +- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (org.apache.commons:commons-collections4:jar:4.5.0:compile - version managed from 4.4; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.apache.commons:commons-math3:jar:3.6.1:compile\r\n"
			+ "[INFO] |  |  |  +- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- com.zaxxer:SparseBitSet:jar:1.3:compile\r\n"
			+ "[INFO] |  |  |  \\- (org.apache.logging.log4j:log4j-api:jar:2.25.3:compile - version managed from 2.24.3; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.apache.poi:poi-scratchpad:jar:5.4.1:compile\r\n"
			+ "[INFO] |  |  |  +- (org.apache.poi:poi:jar:5.4.1:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (org.apache.logging.log4j:log4j-api:jar:2.25.3:compile - version managed from 2.24.3; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (org.apache.commons:commons-math3:jar:3.6.1:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.apache.poi:poi-ooxml:jar:5.4.1:compile\r\n"
			+ "[INFO] |  |  |  +- (org.apache.poi:poi:jar:5.4.1:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.apache.poi:poi-ooxml-lite:jar:5.4.1:compile\r\n"
			+ "[INFO] |  |  |  |  \\- (org.apache.xmlbeans:xmlbeans:jar:5.3.0:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.apache.xmlbeans:xmlbeans:jar:5.3.0:compile\r\n"
			+ "[INFO] |  |  |  +- (org.apache.commons:commons-compress:jar:1.28.0:compile - version managed from 1.27.1; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- com.github.virtuald:curvesapi:jar:1.08:compile\r\n"
			+ "[INFO] |  |  |  +- (org.apache.logging.log4j:log4j-api:jar:2.25.3:compile - version managed from 2.24.3; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- (org.apache.commons:commons-collections4:jar:4.5.0:compile - version managed from 4.4; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- commons-logging:commons-logging:jar:1.3.5:compile (version managed from 1.3.5)\r\n"
			+ "[INFO] |  |  +- com.healthmarketscience.jackcess:jackcess:jar:4.0.8:compile\r\n"
			+ "[INFO] |  |  |  +- (org.apache.commons:commons-lang3:jar:3.18.0:compile - version managed from 3.10; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- (commons-logging:commons-logging:jar:1.3.5:compile - version managed from 1.2; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- com.healthmarketscience.jackcess:jackcess-encrypt:jar:4.0.3:compile\r\n"
			+ "[INFO] |  |  +- (org.bouncycastle:bcjmail-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.bouncycastle:bcprov-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- (org.slf4j:jcl-over-slf4j:jar:2.0.17:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-miscoffice-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-zip-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-text-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-xml-module:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.apache.commons:commons-lang3:jar:3.18.0:compile - version managed from 3.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.apache.commons:commons-collections4:jar:4.5.0:compile (version managed from 4.5.0)\r\n"
			+ "[INFO] |  |  +- (org.apache.poi:poi:jar:5.4.1:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.glassfish.jaxb:jaxb-runtime:jar:4.0.5:compile\r\n"
			+ "[INFO] |  |  |  \\- org.glassfish.jaxb:jaxb-core:jar:4.0.5:compile\r\n"
			+ "[INFO] |  |  |     +- jakarta.xml.bind:jakarta.xml.bind-api:jar:4.0.2:compile\r\n"
			+ "[INFO] |  |  |     |  \\- (jakarta.activation:jakarta.activation-api:jar:2.1.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |     +- jakarta.activation:jakarta.activation-api:jar:2.1.3:compile\r\n"
			+ "[INFO] |  |  |     +- org.eclipse.angus:angus-activation:jar:2.0.2:runtime\r\n"
			+ "[INFO] |  |  |     |  \\- (jakarta.activation:jakarta.activation-api:jar:2.1.3:runtime - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |     +- org.glassfish.jaxb:txw2:jar:4.0.5:compile\r\n"
			+ "[INFO] |  |  |     \\- com.sun.istack:istack-commons-runtime:jar:4.1.2:compile\r\n"
			+ "[INFO] |  |  \\- (org.apache.tika:tika-parser-xmp-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-news-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- com.rometools:rome:jar:2.1.0:compile\r\n"
			+ "[INFO] |  |  |  +- com.rometools:rome-utils:jar:2.1.0:compile\r\n"
			+ "[INFO] |  |  |  |  \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.6; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- org.jdom:jdom2:jar:2.0.6.1:compile\r\n"
			+ "[INFO] |  |  |  \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.6; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.17; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-ocr-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.commons:commons-lang3:jar:3.18.0:compile - version managed from 3.18.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- org.apache.commons:commons-exec:jar:1.5.0:compile\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-pdf-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-xmp-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.apache.pdfbox:pdfbox:jar:3.0.5:compile\r\n"
			+ "[INFO] |  |  |  +- (org.apache.pdfbox:pdfbox-io:jar:3.0.5:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  +- (org.apache.pdfbox:fontbox:jar:3.0.5:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- (commons-logging:commons-logging:jar:1.3.5:compile - version managed from 1.3.5; omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- org.apache.pdfbox:pdfbox-tools:jar:3.0.5:compile\r\n"
			+ "[INFO] |  |  |  +- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  |  \\- info.picocli:picocli:jar:4.7.6:compile\r\n"
			+ "[INFO] |  |  +- org.apache.pdfbox:jempbox:jar:1.8.17:compile\r\n"
			+ "[INFO] |  |  +- (org.bouncycastle:bcjmail-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  +- (org.bouncycastle:bcprov-jdk18on:jar:1.81:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- (org.glassfish.jaxb:jaxb-runtime:jar:4.0.5:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-pkg-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- org.tukaani:xz:jar:1.10:compile\r\n"
			+ "[INFO] |  |  +- org.brotli:dec:jar:0.1.2:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.tika:tika-parser-zip-commons:jar:3.2.3:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- com.github.junrar:junrar:jar:7.5.5:compile\r\n"
			+ "[INFO] |  |     \\- (org.slf4j:slf4j-api:jar:2.0.17:runtime - version managed from 1.7.36; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-text-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- com.github.albfernandez:juniversalchardet:jar:2.5.0:compile\r\n"
			+ "[INFO] |  |  +- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- org.apache.commons:commons-csv:jar:1.14.1:compile\r\n"
			+ "[INFO] |  |     +- (commons-io:commons-io:jar:2.20.0:compile - version managed from 2.20.0; omitted for duplicate)\r\n"
			+ "[INFO] |  |     \\- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-webarchive-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- org.netpreserve:jwarc:jar:0.32.0:compile\r\n"
			+ "[INFO] |  |  \\- (org.apache.commons:commons-compress:jar:1.28.0:compile - version managed from 1.28.0; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-xml-module:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  \\- (commons-codec:commons-codec:jar:1.19.0:compile - version managed from 1.19.0; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.apache.tika:tika-parser-xmp-commons:jar:3.2.3:compile\r\n"
			+ "[INFO] |  |  +- (org.apache.pdfbox:jempbox:jar:1.8.17:compile - omitted for duplicate)\r\n"
			+ "[INFO] |  |  \\- org.apache.pdfbox:xmpbox:jar:3.0.5:compile\r\n"
			+ "[INFO] |  |     \\- (commons-logging:commons-logging:jar:1.3.5:compile - version managed from 1.3.5; omitted for duplicate)\r\n"
			+ "[INFO] |  +- org.gagravarr:vorbis-java-tika:jar:0.8:compile\r\n"
			+ "[INFO] |  \\- org.gagravarr:vorbis-java-core:jar:0.8:compile\r\n"
			+ "[INFO] +- org.slf4j:slf4j-api:jar:2.0.17:compile\r\n"
			+ "[INFO] +- org.apache.logging.log4j:log4j-to-slf4j:jar:2.25.3:compile\r\n"
			+ "[INFO] |  +- org.apache.logging.log4j:log4j-api:jar:2.25.3:compile (version managed from 2.25.3)\r\n"
			+ "[INFO] |  \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.17; omitted for duplicate)\r\n"
			+ "[INFO] +- org.slf4j:jcl-over-slf4j:jar:2.0.17:compile\r\n"
			+ "[INFO] |  \\- (org.slf4j:slf4j-api:jar:2.0.17:compile - version managed from 2.0.17; omitted for duplicate)\r\n"
			+ "[INFO] \\- ch.qos.logback:logback-classic:jar:1.5.22:test\r\n"
			+ "[INFO]    +- ch.qos.logback:logback-core:jar:1.5.22:test\r\n"
			+ "[INFO]    \\- (org.slf4j:slf4j-api:jar:2.0.17:test - version managed from 2.0.17; omitted for duplicate)\r\n";

		analyzeMavenTree(rawInput);
	}
}