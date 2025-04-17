package io.github.mzattera.predictivepowers.openai.client;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mzattera.predictivepowers.openai.client.OpenAiError.ErrorDetails;
import lombok.Getter;
import lombok.NonNull;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * Specialized HttpException that is thrown when an error is returned by the
 * OpenAI API.
 * 
 * @author Massimiliano "Maxi" Zattera
 */
public class OpenAiException extends HttpException {

    private static final long serialVersionUID = 1L;

    @NonNull
    private final OpenAiError error;

    @Getter
    private boolean contextLengthExceeded = false;
    @Getter
    private int promptLength = -1;
    @Getter
    private int completionLength = -1;
    @Getter
    private int requestLength = -1;
    @Getter
    private int maxContextLength = -1;

    // New fields for headers
    @Getter
    private final String openaiOrganization;
    @Getter
    private final String openaiProcessingMs;
    @Getter
    private final String openaiVersion;
    @Getter
    private final String requestId;

    @Getter
    private final int rateLimitLimitRequests;
    @Getter
    private final int rateLimitLimitTokens;
    @Getter
    private final int rateLimitRemainingRequests;
    @Getter
    private final int rateLimitRemainingTokens;
    @Getter
    private final int rateLimitResetRequests;
    @Getter
    private final int rateLimitResetTokens;

	private final static Pattern PATTERN01 = Pattern.compile(
			"This model's maximum context length is ([0-9]+) tokens(\\. However,|, however) you requested ([0-9]+) tokens \\(([0-9]+) (in the messages,|in your prompt;) ([0-9]+) (in|for) the completion\\)\\.");

	private final static Pattern PATTERN02 = Pattern.compile(
			"max_tokens is too large: ([0-9]+)\\. This model supports at most ([0-9]+) completion tokens, whereas you provided ([0-9]+)\\.");

    public OpenAiException(@NonNull HttpException rootCause) throws IOException {
        super(rootCause.response());
        initCause(rootCause);
        Response<?> response = rootCause.response();
        error = OpenAiClient.getJsonMapper().readValue(response.errorBody().string(), OpenAiError.class);

        // Pattern matching for context length exceeded
        if ((rootCause.code() == 400) && (error.getError().getMessage() != null)) {
            Matcher m = PATTERN01.matcher(error.getError().getMessage());
            if (m.find()) {
                maxContextLength = Integer.parseInt(m.group(1));
                requestLength = Integer.parseInt(m.group(3));
                promptLength = Integer.parseInt(m.group(4));
                completionLength = Integer.parseInt(m.group(6));
                contextLengthExceeded = true;
            } else {
                m = PATTERN02.matcher(error.getError().getMessage());
                if (m.find()) {
                    requestLength = Integer.parseInt(m.group(1));
                    completionLength = Integer.parseInt(m.group(2));
                    contextLengthExceeded = true;
                }
            }
        }

        // Extract headers (nullable, as they might not be present)
        openaiOrganization = response.headers().get("openai-organization");
        openaiProcessingMs = response.headers().get("openai-processing-ms");
        openaiVersion = response.headers().get("openai-version");
        requestId = response.headers().get("x-request-id");

        rateLimitLimitRequests = parseIntHeader(response, "x-ratelimit-limit-requests");
        rateLimitLimitTokens = parseIntHeader(response, "x-ratelimit-limit-tokens");
        rateLimitRemainingRequests = parseIntHeader(response, "x-ratelimit-remaining-requests");
        rateLimitRemainingTokens = parseIntHeader(response, "x-ratelimit-remaining-tokens");
        rateLimitResetRequests = parseIntHeader(response, "x-ratelimit-reset-requests");
        rateLimitResetTokens = parseIntHeader(response, "x-ratelimit-reset-tokens");
    }

    private int parseIntHeader(Response<?> response, String headerName) {
        String value = response.headers().get(headerName);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public String getMessage() {
        return "HTTP " + ((HttpException) getCause()).code() + ": " + error.getError().getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    public ErrorDetails getErrorDetails() {
        return error.getError();
    }
}
