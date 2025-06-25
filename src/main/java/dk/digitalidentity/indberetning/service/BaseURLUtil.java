package dk.digitalidentity.indberetning.service;

import org.springframework.util.StringUtils;

public class BaseURLUtil {

	public static String getBaseURL(String url) {
		if (StringUtils.hasText(url) && !url.endsWith("/")) {
			return url + "/";
		}
		return url;
	}
}
