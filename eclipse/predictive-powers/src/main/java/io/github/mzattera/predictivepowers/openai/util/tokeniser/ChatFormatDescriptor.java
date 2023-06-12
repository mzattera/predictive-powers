/*
 * Copyright (c) 2023 Mariusz Bernacki <info@didalgo.com>
 * SPDX-License-Identifier: MIT
 */

/*
 * MIT License
 * 
 * Copyright (c) 2023 didalgo2
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.mzattera.predictivepowers.openai.util.tokeniser;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Describes the various chat messaging formats for the purpose of counting
 * tokens in chat conversations against different models.
 *
 * @author Mariusz Bernacki
 * @author Massimiliano "Maxi" Zattera
 */
@AllArgsConstructor
@Getter
public class ChatFormatDescriptor {

	private Encoding encoding;
	private int extraTokenCountPerMessage;
	private int extraTokenCountPerRequest;

	public static ChatFormatDescriptor forModel(String modelName) {
		switch (modelName) {
		case "gpt-3.5-turbo":
			return forModel("gpt-3.5-turbo-0301");
		case "gpt-4":
			return forModel("gpt-4-0314");
		case "gpt-3.5-turbo-0301":
			return new ChatFormatDescriptor(Encoding.forModel(modelName), 4, 3);
		case "gpt-4-0314":
			return new ChatFormatDescriptor(Encoding.forModel(modelName), 3, 3);
		default:
			// If we get here, it might be we are not using a chat model let's not block the execution for that, it might not be needed.
			return null;
//			throw new IllegalArgumentException(String.format("Model `%s` not found", modelName));
		}
	}
}
