package dk.digitalidentity.indberetning.model.datatable.dao;

import dk.digitalidentity.indberetning.model.entity.ReportView;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;


public interface ReportDatatableDao  extends DataTablesRepository<ReportView, Long> {

}
