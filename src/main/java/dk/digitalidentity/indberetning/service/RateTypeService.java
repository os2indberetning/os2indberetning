package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.RateTypeDao;
import dk.digitalidentity.indberetning.model.entity.RateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RateTypeService {
    private final RateTypeDao rateTypeDao;

    public RateType getByName(String name) { return rateTypeDao.findByName(name); }

    public RateType getById(long id) { return rateTypeDao.findById(id).orElse(null); }

    public List<RateType> getAll() { return rateTypeDao.findAll(); }

    public RateType save(RateType rateType) { return rateTypeDao.save(rateType); }

    public void delete(long id) { rateTypeDao.deleteById(id); }

	public RateType findPrimeRateType() {
		return rateTypeDao.findByPrimeTrue();
	}
}
