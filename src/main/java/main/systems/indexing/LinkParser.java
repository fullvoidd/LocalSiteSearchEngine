package main.systems.indexing;

import main.dto.DataForIndexingOnePage;
import main.dto.DataForPrimaryParsing;
import main.dto.DataForSecondaryParsing;
import main.model.Field;
import main.model.Page;
import main.model.Site;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a main class for link parsing and further processing and DB filling.
 * {@code LinkParser} extends {@link RecursiveTask} for speed up the parsing and indexing processes.
 * This class request a main task, then fork it into other tasks.
 */
public class LinkParser extends RecursiveTask<List<String>> {
    private HashSet<String> uniqueUrls = new HashSet<>();
    private final String url;
    private String mySite;
    private Site site;
    private final Session session;
    private volatile AtomicBoolean isIndexing;
    private final IndexingService indexingService;
    private final Lemmatizer lemmatizer;
    private boolean indexOnePage = false;

    /**
     * This is a primary constructor, used to open session,
     * fill DB with initial data and start parsing and indexing.
     *
     * @param data DTO-object, which contains primary data
     */
    public LinkParser(DataForPrimaryParsing data) {
        this.lemmatizer = data.getLemmatizer();
        this.indexingService = data.getIndexingService();
        this.isIndexing = data.getIsIndexing();
        StandardServiceRegistry registry;
        String cfgPath;
        int number = data.getNumber();
        if (number == 0) {
            cfgPath = "hibernate.cfg.xml";
        } else {
            cfgPath = "hibernate_update.cfg.xml";
        }
        registry = new StandardServiceRegistryBuilder().configure(cfgPath).build();
        Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
        SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
        Session session = sessionFactory.openSession();
        this.session = session;
        session.beginTransaction();

        if (number == 0) {
            Field title = new Field("title", "title", (float) 1.0);
            Field body = new Field("body", "body", (float) 0.8);
            session.persist(title);
            session.persist(body);
        }
        Site site = data.getSite();
        indexingService.saveSite(site);
        this.site = indexingService.getSiteByUrl(site.getUrl());
        this.url = site.getUrl();
        mySite = url;
    }

    /**
     * This is a secondary constructor that request DTO-object, which contains data
     * for parsing and indexing links that follows from the previous link.
     *
     * @param data DTO-object, that contains main data for processing
     */
    public LinkParser(DataForSecondaryParsing data) {
        this.indexingService = data.getIndexingService();
        this.isIndexing = data.getIsIndexing();
        this.site = data.getSite();
        this.url = data.getUrl();
        this.mySite = data.getMySite();
        this.uniqueUrls = data.getUniqueUrls();
        this.session = data.getSession();
        this.lemmatizer = data.getLemmatizer();
    }

    /**
     * This is a situational constructor special for indexing one page.
     * It requests data that contains flag which switches system to one-page mode.
     *
     * @param data DTO-object, contains initial data for indexing one page
     */
    public LinkParser(DataForIndexingOnePage data) {
        this.indexOnePage = data.isIndexOnePage();
        this.lemmatizer = data.getLemmatizer();
        this.url = data.getUrl();
        this.indexingService = data.getIndexingService();
        indexingService.getAllSites().forEach(site -> {
            if (url.contains(site.getUrl())) {
                this.site = site;
                this.mySite = site.getUrl();
            }
        });

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().
                configure("hibernate_update.cfg.xml").build();
        Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
        SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();

        this.session = sessionFactory.openSession();
    }


    @Override
    protected List<String> compute() {
        if (indexOnePage) {
            linkProcessing(url, null, null);
            return null;
        }

        if (Thread.interrupted() || !isIndexing.get()) return null;

        List<String> list = new ArrayList<>();
        List<LinkParser> tasks = new ArrayList<>();
        Elements links = getLinks();

        if (links != null && !links.isEmpty())
            links.stream().map((link) -> link.absUrl("href")).forEachOrdered(thisUrl ->
                    linkProcessing(thisUrl, list, tasks));

        for (LinkParser task : tasks) {
            if (Thread.interrupted() || !isIndexing.get()) {
                return null;
            }
            list.addAll(task.join());
        }

        return list;
    }

    /**
     * This method connects to the url of the current page and
     * returns links to the other pages on this page.
     *
     * @return {@link Elements} that contains href-selector
     */
    private Elements getLinks() {
        Document doc = null;
        try {
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT " +
                            "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com");
            if (!url.equals(mySite)) connection.ignoreHttpErrors(true);
            Connection.Response response = connection.execute();
            if (Integer.toString(response.statusCode()).startsWith("2")) {
                doc = response.parse();
            }
        } catch (Exception ex) {
            System.out.println(url);
            if (url.equals(mySite)) {
                Site site = indexingService.getSiteByUrl(mySite);
                site.setLastError("Ошибка индексации: главная страница сайта недоступна.");
                indexingService.saveSite(site);
            }
            ex.printStackTrace();
        }
        if (doc != null) {
            return doc.select("a[href^=/]");
        }
        return null;
    }

    /**
     * This method checks for url format.
     *
     * @param thisUrl url of href-selector
     * @return boolean flag, that tells is the format correct
     */
    private boolean isConnect(String thisUrl) {
        if (!thisUrl.contains(mySite)) return false;
        if (thisUrl.contains("#")) return false;
        if ((thisUrl.endsWith(".html")) || (!thisUrl.substring(thisUrl.lastIndexOf("/")).contains("."))) {
            return indexOnePage || uniqueUrls.add(thisUrl);
        }
        return false;
    }

    /**
     * This method processing the specific link and filling DB with corresponding data (lemmas, pages, etc.).
     *
     * @param thisUrl url of the specific page
     * @param list {@code List} of urls
     * @param tasks {@code List} of the tasks, which further will be joined
     */
    private void linkProcessing(String thisUrl, List<String> list, List<LinkParser> tasks) {
        if (!isConnect(thisUrl)) {
            return;
        }
        Page page = pagePersisting(thisUrl);
        if (page == null) return;

        if (!page.getContent().equals("")) new IndexingSystem(page, indexingService, lemmatizer).run();

        if (indexOnePage) return;
        if (!(Thread.interrupted() || page.getContent().equals(""))) {
            list.add(thisUrl);
            DataForSecondaryParsing data = new DataForSecondaryParsing(thisUrl, mySite, uniqueUrls,
                    site, indexingService, isIndexing, session, lemmatizer);
            LinkParser task = new LinkParser(data);
            task.fork();
            tasks.add(task);
        }
    }

    /**
     * This method checks for availability of content, status code on specific page
     * and persist this page if all of this is not null.
     *
     * @param thisUrl url of the specific page
     * @return {@link Page} object for further processing
     */
    private Page pagePersisting(String thisUrl) {
        if (isIndexing != null && (Thread.interrupted() || !isIndexing.get())) return null;
        String path = thisUrl.substring(mySite.length() - 1);
        while (path.indexOf("/") != 0) path = path.substring(1);
        int responseCode = 0;
        String content = "";
        try {
            Connection.Response response = Jsoup.connect(thisUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .execute();
            responseCode = response.statusCode();
            if (Integer.toString(responseCode).startsWith("2")) {
                content = response.parse().toString();
            }
        }
        catch (Exception e) {
            System.out.println(thisUrl);
            e.printStackTrace();
        }
        Page page = !content.equals("")
                ? new Page(content, responseCode, path, site)
                : new Page("", responseCode, path, site);
        if (indexOnePage) {
            Page pageFromDB = indexingService.getPageByPathAndSiteId(path, site.getId());
            if (pageFromDB != null) indexingService.deletePageWhenReindexing(pageFromDB);
        }
        indexingService.savePage(page);
        return page;
    }

    /**
     * This method used to stop the parsing and indexing processes.
     *
     * @param isIndexing flag for stopping all processes
     */
    public void setIndexing(AtomicBoolean isIndexing) {
        this.isIndexing = isIndexing;
        if (session.isOpen()) session.close();
    }
}
