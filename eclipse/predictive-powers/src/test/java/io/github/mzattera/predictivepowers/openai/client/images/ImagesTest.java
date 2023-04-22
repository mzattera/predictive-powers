package io.github.mzattera.predictivepowers.openai.client.images;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesGenerationsRequest.ImageSize;

class ImagesTest {

	@Test
	void test01() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";
		ImagesGenerationsRequest req = new ImagesGenerationsRequest();

		req.setPrompt(prompt);
		req.setSize(ImageSize._256x256);

		System.out.println(req.toString());

		ImagesResponse resp = oai.getClient().createImage(req);

//		ImageUtil.write("D:\\DALLE-2.png", ImageUtil.fromUrl(resp.getData()[0].getUrl()));

		assertEquals(resp.getData().length, 1);
	}
}
