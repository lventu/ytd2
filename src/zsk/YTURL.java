package zsk;

public class YTURL {
	private String sQUALITY = null;	// Quality text from html source code
	private String sSTEREO3D = null;	// Format text from html source code
	private String sYTID = null;	// unique youtube video ID (11 alphanum letters) from html source code
	private String sITAG = null;	// unique ID format from html source code
	private String sTYPE = null;	// type  from html source code
	private String sURL = null;		// URL to video of certian format (mpg,flv,webm,..?)
	private String sRESPART = null; // LD or 3D for filename

	private void constructor(String sURL, String sVideoURL) {
		try {
			this.sURL = sURL;
			String[] ssplittemp = sURL.split("&");

			this.sYTID = sVideoURL.substring( sVideoURL.indexOf("v=")+2);

			for (int i = 0; i < ssplittemp.length; i++) {
				if(ssplittemp[i].startsWith("quality")) this.sQUALITY = ssplittemp[i].substring( ssplittemp[i].indexOf('=')+1);
				if(ssplittemp[i].startsWith("itag")) this.sITAG = ssplittemp[i].substring( ssplittemp[i].indexOf('=')+1);
				if(ssplittemp[i].startsWith("type")) this.sTYPE = ssplittemp[i].substring( ssplittemp[i].indexOf('=')+1);
				if(ssplittemp[i].startsWith("stereo3d")) this.sSTEREO3D = ssplittemp[i].substring( ssplittemp[i].indexOf('=')+1);
			}
			JFCMainClient.debugoutput(String.format( "video url saved with key: %s - %s %s    - %s",this.getsITAG(), this.getsTYPE(), this.getsQUALITY(), this.getsURL() ));
		} catch (NullPointerException npe) {
			// happens if URL for selection resolution could not be found
		}
	} // constructor
	
	private void constructor(String sURL, String sVideoURL,String sRESPART) {
		this.constructor(sURL, sVideoURL);
		this.sRESPART = sRESPART;
	} // constructor
	
	public YTURL(String sURL, String sVideoURL) {
		super();
		this.constructor(sURL, sVideoURL);
	} // YTURL()
	
	public YTURL(String sURL, String sVideoURL, String sRESPART) {
		this.constructor(sURL, sVideoURL,sRESPART);
	}

	/**
	 * @return the sQUALITY
	 */
	String getsQUALITY() {
		return this.sQUALITY;
	}

	/**
	 * @return the ssSTEREO3D
	 */
	String getsSTEREO3D() {
		return this.sSTEREO3D;
	}

	/**
	 * @return the sYTID
	 */
	String getsYTID() {
		return this.sYTID;
	}

	/**
	 * @return the sITAG
	 */
	String getsITAG() {
		return this.sITAG;
	}

	/**
	 * @return the sURL
	 */
	String getsURL() {
		return this.sURL;
	}
	
	/**
	 * @return the ssTYPE
	 */
	String getsTYPE() {
		return this.sTYPE;
	}
	/**
	 * @return the sRESPART
	 */
	String getsRESPART() {
		if (this.sRESPART==null)
			return "";
		else
			return this.sRESPART;
	}

	

} // class YTURL
