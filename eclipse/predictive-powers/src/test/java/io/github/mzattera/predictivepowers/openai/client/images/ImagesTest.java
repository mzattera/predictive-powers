package io.github.mzattera.predictivepowers.openai.client.images;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.mzattera.predictivepowers.OpenAiEndpoint;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ImageSize;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest.ResponseFormat;
import io.github.mzattera.predictivepowers.util.ImageUtil;

class ImagesTest {

	// TODO: use resources or temp file
	private final static String FILE_NAME = "D:\\DALLE-2";
	
	@Test
	void test01() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		String prompt = "A portrait of a blonde lady, with green eyes, holding a green apple. On the background a red wall with a window opened on a country landscape with a lake. In the sky an eagle flies. Neoromantic oil portrait style";
		ImagesRequest req = new ImagesRequest();

		req.setPrompt(prompt);
		req.setSize(ImageSize._256x256);

		System.out.println(req.toString());

		ImagesResponse resp = oai.getClient().createImage(req);

		ImageUtil.toFile(FILE_NAME+".png", ImageUtil.fromUrl(resp.getData()[0].getUrl()));

		assertEquals(resp.getData().length, 1);
	}

	@Test
	void test02() throws IOException {
		OpenAiEndpoint oai = OpenAiEndpoint.getInstance();
		ImagesRequest req = new ImagesRequest();

		req.setSize(ImageSize._256x256);
		req.setResponseFormat(ResponseFormat.BASE_64);

		System.out.println(req.toString());

		ImagesResponse resp = oai.getClient().createImageVariation(req, ImageUtil.fromFile(FILE_NAME+".png"));

		ImageUtil.toFile(FILE_NAME+"_variation.png", ImageUtil.fromBase64(resp.getData()[0].getB64Json()));

		assertEquals(resp.getData().length, 1);
	}
}
