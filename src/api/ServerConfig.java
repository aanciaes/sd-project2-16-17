package api;

public class ServerConfig {

	private String apiKey;
	private String apiSecret;
	private String token;
	private String tokenSecret;

	public ServerConfig() {
	}

	public ServerConfig(String apiKey, String apuSecret, String token, String tokenSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apuSecret;
		this.token = token;
		this.tokenSecret = tokenSecret;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTokenSecret() {
		return tokenSecret;
	}

	public void setTokenSecret(String tokenSecret) {
		this.tokenSecret = tokenSecret;
	}
}