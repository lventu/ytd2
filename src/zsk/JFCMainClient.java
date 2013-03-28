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


import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
//necessary external libraries
//http://commons.apache.org -> commons-configuration-x.y-bin, commons-cli-x.y-bin, commons-lang-x.y.jar
//http://hc.apache.org -> httpclient-x.y.z.jar, httpcore-x.y.z.jar, httpmime-x.y.z.jar
//
//plus corresponding sources as Source Attachment for Eclipse Project Properties

//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.CommandLineParser;
//import org.apache.commons.cli.GnuParser;
//import org.apache.commons.cli.HelpFormatter;
//import org.apache.commons.cli.Option;
//import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
//import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * knoedel@section60:~/workspace/ytd2$ echo " *" `egrep -v "(^\s*(\/\*|\*|//)|^\s*$)" src/zsk/*java | wc -l` java code lines && echo -e " *" `egrep "(^\s*(\/\*|\*|//)|^\s*$)" src/zsk/*java | wc -l` empty/comment lines "\n *"
 * 1882 java code lines
 * 710 empty/comment lines 
 *
 * knoedel@section60:~/workspace/ytd2$ date && uname -a && lsb_release -a && java -version
 * Sun Feb 24 08:26:55 CET 2013
 * Linux section60 3.2.0-38-generic #61-Ubuntu SMP Tue Feb 19 12:18:21 UTC 2013 x86_64 x86_64 x86_64 GNU/Linux
 * No LSB modules are available.
 * Distributor ID: Ubuntu
 * Description:    Ubuntu 12.04.2 LTS
 * Release:        12.04
 * Codename:       precise
 * java version "1.7.0_15"
 * OpenJDK Runtime Environment (IcedTea7 2.3.7) (7u15-2.3.7-0ubuntu1~12.04)
 * OpenJDK 64-Bit Server VM (build 23.7-b01, mixed mode)
 * 
 * http://www.youtube.com/watch?v=5nj77mJlzrc  					<meta name="title" content="BF109 G">																																																																																								In lovely memory of my grandpa, who used to fly around the clouds. 
 * http://www.youtube.com/watch?v=I3lq1yQo8OY					<meta name="title" content="Showdown: Air Combat - Me-109">																																																																																			http://www.youtube.com/watch?v=yxXBhKJnRR8
 * http://www.youtube.com/watch?v=RYXd60D_kgQ					<meta name="title" content="Me 262 Flys Again!">
 * http://www.youtube.com/watch?v=6ejc9_yR5oQ					<meta name="title" content="Focke Wulf 190 attacks Boeing B 17 in 2009 at Hahnweide">
 *
 * technobase.fm / We Are One! 
 * 
 * using Eclipse 3.5/3.6/3.7/4.2
 * TODOs are for Eclipse IDE - Tasks View
 * 
 * tested on GNU/Linux SunJava&OpenJDK JRE 1.6/1.7 64bit, M$-Windows XP 64bit JRE 1.6.0 32&64Bit and M$-Windows 7 32Bit JRE 1.6.0 32Bit, M$-Windows 7 64 Bit JRE 1.7.0 64Bit
 * using Mozilla Firefox 3.6-19 and M$-IE (8,9)
 * 
 * source code compliance level is 1.5
 * java files are UTF-8 encoded
 * javac should show no warning
 *
 *
 * @author Stefan "MrKnödelmann" K.
 *
 */
public class JFCMainClient extends JFrame implements ActionListener, WindowListener, DocumentListener, ChangeListener, DropTargetListener, ItemListener {
	public static final String szVersion = "V20130224_1519 by MrKnödelmann";
	
	private static final long serialVersionUID = 6791957129816930254L;

	private static final String newline = "\n";
	
	// more or less (internal) output
	// set to True or add 'd' after mod-time
	private boolean bDEBUG = JFCMainClient.szVersion.matches("V[0-9]+_[0-9]+d.*");
	
	// just report file size of HTTP header - don't download binary data (the video)
	private boolean bNODOWNLOAD = this.bDEBUG;

	// save diskspace - try to download e.g. 720p before 1080p if HD is set
	public static boolean bSaveDiskSpace = false;
	
	// append youtube ID (http://www.youtube.com/watch?v=XY) to filename 
	public static boolean bSaveIDinFilename = false;

	public static String sproxy = null;
	public static String[] saargs = null;
	
	public static String szDLSTATE = "downloading ";
	
	// something like [http://][www.]youtube.[cc|to|pl|ev|do|ma|in]/watch?v=0123456789A 
	public static final String szYTREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?((W|w)(W|w)(W|w)\\.)?(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/(W|w)(A|a)(T|t)(C|c)(H|h)\\?(v|V)=[^&]{11}"; // http://de.wikipedia.org/wiki/CcTLD
	// something like [http://][*].youtube.[cc|to|pl|ev|do|ma|in]/   the last / is for marking the end of host, it does not belong to the hostpart
	public static final String szYTHOSTREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?(.*)\\.(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/";
	
	// RFC-1123 ? hostname [with protocol]	
	public static final String szPROXYREGEX = "(^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?([a-zA-Z0-9]+:[a-zA-Z0-9]+@)?([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*(:[0-90-90-90-9]{1,4})?$)|()";
	
	// RFC-1738 URL characters - not a regex for a real URL!!
	public static final String szURLREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?[a-zA-Z0-9;/\\?@=&\\$\\-_\\.+!*\\'\\(\\),%]+$"; // without ":", plus "%"
	
	private static final String szPLAYLISTREGEX = "/view_play_list\\?p=([A-Za-z0-9]*)&playnext=[0-9]{1,2}&v=";
	
	// Sistem property
	private static final String sfilesep = System.getProperty("file.separator");
	private static final String shomedir = System.getProperty("user.home").concat(sfilesep);
	
	// all characters that do not belong to an HTTP URL - could be written shorter?? (where did I use this?? dont now anymore)
	final String snotsourcecodeurl = "[^(a-z)^(A-Z)^(0-9)^%^&^=^\\.^:^/^\\?^_^-]";
	
	static JFCMainClient frame = null;

	private static Boolean bQuitrequested = false;
	private static Thread guiThread;
	
	static YTDownloadThread t1;
	static YTDownloadThread t2;
	static YTDownloadThread t3;
	static YTDownloadThread t4;
	static YTDownloadThread t5;
	static YTDownloadThread t6;
	
	JPanel panel = null;
	JSplitPane middlepane = null;
	JTextArea textarea = null;
	JList<String> urllist = null;
	JButton quitbutton = null;
	JButton directorybutton = null;
	JTextField directorytextfield = null;
	JTextField textinputfield = null;
	static DefaultListModel<String> dlm = null;
	JRadioButton hdbutton = null;
	JRadioButton stdbutton = null;
	JRadioButton ldbutton = null;
	JRadioButton mpgbutton = null;
	JRadioButton onlyaudiobutton = null;
	JRadioButton flvbutton = null;
	JRadioButton webmbutton = null;
	JCheckBox saveconfigcheckbox = null;
	JCheckBox save3dcheckbox = null;
	 
	final static String szConfigFile = "ytd2.config.xml";
	static XMLConfiguration config = null;
	
	static boolean bIsCLI = false;

	enum eCLIdownloadQuality { LD, SD, HD} ;
	static eCLIdownloadQuality CLIdownloadQuality ;
	enum eCLIdownloadFormat { MPG, WEBM, FLV };
	static eCLIdownloadFormat CLIdownloadFormat; 
	
	static Options sCLIOptions = new Options();
	
	static {
		initializeOptions();
	}

	// by Benjamin Reed for Mac OS
	private static void initializeOptions() {
//		sCLIOptions.addOption(OptionBuilder.withLongOpt("help").withDescription("This help.").create('?'));
//		sCLIOptions.addOption(OptionBuilder.withLongOpt("debug").withDescription("Enable debug output.").create('d'));
//		sCLIOptions.addOption(OptionBuilder.withDescription("Dowload low-quality video. (default: standard-quality)").withLongOpt("low").create('l'));
//		sCLIOptions.addOption(OptionBuilder.withDescription("Dowload high-quality video. (default: standard-quality)").withLongOpt("high").create('h'));
//		sCLIOptions.addOption(OptionBuilder.withDescription("FLV/WEBM format. (default: MPEG)").withLongOpt("flv").create('f'));
	}
	
	public static synchronized Boolean getbQuitrequested() {
		return bQuitrequested;
	}


	public synchronized static void setbQuitrequested(Boolean bQuitrequested) {
		JFCMainClient.bQuitrequested = bQuitrequested;
	}
	
	/**
	 * get state of downloadbutton as Integer 
	 * 
	 * @return
	 */
	public synchronized static int getIdlbuttonstate() {
		if (bIsCLI) {
			return ((JFCMainClient.CLIdownloadQuality==JFCMainClient.eCLIdownloadQuality.HD?4:0) + (JFCMainClient.CLIdownloadQuality==JFCMainClient.eCLIdownloadQuality.SD?2:0) + (JFCMainClient.CLIdownloadQuality==JFCMainClient.eCLIdownloadQuality.LD?1:0));
		} else {
			return ((JFCMainClient.frame.hdbutton.isSelected()?4:0) + (JFCMainClient.frame.stdbutton.isSelected()?2:0) + (JFCMainClient.frame.ldbutton.isSelected()?1:0));
		}
	} //getIdlbuttonstate

	
	/**
	 * get state of formatbutton for mpg as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean getBmpgbuttonstate() {
		if (bIsCLI) {
			return (JFCMainClient.CLIdownloadFormat==JFCMainClient.eCLIdownloadFormat.MPG);
		} else {
			return (JFCMainClient.frame.mpgbutton.isSelected()); 
		}
	} // getBmpgbuttonstate
	
	/**
	 * get state of formatbutton for flv as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean getBflvbuttonstate() {
		if (bIsCLI) {
			return (JFCMainClient.CLIdownloadFormat==JFCMainClient.eCLIdownloadFormat.FLV);
		} else {
			return (JFCMainClient.frame.flvbutton.isSelected()); 
		}
	} // getBflvbuttonstate
	
	/**
	 * get state of formatbutton for webm as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean getBwebmbuttonstate() {
		if (bIsCLI) {
			return (JFCMainClient.CLIdownloadFormat==JFCMainClient.eCLIdownloadFormat.WEBM);
		} else {
			return (JFCMainClient.frame.webmbutton.isSelected()); 
		}
	} // getBwebmbuttonstate


	/**
	 * get state of 3dbutton as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean get3Dbuttonstate() {
		if (bIsCLI) {
			return (false);
		} else {
			return (JFCMainClient.frame.save3dcheckbox.isSelected()); 
		}
	} // get3Dbuttonstate	
	
	/**
	 * append text to textarea
	 * 
	 * @param Object o
	 */
	public static void addTextToConsole( Object o ) {
		try {
			// append() is threadsafe
			JFCMainClient.frame.textarea.append( o.toString().concat( newline ) );
			JFCMainClient.frame.textarea.setCaretPosition( JFCMainClient.frame.textarea.getDocument().getLength() );
			JFCMainClient.frame.textinputfield.requestFocusInWindow();
			System.out.println(o);
		} catch (NullPointerException npe) {
			// for CLI run only
			System.out.println(o);
		} catch (Exception e) {
			@SuppressWarnings( "unused" ) // for debuging
			String s = e.getMessage();
		}
	} // addTexttoconsole()
	
	
	public static void addYTURLToList( String sname ) {
		String sn = sname;
		// bring all URLs into the same form
		if (sname.toLowerCase().startsWith("youtube")) sn = "http://www.".concat(sname);
		if (sname.toLowerCase().startsWith("www")) sn = "http://".concat(sname);
		synchronized (JFCMainClient.dlm) {
			JFCMainClient.dlm.addElement( sn );
			debugoutput("notify() ");
			JFCMainClient.dlm.notify();
		}
	} // addYTURLToList
	
	public static void exchangeYTURLInList( String sfromname, String stoname) {
		synchronized (JFCMainClient.dlm) {
			try {
				int i = JFCMainClient.dlm.indexOf( sfromname );
				JFCMainClient.dlm.setElementAt(stoname, i);
			} catch (IndexOutOfBoundsException ioobe) {}
		}
	} // exchangeYTURLInList

	public static void removeURLFromList( String sname ) {
		synchronized (JFCMainClient.dlm) {
			try {
				int i = JFCMainClient.dlm.indexOf( sname );
				JFCMainClient.dlm.remove( i );
			} catch (IndexOutOfBoundsException ioobe) {}
		}
	} // removeURLFromList
	
	public static String getfirstURLFromList( ) {
		String src = null;
		synchronized (JFCMainClient.dlm) {
			try {
				int i;
				// try to find the index of an URL entry in the list without "downloading " at the beginning
				for ( i = 0; i < JFCMainClient.dlm.getSize(); i++) {
					if (!((String)JFCMainClient.dlm.get(i)).startsWith( JFCMainClient.szDLSTATE )) break;
				}
				src = ((String) JFCMainClient.dlm.get(i)).replaceFirst( JFCMainClient.szDLSTATE, "" );
			} catch (IndexOutOfBoundsException ioobe) {}
		}
		return src;
	} // getfirstURLFromList

	public static void clearURLList() {
		try {
			synchronized (JFCMainClient.dlm) {
				JFCMainClient.dlm.clear();
			}
		} catch (NullPointerException npe) {}
	} // clearURLList
	
	public static boolean isgerman() {
		return Locale.getDefault().toString().startsWith("de_") || (JFCMainClient.getbDEBUG() && System.getProperty("user.home").equals("/home/knoedel"));
	} // isgerman

	public void setfocustotextfield() {
		this.textinputfield.requestFocusInWindow();
	} // setfocustotextfield()
	
	static void saveConfigFile() {
		debugoutput("saveConfigFile()");
		
		if (JFCMainClient.frame == null)
			return;
		
		if (JFCMainClient.frame.saveconfigcheckbox.isSelected()) {

//			String sdirectorychoosed;
//			try {
//				synchronized (JFCMainClient.frame.directorytextfield) {
//					sdirectorychoosed = JFCMainClient.frame.directorytextfield.getText();
//				}
//			} catch (NullPointerException npe) {
//				// for CLI-only run
//				sdirectorychoosed = JFCMainClient.shomedir;
//			}

			// if config wasn't saved before create a new file
			if (config.getFile() == null)
				config.setFile(new File(JFCMainClient.szConfigFile));

			config.setProperty("http_proxy", sproxy);
			config.setProperty("targetfolder", config.getProperty("savefolder"));
			config.setProperty("videobutton", getIdlbuttonstate());
			config.setProperty("mpgbutton", getBmpgbuttonstate());
			try {
				JFCMainClient.config.save();
				// debugoutput will not work here anymore because the GUI gets not updated - output is for CLI-run only
				debugoutput("config file written: ".concat(config.getBasePath()));
			} catch (ConfigurationException e) {
				debugoutput("error writing config file: ".concat(config.getBasePath()));
			}
		}

	} // saveconfigfile()
	
	static public void shutdownAppl() {
		// try to save settings to an xml-file
		saveConfigFile();
		
		// running downloads are difficult to terminate (Thread.isInterrupted() does not work here)
		JFCMainClient.setbQuitrequested(true);
		debugoutput("bQuitrequested = true");
		
		// TODO mayby we use a threadpool here?!
		try {JFCMainClient.t1.interrupt();} catch (NullPointerException npe) {}
		try {JFCMainClient.t2.interrupt();} catch (NullPointerException npe) {}
		try {JFCMainClient.t3.interrupt();} catch (NullPointerException npe) {}
		try {JFCMainClient.t4.interrupt();} catch (NullPointerException npe) {}
		try {JFCMainClient.t5.interrupt();} catch (NullPointerException npe) {}
		try {JFCMainClient.t6.interrupt();} catch (NullPointerException npe) {}
		try {
			try {JFCMainClient.t1.join();} catch (NullPointerException npe) {} 
			try {JFCMainClient.t2.join();} catch (NullPointerException npe) {} 
			try {JFCMainClient.t3.join();} catch (NullPointerException npe) {} 
			try {JFCMainClient.t4.join();} catch (NullPointerException npe) {}
			try {JFCMainClient.t5.join();} catch (NullPointerException npe) {}
			try {JFCMainClient.t6.join();} catch (NullPointerException npe) {}
		} catch (InterruptedException ie) {}
		
		// in cli-mode we do not exit as the shutdown hookhandler would run circular
		if ( (JFCMainClient.frame != null) && (guiThread.isAlive()) ) {
			debugoutput( "quit." );
			output( "quit." );
			guiThread.interrupt();
			frame.dispose();
		}
	} // shutdownAppl()
	
    /**
     * @param string
     * @param regex
     * @param replaceWith
     * @return changed String
     */
    String replaceAll(String string, String regex, String replaceWith) {
        Pattern myPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return (myPattern.matcher(string).replaceAll(replaceWith));
    } // replaceAll 
	
	/**
	 * process events of ActionListener
	 * 
	 */
	public void actionPerformed( final ActionEvent e ) {
		if (e.getSource().equals( frame.textinputfield )) {
			if (!e.getActionCommand().equals( "" )) { 
				if (e.getActionCommand().matches(szYTREGEX))
					addYTURLToList(e.getActionCommand());
				else {
					addTextToConsole(e.getActionCommand());
					cli(e.getActionCommand().toLowerCase());
				}
			}
			synchronized (frame.textinputfield) {
				frame.textinputfield.setText("");				
			}
			return;
		}
		
		// let the user choose another dir
		if (e.getSource().equals( frame.directorybutton )) {
			debugoutput("frame.directorybutton");
			JFileChooser fc = new JFileChooser();
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			synchronized (frame.directorytextfield) {
				// we have to set current directory here because it gets lost when fc is lost
				fc.setCurrentDirectory( new File( frame.directorytextfield.getText()) );
			}
			debugoutput("current dir: ".concat( fc.getCurrentDirectory().getAbsolutePath()) );
			if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			String snewdirectory = fc.getSelectedFile().getAbsolutePath();
			// append file.seperator if last character is not file.seperator (the user choosed a directory other than root)
			snewdirectory.concat(snewdirectory.endsWith(System.getProperty("file.separator"))?"":System.getProperty("file.separator"));
			File ftest = new File(snewdirectory);
			if (ftest.exists()) {
				if (ftest.isDirectory()) {
					synchronized (frame.directorytextfield) {
						frame.directorytextfield.setText( snewdirectory );
					}
					config.setProperty("savefolder", snewdirectory);
					debugoutput("new current dir: ".concat( fc.getCurrentDirectory().getAbsolutePath()) );
					output("new current dir: ".concat( fc.getCurrentDirectory().getAbsolutePath()) );
				} else {
					output("not a directory: ".concat(snewdirectory));
				}
			} else {
				output("directory does not exist: ".concat(snewdirectory));
			}
			return;
		}
		
		// let the user choose another download resolution
		if (e.getActionCommand().equals(JFCMainClient.frame.hdbutton.getActionCommand()) || 
				e.getActionCommand().equals(JFCMainClient.frame.stdbutton.getActionCommand()) ||
				e.getActionCommand().equals(JFCMainClient.frame.ldbutton.getActionCommand()) ) {
			debugoutput("trying: ".concat(e.getActionCommand()));
			output("trying: ".concat( e.getActionCommand().toUpperCase() ));
			
			return;
		}
		
		// let the user choose another video format
		if (e.getActionCommand().equals(JFCMainClient.frame.mpgbutton.getActionCommand()) ||
			e.getActionCommand().equals(JFCMainClient.frame.flvbutton.getActionCommand()) || 
			e.getActionCommand().equals(JFCMainClient.frame.webmbutton.getActionCommand()) ) {
			debugoutput("trying: ".concat(e.getActionCommand()));
			output("trying: ".concat( e.getActionCommand().toUpperCase().concat( "  ".concat(isgerman()?"(Dateien werden immer nach MIME-Typ benannt!)":"(files will always be named according to MIME type!)")) ));
			return;
		} 
		
		if (e.getActionCommand().equals( "quit" )) {
			addTextToConsole(isgerman()?"Programmende ausgewählt - beende DownloadThreads, Vorgang kann eine Weile dauern!":"quit requested - signaling donwload threads to terminate, this may take a while!");
			// seems to have to effect:
			//JFCMainClient.frame.repaint();
			JFCMainClient.shutdownAppl();
			return;
		}
		debugoutput("unknown action. ".concat(e.getSource().toString()));
		output("unbekannte Aktion");
		
	} // actionPerformed()

	/**
	 * process input events
	 * 
	 * @param scmd
	 */
	void cli(String scmd) {
		if (scmd.matches("^(hilfe|help|[-/]?[h|\\?])")) {
			addTextToConsole("debug[ on| off]\t: ".concat(JFCMainClient.isgerman()?"mehr oder weniger (interne) Ausgaben":"more or less (internal) output"));
			addTextToConsole("help|-h|/?]\t\t: ".concat(JFCMainClient.isgerman()?"zeige diesen Text":"show this text"));
			addTextToConsole("id[ on| off]\t\t: ".concat(JFCMainClient.isgerman()?"Video ID in Dateinamen vermerken":"append video id to filename"));
			addTextToConsole("ndl[ on| off]\t\t: ".concat(JFCMainClient.isgerman()?"kein Herunterladen, nur Dateigröße ausgeben":"no download, just report file size"));
			addTextToConsole("proxy[ URL]\t\t: ".concat(JFCMainClient.isgerman()?"Proxy Variable anzeigen oder ändern":"get or set proxy variable").concat(" - [http[s]://]proxyhost:proxyport"));
			addTextToConsole("quit|exit\t\t: ".concat(JFCMainClient.isgerman()?"Anwendung beenden":"shutdown application"));
			addTextToConsole("sds[ on| off]\t\t: ".concat(JFCMainClient.isgerman()?"Speicherplatz sparen, geringere Aufl. zu erst herunterladen (z.B. 720p vor 1080p)":"save disk space, lower res download first (e.g. 720p before 1080p)"));
			addTextToConsole("version|-v|\t\t: ".concat(JFCMainClient.isgerman()?"Version anzeigen":"show version"));
		} else if (scmd.matches("^(-?v(ersion)?)")) {
			addTextToConsole(szVersion);
		} else if (scmd.matches("^(debug)( on| off| true| false)?")) {
			if (scmd.matches(".*(on|true)$"))
				JFCMainClient.frame.bDEBUG = true;
			else if (scmd.matches(".*(off|false)$"))
				JFCMainClient.frame.bDEBUG = false;

			addTextToConsole("debug: ".concat(Boolean.toString(JFCMainClient.frame.bDEBUG)));
		} else if (scmd.matches("^(ndl)( on| off| true| false)?")) {
			if (scmd.matches(".*(on|true)$"))
				setbNODOWNLOAD(true);
			else if (scmd.matches(".*(off|false)$"))
				setbNODOWNLOAD(false);

			addTextToConsole("ndl: ".concat(Boolean.toString(JFCMainClient.getbNODOWNLOAD())));
		} else if (scmd.matches("^(sds)( on| off| true| false)?")) {
			if (scmd.matches(".*(on|true)$"))
				JFCMainClient.bSaveDiskSpace = true;
			else if (scmd.matches(".*(off|false)$"))
				JFCMainClient.bSaveDiskSpace = false;

			addTextToConsole("sds: ".concat(Boolean.toString(JFCMainClient.bSaveDiskSpace)));
		} else if (scmd.matches("^(quit|exit)")) {
			JFCMainClient.shutdownAppl();
		} else if (scmd.matches("^(proxy)( .*)?")) {
			if (!scmd.matches("^(proxy)$")) {
				// we replace "" and '' with <nothing> otherwise it's interpreted as host
				// perhaps some users don't know how to input
				// "proxy[ URL]" with an empty URL ;-)
				String snewproxy = scmd.replaceAll("\"", "").replaceAll("'", "").replaceFirst("proxy ", "");
				debugoutput("snewproxy: ".concat(snewproxy));
				if (snewproxy.matches(JFCMainClient.szPROXYREGEX))
					JFCMainClient.sproxy = snewproxy;
				else
					addTextToConsole(isgerman() ? "Proxy-Zeichenkette entspricht nicht der Spezifikation für einen Rechner!": "proxy string does not match hostname specification!");
			}
			addTextToConsole("proxy: ".concat(JFCMainClient.sproxy));
		} else if (scmd.matches("^(config|c)$")) {
			addTextToConsole("config: ");
//			for ( Iterator<String> i = config.getKeys(); i.hasNext(); ) {
//				addTextToConsole(String.format("%s: %s", i, config.getProperty(i.toString())));
//			}
		} else if (scmd.matches("^(id)( .*)?")) {
			if (scmd.matches(".*(on|true)$"))
				JFCMainClient.setbSaveIDinFilename(true);
			else if (scmd.matches(".*(off|false)$"))
				JFCMainClient.setbSaveIDinFilename(false);

			addTextToConsole("id: ".concat(Boolean.toString(JFCMainClient.isbSaveIDinFilename())));
		} else
			addTextToConsole(isgerman() ? "? (versuche hilfe|help|-h|/?)": "? (try help|-h|/?)");

	} // cli()

	static synchronized void setbNODOWNLOAD( boolean bNODOWNLOAD ) {
		JFCMainClient.frame.bNODOWNLOAD = bNODOWNLOAD;
	} // setbNODOWNLOAD
	
	static synchronized boolean getbNODOWNLOAD() {
		// no download if we debug
		try {
			return(JFCMainClient.frame.bNODOWNLOAD);
		} catch (NullPointerException npe) {
			return(JFCMainClient.getbDEBUG());
		}
	} // getbNODOWNLOAD
	
	static synchronized boolean isbSaveIDinFilename() {
		return bSaveIDinFilename;
	} // isbSaveIDinFilename

	private static synchronized void setbSaveIDinFilename(boolean bSaveIDinFilename) {
		JFCMainClient.bSaveIDinFilename = bSaveIDinFilename;
	} // setbSaveIDinFilename
	
	static synchronized boolean getbDEBUG() {
		try {
			return(JFCMainClient.frame.bDEBUG);
		} catch (NullPointerException npe) {
			return JFCMainClient.szVersion.matches("V[0-9]+_[0-9]+d.*");
		}
	} // getbDEBUG

	/**
	 * @param pane
	 * @param downloadDir 
	 */
	public void addComponentsToPane( final Container pane, String downloadDir ) {
		this.panel = new JPanel();

		this.panel.setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.anchor = GridBagConstraints.WEST;

		JFCMainClient.dlm = new DefaultListModel<String>();
		this.urllist = new JList<String>( JFCMainClient.dlm );
		// TODO maybe we add a button to remove added URLs from list?
//		this.userlist.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.urllist.setFocusable( false );
		this.textarea = new JTextArea( 2, 2 );
		this.textarea.setEditable( true );
		this.textarea.setFocusable( false );

		JScrollPane leftscrollpane = new JScrollPane( this.urllist );
		JScrollPane rightscrollpane = new JScrollPane( this.textarea );
		this.middlepane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, leftscrollpane, rightscrollpane );
		this.middlepane.setOneTouchExpandable( true );
		this.middlepane.setDividerLocation( 150 );

		Dimension minimumSize = new Dimension( 25, 25 );
		leftscrollpane.setMinimumSize( minimumSize );
		rightscrollpane.setMinimumSize( minimumSize );
		
		this.directorybutton = new JButton("", createImageIcon("images/open.png",""));
		gbc.gridx = 0;
		gbc.gridy = 0;
		this.directorybutton.addActionListener( this );
		this.panel.add( this.directorybutton, gbc );
		
		this.saveconfigcheckbox = new JCheckBox(isgerman()?"Konfig. speichern":"Save config");
		this.saveconfigcheckbox.setSelected(false);
		
		this.saveconfigcheckbox.addItemListener(this);
		this.panel.add(this.saveconfigcheckbox);
		
		this.saveconfigcheckbox.setEnabled(false);
		
		// TODO check if initial download directory exists
		// assume that at least the users homedir exists
		//if (System.getProperty("user.home").equals("/home/knoedel")) shomedir = "/home/knoedel/YouTube Downloads/";
		debugoutput("user.home: ".concat(System.getProperty("user.home")).concat("  shomedir: ".concat(shomedir)));
		debugoutput("os.name: ".concat(System.getProperty("os.name")));
		debugoutput("os.arch: ".concat(System.getProperty("os.arch")));
		debugoutput("os.version: ".concat(System.getProperty("os.version")));
		debugoutput("Locale.getDefault: ".concat(Locale.getDefault().toString()));
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.directorytextfield = new JTextField( downloadDir, 20+(JFCMainClient.getbDEBUG()?48:0) );
		//this.directorytextfield = new JTextField( shomedir, 20+(JFCMainClient.getbDEBUG()?48:0) );
		this.directorytextfield.setEnabled( false );
		this.directorytextfield.setFocusable( true );
		this.directorytextfield.addActionListener( this );
		this.panel.add( this.directorytextfield, gbc);
		
		JLabel dirhint = new JLabel( isgerman()?"Speichern im Ordner:":"Download to folder:");

		gbc.gridx = 0;
		gbc.gridy = 1;
		this.panel.add( dirhint, gbc);
		
		debugoutput(String.format("heigth x width: %d x %d",Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height));
		
		this.middlepane.setPreferredSize( new Dimension( Toolkit.getDefaultToolkit().getScreenSize().width/3, Toolkit.getDefaultToolkit().getScreenSize().height/4+(JFCMainClient.getbDEBUG()?200:0) ) );

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 2;
		gbc.weightx = 2;
		gbc.gridwidth = 2;
		this.panel.add( this.middlepane, gbc );

		// radio buttons for resolution to download
		JFCMainClient.frame.hdbutton = new JRadioButton("HD"); JFCMainClient.frame.hdbutton.setActionCommand("hd"); JFCMainClient.frame.hdbutton.addActionListener(this); JFCMainClient.frame.hdbutton.setToolTipText("1080p/720p");
		JFCMainClient.frame.stdbutton = new JRadioButton("Std"); JFCMainClient.frame.stdbutton.setActionCommand("std"); JFCMainClient.frame.stdbutton.addActionListener(this); JFCMainClient.frame.stdbutton.setToolTipText("480p/360p");
		JFCMainClient.frame.ldbutton = new JRadioButton("LD"); JFCMainClient.frame.ldbutton.setActionCommand("ld"); JFCMainClient.frame.ldbutton.addActionListener(this); JFCMainClient.frame.ldbutton.setToolTipText("< 360p");
		
		JFCMainClient.frame.stdbutton.setSelected(true);
		JFCMainClient.frame.hdbutton.setEnabled(true);
		JFCMainClient.frame.ldbutton.setEnabled(true);
		
		ButtonGroup bgroup = new ButtonGroup();
		bgroup.add(JFCMainClient.frame.hdbutton);
		bgroup.add(JFCMainClient.frame.stdbutton);
		bgroup.add(JFCMainClient.frame.ldbutton);
		
		JPanel radiopanel = new JPanel(new GridLayout(1,0));
		radiopanel.add(JFCMainClient.frame.hdbutton);
		radiopanel.add(JFCMainClient.frame.stdbutton);
		radiopanel.add(JFCMainClient.frame.ldbutton);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 0;
		gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.panel.add( radiopanel, gbc );

		// radio buttons for video format to download
		JFCMainClient.frame.mpgbutton = new JRadioButton("MPEG"); JFCMainClient.frame.mpgbutton.setActionCommand("mpg"); JFCMainClient.frame.mpgbutton.addActionListener(this); JFCMainClient.frame.mpgbutton.setToolTipText("Codec: H.264 MPEG-4");
		JFCMainClient.frame.webmbutton = new JRadioButton("WEBM"); JFCMainClient.frame.webmbutton.setActionCommand("webm"); JFCMainClient.frame.webmbutton.addActionListener(this); JFCMainClient.frame.webmbutton.setToolTipText("Codec: Google/On2's VP8 or Googles WebM");
		JFCMainClient.frame.flvbutton = new JRadioButton("FLV"); JFCMainClient.frame.flvbutton.setActionCommand("flv"); JFCMainClient.frame.flvbutton.addActionListener(this); JFCMainClient.frame.flvbutton.setToolTipText("Codec: Flash Video (FLV1)");
		
		bgroup = new ButtonGroup();
		bgroup.add(JFCMainClient.frame.mpgbutton);
		bgroup.add(JFCMainClient.frame.webmbutton);
		bgroup.add(JFCMainClient.frame.flvbutton);
		
		JFCMainClient.frame.mpgbutton.setSelected(true);
		JFCMainClient.frame.mpgbutton.setEnabled(true);
		JFCMainClient.frame.webmbutton.setEnabled(true);
		JFCMainClient.frame.flvbutton.setEnabled(true);
		
		JFCMainClient.frame.save3dcheckbox = new JCheckBox("3D");
		JFCMainClient.frame.save3dcheckbox.setToolTipText("stereoscopic video");
		JFCMainClient.frame.save3dcheckbox.setSelected(false);
		JFCMainClient.frame.save3dcheckbox.setEnabled(true);
		JFCMainClient.frame.save3dcheckbox.addItemListener(this);
		
		radiopanel = new JPanel(new GridLayout(1,0));
		radiopanel.add(JFCMainClient.frame.save3dcheckbox);
		radiopanel.add(JFCMainClient.frame.mpgbutton);
		radiopanel.add(JFCMainClient.frame.webmbutton);
		radiopanel.add(JFCMainClient.frame.flvbutton);
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 0;
		gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.panel.add( radiopanel, gbc );
		
		JLabel hint = new JLabel( isgerman()?"eingeben, reinkopieren, reinziehen von YT-Webadressen oder YT-Videobilder:":"Type, paste or drag'n drop a YouTube video address:");

		gbc.fill = 0;
		gbc.gridwidth = 0;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.WEST;
		this.panel.add( hint, gbc );
		
		this.textinputfield = new JTextField( 20 );
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		this.textinputfield.setEnabled( true );
		this.textinputfield.setFocusable( true );
		this.textinputfield.addActionListener( this );
		this.textinputfield.getDocument().addDocumentListener(this);
		this.panel.add( this.textinputfield, gbc );
		
		this.quitbutton = new JButton( "" ,createImageIcon("images/exit.png",""));		
		gbc.gridx = 2;
		gbc.gridy = 5;
		gbc.gridwidth = 0;
		this.quitbutton.addActionListener( this );
		this.quitbutton.setActionCommand( "quit" );
		this.quitbutton.setToolTipText( "Exit." );

		this.panel.add( this.quitbutton, gbc );

		pane.add( this.panel );
		addWindowListener( this );
		
		JFCMainClient.frame.setDropTarget(new DropTarget(this, this));
		JFCMainClient.frame.textarea.setTransferHandler(null); // otherwise the dropped text would be inserted

	} // addComponentsToPane()

	public JFCMainClient( String name ) {
		super( name );
	}

	public JFCMainClient() {
		
	}
	
	
	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private static void initializeUI(boolean setVisibleFlg, String downloadDir) {
		String sv = "YTD2 ".concat(szVersion).concat(" ").concat("http://sourceforge.net/projects/ytd2/"); // ytd2.sf.net is shorter
		sv = isgerman()?sv.replaceFirst("by", "von"):sv;

		if (! bIsCLI) {
			setDefaultLookAndFeelDecorated(false);
			frame = new JFCMainClient( sv );
			frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
			frame.addComponentsToPane( frame.getContentPane(), downloadDir );
			frame.pack();
			frame.setVisible( setVisibleFlg );
		} else {
			setbSaveIDinFilename(true);
		}
		
		JFCMainClient.szDLSTATE = isgerman()?"heruntergeladen ":JFCMainClient.szDLSTATE;
		
		sv = "version: ".concat( szVersion ).concat(JFCMainClient.getbDEBUG()?" DEBUG ":"");
		sv = isgerman()?sv.replaceFirst("by", "von"):sv;
		output(sv); debugoutput(sv);
		output(""); // \n

		// TODO ensure threads are running even if one ends with an (unhandled) Exception
		
		JFCMainClient.sproxy = System.getenv("http_proxy");
		if (JFCMainClient.sproxy==null) sproxy="";
		sv = "HTTP Proxy: ".concat(sproxy);
		output(sv); debugoutput(sv);
		
		sv = isgerman()?"Speicherverzeichnis: ":"initial download folder: ";
		if (bIsCLI) {
			if (config.getProperty("savefolder")==null) {
				// set initial download folder to users homedir
				String shomedir = System.getProperty("user.dir"); // for CLI-only run
				if (!shomedir.endsWith(System.getProperty("file.separator")))
					shomedir += System.getProperty("file.separator");
				config.setProperty("savefolder", shomedir);
			}
			sv = sv.concat((String)config.getProperty("savefolder"));
		} else {
			sv = sv.concat(JFCMainClient.frame.directorytextfield.getText());
			config.setProperty("savefolder", JFCMainClient.frame.directorytextfield.getText());
		}
		output(sv); debugoutput(sv);
		
		// lets respect the upload limit of google (youtube)
		// downloading is faster than viewing anyway so don't start more than six threads and don't play around with the URL-strings please!!!
		t1 = new YTDownloadThread();
		t1.start();
		t2 = new YTDownloadThread();
		t2.start();
		t3 = new YTDownloadThread();
		t3.start();
		t4 = new YTDownloadThread();
		t4.start();
		t5 = new YTDownloadThread();
		t5.start();
		t6 = new YTDownloadThread();
		t6.start();
		
		output(""); // \n
		output(isgerman()?"besuche sf.net/projects/ytd2/forums für irgendwelche Tipps, Vorschläge, Neuerungen, Fragen!":"Visit sf.net/projects/ytd2/forums for tips, questions, updates and comments!");
		output(""); // \n
		if (!bIsCLI)
			JFCMainClient.frame.cli("help");
		
	} // createAndShowGUI()
	
	
	public void windowActivated( WindowEvent e ) {
			setfocustotextfield();
	} // windowActivated()

	public void windowClosed( WindowEvent e ) {
	}

	/**
	 * quit==exit
	 * 
	 */
	public void windowClosing( WindowEvent e ) {
		System.out.println("---> CHIUSURA YTD <---");
		JFCMainClient.shutdownAppl();
	} // windowClosing()

	public void windowDeactivated( WindowEvent e ) {
	}

	public void windowDeiconified( WindowEvent e ) {
	}

	public void windowIconified( WindowEvent e ) {
	}

	public void windowOpened( WindowEvent e ) {
	}
	
	public void processComponentEvent(ComponentEvent e) {
		switch (e.getID()) {
		case ComponentEvent.COMPONENT_MOVED:
			break;
		case ComponentEvent.COMPONENT_RESIZED:
			JFCMainClient.frame.middlepane.setDividerLocation(JFCMainClient.frame.middlepane.getWidth() / 3);
			break;
		case ComponentEvent.COMPONENT_HIDDEN:
			break;
		case ComponentEvent.COMPONENT_SHOWN:
			break;
		}
	} // processComponentEvent
	
	/**
	 * main entry point
	 * 
	 * @param args
	 */
	public static void main( final String[] args ) {
		
		fpInit(args, true, shomedir);

	} // main()


	/** Interfaccia generale pubblica
	 * @param args
	 * @param silentMode
	 * @param downloadDir
	 */
	public static void fpInit(final String[] args, final boolean silentMode, final String downloadDir) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	debugoutput("shutdown hook handler.");
		    	if (JFCMainClient.frame == null) {
		    		JFCMainClient.shutdownAppl();
		    	}
		    	debugoutput("shutdown hook handler. end run()");
		    }
		});
		
		try { // load from file
			config = new XMLConfiguration(szConfigFile);
			debugoutput("configuration loaded from file: ".concat(config.getBasePath()));
			// create empty config entries?
		} catch (ConfigurationException e1) {
			debugoutput("configuration could not be load from file: ".concat(szConfigFile).concat(" creating new one."));
			config = new XMLConfiguration();
		}
		
		JFCMainClient.saargs = args;
		if (args.length>0) {
			bIsCLI = true;
			runCLI(silentMode, downloadDir);			
		} else {
			try {
				UIManager.setLookAndFeel( "javax.swing.plaf.metal.MetalLookAndFeel" );
			} catch (UnsupportedLookAndFeelException ex) {
				ex.printStackTrace();
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
			} catch (InstantiationException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			} catch (java.lang.InternalError ie) {
				System.err.println(ie.getMessage());
				printHelp();
				System.exit(1);
			}

//			try {
				/*javax.swing.SwingUtilities.invokeAndWait(new Runnable() {	});*/
				guiThread = new Thread(new Runnable() {

				    public void run() {
				    	try {
							initializeUI(silentMode, downloadDir);
							while(!guiThread.isInterrupted()) {}
						}  catch (java.awt.HeadlessException he) {
							System.err.println(he.getMessage());
							JFCMainClient.printHelp();
							System.exit(1);
						}
				    }
				    
				});
				guiThread.start();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			}
		} // if
	}
	
	/**Accesso semplificato, invisibile e senza parametri. Solo la directory di default
	 * @param downloadDir
	 */
	public static void fpInit(String downloadDir) {
		fpInit(new String[0], false, downloadDir);
	}
	
	static int parseargs(String[] args) throws ArrayIndexOutOfBoundsException {
		int irc=0;
		
		// TODO 3D as parameter for CLI download
		
		JFCMainClient.CLIdownloadQuality = JFCMainClient.eCLIdownloadQuality.SD;
		JFCMainClient.CLIdownloadFormat = JFCMainClient.eCLIdownloadFormat.MPG;
		
		for (int i=0;i<args.length;i++) {
			if (args[irc].toLowerCase().matches("(--)?h(elp|ilfe)?")) {
				printHelp();
				return args.length; // dont load any videos if --help is submitted
			}
			
			if (args[irc].toLowerCase().equals("-l")) {
				JFCMainClient.CLIdownloadQuality = JFCMainClient.eCLIdownloadQuality.LD;
				debugoutput("parameter -l");
				irc++;
			} 
			if (args[irc].toLowerCase().equals("-h")) {
				JFCMainClient.CLIdownloadQuality = JFCMainClient.eCLIdownloadQuality.HD;
				debugoutput("parameter -h");
				irc++;
			}
			
			if (args[irc].toLowerCase().equals("-f")) {
				JFCMainClient.CLIdownloadFormat = JFCMainClient.eCLIdownloadFormat.FLV;
				debugoutput("parameter -f");
				irc++;
			}
			if (args[irc].toLowerCase().equals("-w")) {
				JFCMainClient.CLIdownloadFormat = JFCMainClient.eCLIdownloadFormat.WEBM;
				debugoutput("parameter -w");
				irc++;
			}

		} // for
		
		return irc;
	} //
	
	// TODO long parameters must be tested to
	//
	static void printHelp() {
//		final HelpFormatter formatter = new HelpFormatter();
		System.out.println(szVersion);
		System.out.println("Usage: \n");
		System.out.println("java -jar runytd2.jar <Q> <F> ['youtube-url' ['youtube-url']]\n");
		System.out.println("<Q> = [-l|-h]   for video quality (low, high) - defaults to standard if <Q> is omitted");
		System.out.println("<F> = [-m]      for video format (mpeg) - defaults to mpeg if <F> is omitted");
		System.out.println("<F> = [-w]      for video format (webm) - defaults to mpeg if <F> is omitted");
		System.out.println("<F> = [-f]      for video format (flv)  - defaults to mpeg if <F> is omitted");
		// TODO we implement that later - some testing is still needed
		// formatter.printHelp("java -jar runytd2.jar", sCLIOptions, true);
		
		System.out.println("");
		System.out.println(JFCMainClient.isgerman()?"Nicht vergessen die URL mit ' oder \" einzuschließen, wenn die Adresse & oder <LEERZEICHEN> enthält!":"Don't forget to put ' or \" around an URL if it contains & or <space> !");
		System.out.println("");
		System.out.println(isgerman()?"besuche sf.net/projects/ytd2/forums für irgendwelche Tipps, Vorschläge, Neuerungen, Fragen!":"Visit sf.net/projects/ytd2/forums for tips, questions, updates and comments!");
		System.out.println("");
	} // printHelp
	
	public static void runCLI(boolean setVisibleFlg, String downloadDir) {
		JFCMainClient.dlm = new DefaultListModel<String>();
		
		Boolean bStartThreads=false;
		int istarturls = 0;
		try {
			istarturls = parseargs(JFCMainClient.saargs);
		} catch (ArrayIndexOutOfBoundsException aioob) {
			output(JFCMainClient.isgerman()?"nicht genügend Parameter":"not enough parameters");
			debugoutput(JFCMainClient.isgerman()?"nicht genügend Parameter":"not enough parameters");
			istarturls = JFCMainClient.saargs.length;
		}

		if (istarturls < JFCMainClient.saargs.length) {
			for (int i = istarturls; i < JFCMainClient.saargs.length; i++) {
				if (JFCMainClient.saargs[i].matches(szYTREGEX.concat(".*"))) {
					JFCMainClient.addYTURLToList(JFCMainClient.saargs[i]);
					debugoutput("adding URL: ".concat(JFCMainClient.saargs[i]));
					bStartThreads = true;
				} else {
					debugoutput("wrong URL: ".concat(JFCMainClient.saargs[i]));
					output(String.format((JFCMainClient.isgerman()?"URL: %d sieht nicht aus wie eine YouTube-URL - %s":"URL: %d does not look like a youtube-URL - %s"),i, JFCMainClient.saargs[i]));
				}
			}
		}
		if (bStartThreads)
			JFCMainClient.initializeUI(setVisibleFlg, downloadDir);
		else {
			if (!JFCMainClient.saargs[0].equals("--help")) {
				JFCMainClient.saargs[0]="--help";
				parseargs(JFCMainClient.saargs);
			}
		}
	} //
	
	static void debugoutput (String s) {
		if (!JFCMainClient.getbDEBUG())
			return;

		JFCMainClient.addTextToConsole("#DEBUG ".concat(s));
	} // debugoutput
	
	static void output (String s) {
		try {
			if (JFCMainClient.getbDEBUG())
				return;
		} catch (NullPointerException npe) {}

		JFCMainClient.addTextToConsole("#info - ".concat(s));
	} // output


	public void changedUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	}


	public void insertUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	} 

	public void removeUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	}
	
//	private String getHost(String sURL) {
//		String shost = sURL.replaceFirst(JFCMainClient.szHOSTREGEX, "");
//		shost = sURL.substring(0, sURL.length()-shost.length());
//		shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
//		return(shost);
//	} // gethost
	
	/**
	 * check if a youtube-URL was pasted or typed in
	 * if yes cut it out and send it to the URLList to get processed by one of the threads
	 * 
	 * the user can paste a long string containing many youtube-URLs .. but here is work to do because we have to erase the string(s) that remain(s)
	 */
	void checkInputFieldforYTURLs() {
		String sinput = frame.textinputfield.getText(); // don't call .toLowerCase() !

//		sinput = sinput.replaceAll("&feature=fvwp&", "&");
//		sinput = sinput.replaceAll("&feature=fvwphttp", "http");
//		sinput = sinput.replaceAll("&feature=fvwp", "");
//		sinput = sinput.replaceAll("&feature=related&", "&");
//		sinput = sinput.replaceAll("&feature=relatedhttp", "http");
//		sinput = sinput.replaceAll("&feature=related", "");
//		sinput = sinput.replaceAll("&feature=mfu_in_order&list=[0-9A-Z]{1,2}", "");
//		sinput = sinput.replaceAll("&feature=[0-9a-zA-Z]{1,3}&list=([a-zA-Z0-9]*)(&index=[0-9]{1,2})?", "");
//		sinput = sinput.replaceAll("&feature=[0-9a-zA-Z]{1,3}&list=(PL[a-zA-Z0-9]{16})(&index=[0-9]{1,2})?", "");
//		sinput = sinput.replaceAll("&playnext=[0-9a-zA-Z]{1,3}&list=(PL[a-zA-Z0-9]{16})", "");
//		sinput = sinput.replaceAll("&NR=[0-9]&", "&");
//		sinput = sinput.replaceAll("&NR=[0-9]http", "http");
//		sinput = sinput.replaceAll("&NR=[0-9]", "");
		//http://www.youtube.com/watch?feature=endscreen&NR=1&v=S6ZRpmciM-s
		sinput = sinput.replaceAll("/watch?.*&v=", "/watch?v=");
		sinput = sinput.replaceAll(" ", "");
		sinput = sinput.replaceAll(szPLAYLISTREGEX, "/watch?v=");

		String surl = sinput.replaceFirst(szYTREGEX, "");
		
		// if nothing could be replaced we have to yt-URL found
		if (sinput.equals(surl)) return;

		debugoutput("sinput: ".concat(sinput).concat(" surl: ".concat(surl)));
		
		// starting at index 0 because szYTREGEX should start with ^ // if szYTREGEX does not start with ^ then you have to find the index where the match is before you can cut out the URL 
		surl = sinput.substring(0, sinput.length()-surl.length());
		addYTURLToList(surl);
		sinput = sinput.substring(surl.length());
		debugoutput(String.format("sinput: %s surl: %s",sinput,surl));
		
		// if remaining text is shorter than shortest possible yt-url we delete it
		if (sinput.length()<"youtube.com/watch?v=0123456789a".length()) sinput = "";
		
		//frame.textinputfield.setText(sinput); // generates a java.lang.IllegalStateException: Attempt to mutate in notification
		
		final String fs = sinput;

		// let a thread update the textfield in the UI
		Thread worker = new Thread() {
            public void run() {
            	synchronized (JFCMainClient.frame.textinputfield) {
            		JFCMainClient.frame.textinputfield.setText(fs);
				}
            }
        };
        SwingUtilities.invokeLater (worker);
	} // checkInputFieldforYTURLS
	
	ImageIcon createImageIcon(String path, String description) {
	    java.net.URL imgURL = getClass().getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	} // createImageIcon

	public void stateChanged(ChangeEvent e) {
	}


	public void dragEnter(DropTargetDragEvent dtde) {
	}


	public void dragOver(DropTargetDragEvent dtde) {
	}


	public void dropActionChanged(DropTargetDragEvent dtde) {
	}


	public void dragExit(DropTargetEvent dte) {
	}


	/**
	 * processing event of dropping a HTTP URL, YT-Video Image or plain text (URL) onto the frame
	 * 
	 * seems not to work with M$-IE (8,9) - what a pity!
	 */
	public void drop(DropTargetDropEvent dtde) {
			Transferable tr = dtde.getTransferable();
			DataFlavor[] flavors = tr.getTransferDataFlavors();
			DataFlavor fl = null;
			String str = "";
			
			debugoutput("DataFlavors found: ".concat(Integer.toString( flavors.length )));
		for (int i = 0; i < flavors.length; i++) {
			fl = flavors[i];
			if (fl.isFlavorTextType() /* || fl.isMimeTypeEqual("text/html") || fl.isMimeTypeEqual("application/x-java-url") || fl.isMimeTypeEqual("text/uri-list")*/) {
				try {
					dtde.acceptDrop(dtde.getDropAction());
				} catch (Throwable t) {
				}
				try {
					if (tr.getTransferData(fl) instanceof InputStreamReader) {
						debugoutput("Text-InputStream");
						BufferedReader textreader = new BufferedReader(
								(Reader) tr.getTransferData(fl));
						String sline = "";
						try {
							while (sline != null) {
								sline = textreader.readLine();
								if (sline != null)
									str += sline;
							}
						} catch (Exception e) {
						} finally {
							textreader.close();
						}
						str = str.replaceAll("<[^>]*>", ""); // remove HTML tags, especially a hrefs - ignore HTML characters like &szlig; (which are no tags)
					} else if (tr.getTransferData(fl) instanceof InputStream) {
						debugoutput("Byte-InputStream");
						InputStream input = new BufferedInputStream(
								(InputStream) tr.getTransferData(fl));
						int idata = input.read();
						String sresult = "";
						while (idata != -1) {
							if (idata != 0)
								sresult += new Character((char) idata)
										.toString();
							idata = input.read();
						} // while
						debugoutput("sresult: ".concat(sresult));
					} else {
						str = tr.getTransferData(fl).toString();
					}
				} catch (IOException ioe) {
				} catch (UnsupportedFlavorException ufe) {
				}

				debugoutput("drop event text: ".concat(str).concat(" (").concat(fl.getMimeType()).concat(") "));
				// insert text into textfield - almost the same as user drops text/url into this field
				// except special characaters -> from http://de.wikipedia.org/wiki/GNU-Projekt („GNU is not Unix“)(&bdquo;GNU is not Unix&ldquo;)
				// two drops from same source .. one time in textfield and elsewhere - maybe we change that later?!
				if (str.matches(szYTREGEX.concat("(.*)"))) {
					synchronized (JFCMainClient.frame.textinputfield) {
						JFCMainClient.frame.textinputfield.setText(str.concat(JFCMainClient.frame.textinputfield.getText()));
					}
					debugoutput("breaking for-loop with str: ".concat(str));
					break;
				}
			} else {
				String sv = "drop event unknown type: ".concat(fl.getHumanPresentableName());
				//output(sv);
				debugoutput(sv);
			}
		} // for

		dtde.dropComplete(true);
	} // drop()

	public void itemStateChanged(ItemEvent e) {
		if ( e.getSource()==this.saveconfigcheckbox )
			if ( e.getStateChange()==java.awt.event.ItemEvent.SELECTED)
				debugoutput("saving config on exit.");
			else
				debugoutput("don't saving config on exit.");
		
		if ( e.getSource()==this.save3dcheckbox )
			if ( e.getStateChange()==java.awt.event.ItemEvent.SELECTED) {
				debugoutput("trying: 3D");
				output("trying: 3D");
			} else {
				debugoutput("trying: normal 2D");
				output("trying: normal 2D");
			}

	} //itemStateChanged()
	
} // class JFCMainClient