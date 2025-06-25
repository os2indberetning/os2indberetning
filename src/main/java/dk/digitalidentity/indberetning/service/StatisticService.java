package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.StatisticDao;
import dk.digitalidentity.indberetning.model.entity.Statistic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticService {
    private final StatisticDao statisticDao;
    private final RouteService routeService;


    public Statistic findByFunctionName(String name) {
        return statisticDao.findByFunctionName(name);
    }

    public void save(Statistic statistic) {
        statisticDao.save(statistic);
    }

    public List<Statistic> findAll() {
        return statisticDao.findAll();
    }

    public void deleteAll(List<Statistic> statistics) {
        statisticDao.deleteAll(statistics);
    }

    public void clearAll() {
        deleteAll(findAll());
    }

    public void flush() {
        statisticDao.flush();
    }

    public void updateStatistics() {
        Map<String, Integer> map = routeService.getStatisticsMap();
        List<Statistic> toBeUpdated = new ArrayList<>();
        if (map != null && !map.isEmpty()) {
            map.entrySet().forEach(entry -> {
                Statistic statistic = findByFunctionName(entry.getKey());
                if (statistic != null) {
                    statistic.setCount(statistic.getCount() + entry.getValue());
                    toBeUpdated.add(statistic);
                }
                else {
                    statistic = new Statistic();
                    statistic.setCount(entry.getValue());
                    statistic.setFunctionName(entry.getKey());
                    toBeUpdated.add(statistic);
                }
            });
            saveAll(toBeUpdated);
            routeService.setStatisticsMap(new ConcurrentHashMap<>());
        }
    }

    private void saveAll(List<Statistic> toBeUpdated) {
        statisticDao.saveAll(toBeUpdated);
    }
}