package main.controllers;

import main.config.ApplicationProperties;
import main.dto.DataForIndexingOnePage;
import main.dto.DataForPrimaryParsing;
import main.dto.ErrorResultResponse;
import main.dto.SearchServiceData;
import main.systems.indexing.LinkParser;
import main.dto.SearchResult;
import main.model.Site;
import main.model.SiteIndexingStatus;
import main.repositories.*;
import main.services.IndexingService;
import main.services.SearchService;
import main.services.StatisticsService;
import main.systems.lemmatize.Lemmatizer;
import main.systems.search.SearchSystem;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class IndexingController {

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private List<LinkParser> linkParserList;
    private ForkJoinPool pool;
    private final List<Thread> threadOfEachSiteList = new ArrayList<>();
    private HashMap<SearchResult, Float> searchResult = new HashMap<>();
    private String lastQuery;
    private String lastSite;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private IndexRepository indexRepository;

    @GetMapping("/startIndexing")
    public ResponseEntity<JSONObject> startIndexing() {
        JSONObject result = new JSONObject();
        if (isIndexing.get()) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Индексация уже запущена"));
        }
        try {
            isIndexing.set(true);
            pool = new ForkJoinPool();
            linkParserList = new ArrayList<>();
            IndexingService indexingService = new IndexingService(
                    siteRepository, lemmaRepository, pageRepository, fieldRepository, indexRepository);
            List<ApplicationProperties.CfgSite> cfgSiteList = applicationProperties.getSites();
            for (ApplicationProperties.CfgSite cfgSite : cfgSiteList) {
                Site site = new Site(SiteIndexingStatus.INDEXING, new Date(), null,
                        cfgSite.getUrl(), cfgSite.getName());
                LinkParser linkParser = new LinkParser(new DataForPrimaryParsing(
                        site, isIndexing, indexingService, cfgSiteList.indexOf(cfgSite), new Lemmatizer()));
                Thread thread = new Thread(() -> pool.invoke(linkParser));
                thread.setName(site.getUrl());
                thread.start();
                threadOfEachSiteList.add(thread);
                linkParserList.add(linkParser);
            }
            result.put("result", true);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Внутренняя ошибка сервера: " +
                    ex.getMessage()));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<JSONObject> stopIndexing() {
        JSONObject result = new JSONObject();
        if (!isIndexing.get()) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Индексация не запущена"));
        }
        try {
            isIndexing.set(false);
            linkParserList.forEach(linkParser -> linkParser.setIndexing(isIndexing));
            pool.shutdownNow();
            pool.awaitTermination(3, TimeUnit.MINUTES);

            siteRepository.findAll().forEach(site -> {
                site.setStatus(SiteIndexingStatus.FAILED);
                site.setLastError("Индексация приостановлена");
                site.setStatusTime(new Date());
            });
            siteRepository.flush();

            result.put("result", true);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Внутренняя ошибка сервера: " +
                    ex.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<JSONObject> getStatistics() {
        List<Site> siteList = siteRepository.findAll();
        threadOfEachSiteList.forEach(thread -> {
            Site site = new Site();
            for (Site siteForFind : siteList) if (siteForFind.getUrl().equals(thread.getName())) site = siteForFind;
            if (!thread.getState().equals(Thread.State.TERMINATED) || site.getLastError() != null) {
                return;
            }
            if (site.getStatus() != SiteIndexingStatus.INDEXED) {
                site.setStatusTime(new Date());
                site.setStatus(SiteIndexingStatus.INDEXED);
                siteRepository.save(site);
            }
        });
        if (siteList.stream().allMatch(s -> s.getStatus().equals(SiteIndexingStatus.INDEXED)) && isIndexing.get()) {
            isIndexing.set(false);
        }
        try {
            StatisticsService statisticsService =
                    new StatisticsService(siteRepository, lemmaRepository, pageRepository);
            return ResponseEntity.ok().body(statisticsService.getStatisticsResult(isIndexing.get()));
        } catch (Exception ex) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Внутренняя ошибка сервера: " +
                    ex.getMessage()));
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<JSONObject> indexPage(@RequestParam String url) {
        if (url.isEmpty()) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Задан пустой поисковый запрос"));
        }
        JSONObject result = new JSONObject();
        try {
            for (ApplicationProperties.CfgSite site : applicationProperties.getSites()) {
                if (!url.contains(site.getUrl())) {
                    continue;
                }
                IndexingService indexingService = new IndexingService(siteRepository, lemmaRepository,
                        pageRepository, fieldRepository, indexRepository);
                DataForIndexingOnePage data =
                        new DataForIndexingOnePage(url, indexingService, new Lemmatizer(), true);
                LinkParser indexOnePage = new LinkParser(data);
                if (pool == null || pool.isTerminated()) {
                    pool = new ForkJoinPool();
                }
                new Thread(() -> pool.invoke(indexOnePage)).start();
                result.put("result", true);
                return new ResponseEntity<>(result, HttpStatus.OK);
            }
            return ResponseEntity.ok().body(ErrorResultResponse.getResult(
                    "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        } catch (Exception ex) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Внутренняя ошибка сервера: " +
                    ex.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<JSONObject> search(@RequestParam String query,
                                             @RequestParam(required = false) String site,
                                             @RequestParam(defaultValue = "0") int offset,
                                             @RequestParam(defaultValue = "20") int limit) {
        if (isIndexing.get()) {
            return ResponseEntity.ok().body(
                    ErrorResultResponse.getResult("Приостановите индексацию или дождитесь её завершения"));
        }
        if (query.isEmpty()) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Задан пустой поисковый запрос"));
        }
        try {
            limit = applicationProperties.getLimit();
            boolean isEqualLastSite = Objects.equals(site, lastSite);
            if (!query.equals(lastQuery) || !isEqualLastSite) {
                SearchSystem searchSystem = new SearchSystem();
                searchResult = searchSystem.find(query, new IndexingService(
                                siteRepository, lemmaRepository, pageRepository, fieldRepository, indexRepository),
                        site);
                lastQuery = query;
                lastSite = site;
            }
            SearchServiceData searchServiceData = new SearchServiceData(siteRepository,
                    searchResult, applicationProperties.getSites(), limit, offset);
            JSONObject result = new SearchService(searchServiceData).getResult();
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.ok().body(ErrorResultResponse.getResult("Внутренняя ошибка сервера: " +
                    ex.getMessage()));
        }
    }
}
