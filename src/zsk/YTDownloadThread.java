/**
 *  This file is part of ytd2
 *
 *  ytd2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ytd2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with ytd2.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package zsk;

import java.io.BufferedReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;
import org.apache.commons.lang.StringUtils;
// necessary external libraries
// http://hc.apache.org/downloads.cgi -> httpcomponents-core-4.2.2-bin, httpcomponents-client-4.2.2-bin
// plus corresponding sources as Source Attachment within the Eclipse Project Properties
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
//import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.cookie.CookieOrigin;
//import org.apache.http.cookie.CookieSpec;
//import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
//import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * http://www.youtube.com/watch?v=9QFK1cLhytY					Javatar and .NOT
 * http://www.youtube.com/watch?v=Mt7zsortIXs				 	1080p "Lady Java"
 * http://www.youtube.com/watch?v=WowZLe95WDY					Tom Petty And the Heartbreakers - Learning to Fly (with lyrics)
 * http://www.youtube.com/watch?v=86OfBExGSE0					URZ 720p
 * http://www.youtube.com/watch?v=cNOP2t9FObw 					Blade 360 - 480
 * http://www.youtube.com/watch?v=HvQBrM_i8bU					MZ 1000 Street Fighter
 * http://www.youtube.com/watch?v=yVpbFMhOAwE					How Linx is build
 * http://www.youtube.com/watch?v=4XpnKHJAok8					Tech Talk: Linus Torvalds on git 
 *
 * ODOT http://sourceforge.net/p/ytd2/bugs/7/ http://www.youtube.com/watch?v=fRVVzXnRsUQ   uses RTMPE (http://en.wikipedia.org/wiki/Protected_Streaming), which ytd2 cannot download atm 
 *
 */
public class YTDownloadThread extends Thread {
	
	boolean bNODOWNLOAD;

	static int iThreadcount = 0;
	
	public static int getiThreadcount() {
		return iThreadcount;
	}

	int iThreadNo = YTDownloadThread.iThreadcount++; // every download thread get its own number
	private static YTDEventDispatcher eventDispatcher = new YTDEventDispatcher(); // notify changes
	public static YTDEventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}
	
	final String ssourcecodeurl = "http://";
	final String ssourcecodeuri = "[a-zA-Z0-9%&=\\.]";

	String sURL = null;				// main URL (youtube start web page)
	String sTitle = null;			// will be used as filename
	
	String sVideoURL = null;		// one video web resource
	Vector<YTURL> vNextVideoURL = new Vector<YTURL>();	// list of URLs from webpage source
	String sFileName = null;		// contains the absolute filename
	//CookieStore bcs = null;			// contains cookies after first HTTP GET
	boolean bisinterrupted = false; // basically the same as Thread.isInterrupted()
	int iRecursionCount = -1;		// counted in downloadone() for the 3 webrequest to one video

	String 				sContentType = null;
	BufferedReader		textreader = null;
	BufferedInputStream binaryreader = null;
	HttpGet				httpget = null;
	HttpClient			httpclient = null;
	HttpHost			proxy = null;
	HttpHost			target = null;
	HttpContext			localContext = null;
    HttpResponse		response = null;
    
	public YTDownloadThread() {
		super();
		String sv = "thread started: ".concat(this.getMyName()); 
		debugoutput(sv);
	} // YTDownloadThread()
	
	boolean downloadone(String sURL) {
		boolean rc = false;
		boolean rc204 = false;
		boolean rc302 = false;
	
		this.iRecursionCount++;
		
		// stop recursion
		try {
			if (sURL.equals("")) return(false);
		} catch (NullPointerException npe) {
			return(false);
		}
		if (JFCMainClient.getbQuitrequested()) return(false); // try to get information about application shutdown
		
		debugoutput("start.");
		
		// TODO GUI option for proxy?
		// http://wiki.squid-cache.org/ConfigExamples/DynamicContent/YouTube
		// using local squid to save download time for tests

		try {
			// determine http_proxy environment variable
			if (!this.getProxy().equals("")) {

				String sproxy = JFCMainClient.sproxy.toLowerCase().replaceFirst("http://", "") ;
				this.proxy = new HttpHost( sproxy.replaceFirst(":(.*)", ""), Integer.parseInt( sproxy.replaceFirst("(.*):", "")), "http");

				SchemeRegistry supportedSchemes = new SchemeRegistry();
				supportedSchemes.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
				supportedSchemes.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, "UTF-8");
				HttpProtocolParams.setUseExpectContinue(params, true);

				ClientConnectionManager ccm = new PoolingClientConnectionManager(supportedSchemes);

				// with proxy
				this.httpclient = new DefaultHttpClient(ccm, params);
				this.httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, this.proxy);
				this.httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			} else {
				// without proxy
				this.httpclient = new DefaultHttpClient();
				this.httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			}
			this.httpget = new HttpGet( getURI(sURL) );	
			if (sURL.toLowerCase().startsWith("https"))
				this.target = new HttpHost( getHost(sURL), 443, "https" );
			else
				this.target = new HttpHost( getHost(sURL), 80, "http" );
		} catch (Exception e) {
			debugoutput(e.getMessage());
		}
		
        debugoutput("executing request: ".concat( this.httpget.getRequestLine().toString()) );
        debugoutput("uri: ".concat( this.httpget.getURI().toString()) );
        debugoutput("host: ".concat( this.target.getHostName() ));
        debugoutput("using proxy: ".concat( this.getProxy() ));
        
        // we dont need cookies at all because the download runs even without it (like my wget does) - in fact it blocks downloading videos from different webpages, because we do not handle the bcs for every URL (downloading of one video with different resolutions does work)
        /*
        this.localContext = new BasicHttpContext();
        if (this.bcs == null) this.bcs = new BasicCookieStore(); // make cookies persistent, otherwise they would be stored in a HttpContext but get lost after calling org.apache.http.impl.client.AbstractHttpClient.execute(HttpHost target, HttpRequest request, HttpContext context)
		((DefaultHttpClient) httpclient).setCookieStore(this.bcs); // cast to AbstractHttpclient would be best match because DefaultHttpClass is a subclass of AbstractHttpClient
		*/
        
        // TODO maybe we save the video IDs+res that were downloaded to avoid downloading the same video again?
       
		try {
			this.response = this.httpclient.execute(this.target,this.httpget,this.localContext);
		} catch (ClientProtocolException cpe) {
			debugoutput(cpe.getMessage());
		} catch (UnknownHostException uhe) {
			output((JFCMainClient.isgerman()?"Fehler bei der Verbindung zu: ":"error connecting to: ").concat(uhe.getMessage()));
			debugoutput(uhe.getMessage());
		} catch (IOException ioe) {
			debugoutput(ioe.getMessage());
		} catch (IllegalStateException ise) {
			debugoutput(ise.getMessage());
		}
		
		/*
		CookieOrigin cookieOrigin = (CookieOrigin) localContext.getAttribute( ClientContext.COOKIE_ORIGIN);
		CookieSpec cookieSpec = (CookieSpec) localContext.getAttribute( ClientContext.COOKIE_SPEC);
		CookieStore cookieStore = (CookieStore) localContext.getAttribute( ClientContext.COOKIE_STORE) ;
		try { debugoutput("HTTP Cookie store: ".concat( cookieStore.getCookies().toString( )));
		} catch (NullPointerException npe) {} // useless if we don't set our own CookieStore before calling httpclient.execute
		try {
			debugoutput("HTTP Cookie origin: ".concat(cookieOrigin.toString()));
			debugoutput("HTTP Cookie spec used: ".concat(cookieSpec.toString()));
			debugoutput("HTTP Cookie store (persistent): ".concat(this.bcs.getCookies().toString()));
		} catch (NullPointerException npe) {
		}
		*/

		try {
			debugoutput("HTTP response status line:".concat( this.response.getStatusLine().toString()) );
			//for (int i = 0; i < response.getAllHeaders().length; i++) {
			//	debugoutput(response.getAllHeaders()[i].getName().concat("=").concat(response.getAllHeaders()[i].getValue()));
			//}

			// abort if HTTP response code is != 200, != 302 and !=204 - wrong URL?
			if (!(rc = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)200(.*)")) & 
					!(rc204 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)204(.*)")) &
					!(rc302 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)302(.*)"))) {
				debugoutput(this.response.getStatusLine().toString().concat(" ").concat(sURL));
				output(this.response.getStatusLine().toString().concat(" \"").concat(this.sTitle).concat("\""));
				return(rc & rc204 & rc302);
			}
			if (rc204) {
				debugoutput("last response code==204 - download: ".concat(this.vNextVideoURL.get(0).getsYTID()));
				rc = downloadone(this.vNextVideoURL.get(0).getsURL());
				return(rc);
			}
			if (rc302) 
				debugoutput("location from HTTP Header: ".concat(this.response.getFirstHeader("Location").toString()));

		} catch (NullPointerException npe) {
			// if an IllegalStateException was catched while calling httpclient.execute(httpget) a NPE is caught here because
			// response.getStatusLine() == null
			this.sVideoURL = null;
		}
		
		HttpEntity entity = null;
        try {
            entity = this.response.getEntity();
        } catch (NullPointerException npe) {
        }
        
        // try to read HTTP response body
        if (entity != null) {
			try {
				if (this.response.getFirstHeader("Content-Type").getValue().toLowerCase().matches("^text/html(.*)"))
					this.textreader = new BufferedReader(new InputStreamReader(entity.getContent()));
				else
					this.binaryreader = new BufferedInputStream( entity.getContent());
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            try {
            	// test if we got a webpage
            	this.sContentType = this.response.getFirstHeader("Content-Type").getValue().toLowerCase();
            	if (this.sContentType.matches("^text/html(.*)")) {
            		rc = savetextdata();
            	// test if we got the binary content
            	} else if (this.sContentType.matches("video/(.)*")) {
            		if (JFCMainClient.getbNODOWNLOAD())
            			reportheaderinfo();
            		else
            			savebinarydata();
            	} else { // content-type is not video/
            		rc = false;
            		this.sVideoURL = null;
            	}
            } catch (IOException ex) {
                try {
					throw ex;
				} catch (IOException e) {
					e.printStackTrace();
				}
            } catch (RuntimeException ex) {
                try {
					throw ex;
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
        } //if (entity != null)
        
       	this.httpclient.getConnectionManager().shutdown();

        debugoutput("done: ".concat(sURL));
        if (this.sVideoURL==null)
        	this.sVideoURL=""; // to prevent NPE
       	
        if (!this.sVideoURL.matches(JFCMainClient.szURLREGEX)) {
        	// no more recursion - html source hase been read
        	// test !rc than video could not downloaded because of some error (like wrong protocol or restriction)
        	if (!rc) {
        		debugoutput("cannot download video - URL does not seem to be valid or could not be found: ".concat(this.sURL));
        		output(JFCMainClient.isgerman()?"es gab ein Problem die Video URL zu finden! evt. wegen Landesinschränkung?!":"there was a problem getting the video URL! perhaps not allowed in your country?!");
        		output((JFCMainClient.isgerman()?"erwäge die URL dem Autor mitzuteilen!":"consider reporting the URL to author! - ").concat(this.sURL));
        		this.sVideoURL = null;
        	}
        } else {
        	// enter recursion - download video resource
        	debugoutput("try to download video from URL: ".concat(this.sVideoURL));
        	rc = downloadone(this.sVideoURL);
        }
        this.sVideoURL = null;


    	return(rc);
		
	} // downloadone()

	void reportheaderinfo() {
		if (JFCMainClient.getbDEBUG()) {
			debugoutput("");
			debugoutput("NO-DOWNLOAD mode active (ndl on)");
			debugoutput("all HTTP header fields:");
			for (int i = 0; i < this.response.getAllHeaders().length; i++) {
				debugoutput(this.response.getAllHeaders()[i].getName().concat("=").concat(this.response.getAllHeaders()[i].getValue()));
			}
			debugoutput("filename would be: ".concat(this.getTitle()).concat(".").concat(this.response.getFirstHeader("Content-Type").getValue().replaceFirst("video/", "").replaceAll("x-", ""))); // title contains just filename, no path
		} else {
			Long iFileSize = Long.parseLong(this.response.getFirstHeader("Content-Length").getValue());
			output("");
			output("NO-DOWNLOAD active (ndl on)");
			output("some HTTP header fields:");
			output("content-type: ".concat( this.response.getFirstHeader("Content-Type").getValue()) );
			output("content-length: ".concat(iFileSize.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iFileSize/1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iFileSize/1024/1024)).concat(" MiB")) );
			if (JFCMainClient.getbNODOWNLOAD())
				output((JFCMainClient.isgerman()?"Dateiname würde sein: ":"filename would be: ").concat(this.getTitle().concat(".").concat(this.response.getFirstHeader("Content-Type").getValue().replaceFirst("video/", "").replaceAll("x-", "")))); // title contains just filename, no path
		}
		this.sVideoURL = null;
	} // reportheaderinfo()

	private int addMPEG_HD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;
		// try 3D HD first if 3D is selected 
		if (JFCMainClient.get3Dbuttonstate())
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("84"),this.sURL,"3D")); // mpeg 3D full HD
		
		// if SDS is on reverse order! - 720p before 1080p for HD and so on
		if (!JFCMainClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("37"),this.sURL)); // mpeg full HD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("22"),this.sURL)); // mpeg half HD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("22"),this.sURL)); // mpeg half HD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("37"),this.sURL)); // mpeg full HD
		}
		return inewiindex;
	}
	
	private int addWBEM_HD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;
		// try 3D HD first if 3D is selected 
		if (JFCMainClient.get3Dbuttonstate())
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("100"),this.sURL,"3D")); // webm 3D HD

		if (!JFCMainClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("46"),this.sURL)); // webm full HD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("45"),this.sURL)); // webm half HD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("45"),this.sURL)); // webm half HD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("46"),this.sURL)); // webm full HD
		}
		return inewiindex;
	}
	
	private int addWBEM_SD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;
		// try 3D  first if 3D is selected 
		if (JFCMainClient.get3Dbuttonstate())
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("102"),this.sURL,"3D")); // webm 3D SD

		if (!JFCMainClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("44"),this.sURL)); // webm SD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("43"),this.sURL)); // webm SD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("43"),this.sURL)); // webm SD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("44"),this.sURL)); // webm SD
		}
		return inewiindex;
	}
	
	private int addFLV_SD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;

		if (!JFCMainClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("35"),this.sURL)); // flv SD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("34"),this.sURL)); // flv SD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("34"),this.sURL)); // flv SD
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("35"),this.sURL)); // flv SD
		}
		return inewiindex;
	}
	
	private int addMPEG_SD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;

		// try 3D  first if 3D is selected 
		if (JFCMainClient.get3Dbuttonstate())
			this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("82"),this.sURL,"3D")); // mpeg 3D SD

		this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("18"),this.sURL)); // mpeg SD
		return inewiindex;
	}
	
	private int addMPEG_LD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;
		this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("36"),this.sURL)); // mpeg LD
		this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get("17"),this.sURL)); // mpeg LD
		return inewiindex;
	}
	private int addFLV_LD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls) {
		int inewiindex = iindex;

		this.vNextVideoURL.add(inewiindex++, new YTURL(ssourcecodevideourls.get( "5"),this.sURL)); // flv LD
		return inewiindex;
	}

	
	boolean savetextdata() throws IOException {
		boolean rc = false;
		// read html lines one by one and search for java script array of video URLs
		String sline = "";
		while (sline != null) {
			sline = this.textreader.readLine();
			try {
				if (this.iRecursionCount==0 && sline.matches("(.*)\"url_encoded_fmt_stream_map\":(.*)")) {
					rc = true;
    				HashMap<String, String> ssourcecodevideourls = new HashMap<String, String>();

					sline = sline.replaceFirst(".*\"url_encoded_fmt_stream_map\": \"", "");
					sline = sline.replaceFirst("\".*", "");
					sline = sline.replace("%25","%");
					sline = sline.replace("\\u0026", "&");
					sline = sline.replace("\\", "");
					
					// by anonymous
					String[] ssourcecodeyturls = sline.split(",");
					debugoutput("ssourcecodeuturls.length: ".concat(Integer.toString(ssourcecodeyturls.length)));
					String sResolutions = JFCMainClient.isgerman()?"gefundene Video URL für Auflösung: ":"found video URL for resolution: ";
					
					for (String urlString : ssourcecodeyturls) {

						// assuming rtmpe is used for all resolutions, if found once - end download
						if (urlString.matches(".*conn=rtmpe.*")) {
							debugoutput("RTMPE found. cannot download this one!");
							output(JFCMainClient.isgerman()?"Herunterladen des Videos wegen nicht unterstütztem Protokoll (RTMPE) leider nicht möglich!":"Unable to download video due to unsupported protocol (RTMPE). sry!");
							break;
						}
						String[] fmtUrlPair = urlString.split("url=http",2);
						fmtUrlPair[1] = "url=http"+fmtUrlPair[1]+"&"+fmtUrlPair[0];
						// grep itag=xz out and use xy as hash key
						// 2013-02 itag now has up to 3 digits
						fmtUrlPair[0] = fmtUrlPair[1].substring(fmtUrlPair[1].indexOf("itag=")+5, fmtUrlPair[1].indexOf("itag=")+5+1+(fmtUrlPair[1].matches(".*itag=[0-9]{2}.*")?1:0)+(fmtUrlPair[1].matches(".*itag=[0-9]{3}.*")?1:0));
						fmtUrlPair[1] = fmtUrlPair[1].replaceFirst("url=http%3A%2F%2F", "http://");
						fmtUrlPair[1] = fmtUrlPair[1].replaceAll("%3F","?").replaceAll("%2F", "/").replaceAll("%3B",";").replaceAll("%2C",",").replaceAll("%3D","=").replaceAll("%26", "&").replaceAll("%252C", "%2C").replaceAll("sig=", "signature=").replaceAll("&s=", "&signature=").replaceAll("\\?s=", "?signature=");
						
						// remove duplicated &itag=xy
						if (StringUtils.countMatches(fmtUrlPair[1], "itag=")==2)
							fmtUrlPair[1] = fmtUrlPair[1].replaceFirst("itag=[0-9]{1,3}", "");
						
						try {
							ssourcecodevideourls.put(fmtUrlPair[0], fmtUrlPair[1]); // save that URL
							//debugoutput(String.format( "video url saved with key %s: %s",fmtUrlPair[0],ssourcecodevideourls.get(fmtUrlPair[0]) ));
							sResolutions = sResolutions.concat(
									fmtUrlPair[0].equals( "37")?"1080p mpeg, ":		// HD		type=video/mp4;+codecs="avc1.64001F,+mp4a.40.2"
									fmtUrlPair[0].equals( "22")?"720p mpeg, ":		// HD		type=video/mp4;+codecs="avc1.64001F,+mp4a.40.2"
									fmtUrlPair[0].equals( "84")?"1080p 3d mpeg, ":	// HD 3D	type=video/mp4;+codecs="avc1.64001F,+mp4a.40.2"
									fmtUrlPair[0].equals( "35")?"480p flv, ":		// SD		type=video/x-flv
									fmtUrlPair[0].equals( "18")?"360p mpeg, ":		// SD		type=video/mp4;+codecs="avc1.42001E,+mp4a.40.2"
									fmtUrlPair[0].equals( "34")?"360p flv, ":		// SD		type=video/x-flv
									fmtUrlPair[0].equals( "82")?"360p 3d mpeg, ":	// SD 3D	type=video/mp4;+codecs="avc1.42001E,+mp4a.40.2"
									fmtUrlPair[0].equals( "36")?"240p mpeg 3gpp, ":	// LD		type=video/3gpp;+codecs="mp4v.20.3,+mp4a.40.2"
									fmtUrlPair[0].equals( "17")?"114p mpeg 3gpp, ":	// LD		type=video/3gpp;+codecs="mp4v.20.3,+mp4a.40.2"
										
									fmtUrlPair[0].equals( "46")?"1080p webm, ":		// HD 		type=video/webm;+codecs="vp8.0,+vorbis"&
									fmtUrlPair[0].equals( "45")?"720p webm, ":		// HD		type=video/webm;+codecs="vp8.0,+vorbis"
									fmtUrlPair[0].equals("100")?"1080p 3d webm, ":	// HD 3D	type=video/webm;+codecs="vp8.0,+vorbis"&
									fmtUrlPair[0].equals( "44")?"480p webm, ":		// SD		type=video/webm;+codecs="vp8.0,+vorbis"
									fmtUrlPair[0].equals( "43")?"360p webm, ":		// SD		type=video/webm;+codecs="vp8.0,+vorbis"
									fmtUrlPair[0].equals("102")?"360p 3d webm, ":	// SD 3D	type=video/webm;+codecs="vp8.0,+vorbis"&						
									fmtUrlPair[0].equals(  "5")?"240p flv, ":		// LD		type=video/x-flv
									"unknown resolution! (".concat(fmtUrlPair[0]).concat(")"));
						} catch (java.lang.ArrayIndexOutOfBoundsException aioobe) {
						}
					} // for
					
					if (JFCMainClient.frame!=null)
						output(sResolutions);
					debugoutput(sResolutions);
					
					int iindex;	iindex = 0;
					this.vNextVideoURL.removeAllElements();
					
					debugoutput("ssourcecodevideourls.length: ".concat(Integer.toString(ssourcecodevideourls.size())));
					// figure out what resolution-button is pressed now and fill list with possible URLs
					switch (JFCMainClient.getIdlbuttonstate()) {
					case 4: // HD
						// try 1080p/720p in selected format first. if it's not available than the other format will be used 
						if (JFCMainClient.getBmpgbuttonstate())
							iindex=addMPEG_HD_Urls(iindex,ssourcecodevideourls);
												
						if (JFCMainClient.getBwebmbuttonstate())
							iindex=addWBEM_HD_Urls(iindex,ssourcecodevideourls);

						// there are no FLV HD URLs for now, so at least try mpg,wbem HD then
						iindex = addMPEG_HD_Urls(iindex, ssourcecodevideourls);
						iindex = addWBEM_HD_Urls(iindex, ssourcecodevideourls);
						
						//$FALL-THROUGH$
					case 2: // SD
						// try to download desired format first, if it's not available we take the other of same res 
						if (JFCMainClient.getBmpgbuttonstate())
							iindex=addMPEG_SD_Urls(iindex, ssourcecodevideourls);
						
						if (JFCMainClient.getBwebmbuttonstate())
							iindex=addWBEM_SD_Urls(iindex,ssourcecodevideourls);
						
						if (JFCMainClient.getBflvbuttonstate())
							iindex=addFLV_SD_Urls(iindex, ssourcecodevideourls);

						iindex = addMPEG_SD_Urls(iindex, ssourcecodevideourls);
						iindex = addWBEM_SD_Urls(iindex, ssourcecodevideourls);
						iindex = addFLV_SD_Urls(iindex, ssourcecodevideourls);

						//$FALL-THROUGH$
					case 1:	// LD
				
						// TODO this.sFilenameResPart = "(LD)"; // adding LD to filename because HD-Videos are almost already named HD (?)
						if (JFCMainClient.getBmpgbuttonstate())
							iindex = addMPEG_LD_Urls(iindex, ssourcecodevideourls);
						
						if (JFCMainClient.getBwebmbuttonstate()) {
							// there are no wbem LD URLs for now
						}
						
						if (JFCMainClient.getBflvbuttonstate())
							iindex = addFLV_LD_Urls(iindex, ssourcecodevideourls);
						
						// we must ensure all (16) possible URLs get added to the list so that the list of URLs is never empty
						iindex = addMPEG_LD_Urls(iindex, ssourcecodevideourls);
						iindex = addFLV_LD_Urls(iindex, ssourcecodevideourls);

						break;
					default:
						//this.vNextVideoURL = null;
						this.sVideoURL = null;
						break;
					}
					
					// if the first 2 entries are null than there are no URLs for the selected resolution
					// strictly speaking this is only true for HD as there are only two URLs in contrast to three of SD - in this case the output will not be shown but downloading should work anyway
					if (this.vNextVideoURL.get(0).getsURL()==null && this.vNextVideoURL.get(1).getsURL()==null) {
						String smsg = JFCMainClient.isgerman()?"Video URL für ausgewählte Auflösung nicht gefunden! versuche geringere Auflösung...":"could not find video url for selected resolution! trying lower res...";
						output(smsg); debugoutput(smsg);
					}
					
					// remove null entries in list - we later try to download the first (index 0) and if it fails the next one (at index 1) and so on
					for (int x=this.vNextVideoURL.size()-1;x>=0;x--) {
						if (this.vNextVideoURL.get(x).getsURL()==null) this.vNextVideoURL.remove(x);
					}
					
					try {
						this.sVideoURL = this.vNextVideoURL.get(0).getsURL();
						debugoutput(String.format("trying this one: %s %s %s",this.vNextVideoURL.get(0).getsITAG(),this.vNextVideoURL.get(0).getsQUALITY(),this.vNextVideoURL.get(0).getsTYPE()));
					} catch (ArrayIndexOutOfBoundsException aioobe) {
					}
					
					this.setTitle( this.getTitle()
							.concat(JFCMainClient.isbSaveIDinFilename()?"."+this.vNextVideoURL.get(0).getsYTID():"")
							.concat(!this.vNextVideoURL.get(0).getsRESPART().equals("")?"."+this.vNextVideoURL.get(0).getsRESPART():""));

				}
				
				if (this.iRecursionCount==0 && sline.matches("(.*)<meta name=\"title\" content=(.*)")) {
					String stmp = sline.replaceFirst("(.*)<meta name=\"title\" content=", "").trim();
					// change html characters to their UTF8 counterpart
					stmp = UTF8.changeHTMLtoUTF8(stmp);
					stmp = stmp.replaceFirst("^\"", "").replaceFirst("\">$", "");
					this.setTitle( stmp ); // complete file name without path 
				}

			} catch (NullPointerException npe) {
			}
		} // while
		return rc;
	} // savetextdata()

	void savebinarydata() throws IOException {
		FileOutputStream fos = null;
		try {
			File f; Integer idupcount = 0;
			String sdirectorychoosed;
//			try {
//				synchronized (JFCMainClient.frame.directorytextfield) {
//					sdirectorychoosed = JFCMainClient.frame.directorytextfield.getText();
//				}
//			} catch (NullPointerException npe) {
//				// for CLI-only run
//				sdirectorychoosed = JFCMainClient.shomedir;
//			}
			sdirectorychoosed=(String) JFCMainClient.config.getProperty("savefolder");

			String sfilename = this.getTitle();
    		debugoutput("title: ".concat(this.getTitle()).concat("sfilename: ").concat(sfilename));
			do {
				f = new File(sdirectorychoosed, sfilename.concat((idupcount>0?"(".concat(idupcount.toString()).concat(")"):"")).concat(".").concat(this.sContentType.replaceFirst("video/", "").replaceAll("x-", "")));
				idupcount += 1;
			} while (f.exists());
			this.setFileName(f.getAbsolutePath());
			
			Long iBytesReadSum = (long) 0;
			Long iPercentage = (long) -1;
			Long iBytesMax = Long.parseLong(this.response.getFirstHeader("Content-Length").getValue());
			fos = new FileOutputStream(f);
			
			debugoutput(String.format("writing %d bytes to: %s",iBytesMax,this.getFileName()));
			output((JFCMainClient.isgerman()?"Dateigröße von \"":"file size of \"").concat(this.getTitle()).concat("\" = ").concat(iBytesMax.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iBytesMax/1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iBytesMax/1024/1024)).concat(" MiB")));
		    
			byte[] bytes = new byte[4096];
			Integer iBytesRead = 1;
			String sOldURL = JFCMainClient.szDLSTATE.concat(this.sURL);
			String sNewURL = "";
			
			// adjust blocks of percentage to output - larger files are shown with smaller pieces
			Integer iblocks = 10; if (iBytesMax>20*1024*1024) iblocks=4; if (iBytesMax>32*1024*1024) iblocks=2; if (iBytesMax>56*1024*1024) iblocks=1;
			while (!this.bisinterrupted && iBytesRead>0) {
				iBytesRead = this.binaryreader.read(bytes);
				iBytesReadSum += iBytesRead;
				// drop a line every x% of the download 
				if ( (((iBytesReadSum*100/iBytesMax) / iblocks) * iblocks) > iPercentage ) {
					iPercentage = (((iBytesReadSum*100/iBytesMax) / iblocks) * iblocks);
					sNewURL = JFCMainClient.szDLSTATE.concat("(").concat(Long.toString(iPercentage).concat(" %) ").concat(this.sURL));
					JFCMainClient.exchangeYTURLInList(sOldURL, sNewURL);
					sOldURL = sNewURL ; 
				}
				// TODO calculate and show ETA for bigger downloads (remaining time > 60s) - every 20%?
				
				try {fos.write(bytes,0,iBytesRead);} catch (IndexOutOfBoundsException ioob) {}
				this.bisinterrupted = JFCMainClient.getbQuitrequested(); // try to get information about application shutdown
			} // while
			
			JFCMainClient.exchangeYTURLInList(sNewURL, JFCMainClient.szDLSTATE.concat(this.sURL));
			
			// rename files if download was interrupted before completion of download
			if (this.bisinterrupted && iBytesReadSum<iBytesMax) {
				try {
					// this part is especially for our M$-Windows users because of the different behavior of File.renameTo() in contrast to non-windows
					// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298  and others
					// even with Java 1.6.0_22 the renameTo() does not work directly on M$-Windows! 
					fos.close();
				} catch (Exception e) {
				}
//				System.gc(); // we don't have to do this but to be sure the file handle gets released we do a thread sleep 
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
		         
				// this part runs on *ix platforms without closing the FileOutputStream explicitly
				this.httpclient.getConnectionManager().shutdown(); // otherwise binaryreader.close() would cause the entire datastream to be transmitted 
				debugoutput(String.format("download canceled. (%d)",(iBytesRead)));
				changeFileNamewith("CANCELED.");
				String smsg = "renaming unfinished file to: ".concat( this.getFileName() );
				output(smsg); debugoutput(smsg);
				
				// CANCELED filenames overwrite others as we do not test for CANCELED one, two...
				if (!f.renameTo(new File( this.getFileName()))) {
					smsg = "error renaming unfinished file to: ".concat( this.getFileName() );
    				output(smsg); debugoutput(smsg);
				}
			}
			debugoutput("done writing.");
		} catch (FileNotFoundException fnfe) {
			throw(fnfe)		;
		} catch (IOException ioe) {
			debugoutput("IOException");
			throw(ioe);
		} finally {
			this.sVideoURL = null;
			try {
				fos.close();
			} catch (Exception e) {
			}
            try {
				this.textreader.close();
			} catch (Exception e) {
			}
            try {
				this.binaryreader.close();
			} catch (Exception e) {
			}
		} // try
	} // savebinarydata()

	void changeFileNamewith(String string) {
		File f = null;
		Integer idupcount = 0;
		String sfilesep = System.getProperty("file.separator");
		if (sfilesep.equals("\\")) sfilesep += sfilesep; // on m$-windows we need to escape the \

		String sdirectorychoosed="";
		String[] srenfilename = this.getFileName().split(sfilesep);
		
		try {
			for (int i = 0; i < srenfilename.length-1; i++) {
				sdirectorychoosed += srenfilename[i].concat((i<srenfilename.length-1)?sfilesep:""); // constructing folder where file is saved now (could be changed in GUI already)
			}
		} catch (ArrayIndexOutOfBoundsException aioobe) {}
		
		String sfilename = srenfilename[srenfilename.length-1];
		debugoutput("changeFileNamewith() sfilename: ".concat(sfilename));
		do {
			 // filename will be prepended with a parameter string and possibly a duplicate counter
			f = new File(sdirectorychoosed, string.concat((idupcount>0?"(".concat(idupcount.toString()).concat(")"):"")).concat(sfilename));
			idupcount += 1;
		} while (f.exists());
		
		debugoutput("changeFileNamewith() new filename: ".concat(f.getAbsolutePath()));
		this.setFileName(f.getAbsolutePath());
		
	} // changeFileNamewith

	String getProxy() {
		String sproxy = JFCMainClient.sproxy;
		if (sproxy==null) return(""); else return(sproxy);
	} // getProxy() 

	String getURI(String sURL) {
		String suri = "/".concat(sURL.replaceFirst(JFCMainClient.szYTHOSTREGEX, ""));
		return(suri);
	} // getURI

	String getHost(String sURL) {
		String shost = sURL.replaceFirst(JFCMainClient.szYTHOSTREGEX, "");
		shost = sURL.substring(0, sURL.length()-shost.length());
		shost = shost.toLowerCase().replaceFirst("http[s]?://", "").replaceAll("/", "");
		return(shost);
	} // gethost
	
	String getTitle() {
		if (this.sTitle != null) return this.sTitle; else return("");
	}

	void setTitle(String sTitle) {
		this.sTitle = sTitle;
	}
	
	String getFileName() {
		if (this.sFileName != null) return this.sFileName; else return("");
	}

	void setFileName(String sFileName) {
		this.sFileName = sFileName;
	}

	synchronized void debugoutput (String s) {
		if (!JFCMainClient.getbDEBUG())
			return;
		// sometimes this happens:  Exception in thread "Thread-2" java.lang.Error: Interrupted attempt to aquire write lock (on quit only)
		try {
			JFCMainClient.addTextToConsole("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s));
		} catch (Exception e) {
			try { Thread.sleep(50); } catch (InterruptedException e1) {}
			try { JFCMainClient.addTextToConsole("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s)); } catch (Exception e2) {}
		}
	} // debugoutput
	
	void output (String s) {
		if (JFCMainClient.getbDEBUG())
			return;
		JFCMainClient.addTextToConsole("#info - ".concat(s));
	} // output
	
	String getMyName() {
		return this.getClass().getName().concat(Integer.toString(this.iThreadNo));
	} // getMyName()
	
	/*public void setbDEBUG(boolean bDEBUG) {
		this.bDEBUG = bDEBUG;
	} // setbDEBUG*/
	
	public void run() {
		boolean bDOWNLOADOK = false;
		while (!this.bisinterrupted) {
			try {
				synchronized (JFCMainClient.dlm) {
//					debugoutput("going to sleep.");
					JFCMainClient.dlm.wait(1000); // check for new URLs (if they got pasted faster than threads removing them)
//					debugoutput("woke up ".concat(this.getClass().getName()));
					this.bisinterrupted = JFCMainClient.getbQuitrequested(); // if quit was pressed while this threads works it would not get the InterruptedException and therefore prevent application shutdown
					
//					debugoutput("URLs remain in list: ".concat(Integer.toString(JFCMainClient.dlm.size())));
					// running in CLI mode?
					if (JFCMainClient.frame == null) {
						if (JFCMainClient.dlm.size() == 0) {
							debugoutput(this.getMyName().concat(" ran out of work."));
							if (YTDownloadThread.iThreadcount == 0) {
								// this is the last DownloadThread so shutdown Application as well
								debugoutput("all DownloadThreads ended. shuting down ytd2.");
								JFCMainClient.shutdownAppl();
							} else {
								// this is not the last DownloadThread so shutdown this thread only
								this.bisinterrupted = true;
								debugoutput("end this thread.");
								throw new NullPointerException("end this thread.");
							}
						}
					}
				
					this.sURL = JFCMainClient.getfirstURLFromList();
					output((JFCMainClient.isgerman()?"versuche herunterzuladen: ":"try to download: ").concat(this.sURL));
					JFCMainClient.removeURLFromList(this.sURL);
				} // sync dlm
				if (JFCMainClient.frame != null) // only if we have a GUI window insert "downloading" text 
					JFCMainClient.addYTURLToList(JFCMainClient.szDLSTATE.concat(this.sURL));
				
				this.bNODOWNLOAD = JFCMainClient.getbNODOWNLOAD(); // copy ndl-state because this thread should end with a complete file (and report so) even if someone switches to no-download before this thread is finished
				
				// download one webresource and show result
				bDOWNLOADOK = downloadone(this.sURL); this.iRecursionCount=-1;
				if (bDOWNLOADOK && !this.bNODOWNLOAD)
					output((JFCMainClient.isgerman()?"fertig heruntergeladen: ":"download complete: ").concat("\"").concat(this.getTitle()).concat("\"").concat(" to ").concat(this.getFileName()));
				else
					output((JFCMainClient.isgerman()?"Nicht heruntergeladen: ":"not downloaded: ").concat("\"").concat(this.getTitle()).concat("\"")); // not downloaded does not mean it was erroneous
				
				// running in CLI mode?
				if (JFCMainClient.frame == null)
					JFCMainClient.removeURLFromList(this.sURL);
				else
					JFCMainClient.removeURLFromList(JFCMainClient.szDLSTATE.concat(this.sURL));
				// notify download complete
				eventDispatcher.threadNumberChanged(JFCMainClient.dlm.size());
			} catch (InterruptedException e) {
				this.bisinterrupted = true;
			} catch (NullPointerException npe) {
//				debugoutput("npe - nothing to download?");
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		} // while
		debugoutput("thread ended: ".concat(this.getMyName()));
		YTDownloadThread.iThreadcount--;
	} // run()

} // class YTDownloadThread