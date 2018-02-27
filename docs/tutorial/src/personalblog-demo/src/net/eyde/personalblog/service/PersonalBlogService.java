package net.eyde.personalblog.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.eyde.personalblog.beans.BlogProperty;
import net.eyde.personalblog.beans.Comment;
import net.eyde.personalblog.beans.Post;
import net.eyde.personalblog.beans.Referrer;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.tainting.qual.Untainted;

/**
 * @author NEyde
 *     <p>When the user selects a date, they will get the previous 25 posts from the date selected.
 *     <p>When a user selects a specific post, they will see that post only.
 *     <p>When a user selects a month, they will get all the posts for the month.
 */
public class PersonalBlogService {
    // Installation State
    public static final String INSTALLATION_STATE = "installation_state";
    public static final String STATE_UNDEFINED = "undefined";
    public static final String STATE_NO_HIBERNATE_FILE = "no_hibernate_file";
    public static final String STATE_DATABASE_OFF = "database_off";
    public static final String STATE_HIBERNATE_FILE_INVALID = "hibernate_file_invalid";
    public static final String STATE_TABLES_NOT_CREATED = "tables_not_created_yet";
    public static final String STATE_MISSING_PROPERTIES = "missing_properties";
    public static final String STATE_OK = "ok";
    private static Log log = LogFactory.getLog(PersonalBlogService.class);
    private static PersonalBlogService service = null;

    // Property Name Constants
    public static final String WEBLOG_TITLE = "weblog.title";
    public static final String WEBLOG_DESCRIPTION = "weblog.description";
    public static final String WEBLOG_PICTURE = "weblog.ownerpicture";
    public static final String WEBLOG_OWNER_NICK_NAME = "weblog.ownernickname";
    public static final String WEBLOG_URL = "weblog.url";
    public static final String WEBLOG_OWNER = "weblog.owner";
    public static final String WEBLOG_EMAIL = "weblog.email";
    public static final String LINK_POST = "links.post";
    public static final String EMOTICON_VALUES = "emoticon.values";
    public static final String EMOTICON_IMAGES = "emoticon.images";
    public static final String LOGON_ID = "logon.id";
    public static final String LOGON_PASSWORD = "logon.password";
    public static final String EDITOR = "weblog.editor";
    public static final String EMAIL_HOST = "mail.smtp.host";
    public static final String EMAIL_TRANSPORT = "mail.transport";
    public static final String EMAIL_USERNAME = "mail.username";
    public static final String EMAIL_PASSWORD = "mail.password";
    public static final String CATEGORY_TITLES = "category.titles";
    public static final String CATEGORY_VALUES = "category.values";
    public static final String CATEGORY_IMAGES = "category.images";
    Configuration cfg;
    SessionFactory sf;

    int adjustHours;
    PropertyManager pm;
    CacheManager cache;

    // is really necessary when you are going to format it?
    Locale myLocale = Locale.US;
    String dburl;
    String dbuser;
    String dbpassword;
    SimpleDateFormat qf = new SimpleDateFormat("yyyy-MM-dd", myLocale);
    SimpleDateFormat monthNav = new SimpleDateFormat("yyyyMM", myLocale);

    /** Constructor for PersonalBlogService. */
    protected PersonalBlogService(Properties conn) throws InitializationException {
        log.debug("initialization - constructor");

        try {
            cfg =
                    new Configuration()
                            .addClass(Post.class)
                            .addClass(Comment.class)
                            .addClass(Referrer.class)
                            .addClass(BlogProperty.class);

            if (conn != null) {
                cfg.setProperties(conn);
                pm = new PropertyManager(conn);
            } else {
                pm = new PropertyManager();
            }

            // I want to take it out of here, for these
            sf = cfg.buildSessionFactory();
        } catch (Exception e) {
            log.error("Error initializing PersonalBlog Service", e);

            throw new InitializationException(e);
        }
    }

    /** Singleton getInstance method */
    public static PersonalBlogService getInstance() throws ServiceException {
        if (service == null) {
            try {
                log.debug("Initializing PersonalBlog Service (WITHOUT CONNECTION PARMS)");
                service = new PersonalBlogService(null);
            } catch (ServiceException e) {
                log.error("Error getting instance of PersonalBlog Service", e);

                throw e;
            }
        }

        return service;
    }

    public static PersonalBlogService getInstance(Properties conn) throws ServiceException {
        if (service == null) {
            try {
                log.debug("Initializing PersonalBlog Service (WITH CONNECTION PARMS)");
                service = new PersonalBlogService(conn);
            } catch (Exception e) {
                log.error("Error getting instance of PersonalBlog Service", e);
            }
        }

        return service;
    }

    /*
     * This method will return the most recent posts for today's date.  This method
     * will return a maximum of 25 total posts or three days worth of posts.
     *
     */
    public List<?> getPosts() throws ServiceException {
        List<?> posts = null;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        @SuppressWarnings("tainting")
        String startdate = (@Untainted String) qf.format(cal.getTime());

        posts =
                executeQuery(
                        "from post in class net.eyde.personalblog.beans.Post "
                                + "where post.created > '"
                                + startdate
                                + "' order by post.created desc");

        return posts;
    }

    public List<?> getPostsByCategory(String category) throws ServiceException {
        List<?> posts = null;

        posts =
                executeQuery(
                        "from post in class net.eyde.personalblog.beans.Post "
                                + "where post.category like '%"
                                + category
                                + "%' order by post.created desc");

        return posts;
    }

    private <T> List<T> executeQuery(@Untainted String query) {
        try {
            Session session = sf.openSession();
            @SuppressWarnings({"unchecked"})
            List<T> lst = (List<T>) session.find(query);
            session.close();
            return lst;
        } catch (Exception e) {
            log.error("Error while importing data", e);
            return null;
        }
    }
}
