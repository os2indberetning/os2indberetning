package dk.digitalidentity.indberetning.controller.mvc;

import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.RateType;
import dk.digitalidentity.indberetning.security.RequireAdministrator;
import dk.digitalidentity.indberetning.service.RateService;
import dk.digitalidentity.indberetning.service.RateTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Controller
@RequireAdministrator
@RequiredArgsConstructor
public class RateController {
	private final RateService rateService;
	private final RateTypeService rateTypeService;

	@GetMapping("/admin/ratesFragment")
	public String ratesFragment(Model model) {
		List<Rate> rates = rateService.getAll();
        if (!rates.isEmpty()) {
			rates.sort(Comparator.comparing(Rate::getActiveYear, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        List<RateType> rateTypeList = rateTypeService.getAll();
        if (!rateTypeList.isEmpty()) {
			rateTypeList.sort(Comparator.comparing(RateType::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        model.addAttribute("rates", rates);
		model.addAttribute("rateTypes", rateTypeList);
		return "admin/ratesFragment";
	}

	@GetMapping("/admin/rateTypesFragment")
	public String rateTypesFragment(Model model) {
		List<RateType> rateTypeList = rateTypeService.getAll();
		if (!rateTypeList.isEmpty()) {
			rateTypeList.sort(Comparator.comparing(RateType::getName, Comparator.nullsLast(Comparator.naturalOrder())));
		}
		model.addAttribute("rateTypes", rateTypeList);
		return "admin/rateTypesFragment";
	}
}
