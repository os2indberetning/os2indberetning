package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.ApiAccessDao;
import dk.digitalidentity.indberetning.model.entity.ApiAccess;
import dk.digitalidentity.indberetning.model.entity.enums.ApiType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RouteDataAPIService {

	private final ApiAccessDao apiAccessDao;

	public String getApiKey() throws NoSuchElementException {
		Optional<ApiAccess> first = apiAccessDao.findFirstByTypeAndDisabledFalse(ApiType.ROUTE_DATA);
		if (first.isEmpty()) {
			ApiAccess apiAccess = new ApiAccess();
			apiAccess.setApiKey(UUID.randomUUID().toString());
			apiAccess.setType(ApiType.ROUTE_DATA);
			apiAccess.setDisabled(false);
			apiAccessDao.save(apiAccess);
			first = apiAccessDao.findFirstByTypeAndDisabledFalse(ApiType.ROUTE_DATA);
		}

		return first.orElseThrow().getApiKey();
	}
}
