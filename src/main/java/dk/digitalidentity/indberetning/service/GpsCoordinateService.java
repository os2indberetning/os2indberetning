package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.GpsCoordinateDao;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GpsCoordinateService {
	private final GpsCoordinateDao gpsCoordinateDao;

	public void delete(long id) {
		gpsCoordinateDao.deleteById(id);
	}

    public void deleteAll(List<GpsCoordinate> coordinates) {
		gpsCoordinateDao.deleteAll(coordinates);
    }

	public List<GpsCoordinate> getByReport(Report report) {
		return gpsCoordinateDao.findByReport(report);
	}

	public GpsCoordinate getById(long id) {
		return gpsCoordinateDao.findById(id).orElse(null);
	}

	public GpsCoordinate save(GpsCoordinate gpsCoordinate) {
		return gpsCoordinateDao.save(gpsCoordinate);
	}

	public List<GpsCoordinate> saveAll(List<GpsCoordinate> gpsCoordinates) {
		return gpsCoordinateDao.saveAll(gpsCoordinates);
	}
}
