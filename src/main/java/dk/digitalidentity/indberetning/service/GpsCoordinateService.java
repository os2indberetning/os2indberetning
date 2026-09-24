package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.GpsCoordinateDao;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.ReportView;
import dk.digitalidentity.indberetning.model.entity.dto.ReportViewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

	public List<String> findAddressesByReportId(long id) {
		return gpsCoordinateDao.findAddressesByReportId(id);
	}

	public DataTablesOutput<ReportViewDTO> addGpsToReports(DataTablesOutput<ReportView> all) {
		List<ReportViewDTO> dataWithMessages = all.getData().stream()
				.map(reportView -> new ReportViewDTO(reportView.getId(), reportView.getStatus(), reportView.getPersonId(), reportView.getCreatedDate(), reportView.getDriveDate(), reportView.getFullName(), reportView.getEmployeeNumber(), reportView.getApprovedByName(), reportView.getPotentialApprovers(), reportView.getPurpose(), reportView.getKmRateType(), reportView.getComment(), reportView.getDistance(), reportView.getAmountToReimburse(), reportView.isExtraDistance(), reportView.getExtraDistanceAmount(), reportView.isFourKmRule(), reportView.getOrgunitIntials(), reportView.getOrgunitName(), getGps(reportView.getId()), reportView.getRouteGeometry(), reportView.isFromApp(), reportView.isDivergentAddress(), reportView.isRoundTrip(), reportView.getUserComment(), reportView.isStartsAtHome(), reportView.isEndsAtHome(), reportView.getProcessedDate(), reportView.getClosedDate()))
				.collect(Collectors.toCollection(ArrayList::new));

		DataTablesOutput<ReportViewDTO> result = new DataTablesOutput<>();
		result.setData(dataWithMessages);
		result.setDraw(all.getDraw());
		result.setRecordsFiltered(all.getRecordsFiltered());
		result.setRecordsTotal(all.getRecordsTotal());
		result.setError(all.getError());

		return result;
	}

	public String getGps(long reportId) {
		List<String> addresses = findAddressesByReportId(reportId);

		return addresses.stream()
				.filter(addr -> addr != null && !addr.isBlank())
				.collect(Collectors.joining("\n "));
	}
}
