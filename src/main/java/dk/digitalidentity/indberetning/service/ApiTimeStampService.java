package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.ApiTimeStampDao;
import dk.digitalidentity.indberetning.model.entity.ApiTimeStamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTimeStampService {

	private final ApiTimeStampDao apiTimeStampDao;

	public ApiTimeStamp findById(Long id) {
		return apiTimeStampDao.findById(id).orElse(null);
	}

	public ApiTimeStamp find() {
		List<ApiTimeStamp> all = apiTimeStampDao.findAll();
		if (all.size() != 1) {
			return null;
		}

		return all.getFirst();
	}

	public void save(ApiTimeStamp apiTimeStamp) {
		apiTimeStampDao.save(apiTimeStamp);
	}
}