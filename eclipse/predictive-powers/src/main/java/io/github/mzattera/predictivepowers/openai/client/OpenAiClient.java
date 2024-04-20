package io.github.mzattera.predictivepowers.openai.client;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.github.mzattera.predictivepowers.ApiClient;
import io.github.mzattera.predictivepowers.openai.client.assistants.Assistant;
import io.github.mzattera.predictivepowers.openai.client.assistants.AssistantsRequest;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioResponse;
import io.github.mzattera.predictivepowers.openai.client.audio.AudioSpeechRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.chat.ChatCompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsRequest;
import io.github.mzattera.predictivepowers.openai.client.completions.CompletionsResponse;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsRequest;
import io.github.mzattera.predictivepowers.openai.client.embeddings.EmbeddingsResponse;
import io.github.mzattera.predictivepowers.openai.client.files.File;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningJob;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningJobEvent;
import io.github.mzattera.predictivepowers.openai.client.finetuning.FineTuningRequest;
import io.github.mzattera.predictivepowers.openai.client.images.Image;
import io.github.mzattera.predictivepowers.openai.client.images.ImagesRequest;
import io.github.mzattera.predictivepowers.openai.client.models.Model;
import io.github.mzattera.predictivepowers.openai.client.moderations.ModerationsRequest;
import io.github.mzattera.predictivepowers.openai.client.moderations.ModerationsResponse;
import io.github.mzattera.predictivepowers.openai.client.threads.Message;
import io.github.mzattera.predictivepowers.openai.client.threads.MessageFile;
import io.github.mzattera.predictivepowers.openai.client.threads.MessagesRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.OpenAiThread;
import io.github.mzattera.predictivepowers.openai.client.threads.Run;
import io.github.mzattera.predictivepowers.openai.client.threads.RunStep;
import io.github.mzattera.predictivepowers.openai.client.threads.RunsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadAndRunRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ThreadsRequest;
import io.github.mzattera.predictivepowers.openai.client.threads.ToolOutputsRequest;
import io.github.mzattera.predictivepowers.services.messages.FilePart;
import io.github.mzattera.util.FileUtil;
import io.github.mzattera.util.ImageUtil;
import io.reactivex.Single;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.HttpException;

/**
 * This is the base class for OpenAI clients that can be implemented over the
 * OpenAI API or an Azure OpenIA Service resource.
 * 
 * @author Massimiliano "Maxi" Zattera
 */

public abstract class OpenAiClient implements ApiClient {

	/** Used for JSON (de)serialization in API calls */
	@Getter
	protected final static ObjectMapper jsonMapper;

	static {
		jsonMapper = new ObjectMapper();
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		jsonMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

	public abstract List<Model> listModels();

	public abstract Model retrieveModel(@NonNull String modelId);

	public abstract CompletionsResponse createCompletion(@NonNull CompletionsRequest req);

	public abstract ChatCompletionsResponse createChatCompletion(@NonNull ChatCompletionsRequest req);

	public abstract List<Image> createImage(@NonNull ImagesRequest req);

	public List<Image> createImageEdit(@NonNull BufferedImage image, @NonNull ImagesRequest req, BufferedImage mask)
			throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("image", "image", ImageUtil.toRequestBody("png", image));

		if (req.getPrompt() != null) {
			builder.addFormDataPart("prompt", req.getPrompt());
		} else {
			throw new IllegalArgumentException("Prompt cannot be null");
		}
		if (mask != null) {
			builder.addFormDataPart("mask", "", ImageUtil.toRequestBody("png", mask));
		}
		if (req.getN() != null) {
			builder.addFormDataPart("n", req.getN().toString());
		}
		if (req.getSize() != null) {
			builder.addFormDataPart("size", req.getSize().toString());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getUser() != null) {
			builder.addFormDataPart("user", req.getUser());
		}

		return createImageEdit(builder.build());
	}

	protected abstract List<Image> createImageEdit(@NonNull MultipartBody body);

	public List<Image> createImageVariation(@NonNull BufferedImage image, @NonNull ImagesRequest req)
			throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("image", "image", ImageUtil.toRequestBody("png", image));

		if (req.getN() != null) {
			builder.addFormDataPart("n", req.getN().toString());
		}
		if (req.getSize() != null) {
			builder.addFormDataPart("size", req.getSize().toString());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getUser() != null) {
			builder.addFormDataPart("user", req.getUser());
		}

		return createImageVariation(builder.build());
	}

	protected abstract List<Image> createImageVariation(@NonNull MultipartBody body);

	public abstract EmbeddingsResponse createEmbeddings(@NonNull EmbeddingsRequest req);

	// TODO Add streaming for TTS

	/**
	 * Generates audio from the input text.
	 * 
	 * The output file content is stored in memory, this might cause OutOfMemory
	 * errors if the file is too big.
	 */
	public byte[] createSpeech(@NonNull AudioSpeechRequest req) throws IOException {
		return createSpeechResponse(req).bytes();
	}

	/**
	 * Generates audio from the input text and downloads it into a file.
	 * 
	 * @param downloadedFile A File where the contents of generated audio will be
	 *                       stored.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void createSpeech(AudioSpeechRequest req, @NonNull java.io.File downloadedFile)
			throws FileNotFoundException, IOException {
		download(createSpeechResponse(req), downloadedFile);
	}

	protected abstract ResponseBody createSpeechResponse(@NonNull AudioSpeechRequest req);

	/**
	 * Creates an audio transcription. Notice the result is returned as a String
	 * which might contain JSON, depending on the velue of
	 * {@link AudioRequest#setResponseFormat(io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest.ResponseFormat)}.
	 * 
	 * @param audio Audio file to transcribe.
	 * @param req
	 * 
	 * @return A transcription of given stream.
	 */
	public String createTranscription(@NonNull java.io.File audio, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(audio)) {
			return createTranscription(is, FileUtil.getExtension(audio), req);
		}
	}

	/**
	 * Creates an audio transcription. Notice the result is returned as a String
	 * which might contain JSON, depending on the velue of
	 * {@link AudioRequest#setResponseFormat(io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest.ResponseFormat)}.
	 * 
	 * @param fileName Name of audio file to transcribe.
	 * @param req
	 * 
	 * @return A transcription of given stream.
	 */
	public String createTranscription(@NonNull String fileName, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(fileName)) {
			return createTranscription(is, FileUtil.getExtension(fileName), req);
		}
	}

	/**
	 * Creates an audio transcription. Notice the result is returned as a String
	 * which might contain JSON, depending on the velue of
	 * {@link AudioRequest#setResponseFormat(io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest.ResponseFormat)}.
	 * 
	 * @param audio  Audio stream.
	 * @param format Format of the audio stream (e.g. wav).
	 * @param req
	 * 
	 * @return A transcription of given stream.
	 */
	public String createTranscription(@NonNull InputStream audio, @NonNull String format, @NonNull AudioRequest req)
			throws IOException {
		return createTranscription(audio.readAllBytes(), format, req);
	}

	/**
	 * Creates an audio transcription. Notice the result is returned as a String
	 * which might contain JSON, depending on the velue of
	 * {@link AudioRequest#setResponseFormat(io.github.mzattera.predictivepowers.openai.client.audio.AudioRequest.ResponseFormat)}.
	 * 
	 * @param audio  Audio stream.
	 * @param format Format of the audio stream (e.g. wav).
	 * @param req
	 * 
	 * @return A transcription of given stream.
	 */
	public String createTranscription(@NonNull byte[] audio, @NonNull String format, @NonNull AudioRequest req)
			throws IOException {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("model", req.getModel())
				.addFormDataPart("file", "file." + format, RequestBody.create(MediaType.parse("audio/*"), audio));

		if (req.getPrompt() != null) {
			builder.addFormDataPart("prompt", req.getPrompt());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getTemperature() != null) {
			builder.addFormDataPart("temperature", req.getTemperature().toString());
		}
		if (req.getLanguage() != null) {
			builder.addFormDataPart("language", req.getLanguage());
		}

		return createTranscription(req.getModel(), builder.build()).string();
	}

	protected abstract ResponseBody createTranscription(@NonNull String model, @NonNull MultipartBody body);

	public String createTranslation(@NonNull java.io.File audio, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(audio)) {
			return createTranslation(is, FileUtil.getExtension(audio), req);
		}
	}

	public String createTranslation(@NonNull String fileName, @NonNull AudioRequest req) throws IOException {
		try (InputStream is = new FileInputStream(fileName)) {
			return createTranslation(is, FileUtil.getExtension(fileName), req);
		}
	}

	// TODO extend audio methods so the result is not saved into a String but on a
	// file/stream

	/**
	 * Creates an audio translation.
	 * 
	 * @param audio  Audio stream.
	 * @param format Format of the audio stream (e.g. wav).
	 * @param req
	 * 
	 * @return A translation of given stream.
	 */
	public String createTranslation(@NonNull InputStream audio, @NonNull String format, @NonNull AudioRequest req)
			throws IOException {
		return createTranslation(audio.readAllBytes(), format, req);
	}

	public String createTranslation(@NonNull byte[] audio, @NonNull String format, @NonNull AudioRequest req) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("model", req.getModel())
				.addFormDataPart("file", "file." + format, RequestBody.create(MediaType.parse("audio/*"), audio));

		if (req.getPrompt() != null) {
			builder.addFormDataPart("prompt", req.getPrompt());
		}
		if (req.getResponseFormat() != null) {
			builder.addFormDataPart("response_format", req.getResponseFormat().toString());
		}
		if (req.getTemperature() != null) {
			builder.addFormDataPart("temperature", req.getTemperature().toString());
		}
		if (req.getLanguage() != null) {
			builder.addFormDataPart("language", req.getLanguage());
		}

		return createTranslation(req.getModel(), builder.build()).getText();
	}

	protected abstract AudioResponse createTranslation(@NonNull String model, @NonNull MultipartBody body);

	public abstract List<File> listFiles();

	public File uploadFile(@NonNull String fileName, @NonNull String purpose) throws IOException {
		return uploadFile(new java.io.File(fileName), purpose);
	}

	public File uploadFile(@NonNull java.io.File file, @NonNull String purpose) throws IOException {
		try (InputStream is = new FileInputStream(file)) {
			return uploadFile(is, file.getName(), purpose);
		}
	}

	public File uploadFile(@NonNull FilePart file, @NonNull String purpose) throws IOException {
		return uploadFile(file.getInputStream(), file.getName(), purpose);
	}

	public File uploadFile(@NonNull InputStream file, @NonNull String fileName, @NonNull String purpose)
			throws IOException {
		return uploadFile(file.readAllBytes(), fileName, purpose);
	}

	public File uploadFile(@NonNull byte[] file, @NonNull String fileName, @NonNull String purpose) {

		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MediaType.get("multipart/form-data"))
				.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("text/file"), file))
				.addFormDataPart("purpose", purpose);

		return uploadFile(builder.build());
	}

	protected abstract File uploadFile(@NonNull MultipartBody body);

	public abstract DeleteResponse deleteFile(@NonNull String fileId);

	public abstract File retrieveFile(@NonNull String fileId);

	/**
	 * Retrieves file content.
	 * 
	 * The file content is stored in memory, this might cause OutOfMemory errors if
	 * the file is too big.
	 * 
	 * @param fileId Id of file to retrieve.
	 * @throws IOException
	 */
	public byte[] retrieveFileContent(@NonNull String fileId) throws IOException {
		return retrieveFileContentResponse(fileId).bytes();
	}

	/**
	 * Downloads file contents.
	 * 
	 * @param fileId         Id of file to retrieve.
	 * @param downloadedFile A File where the contents of retrieved file will be
	 *                       stored.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void retrieveFileContent(String fileId, @NonNull java.io.File downloadedFile)
			throws FileNotFoundException, IOException {
		download(retrieveFileContentResponse(fileId), downloadedFile);
	}

	protected abstract ResponseBody retrieveFileContentResponse(@NonNull String fileId);

	public abstract FineTuningJob createFineTuningJob(@NonNull FineTuningRequest req);

	public DataList<FineTuningJob> listFineTuningJobs() {
		return listFineTuningJobs(null, null);
	}

	public abstract DataList<FineTuningJob> listFineTuningJobs(Integer limit, String after);

	public DataList<FineTuningJobEvent> listFineTuningEvents(@NonNull String fineTuningJobId) {
		return listFineTuningEvents(fineTuningJobId, null, null);
	}

	public abstract DataList<FineTuningJobEvent> listFineTuningEvents(@NonNull String fineTuningJobId, Integer limit,
			String after);

	public abstract FineTuningJob retrieveFineTuningJob(@NonNull String fineTuningJobId);

	public abstract FineTuningJob cancelFineTuning(@NonNull String fineTuningJobId);

	public abstract DeleteResponse deleteFineTunedModel(@NonNull String model);

	public abstract ModerationsResponse createModeration(@NonNull ModerationsRequest req);

	public abstract Assistant createAssistant(@NonNull AssistantsRequest req);

	public File createAssistantFile(@NonNull String assistantId, @NonNull String fileId) {
		Map<String, String> body = new HashMap<>();
		body.put("file_id", fileId);
		return createAssistantFile(assistantId, body);
	}

	protected abstract File createAssistantFile(@NonNull String assistantId, @NonNull Map<String, String> body);

	public DataList<Assistant> listAssistants() {
		return listAssistants(null, null, null, null);
	}

	public abstract DataList<Assistant> listAssistants(SortOrder order, Integer limit, String before, String after);

	public DataList<File> listAssistantFiles(@NonNull String assistantId) {
		return listAssistantFiles(assistantId, null, null, null, null);
	}

	public abstract DataList<File> listAssistantFiles(@NonNull String assistantId, SortOrder order, Integer limit,
			String before, String after);

	/**
	 * Retrieves an assistant from OpenAI.
	 * 
	 * Notice that tool parameters for any tool attached to the agent are not
	 * properly de-serialized, so they will always be empty.
	 * 
	 * Unfortunately, there is no easy workaround as it is not easy to de-serialize
	 * a JSON schema.
	 * 
	 * @param assistantId
	 * @return
	 */
	public abstract Assistant retrieveAssistant(@NonNull String assistantId);

	public abstract File retrieveAssistantFile(@NonNull String assistantId, @NonNull String fileId);

	public abstract Assistant modifyAssistant(@NonNull String assistantId, @NonNull AssistantsRequest req);

	public abstract DeleteResponse deleteAssistant(@NonNull String assistantId);

	public abstract DeleteResponse deteAssistantFile(@NonNull String assistantId, @NonNull String fileId);

	public abstract OpenAiThread createThread(@NonNull ThreadsRequest req);

	public abstract OpenAiThread retrieveThread(@NonNull String threadId);

	public OpenAiThread modifyThread(@NonNull String threadId, @NonNull Map<String, String> metadata) {
		return modifyThread(threadId, new Metadata(metadata));
	}

	public abstract OpenAiThread modifyThread(@NonNull String threadId, @NonNull Metadata metadata);

	public abstract DeleteResponse deleteThread(@NonNull String threadId);

	public abstract Message createMessage(@NonNull String threadId, @NonNull MessagesRequest req);

	public DataList<Message> listMessages(@NonNull String threadId) {
		return listMessages(threadId, null, null, null, null);
	}

	public abstract DataList<Message> listMessages(@NonNull String threadId, SortOrder order, Integer limit,
			String after, String before);

	public DataList<MessageFile> listMessageFiles(@NonNull String threadId, @NonNull String messageId) {
		return listMessageFiles(threadId, messageId, null, null, null, null);
	}

	public abstract DataList<MessageFile> listMessageFiles(@NonNull String threadId, @NonNull String messageId,
			SortOrder order, Integer limit, String after, String before);

	public abstract Message retrieveMessage(@NonNull String threadId, @NonNull String messageId);

	public abstract MessageFile retrieveMessageFile(@NonNull String threadId, @NonNull String messageId,
			@NonNull String fileId);

	public abstract Message modifyMessage(@NonNull String threadId, @NonNull String messageId,
			@NonNull Metadata metadata);

	public abstract Run createRun(@NonNull String threadId, @NonNull RunsRequest req);

	public abstract Run createThreadAndRun(@NonNull ThreadAndRunRequest req);

	public DataList<Run> listRuns(@NonNull String threadId) {
		return listRuns(threadId, null, null, null, null);
	}

	public abstract DataList<Run> listRuns(@NonNull String threadId, @NonNull SortOrder order, Integer limit,
			String after, String before);

	public DataList<RunStep> listRunSteps(@NonNull String threadId, @NonNull String runId) {
		return listRunSteps(threadId, runId, null, null, null, null);
	}

	public abstract DataList<RunStep> listRunSteps(@NonNull String threadId, @NonNull String runId, Integer limit,
			SortOrder order, String after, String before);

	public abstract Run retrieveRun(@NonNull String threadId, @NonNull String runId);

	public abstract RunStep retrieveRunStep(@NonNull String threadId, @NonNull String runId, @NonNull String stepId);

	public abstract Run modifyRun(@NonNull String threadId, @NonNull String runId, @NonNull Metadata metadata);

	public abstract Run submitToolOutputsToRun(@NonNull String threadId, @NonNull String runId,
			@NonNull ToolOutputsRequest req);

	public abstract Run cancelRun(@NonNull String threadId, @NonNull String runId);

	/**
	 * Downloads a file, returned by an HTTP response.
	 */
	protected void download(ResponseBody body, java.io.File downloadedFile) throws IOException {
		try (InputStream is = body.byteStream(); FileOutputStream os = new FileOutputStream(downloadedFile)) {

			byte[] buffer = new byte[4096];
			int lengthRead;
			while ((lengthRead = is.read(buffer)) > 0) {
				os.write(buffer, 0, lengthRead);
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////

	protected <T> T callApi(Single<T> apiCall) {
		try {
			return apiCall.blockingGet();
		} catch (HttpException e) {

			OpenAiException oaie;
			try {
				oaie = new OpenAiException(e);
			} catch (Exception ex) {
				throw e;
			}
			throw oaie;
		}
	}

	@Override
	public void close() {
	}
}