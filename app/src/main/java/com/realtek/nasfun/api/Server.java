package com.realtek.nasfun.api;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtek.nasfun.HttpHelper;

import com.realtek.nasfun.api.CGIResponse.Item;
import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Server {
    private final static String TAG = "Server";

    public final static int LOG_SYSTEM_BOOT_MSG = 0;
    public final static int LOG_SYSTEM_LOG = 1;
    public final static int LOG_WEB_SERER = 2;
    public final static int LOG_FTP = 3;
    public final static int LOG_ITUNES = 4;
    public final static int LOG_MEDIA_SERVER_SERVICE = 5;
    private final static String[] LOG_FILE_NAME = {
            "boot.log",
            "system.log",
            "webserver.log",
            "ftps.log",
            "daap.log",
            "dlna.log"};

    //NAS FW type
    public final static int FW_TYPE_JUPITER_MEDIA_NAS_IPODCGI = 0;    //	1185 Jupiter/1186 Saturn media nas(dvdplayer) with ipodCGI
    public final static int FW_TYPE_JUPITER_PURE_NAS_IPODCGI = 1;    //	1185 Jupiter/1186 Saturn pure nas with ipodCGI
    public final static int FW_TYPE_PHOENIX_MEDIA_NAS_RTCGI = 2;    //	1195 Phoenix media nas (android system) with rtCGI
    public final static int FW_TYPE_PHOENIX_PURE_NAS_IPODCGI = 3;    //	1195 Phoenix pure nas with ipodCGI

    //Servers are temporarily classified by NAS FW type
    /*public enum ServerType{
		UNDEFINITION(-1),
		DEFAULT_NAS(R.string.title_nas_display_name_default),
		JUPITER_MEDIA_NAS(R.string.title_nas_display_name_jupiter_media),
		JUPITER_PURE_NAS(R.string.title_nas_display_name_jupiter_pure),
		PHOENIX_MEDIA_NAS(R.string.title_nas_display_name_pheonix_media),
		PHOENIX_PURE_NAS(R.string.title_nas_display_name_pheonix_pure);
		
		private final int iDisplayName; 
		
		ServerType(int iDisplayName){
			this.iDisplayName = iDisplayName;
		}
		
		public final int getDisplayNameRes(){
			return iDisplayName;
		}


    	<!-- realtek, add following strings to strings.xml -->
    	<string name="title_nas_display_name_default">RTK - Nas</string>
    	<string name="title_nas_display_name_jupiter_media">RTK - Jupiter Media Nas</string>
    	<string name="title_nas_display_name_jupiter_pure">RTK - Jupiter Pure Nas</string>
    	<string name="title_nas_display_name_pheonix_media">RTK - Pheonix Media Nas</string>
    	<string name="title_nas_display_name_pheonix_pure">RTK - Pheonix Pure Nas</string>

	}*/

    //NAS API
    private final static String NAS_LOGIN_PATH = "/nas/login";
    private final static String NAS_LOGOUT_PATH = "/nas/logout";
    private final static String NAS_GEN_HASH_PATH = "/nas/gen/hash";
    private final static String CGI_PATH = "/cgi-bin/IpodCGI.cgi";
    private final static String NAS_GET_MNAS_PATH = "/nas/get/mnas";
    private final static String NAS_GET_TUTK_PATH = "/nas/get/tutk";
    private final static String NAS_VERSION_PATH = "/movie-api/tver.xml";
    private final static String NAS_CHANGE_PASSWORD_PATH = "/nas/edit/user";
    private final static String NAS_GET_APLIST_PATH = "/nas/get/aplist";
    private final static String NAS_GET_INFO = "/nas/get/info";
    private final static String NAS_GET_USERS = "/nas/get/users";

    //NAS property default value
    public final static String DEFAULT_HOSTNAME = "192.168.59.254";
    public final static String DEFAULT_USERNAME = "admin";
    public final static String DEFAULT_PASSWORD = "Realtek";
    public final static String DEFAULT_GUEST_USERNAME = "admin";
    public final static String DEFAULT_GUEST_PAASWORD = "Realtek";
    public final static String HOME = "/homes/";
    //DAV home path
    public final static String DEVICE_DAV_HOME = "/dav/device/homes";
    public final static String USER_DAV_HOME = "/dav/home";

    // audio/video streaming playback folder(HOME is mapped to user's home directory by session id)
    public final static String STREAMING_FOLDER = "HOME";

    /**
     * NAS Supported Services
     */
    public enum Service {
        SAMBA("/nas/get/samba", "/nas/set/samba", new SambaStatus(), true),
        AFP("/nas/get/afpd", "/nas/set/afpd", new AfpdStatus(), true),
        FTP("/nas/get/ftpd", "/nas/set/ftpd", new FtpStatus(), true),
        ITUNES("/nas/get/daap", "/nas/set/daap", new DaapStatus(), true),
        DOWNLOADER("/nas/get/bt", "/nas/set/bt", new BtStatus(), true),
        DLNA("/nas/get/dlna", "/nas/set/dlna", new DlnaStatus(), true),
        ADVANCED("/nas/get/advance", "/nas/set/advance", new AdvanceStatus(), true),
        CAMERA("/nas/get/ipcam", "/nas/set/ipcam", new IPCamStatus(), true),
        NETWORK("/nas/get/network", "/nas/set/network", new NetworkStatus(), true),
        SOFTAP("/nas/get/softap", "/nas/set/softap", new SoftAPStatus(), false),
        WIFI("/nas/get/station", "/nas/set/station", new WiFiStatus(), false);
        // MEDIA("/nas/get/media", "/nas/set/media", new xxxxxxx()),
        // SMTP("/nas/get/smtp", "/nas/set/smtp", new xxxxxxx()),
        // DATE("/nas/get/date", "/nas/set/date", new xxxxxxx()),
        // ZONES("/nas/get/zones", "/nas/set/zones", new xxxxxxx()),
        // DATE("/nas/get/ntp", "/nas/set/ntp", new xxxxxxx()),
        // SPINDOWN("/nas/get/spindown", "/nas/set/spindown", new xxxxxxx()),	// Power Management
        // ADVANCE("/nas/get/advance", "/nas/set/advance", new xxxxxxx()),

        private boolean isAvailable;
        private final String getPath;
        private final String setPath;
        private final ServiceStatus status;

        Service(String getPath, String setPath, ServiceStatus status, boolean isAvail) {
            this.getPath = getPath;
            this.setPath = setPath;
            this.status = status;
            this.isAvailable = isAvail;
        }
    }

    String hostname = null;
    String username = null;
    String password = null;
    String tutkUUID = null;
    String loginError = null;
    //Server always hold its password. However, users can get password only if isPasswordSaved is true.
    boolean isPasswordSaved = false;
    String hash;
    String nasModules;
    String nasPublic;
    String tmp_version;
    //int displayName = ServerType.UNDEFINITION.getDisplayNameRes();
    int firmwareType = -1;
    ServerInfo info = null;
    private boolean isConnected = false;

    //Nas Server support functionalities
    boolean isSupportedVideoUnKnown = true;
    boolean isBackUPServiceAvailable = false;

    ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
    private MusicManager musicManager = null;
    private PhotoManager photoManager = null;
    private VideoManager videoManager = null;
    private FileSystemManager fsManager = null;
    private String lastBackupTime = "";

    /**
     * Create a server instance using default host name, user name and password
     */
    public Server() {
        this(DEFAULT_HOSTNAME, DEFAULT_USERNAME, DEFAULT_PASSWORD);

    }

    /**
     * Create a server instance
     *
     * @param hostname
     * @param username
     * @param password
     */
    public Server(String hostname, String username, String password) {
        // reset service default status
        Service.FTP.isAvailable = true;
        Service.SAMBA.isAvailable = true;
        Service.CAMERA.isAvailable = true;
        Service.SOFTAP.isAvailable = false;
        Service.DOWNLOADER.isAvailable = true;
        isBackUPServiceAvailable = false;

        //rest optional function
        isSupportedVideoUnKnown = false;

        this.hostname = hostname;
        this.username = username;
        this.password = password;
    }

    /**
     * @return if the server is connected or not.
     */
    public boolean isConnected() {
        return this.isConnected;
    }

    /**
     * Login to server.
     * This function will do HTTP connection and should not be called from
     * android UI thread directly.
     *
     * @return successful or not
     */
    public boolean connect(boolean checkTutk) {
        Log.d(TAG, "Connecting to server:" + hostname);

        isConnected = (doGenHash() && doLogin() && doGetServerInfo(hostname));
        if (isConnected && checkTutk)
            isConnected = isConnected && checkTutkuid();

        if (isConnected) {
            Log.i(TAG, "Login to " + hostname + " success");
        } else {
            Log.i(TAG, "Login to " + this + " fail");
        }

        return isConnected;
    }

    /**
     * Logout.
     * This function will do HTTP connection and should not be called from
     * android UI thread directly.
     */
    public void close() {
        Log.i(TAG, "Logout from " + hostname);
        if (isConnected) {
            doLogout();
            this.hash = null;
            this.isConnected = false;
        }
    }

    /**
     * Get NAS Server information, need to login first.
     *
     * @return
     */
    public ServerInfo getServerInfo() {
        return info;
    }

    public int getFirmwareType() {
        return firmwareType;
    }

    public boolean setFirmwareType() {
        boolean isSuccess = true;
        //1195
        if (info.hardware.equals("1195")) {
            if (isMediaNas())
                firmwareType = FW_TYPE_PHOENIX_MEDIA_NAS_RTCGI;
            else
                firmwareType = FW_TYPE_PHOENIX_PURE_NAS_IPODCGI;
            //1185 or 1186
        } else if (info.hardware.equals("Jupiter") || info.hardware.equals("Saturn")) {
            if (isMediaNas())
                firmwareType = FW_TYPE_JUPITER_MEDIA_NAS_IPODCGI;
            else
                firmwareType = FW_TYPE_JUPITER_PURE_NAS_IPODCGI;
        } else {
            isSuccess = false;
            Log.d(TAG, "not support this hardware = " + info.hardware);
        }
        Log.d(TAG, "set firmwareType = " + firmwareType);
        return isSuccess;
    }
	
	/*get the string resource id */
	/*public int getDisplayName() {
		if(displayName == ServerType.UNDEFINITION.getDisplayNameRes()) {
			if(firmwareType == FW_TYPE_JUPITER_MEDIA_NAS_IPODCGI)
				displayName = ServerType.JUPITER_MEDIA_NAS.getDisplayNameRes();
			else if(firmwareType == FW_TYPE_JUPITER_PURE_NAS_IPODCGI)
				displayName = ServerType.JUPITER_PURE_NAS.getDisplayNameRes();
			else if(firmwareType == FW_TYPE_PHOENIX_MEDIA_NAS_RTCGI)
				displayName = ServerType.PHOENIX_MEDIA_NAS.getDisplayNameRes();
			else if(firmwareType == FW_TYPE_PHOENIX_PURE_NAS_IPODCGI)
				displayName = ServerType.PHOENIX_PURE_NAS.getDisplayNameRes();
			else 
				displayName = ServerType.DEFAULT_NAS.getDisplayNameRes();
		}
		
		return displayName;
	}*/

    /**
     * GET "http://"+hostname+NAS_VERSION_PATH
     * Response Example
     * <root>
     * <movie-api>
     * <op>
     * <element>
     * <unknown>yes</unknown>
     * </element>
     * </op>
     * </movie-api>
     * <services>
     * <service>samba</service>
     * <service>softap</service>
     * <service>one_click_backup</service>
     * </services>
     * <tmp_version>20141224</tmp_version>
     * </root>
     * To parse this XML file for getting the value of :
     * (1)tmp_version
     * (2)isSupportedVideoUnKnown
     * (3)available services
     */
    private void getServerProfile() {
        try {
            String commandURL = "http://" + hostname + NAS_VERSION_PATH;
            Log.d(TAG, "Get " + commandURL);
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpGet httpGet = new HttpGet(commandURL);
            HttpResponse httpResponse;
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                        if (curTagName.equals("services")) {
                            // from version 20141224, add "services" tag to
                            // indicate these services' enable/disable
                            Service.FTP.isAvailable = false;
                            Service.SAMBA.isAvailable = false;
                            Service.CAMERA.isAvailable = false;
                            Service.SOFTAP.isAvailable = false;
                            Service.DOWNLOADER.isAvailable = false;
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("tmp_version")) {
                                tmp_version = new String(text);
                                Log.i(TAG, "nas server's tmp_version is " + tmp_version);
                                break;
                            } else if (curTagName.equals("unknown")) {
                                String unknown = text;

                                if (unknown != null) {
                                    Log.i(TAG, "unknown = " + unknown);
                                    if (unknown.equals("yes")) {
                                        isSupportedVideoUnKnown = true;
                                    }
                                } else {
                                    Log.d(TAG, "Can't get unknown info ");
                                }
                            } else if (curTagName.equals("service")) {
                                // specified service is available
                                if (text.equals("samba")) {
                                    Service.SAMBA.isAvailable = true;
                                } else if (text.equals("softap")) {
                                    Service.SOFTAP.isAvailable = true;
                                } else if (text.equals("ftpd")) {
                                    Service.FTP.isAvailable = true;
                                } else if (text.equals("ipcam")) {
                                    Service.CAMERA.isAvailable = true;
                                } else if (text.equals("bt")) {
                                    Service.DOWNLOADER.isAvailable = true;
                                } else if (text.equals("one_click_backup")) {
                                    isBackUPServiceAvailable = true;
                                }
                            }
                        }
                    }

                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * <nas>
     * <enable>yes</enable>
     * </nas>
     *
     * @return Is this a media nas? (true/false)
     */
    public boolean isMediaNas() {
        boolean isMediaNas = true;

        try {
            String commandURL = "http://" + hostname + NAS_GET_MNAS_PATH + "?session=" + hash;
            Log.d(TAG, "Get " + commandURL);
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpGet httpGet = new HttpGet(commandURL);
            HttpResponse httpResponse;
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("enable")) {
                                String enable = text;

                                if (enable != null) {
                                    if (enable.equals("no")) isMediaNas = false;
                                    Log.d(TAG, "enable = " + enable);
                                    break;
                                } else {
                                    Log.d(TAG, "Can't get enalbe/disable info of media nas");
                                }
                            }
                        }
                    }

                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return isMediaNas;
    }

    /**
     * <nas>
     * <enable>yes</enable>
     * </nas>
     *
     * @return Is this a media nas? (true/false)
     */
    public boolean checkTutkuid() {
        boolean isTutkNas = false;
        try {
            String commandURL = "http://" + hostname + NAS_GET_TUTK_PATH + "?session=" + hash;
            Log.d(TAG, "Get " + commandURL);
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpGet httpGet = new HttpGet(commandURL);
            HttpResponse httpResponse;
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("tutkuid")) {
                                String enable = text;
                                if (enable != null) {
                                    if (!enable.equals("")) {
                                        isTutkNas = true;
                                        setTutkUUID(enable);
                                    }
                                    Log.d(TAG, "tutkuid = " + enable);
                                    break;
                                }
                            } else if (curTagName.equals("reason")) {
                                setTutkUUID(null);
                                if ("No Permission".equals(text)) {
                                    loginError = "Please update StoreJet Cloud firmware";
                                } else {
                                    loginError = text;
                                }
                                Log.d(TAG, "get device info fail due to : " + loginError);
                                return false;
                            }
                        }
                    }

                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return isTutkNas;
    }


    public MusicManager getMusicManager() {
        if (musicManager == null) {
            switch (firmwareType) {
                case FW_TYPE_JUPITER_MEDIA_NAS_IPODCGI:
                    musicManager = new IpodCGIMusicManager(this);
                    break;
                case FW_TYPE_JUPITER_PURE_NAS_IPODCGI:
                    musicManager = new IpodCGIMusicManager(this);
                    break;
                case FW_TYPE_PHOENIX_MEDIA_NAS_RTCGI:
                    musicManager = new RtCGIMusicManager(this);
                    break;
                case FW_TYPE_PHOENIX_PURE_NAS_IPODCGI:
                    musicManager = new IpodCGIMusicManager(this);
                    break;
            }
        }

        return musicManager;
    }

    public PhotoManager getPhotoManager() {
        if (photoManager == null) {
            switch (firmwareType) {
                case FW_TYPE_JUPITER_MEDIA_NAS_IPODCGI:
                    photoManager = new IpodCGIPhotoManager(this);
                    break;
                case FW_TYPE_JUPITER_PURE_NAS_IPODCGI:
                    photoManager = new IpodCGIPhotoManager(this);
                    break;
                case FW_TYPE_PHOENIX_MEDIA_NAS_RTCGI:
                    photoManager = new RtCGIPhotoManager(this);
                    break;
                case FW_TYPE_PHOENIX_PURE_NAS_IPODCGI:
                    photoManager = new IpodCGIPhotoManager(this);
                    break;
            }
        }

        return photoManager;
    }

    public VideoManager getVideoManager() {
        if (videoManager == null) {
            videoManager = new VideoManager(this, isSupportedVideoUnKnown);
        }
        return videoManager;
    }

    public FileSystemManager getFileSystemManager() {
        if (fsManager == null) {
            fsManager = new FileSystemManager(this, isBackUPServiceAvailable);
        }
        return fsManager;
    }

    /**
     * Download server logs
     * TODO: not tested
     *
     * @return
     */
    public File downloadServerLogFile(int logType, File storageDir) {
        File logFile = null;
        if (logType < LOG_SYSTEM_BOOT_MSG || logType > LOG_MEDIA_SERVER_SERVICE) {
            Log.e(TAG, "Wrong log types=" + logType);
            return null;
        }
        String commandURL = "http://" + hostname + "/" + hash + "/" + LOG_FILE_NAME[logType];
        HttpGet httpGet = new HttpGet(commandURL);
        try {
            logFile = File.createTempFile(LOG_FILE_NAME[logType], null, storageDir);
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            FileUtils.copyInputStreamToFile(inputStream, logFile);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return logFile;
    }

    //public List<User> getUsers(){}
    //public boolean addUser(User user){}
    //public boolean delUser(User user){}
    //public boolean reboot(){} // http://nas/leave, action=reboot
    //public boolean shutdown(){}

    /**
     * Get service status.
     * This function will do HTTP connection and should not be called from
     * android UI thread directly.
     *
     * @param service
     * @return
     */
    public <E extends ServiceStatus> E getServiceStatus(Service service) {
        try {

            String commandURL = "http://" + hostname + service.getPath;
            HttpPost httpPost = new HttpPost(commandURL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("session", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            service.status.parse(inputStream, inputEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Log.i(TAG, service.status.toString());
        return (E) service.status;
    }

    /**
     * This function will do HTTP connection and should not be called from
     * android UI thread directly.
     *
     * @param service
     * @return
     */
    public ServiceStatus setServiceStatus(Service service) {
        ServiceStatus status = service.status;
        try {
            Log.d(TAG, "setServiceStatus:" + service.name());
            String commandURL = "http://" + hostname + service.setPath;
            HttpPost httpPost = new HttpPost(commandURL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("session", hash));

            // fill up the name value pare for services
            List<ServiceStatus.Property> serviceProperties = status.getProperties();
            for (ServiceStatus.Property p : serviceProperties) {
                Log.d(TAG, "Set " + p.name + "=" + p.value);
                nameValuePairs.add(new BasicNameValuePair(p.name, p.value));
            }

            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            service.status.parse(inputStream, inputEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Log.i(TAG, service.status.toString());
        return service.status;
    }

    public boolean isServiceAvail(Service service) {
        return service.isAvailable;
    }

    public void enableServiceAvail(Service service, boolean isAvailable) {
        service.isAvailable = isAvailable;
    }

    public boolean isBackUPServiceAvail() {
        return isBackUPServiceAvailable;
    }

    public String getLastBackupTime() {
        return lastBackupTime;
    }

    public void setLastBackupTime(String time) {
        lastBackupTime = time;
    }

    public IPCamStatus startCamcorder(int durationInMins) {

        try {
            /**
             * hash:fbcef039d72f708cbd1d4c916bfe679755cee606
             * enable:yes
             * play:yes
             * duration:1
             */
            String commandURL = "http://" + hostname + Service.CAMERA.setPath;
            HttpPost httpPost = new HttpPost(commandURL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("session", hash));
            nameValuePairs.add(new BasicNameValuePair("enable", "yes"));
            nameValuePairs.add(new BasicNameValuePair("play", "yes"));
            nameValuePairs.add(new BasicNameValuePair("duration", String.valueOf(durationInMins)));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity rspEntity = httpResponse.getEntity();
            InputStream inputStream = rspEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            Service.CAMERA.status.parse(inputStream, inputEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Log.i(TAG, Service.CAMERA.status.toString());
        return (IPCamStatus) Service.CAMERA.status;
    }

    public IPCamStatus stopCamcorder() {
        try {
            /**
             * hash:fbcef039d72f708cbd1d4c916bfe679755cee606
             * enable:yes
             * play:no
             */
            String commandURL = "http://" + hostname + Service.CAMERA.setPath;
            HttpPost httpPost = new HttpPost(commandURL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("session", hash));
            nameValuePairs.add(new BasicNameValuePair("enable", "yes"));
            nameValuePairs.add(new BasicNameValuePair("play", "no"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity rspEntity = httpResponse.getEntity();
            InputStream inputStream = rspEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            // <?xml version="1.0" encoding="utf-8"?><nas><recorder>no</recorder></nas>
            Service.CAMERA.status.parse(inputStream, inputEncoding);
        } catch (Exception e) {
            // UnsupportedEncodingException, ClientProtocolException, IOException
            e.printStackTrace();
        }
        return (IPCamStatus) Service.CAMERA.status;
    }


    /**
     * <nas>
     * <aplist>
     * <ap>
     * <frequency>2437</frequency>
     * <ssid>dir-635</ssid>
     * <signal>-57</signal>
     * <quality>53</quality>
     * <security>mixed-wpa</security>
     * <channel>6</channel>
     * </ap>
     * </aplist>
     * </nas>
     *
     * @author phyllis
     */
    public ArrayList<WiFi> scanWiFi() {

        ArrayList<WiFi> wifiList = new ArrayList<WiFi>();
        try {
            /**
             * hash:fbcef039d72f708cbd1d4c916bfe679755cee606
             * */

            String commandURL = "http://" + hostname + NAS_GET_APLIST_PATH + "?session=" + hash;
            HttpPost httpPost = new HttpPost(commandURL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("session", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {

                        if (tagName.equals("ap")) {
                            WiFi wifi = new WiFi();
                            String subTagName = null;
                            int subEventType;
                            do {
                                subEventType = xpp.next();

                                if (subEventType == XmlPullParser.START_TAG) {
                                    subTagName = xpp.getName();
                                } else if (subEventType == XmlPullParser.TEXT) {
                                    text = xpp.getText();
                                    wifi.processCustomTag(subTagName, text);
                                } else if (subEventType == XmlPullParser.END_TAG) {
                                    if (xpp.getName() != null) {
                                        subTagName = xpp.getName();
                                        Log.d(TAG, "scanWiFi, END_TAG, name = " + subTagName);
                                    }
                                }
                            }
                            while (!subTagName.equals("ap") || subEventType != XmlPullParser.END_TAG);
                            wifiList.add(wifi);
                        }
                    }

                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Log.i(TAG, wifiList.toString());
        return wifiList;

    }

    /**
     * We do not use password field here to force user input old password again
     * Success response:
     * <nas><edituser>update password</edituser></nas>
     * Fail response:
     * <nas><error><reason>Wrong Username or Password</reason><detail/></error></nas>
     *
     * @param oldPassword
     * @param newPassword
     * @return
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        boolean isSuccess = false;
        String oldEncryped = null;
        String newEncryped = null;

        DefaultHttpClient httpClient = HttpClientManager.getClient();
        do {
            oldEncryped = Server.encryptPassword(oldPassword, nasModules, nasPublic);
            if (oldEncryped == null)
                break;
            newEncryped = Server.encryptPassword(newPassword, nasModules, nasPublic);
            if (newEncryped == null)
                break;

            // Send login request to server
            String commandURL = "http://" + hostname + NAS_CHANGE_PASSWORD_PATH;
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("username", username));
            nameValuePairs.add(new BasicNameValuePair("password", oldEncryped));
            nameValuePairs.add(new BasicNameValuePair("newpassword", newEncryped));
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            nameValuePairs.add(new BasicNameValuePair("lang", "zh-tw"));
            try {
                HttpPost httpPost = new HttpPost(commandURL);
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity == null) {
                    Log.e(TAG, "httpEntity is null");
                    break;
                }
                String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }
                String rspString = EntityUtils.toString(httpEntity, inputEncoding);
                Log.d(TAG, "Change password, rsp=" + rspString);
                if (rspString.contains("update password")) {
                    isSuccess = true;
                } else if (rspString.contains("error")) {
                    int idx1 = rspString.indexOf("<reason>");
                    int idx2 = rspString.indexOf("</reason>");
                    Log.w(TAG, "Change password fail, reason=" + rspString.substring(idx1 + 8, idx2));
                }
            } catch (Exception e) {
                // UnsupportedEncodingException, XmlPullParserException, IOException
                e.printStackTrace();
                Log.e(TAG, "Fail to post login info to server");
            }
        } while (false);

        return isSuccess;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTutkUUID() {
        return tutkUUID;
    }

    public void setTutkUUID(String tutkUUID) {
        this.tutkUUID = tutkUUID;
    }

    public String getLoginError() {
        return loginError;
    }

    public void enablePasswordSaved(boolean enable) {
        this.isPasswordSaved = enable;
    }

    public boolean isPasswordSaved() {
        return isPasswordSaved;
    }

    /**
     * @return
     * @deprecated
     */
    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "Server [hostname=" + hostname + ", username=" + username
                + ", password=" + password + ", hash=" + hash
                + ", isConnected=" + isConnected + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((hostname == null) ? 0 : hostname.hashCode());
        result = prime * result
                + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    /**
     * only compare hostname and username.
     * password is not compared.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Server other = (Server) obj;

        if (hostname == null) {
            if (other.hostname != null)
                return false;
        } else if (!hostname.equals(other.hostname))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;

        return true;
    }


    CGIResponse sendCGICommand(String command) {
        return sendCGICommand(command, 0, 9999);
    }

    CGIResponse sendCGICommand(String command, int index, int count) {
        Log.d(TAG, "sendCGICommand:" + command + ", index=" + index + ", count=" + count);
        CGIResponse cgiResponse = null;
        String commandURL = "http://" + getHostname() + CGI_PATH;
        String reqString = null;
        String rspString = null;
        int totalCount = 0;
        int curCount = 0;
        ArrayList<Item> items = new ArrayList<Item>();

        try {
            do {
                Log.d(TAG, "Query, index=" + index + ", count=10");
                // prepare CGI request object
                CGIRequest cgiReq = new CGIRequest();
                CGIRequest.Request req = new CGIRequest.Request();
                req.setId(command);
                req.setNumberOfItems(String.valueOf(10));
                req.setRequestType("itemList");
                req.setStartIndex(String.valueOf(index));
                req.setTs(String.valueOf(System.currentTimeMillis()));
                cgiReq.setRequest(req);
                reqString = mapper.writeValueAsString(cgiReq);
                StringEntity strEntity = new StringEntity(reqString, "UTF-8");

                // prepare http post request
                HttpPost httpPost = new HttpPost(commandURL);
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setEntity(strEntity);
                DefaultHttpClient httpClient = HttpClientManager.getClient();
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity rspEntity = httpResponse.getEntity();
                //InputStream inputStream = rspEntity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.UTF_8;
                }
                rspString = EntityUtils.toString(rspEntity, inputEncoding);
                cgiResponse = mapper.readValue(rspString, CGIResponse.class);

                totalCount = cgiResponse.getTotalNumberOfItems();
                curCount += cgiResponse.getTotalNumberOfChildren();
                index += cgiResponse.getTotalNumberOfChildren();
                items.addAll(cgiResponse.getChildren());

                Log.d(TAG, "curCount:" + curCount +
                        ", TotalNumberOfItems:" + cgiResponse.getTotalNumberOfItems());

                // number of items is enough
                if (curCount >= count)
                    break;
                // no more items
                if (index >= totalCount)
                    break;
            } while (true);

            // change the final response contain
            cgiResponse.setChildren(items);
            cgiResponse.setTotalNumberOfChildren(items.size());

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            Log.d(TAG, "Request=" + reqString);
            Log.d(TAG, "Response=" + rspString);
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        //Log.d(TAG, "sendCGICommand("+command+")="+cgiResponse);
        return cgiResponse;
    }

    /**
     * getRtCGIResponse for RT CGI command
     */
    Object getRtCGIResponse(String command, Class<?> t) {
        Log.d(TAG, "To getRtCGIResponse for the cmd:" + command);
        String rspString = null;
        try {
            // prepare http get request
            HttpGet httpGet = new HttpGet(command);
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity rspEntity = httpResponse.getEntity();
            String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
            if (inputEncoding == null)
                inputEncoding = HTTP.UTF_8;
            rspString = EntityUtils.toString(rspEntity, inputEncoding);
            Log.d(TAG, "rspString  = " + rspString);
            return mapper.readValue(rspString, t);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            Log.d(TAG, "Response=" + rspString);
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Response=" + rspString);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * <nas>
     * <modulus>...</modulus>
     * <hash>...</hash>
     * <public>...</public>
     * </nas>
     *
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     * @throws XmlPullParserException
     */
    private boolean doGenHash() {
        boolean isSuccess = false;

        DefaultHttpClient httpClient = HttpClientManager.getClient();
        // Generate hash
        String commandURL = "http://" + hostname + NAS_GEN_HASH_PATH;
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do {
                HttpGet httpGet = new HttpGet(commandURL);
                response = httpClient.execute(httpGet);
                if (response == null) {
                    Log.e(TAG, "response is null");
                    break;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.e(TAG, "response entity is null");
                    break;
                }
                inputStream = entity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    //String uri = xpp.getNamespace();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("modulus")) {
                                this.nasModules = text;
                            } else if (curTagName.equals("hash")) {
                                this.hash = text;
                            } else if (curTagName.equals("public")) {
                                this.nasPublic = text;
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);

                if (nasModules != null && hash != null && nasPublic != null) {
                    isSuccess = true;
                } else {
                    Log.w(TAG, "After parsing XML, nasModules=" + nasModules + " hash=" + hash + " nasPublic=" + nasPublic);
                }
            } while (false);

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XML Parser error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                isSuccess = false;
                e.printStackTrace();
            }
        }

        return isSuccess;
    }


    /**
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     * @throws ClientProtocolException
     * @throws XmlPullParserException
     */
    private boolean doLogin() {
        boolean isSuccess = false;
        String encryptedPassword = null;
        InputStream inputStream = null;
        LoginInfo loginInfo = new LoginInfo();
        DefaultHttpClient httpClient = HttpClientManager.getClient();

        do {
            // Prepare login information
            encryptedPassword = Server.encryptPassword(password, nasModules, nasPublic);
            if (encryptedPassword == null)
                break;

            // Send login request to server
            String commandURL = "http://" + hostname + NAS_LOGIN_PATH;
            Log.d(TAG, "send Command: " + NAS_LOGIN_PATH);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("user", username));
            nameValuePairs.add(new BasicNameValuePair("pass", encryptedPassword));
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            nameValuePairs.add(new BasicNameValuePair("lang", "zh-tw"));

            try {
                HttpPost httpPost = new HttpPost(commandURL);
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                if (httpResponse == null) {
                    Log.e(TAG, "httpResponse is null");
                    break;
                }

                StatusLine statusLine = httpResponse.getStatusLine();
                int status_code = statusLine.getStatusCode();
                Log.d(TAG, "status_code = " + status_code);

                if (status_code == 200) {
                    HttpEntity httpEntity = httpResponse.getEntity();
                    if (httpEntity == null) {
                        Log.e(TAG, "httpResponse's entity is null");
                        break;
                    }

                    inputStream = httpEntity.getContent();
                    String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
                    if (inputEncoding == null) {
                        inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                    }
                    loginInfo.parse(inputStream, inputEncoding);

                    if (loginInfo.isError) {
                        Log.d(TAG, "Login fail, username= " + username + ", hash= " + hash + ", encryptedPassword= " + encryptedPassword);
                        Log.d(TAG, "reason = " + loginInfo.reason);
                        loginError = loginInfo.reason;
                    } else {
                        isSuccess = true;
                    }
                } else {
                    Log.e(TAG, "httpResponse's statusLine = " + statusLine.toString());
                    break;
                }
            } catch (Exception e) {
                // UnsupportedEncodingException, XmlPullParserException, IOException
                e.printStackTrace();
                Log.e(TAG, "Fail to post login info to server");
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        isSuccess = false;
                        e.printStackTrace();
                    }
                }
            }
        } while (false);

        return isSuccess;
    }

    /**
     * Fail:
     * <?xml version="1.0" encoding="utf-8"?><nas><error><reason>Not Login</reason></error></nas>
     * Success:
     * <?xml version="1.0" encoding="utf-8"?><nas><logout>...</logout></nas>
     *
     * @return
     */
    private boolean doLogout() {
        boolean isSuccess = false;

        try {
            String commandURL = "http://" + hostname + NAS_LOGOUT_PATH;
            Log.i(TAG, "Post to URL:" + commandURL);

            HttpPost httpPost = new HttpPost(commandURL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("user", username));
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            String result = EntityUtils.toString(httpEntity);
            if (result.contains("error")) {
                isSuccess = false;
                Log.w(TAG, "Logout fail, response=[" + result + "]");
            } else {
                isSuccess = true;
            }
        } catch (Exception e) {
            // UnsupportedEncodingException, ClientProtocolException, printStackTrace
            e.printStackTrace();
            Log.e(TAG, "Fail to send logout command to server");
        }

        return isSuccess;
    }

    public boolean doGetServerInfo(String hostname) {
        boolean isSuccess = false;
        InputStream inputStream = null;
        String commandURL = "http://" + hostname + NAS_GET_INFO;
        DefaultHttpClient httpClient = HttpClientManager.getClient();

        do {
            try {
                Log.d(TAG, "Get " + commandURL);
                HttpGet httpGet = new HttpGet(commandURL);
                HttpResponse httpResponse;
                httpResponse = httpClient.execute(httpGet);
                if (httpResponse == null) {
                    Log.e(TAG, "httpResponse is null");
                    break;
                }
                HttpEntity httpEntity = httpResponse.getEntity();
                inputStream = httpEntity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                // Get server information from response
                info = new ServerInfo();
                info.parse(inputStream, inputEncoding);
                Log.d(TAG, info.toString());
                // check info
                if (info.hardware != null) {
                    isSuccess = true;
                } else {
                    break;
                }
                // use ServerInfo to set firmware
                // but for StoreJet Cloud, we don't need to check firmwareType
                // isSuccess = setFirmwareType();
                // for StoreJet Cloud, we don't need get server profile
                //getServerProfile();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        isSuccess = false;
                        e.printStackTrace();
                    }
                }
            }
        } while (false);
        return isSuccess;
    }

    /**
     * Encrypt password
     *
     * @param plainText (password)
     * @param module
     * @param pub
     * @return
     */
    static String encryptPassword(String plainText, String module, String pub) {
        String result = null;

        // Encrypt password
        try {
            BigInteger modulus = new BigInteger(1, HttpHelper.hexStringToByteArray(module));
            Integer exponent = Integer.parseInt(pub, 16);
            BigInteger publicExponent = new BigInteger(exponent.toString());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, publicExponent);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedText = cipher.doFinal(plainText.getBytes());
            result = HttpHelper.byteArrayToHexString(encryptedText);
        } catch (Exception e) {
            // including InvalidKeySpecException, NoSuchAlgorithmException,
            // NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException
            // BadPaddingException
            e.printStackTrace();
            Log.e(TAG, "Fail to generate encryped password");
        }

        return result;
    }

    public boolean isUserAdmin() {
        if (username.equals("admin"))
            return true;
        else
            return false;
    }

    public String getDavHome() {
        if (isUserAdmin())
            return DEVICE_DAV_HOME;
        else
            return USER_DAV_HOME;
    }

    /**
     * To get mtime of function config
     */
    public String getConfigMTime() {
        return tmp_version;
    }

}
