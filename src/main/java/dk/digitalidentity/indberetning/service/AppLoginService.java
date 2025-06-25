package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.AppLoginDao;
import dk.digitalidentity.indberetning.model.entity.AppLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppLoginService {
	private final AppLoginDao appLoginDao;

	public AppLogin getById(long id) {
		return appLoginDao.findById(id);
	}

	public AppLogin getByUuid(String uuid) {
		return appLoginDao.findByUuid(uuid).orElse(null);
	}

	public AppLogin getByUsername(String username) {
		return appLoginDao.findByUsername(username).orElse(null);
	}

	public AppLogin getByPersonId(long id) {
		return appLoginDao.findByPersonId(id);
	}

	public AppLogin save(AppLogin appLogin) {
		return appLoginDao.save(appLogin);
	}

	public void delete(long id) {
		appLoginDao.deleteById(id);
	}

	public AppLogin setNewUuid(AppLogin appLogin) {
		appLogin.setUuid(UUID.randomUUID().toString());
		return save(appLogin);
	}
}
