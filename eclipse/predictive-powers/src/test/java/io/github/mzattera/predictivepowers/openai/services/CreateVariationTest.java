package io.github.mzattera.predictivepowers.openai.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.MultipartField;
import com.openai.models.images.ImageEditParams;
import com.openai.models.images.ImagesResponse;

import io.github.mzattera.predictivepowers.util.ResourceUtil;

public class CreateVariationTest {

	public static void main(String[] args) throws FileNotFoundException {

		OpenAIClient client = OpenAIOkHttpClient.fromEnv();

		File input = ResourceUtil.getResourceFile("eagle.png");

		ImageEditParams req = ImageEditParams.builder() //
				.model("dall-e-2") //
				.prompt("Replace all humans with tables") //
				.image(MultipartField.<ImageEditParams.Image>builder() //
						.filename(input.getName()) //
						.contentType("image/png") //
						.value(ImageEditParams.Image.ofInputStream(new FileInputStream(input))) //
						.build()
					) //
				.n(1) //
				.size(ImageEditParams.Size._512X512).build();

		ImagesResponse resp = client.images().edit(req);
		System.out.println("Generated " + resp.data().get().size() + " images.");
	}
}