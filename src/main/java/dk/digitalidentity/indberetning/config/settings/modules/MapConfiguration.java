package dk.digitalidentity.indberetning.config.settings.modules;

import dk.digitalidentity.indberetning.service.BaseURLUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapConfiguration {
	private String mapServiceBaseUrl;
	private String septimaBaseUrl;
	private String septimaApikey;

	public String getMapServiceBaseUrl() {
		if (mapServiceBaseUrl == null) {
			throw new IllegalStateException("Unexpected value: mapServiceBaseUrl is null");
		}

		return BaseURLUtil.getBaseURL(mapServiceBaseUrl);
	}
}
