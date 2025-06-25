package dk.digitalidentity.indberetning.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import dk.digitalidentity.indberetning.config.settings.OS2indberetningConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

	private final OS2indberetningConfiguration configuration;

	@Bean(name = "defaultRestClient")
	public RestClient restTemplate() {
		return RestClient.create();
	}

	@Bean(name = "opusRestClient")
	public RestClient opusRestTemplate() throws Exception {
		final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;

		// Load our accepting trust strategy AND our Client Certificate into the context
		org.apache.hc.core5.ssl.SSLContextBuilder builder = SSLContexts.custom();

		if (StringUtils.hasLength(configuration.getOpus().getKeystoreLocation()) && StringUtils.hasLength(configuration.getOpus().getKeystorePassword())) {
			builder.loadKeyMaterial(
					ResourceUtils.getFile(configuration.getOpus().getKeystoreLocation()),
					configuration.getOpus().getKeystorePassword().toCharArray(),
					configuration.getOpus().getKeystorePassword().toCharArray());
		}
		else {
			log.error("No KMD OPUS Keystore or password was provided. Export of reports to OPUS will not work!");
		}

		builder.loadTrustMaterial(null, acceptingTrustStrategy);
		final SSLContext sslContext = builder.build();

		// Create Connection factory from our context above
		final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

		// Create a list of connection factories that handle different protocols.
		// Add our custom one.
		final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslsf)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		// Create a connection manager to use with a HttpClient, instantiate with our list of factories
		final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();

		// Create a Request Factory with custom timeouts that use the above configured HTTPClient to handle calls
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setConnectionRequestTimeout(3 * 60 * 1000);
		requestFactory.setConnectTimeout(3 * 60 * 1000);
		requestFactory.setHttpClient(httpClient);

		// Build a RestClient (new in Spring 6)
		// set up the error handler so no exceptions are thrown (just status codes) and configure RestClient based on our above configurations
		return RestClient.builder().requestFactory(requestFactory).defaultStatusHandler(new ResponseErrorHandler() {

			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				// returning false means no exception is ever thrown
				return false;
			}

			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
				// false above means we never call this method
			}
		}).build();
	}
}
