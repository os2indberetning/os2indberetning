package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.RateDao;
import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.RateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RateService {

    private final RateDao rateDao;

    public Rate getByYearAndType(int year, RateType type) {
        return rateDao.findByActiveYearAndRateType(year, type).orElse(null);
    }

    public Rate getById(long id) {
        return rateDao.findById(id).orElse(null);
    }

    public List<Rate> getAll() {
        return rateDao.findAll();
    }

    public Rate save(Rate rate) {
        return rateDao.save(rate);
    }

    public void delete(long id) {
        rateDao.deleteById(id);
    }

    public List<Rate> getByActiveYear(int year) {
        return rateDao.findByActiveYear(year);
    }

}
