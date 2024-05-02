
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.sharedlib.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public abstract class JsonClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected final HttpUrl baseUrl;
    protected final OkHttpClient client;
    protected final ObjectMapper mapper;

    private final Logger logger;

    private static OkHttpClient getClient(int timeout) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(timeout, TimeUnit.SECONDS);
        builder.readTimeout(timeout, TimeUnit.SECONDS);
        builder.writeTimeout(timeout, TimeUnit.SECONDS);
        return builder.build();
    }

    public JsonClient(HttpUrl baseUrl, int timeout, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.client = getClient(timeout);
        this.mapper = objectMapper;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    public JsonClient(HttpUrl baseUrl, int timeout) {
        this(baseUrl, timeout, new ObjectMapper());
    }

    protected HttpUrl toUrl(String path, Map<String, String> queryParams) {
        okhttp3.HttpUrl.Builder builder = baseUrl.newBuilder();
        builder.addPathSegments(path);
        if (queryParams != null) {
            for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
                builder.addQueryParameter(queryParam.getKey(), queryParam.getValue());
            }
        }
        return builder.build();
    }

    protected void headers(HttpUrl url, Request.Builder builder) {
    }

    private Response request(String path, Object body, HttpMethod httpMethod) throws IOException {
        return this.request(path, body, httpMethod, null);
    }

    private Response request(String path, Object body, HttpMethod httpMethod, Map<String, String> queryParams)
            throws IOException {
        final HttpUrl url = toUrl(path, queryParams);
        final Request.Builder builder = new Request.Builder().url(url);
        headers(url, builder);

        try {
            switch (httpMethod) {
                case GET:
                    builder.get();
                    break;
                case HEAD:
                    builder.head();
                    break;
                case POST:
                    builder.post(RequestBody.create(mapper.writeValueAsString(body), JSON));
                    break;
                case DELETE:
                    builder.delete(RequestBody.create(mapper.writeValueAsString(body), JSON));
                    break;
                case PUT:
                    builder.put(RequestBody.create(mapper.writeValueAsString(body), JSON));
                    break;
                case PATCH:
                    builder.patch(RequestBody.create(mapper.writeValueAsString(body), JSON));
                    break;
            }
        } catch (JsonProcessingException e) {
            throw new ClientException("Could not create body", e);
        }

        final Request request = builder.build();
        logRequest(request);

        return client.newCall(request).execute();
    }

    protected JsonNode execute(String path, Object body) {
        return execute(path, body, HttpMethod.POST, null);
    }

    protected JsonNode execute(String path, Object body, HttpMethod httpMethod, Map<String, String> queryParams) {
        try (final Response response = request(path, body, httpMethod, queryParams)) {
            logResponse(response);
            checkResponse(response);
            return mapper.readTree(response.body().byteStream());
        } catch (IOException e) {
            throw new ClientException(e);
        }
    }

    protected <T> T execute(String path, Object body, Class<T> type) {
        return execute(path, body, type, HttpMethod.POST);
    }

    protected <T> T execute(String path, Object body, Class<T> type, HttpMethod httpMethod) {
        try (final Response response = request(path, body, httpMethod)) {
            logResponse(response);
            checkResponse(response);
            return mapper.readValue(response.body().byteStream(), type);
        } catch (IOException e) {
            throw new ClientException(e);
        }
    }

    protected void checkResponse(Response response) {
        if (!response.isSuccessful()) {
            throw new ClientException(String.format("Unexpected return code %d", response.code()), response.code());
        }
    }

    protected void logRequest(Request request) throws IOException {
        if (!logger.isInfoEnabled())
            return;
        RequestBody body = request.body();
        if (body != null) {
            final Buffer buffer = new Buffer();
            body.writeTo(buffer);
            logger.info("{} - {}", request, buffer.readString(StandardCharsets.UTF_8));
        } else {
            logger.info("empty RequestBody");
        }

    }

    protected void logResponse(Response response) {
        logger.info("{}", response);
    }
}
