package dk.digitalidentity.indberetning.controller.rest;

import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.RateType;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.RateService;
import dk.digitalidentity.indberetning.service.RateTypeService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Hidden
@Slf4j
@RestController
@RequireAdministrator
@RequiredArgsConstructor
public class RatesRestController {

    private final RateService rateService;
    private final RateTypeService rateTypeService;

    public record RateRecord(long id, int ratePerKm, int activeYear, long rateType) {}
    public record RateTypeRecord(long id, String name, int payType, String sqNumber) {}

    // Create mappings
    @PostMapping("/rest/rate/create")
    public ResponseEntity<String> createRate(@RequestBody RateRecord rateRecord) {
        Rate rate = new Rate();

        rate.setRateType(rateTypeService.getById(rateRecord.rateType));
        rate.setRatePerKm(rateRecord.ratePerKm);
        rate.setActiveYear(rateRecord.activeYear);
        if (rateService.getByYearAndType(rate.getActiveYear(), rate.getRateType()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        rateService.save(rate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rest/rType/create")
    public ResponseEntity<String> createRateType(@RequestBody RateTypeRecord rTypeRecord) {
        RateType rType = new RateType();
        rType.setName(rTypeRecord.name);

        // TODO: Per the dokumentation no "Lønart" (PayType) should be under 4000, maybe we should validate this for newly created types.
        rType.setPayType(rTypeRecord.payType);
        rType.setSequentialNumber(Integer.parseInt(rTypeRecord.sqNumber));
        rType.setPrime(false);

        // Rate types should be unique on names
        if (rateTypeService.getByName(rType.getName()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        rateTypeService.save(rType);
        return ResponseEntity.ok().build();
    }

    // Update mappings
    @PostMapping("/rest/rate/update")
    public ResponseEntity<String> updateRate(@RequestBody RateRecord rateRecord) {
        Rate rate = rateService.getById(rateRecord.id);
        if (rate == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

		// Check if any other rate already matches on year and ratetype
		Rate byYearAndType = rateService.getByYearAndType(rate.getActiveYear(), rate.getRateType());
		if (!Objects.equals(byYearAndType.getId(), rate.getId())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}

		rate.setRateType(rateTypeService.getById(rateRecord.rateType));
		rate.setRatePerKm(rateRecord.ratePerKm);
		rate.setActiveYear(rateRecord.activeYear);

        rateService.save(rate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rest/rType/update")
    public ResponseEntity<String> updateRateType(@RequestBody RateTypeRecord rTypeRecord) {
        RateType rType = rateTypeService.getById(rTypeRecord.id);
        if (rType == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        // Rate types should be unique on names, check if any other ratetype is already
		RateType byName = rateTypeService.getByName(rTypeRecord.name.trim());
		if (byName != null && !Objects.equals(rType.getId(), byName.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        rType.setName(rTypeRecord.name.trim());
        rType.setPayType(rTypeRecord.payType);
        rType.setSequentialNumber(Integer.parseInt(rTypeRecord.sqNumber));

        rateTypeService.save(rType);
        return ResponseEntity.ok().build();
    }

    // Delete mappings
    @PostMapping("/rest/rate/delete/{rateId}")
    public ResponseEntity<String> deleteRate(@PathVariable("rateId") long rateId) {
        Rate rate = rateService.getById(rateId);
        if (rate == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        rateService.delete(rate.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rest/rType/updatePrimary/{rateTypeId}")
    public ResponseEntity<String> makePrimary(@PathVariable("rateTypeId") long rateTypeId) {
        RateType rateType = rateTypeService.getById(rateTypeId);
        if (rateType == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        RateType currentPrimary = rateTypeService.findPrimeRateType();
        if (currentPrimary != null) {
            currentPrimary.setPrime(false);
            rateTypeService.save(currentPrimary);
        }

        rateType.setPrime(true);
        rateTypeService.save(rateType);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/rest/rType/removePrimary/{rateTypeId}")
    public ResponseEntity<String> removePrimary(@PathVariable("rateTypeId") long rateTypeId) {
        RateType rateType = rateTypeService.getById(rateTypeId);
        if (rateType == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        rateType.setPrime(false);
        rateTypeService.save(rateType);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rest/rType/delete/{rTypeId}")
    public ResponseEntity<String> deleteRateType(@PathVariable("rTypeId") long rTypeId) {
        RateType rType = rateTypeService.getById(rTypeId);
        if (rType == null) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        rateTypeService.delete(rType.getId());
        return ResponseEntity.ok().build();
    }
}
