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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringUtilTest {

	@Test
	public void test() {

		assertEquals("Test Case", StringUtil.camelCaseToWords("TestCase"));
		assertEquals("Get URL", StringUtil.camelCaseToWords("getURL"));
		assertEquals("To URL", StringUtil.camelCaseToWords("ToURL"));
		assertEquals("XML Parser", StringUtil.camelCaseToWords("XMLParser"));
		assertEquals("La M1a Banana", StringUtil.camelCaseToWords("laM1aBanana"));
		assertEquals("1 Banana", StringUtil.camelCaseToWords("1Banana"));
	}
}
